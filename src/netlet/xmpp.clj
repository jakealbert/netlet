(ns netlet.xmpp
  (:use [netlet.util :only [*users-map*]]
	[hiccup :only [html h]])
  (:require [clojure.xml :as xml])
  (:require [clojure.zip :as zip])
  (:require [clojure.contrib.zip-filter.xml :as zf])
  (:import (java.io ByteArrayInputStream IOException))
  (:import [org.jivesoftware.smack
	    Chat
	    ChatManager
	    ConnectionConfiguration
	    MessageListener
	    SASLAuthentication
	    XMPPConnection
	    XMPPException
	    PacketListener]
	   [org.jivesoftware.smack.packet Packet Message Presence Presence$Type Message$Type]
	   [org.jivesoftware.smack.filter MessageTypeFilter NotFilter PacketTypeFilter]
	   [org.jivesoftware.smack.util StringUtils])
  
  (:import
   [com.google.appengine.api.xmpp XMPPService XMPPServiceFactory MessageBuilder JID]))

(defonce *available-presence* (Presence. Presence$Type/available))
(defonce *not-presence* (NotFilter. (PacketTypeFilter. (Class/forName "org.jivesoftware.smack.packet.Presence"))))
(defonce *chat-message-type-filter* (MessageTypeFilter. Message$Type/chat))


(defn packet-listener [conn processor]
  (proxy
      [PacketListener]
      []
    (processPacket [packet] (processor conn packet))))

(defn mapify-error [e]
  (if (nil? e)
    nil
    {:code (.getCode e) :message (.getMessage e)}))

(defn mapify-message [#^Message m]
  (try
   {:body (.getBody m)
    :subject (.getSubject m)
    :thread (.getThread m)
    :from (.getFrom m)
    :from-name (StringUtils/parseBareAddress (.getFrom m))
    :to (.getTo m)
    :packet-id (.getPacketID m)
    :error (mapify-error (.getError m))
    :type (keyword (str (.getType m)))}
   (catch Exception e (println e) {})))

(defn parse-address [from]
  (try
   (first (.split from "/"))
   (catch Exception e (println e) from)))

(defn create-reply [from-message-map to-message-body]
  (try
   (let [to (:from from-message-map)
	 rep (Message.)]
     (.setTo rep to)
     (.setBody rep (str to-message-body))
     rep)
   (catch Exception e (println e))))

(defn create-message-packet [mm]
  (try
   (let [pack (Message.)]
     (.setTo pack (:to mm))
     (.setBody pack (str mm))
     (.setProperty pack "username" (:username mm))
     (.setProperty pack "netlet-name" (:netlet-name mm))
     (.setProperty pack "outlet-type" (str (:outlet-type mm)))
     (.setProperty pack "outlet-num" (:outlet-num mm))
     (.setProperty pack "value" (:value mm))
     pack)
   (catch Exception e (println e))))
     

(defn reply [from-message-map to-message-body conn]
  (.sendPacket conn (create-reply from-message-map to-message-body)))

(defn with-message-map [handler]
  (fn [conn packet]
    (try
     (handler conn packet)
     (catch Exception e (println e)))))
 

(defn handle-message-map [mm]
  (if (and (:username mm)
	   (@*users-map* (:username mm)))
    (let [userdata (@*users-map* (:username mm))
	  usernetlets (:netlets userdata)
	  usernetlet (first (filter (fn [x] (= (:xmpp x) (:from mm))) usernetlets))
	  useroutlets (:config usernetlet)
	  msgoutlets (:outlets mm)
	  configured-msgoutlets (if useroutlets
				  (filter (fn [x] (useroutlets (:outlet-num x))) msgoutlets)
				  nil)
	  newoutlets (if useroutlets
		       (zipmap
			(map (fn [msgoutlet] (:outlet-num msgoutlet)) configured-msgoutlets)
			(map (fn [msgoutlet]
			       (let [useroutlet        (useroutlets (:outlet-num msgoutlet))
				     newoutletdata     (apply conj
							      (:data useroutlet)
							      (:data msgoutlet))
				     newoutlet         (assoc useroutlet 
							 :data newoutletdata 
							 :value (:value msgoutlet))]
				 newoutlet)) configured-msgoutlets))
		       nil)
	  newnetlet (if useroutlets
		      (assoc usernetlet :config newoutlets)
		      usernetlet)
	  newnetlets (cons newnetlet (filter (fn [x] (not= (:name x) (:name usernetlet))) usernetlets))
	  newuserdata (assoc userdata :netlets newnetlets)]
      (dosync
       (println mm)
       (alter *users-map* assoc (:username mm) newuserdata)))
    (println mm)))

(defn mapify-packet [p]
     (let [xstr (.toXML p)
	  x (xml/parse (ByteArrayInputStream. (.getBytes xstr)))]
       (if (= (:tag x) :message)
	 (let [attrs (:attrs x)
	       id    (:id attrs)
	       to    (:to attrs)
	       from  (:from attrs)
	       msgtype  (:type attrs)
	       content (:content x)]
	   (let [biggie (first content)
		 biggiecon (if (= (:tag biggie) :biggie)
			     (:content biggie)
			     content)
		 tupac (first biggiecon)
		 tupaccon (if (= (:tag tupac) :tupac)
			    (:content tupac)
			    content)
		 netlet (first tupaccon)
		 netletcon (if (= (:tag netlet) :netlet)
			     (:content netlet)
			     nil)
		 netletattr (if (= (:tag netlet) :netlet)
			      (:attrs netlet)
			      nil)
		 netletuser (if netletattr
			      (:username netletattr)
			      nil)
		 outlets (if (not= netletcon nil)
			   (for [outlet netletcon :when (= (:tag outlet) :outlet)]
			     (let
				 [outletattrs (:attrs outlet)
				  value (:value outletattrs)
				  type (:switch-type outletattrs)
				  outlet-num (:outlet-num outletattrs)
				  outletcon (:content outlet)
				  outletdata (for [sensortag outletcon
						 :when (= (:tag sensortag)
							  :sensor-data)]
					       (let [sensorattrs (:attrs sensortag)]
						 (assoc sensorattrs 
						   :timestamp (Integer/parseInt (:timestamp sensorattrs))
						   :current (Double/parseDouble (:current sensorattrs))
						   :power (Double/parseDouble (:power sensorattrs)))))]
			       {:outlet-num (Integer/parseInt outlet-num) :switch-type (keyword type) :value (Integer/parseInt value) :data outletdata}))
			   empty)]
	     (if (and netletcon
		      netletuser)
	       {:username netletuser :to to :from from :outlets outlets}
	       nil)))
	 nil)))

(defn handle-packet [p]
  (let [pm (mapify-packet p)]
    (if pm
      (handle-message-map pm)
      (println (.toXML p)))))

(defn wrap-responder [handler]
  (fn [conn message]
    (handle-packet message)))

(defn start-bot
  [connect-info packet-processor]
  (let [un (:username connect-info)
	pw (:password connect-info)
	host (:host connect-info)
	domain (:domain connect-info)
	connect-config (ConnectionConfiguration. host 5222 domain)
	conn (XMPPConnection. connect-config)]
    (.connect conn)
    (.login conn un pw)
    (.sendPacket conn *available-presence*)
    (.addPacketListener conn (packet-listener conn (with-message-map (wrap-responder packet-processor)))
			*chat-message-type-filter*)   
    conn))

(defn stop-bot [#^XMPPConnection conn]
  (.disconnect conn))
  
(def connect-info
     {:username "netlet-webapp@jabber.org"
      :password "engn1650"
      :host "jabber.org"
      :domain "jabber.org"})

(defn handle-message [message]
  (let [body (:body message)
	from-user (:from message)]
    (do
      (println (str from-user ":" body))
      (str "Hi " from-user ", you sent me '" body "'"))))

(defn reload-helper [message]
  (try
   (handle-message message)
   (catch Exception e (println e))))

;;(defonce my-bot (start-bot connect-info reload-helper))

;;(defn reload []
;;  (stop-bot my-bot))

(defn is-app-engine?
  []
  (let [socketclass (str (try (Class/forName "javax.net.SocketFactory") (catch Exception e "no sockets")))]
     (= socketclass "class com.google.apphosting.runtime.security.shared.stub.javax.net.SocketFactory")))

(defn is-localhost?
  []
  (let [socketclass (str (try (Class/forName "javax.net.SocketFactory") (catch Exception e "no sockets")))]
     (= socketclass "class javax.net.SocketFactory")))


(defn smack-get-connection
  []
  (let [conn (XMPPConnection. (ConnectionConfiguration. "jabber.org" 5222 "jabber.org"))]    (.connect conn)

    (.login conn "netlet-webapp@jabber.org" "engn1650")
    (.sendPacket conn *available-presence*)
    (.addPacketListener conn (packet-listener conn (with-message-map (wrap-responder reload-helper)))
			  *not-presence*)
    conn))

(defn gae-get-connection
  []
  (XMPPServiceFactory/getXMPPService))

(defn get-xmpp-connection 
  []  
  (cond
   (is-app-engine?) (gae-get-connection)
   (is-localhost?) (smack-get-connection)
   :else nil))
  

(def *xmpp-connection* (ref (get-xmpp-connection)))

(defn send-message-to
  [conn message]
  (cond
   (is-app-engine?)	(.sendMessage conn
				      (let [mb (MessageBuilder.)
					    jd (JID. (:to message))]
					(.withRecipientJids mb (into-array [jd]))
					(.withFromJid mb (JID. "mynetlet@appspot.com"))
					(.asXml true)
					(.withBody mb 
						   (html
						    [:property {:name "username" :value (:username message)}]
						    [:property {:name "netlet-name" :value (:netlet-name message)}]
						    [:property {:name "outlet-type" :value (str (:outlet-type message))}]
						    [:property {:name "outlet-num" :value (:outlet-num message)}]
						    [:property {:name "value" :value (:value message)}]))
					(.build mb)))
   (is-localhost?) (.sendPacket conn (create-message-packet  message))))

