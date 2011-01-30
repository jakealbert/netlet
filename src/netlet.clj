(ns netlet
  (:gen-class :extends javax.servlet.http.HttpServlet :main false)
  (:require [org.danlarkin [json :as json]]
	    [clojure.contrib.str-utils2 :as s2])
  (:import (java.io ByteArrayOutputStream
		    ByteArrayInputStream))
  (:use [netlet.response :only [success not-found]]
	[netlet.templates :only [layout]]
	[netlet.lastfm :only [md5-sum]]
	[netlet.content :only [site-title sections netlet-switch-properties netlet-model-properties]]
	[netlet.util :only [section *users-map* flatten get-now]]
	[netlet.xmpp :only [*xmpp-connection* send-message-to get-xmpp-connection]]
	[hiccup :only [html]]
	[incanter core stats charts]
	local-dev
	compojure.core
	ring.middleware.session
	[ring.util.response :only [redirect]]
	[ring.util.servlet :only [defservice]]))

(defn index
  [session params]
  "Returns a SUCCESS of the page/subpage and the 
   corresponding section layout, returns 404 if page/subpage do
   not correspond to anything."
  (let
      [page (params "page")
       section (sections page)
       subpage (params "subpage")
       subsections     (:subsections section)
       subsection  (if (nil? subsections)
		     nil
		     (subsections subpage))
       is-bummer  (or (nil? section)
		      (and (nil? subsection)
			   (not (nil? subpage))))
       fixed-section (if is-bummer
		       (sections "bummer")
		       section)
       status  (if is-bummer
		 success
		 not-found)
       section-title  (:title fixed-section)
       subsection-title (:title subsection)
       title          (if (or (nil? section-title)
			      (= "" section-title))
			site-title
			(str section-title " - " site-title))
       title          (if (nil? subsection-title)
			title
			(str subsection-title " - " title))
       section-layout ((:body fixed-section) session params)]
    (status
     (layout
      session
      params
      title
      section-layout)
     session)))
		       

(defn create-netlet
  [session params]
  (if (and (session :username)
	   (not (= (params "name") "")))
    (let [userdata (@*users-map* (session :username))
	  usernetlets (:netlets userdata)
	  netletname (params "name")
	  netletmodel (params "model")
	  netletdne  (empty? (filter (fn [x] (= (:name x) netletname)) usernetlets))
	  newnetlet   {:name netletname 
		       :model netletmodel 
		       :xmpp "netlet@jabber.org/device"
		       :data (sorted-set-by (fn [a b] (< (:timestamp a)
							 (:timestamp b))))}
	  newuserdata (if usernetlets
			(assoc userdata :netlets (cons newnetlet
						       usernetlets))
			(assoc userdata :netlets (cons newnetlet #{})))]
      (if netletdne
	(dosync
	 (alter *users-map* assoc (session :username) newuserdata)
	 (redirect "/overview"))
	(redirect "/add-netlet")))
    (redirect "/login")))

(defn create-trigger
  [session params]
  (if (session :username)
    (let [userdata (@*users-map* (session :username))
	  usernetlets (:netlets userdata)
	  netlethash (params "namehash")
	  outlethash (s2/butlast (params "outlet") 32)
	  triggerhash (s2/drop (params "outlet") 32)
	  usernetlet  (first (filter (fn [x] (= (md5-sum (:name x)) netlethash))
				     usernetlets))
	  triggernetlet (first (filter (fn [x] (= (md5-sum (:name x)) outlethash))
				       usernetlets))
	  usernetletseq (seq (:config usernetlet))
	  triggernetletseq (seq (:config triggernetlet))
	  outletnum (first (first (filter (fn [x] (= (md5-sum (:outlet-name (second x)))
						     (params "outlethash")))
					  usernetletseq)))
	  triggernum (first (first (filter (fn [x] (= (md5-sum (:outlet-name (second x)))
						      triggerhash))
					   triggernetletseq)))
	  
	  usertriggers (:triggers userdata)
	  edge         (if (= (params "triggeronoff") "on")
			 :rising
			 :falling)
	  newval       (if (= (params "onoff") "on")
			 1
			 0)
	  
	  newuserdata  (if usertriggers
			 (assoc userdata :triggers (cons {:netlet netlethash :outlet outletnum :trigger-netlet outlethash :trigger-outlet triggernum :edge edge :newval newval }
							 usertriggers))
			 (assoc userdata :triggers (cons {:netlet netlethash :outlet outletnum :trigger-netlet outlethash :trigger-outlet triggernum :edge edge :newval newval}
							 #{})))]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect (str "/triggers/" (params "namehash") "/" (params "outlethash")))))
    (redirect "/login")))
	   
(defn delete-netlet
  [session params]
  (if (and (session :username)
	   (not (= (params "namehash") "")))
    (let [userdata (@*users-map* (session :username))
	  usernetlets (:netlets userdata)
	  usertriggers (:triggers userdata)
	  filteredtriggers (filter (fn [x] (not (or (= (:trigger-netlet x) (params "namehash"))
						    (= (:netlet x) (params "namehash")))))
				   usertriggers)
	  filterednetlets (filter (fn [x] (not (= (md5-sum (:name x))
						  (params "namehash"))))
				  usernetlets)
	  newuserdata (assoc userdata :triggers filteredtriggers)
	  newuserdata (assoc newuserdata :netlets filterednetlets)]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect "/overview")))
    (redirect "/login")))

(defn delete-trigger
  [session params]
  (if (and (session :username)
	   (not (= (params "triggerhash") "")))
    (let [userdata (@*users-map* (session :username))
	  usertriggers (:triggers userdata)
	  usertrigger (first (filter (fn [trigger] (= (md5-sum (str (:netlet trigger)
								    (:outlet trigger)
								    (:trigger-netlet trigger)
								    (:trigger-outlet trigger)
								    (:edge trigger)))
						      (params "triggerhash")))
				     usertriggers))
	  netlethash (:netlet usertrigger)
	  usernetlets (:netlets userdata)
	  filterednetlet (first (filter (fn [x] (= (md5-sum (:name x))
						    netlethash))
					 usernetlets))
	  useroutlet ((:config filterednetlet) (:outlet usertrigger))
	  outlethash (md5-sum (:outlet-name useroutlet))
	  filteredtriggers (filter (fn [trigger] (not (= (md5-sum (str (:netlet trigger)
								 (:outlet trigger)
								 (:trigger-netlet trigger)
								 (:trigger-outlet trigger)
								 (:edge trigger)))
						   (params "triggerhash"))))
				   usertriggers)
	  newuserdata (assoc userdata :triggers filteredtriggers)]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect (str "/triggers/" netlethash "/" outlethash))))
    (redirect "/login")))

(defn set-outlet
  [session params]
  (if (and (session :username)
	   (params "netlet")
	   (not (= "" (params "netlet")))
	   (params "outlet")
	   (not (= "" (params "outlet")))
	   (params "newvalue")
	   (not (= "" (params "newvalue"))))
    (let [userdata (@*users-map* (session :username))
	  usernetlets (:netlets userdata)
	  netlet (first (filter (fn [x] (= (md5-sum (:name x)) (params "netlet"))) usernetlets))
	  config (:config netlet)
	  outletnum (Integer/parseInt (params "outlet"))
	  outlet (config outletnum)
	  outlet-type (:switch-type outlet)
	  switch-props (netlet-switch-properties outlet-type)
	  newval  (Integer/parseInt (params "newvalue"))
	  newvalue (if (and (<= (:min-value switch-props) newval)
			    (<= newval (:max-value switch-props)))
		     newval
		     (:value outlet))
	  newoutlet (assoc (dissoc outlet :value) :value newval)
	  newconf (assoc config outletnum newoutlet)
	  newnetlet (assoc netlet :config newconf)
	  newnetlets (cons newnetlet (filter (fn [x] (not (= (md5-sum (:name x)) (params "netlet")))) usernetlets))
	  newuserdata (assoc userdata :netlets newnetlets)
	  message  (str "outlet#: " outletnum "\noutlet-type: " outlet-type "\nvalue: " newvalue)
	  messagemap {:username (session :username)
		      :netlet-name (:name netlet)
		      :outlet-num outletnum
		      :outlet-type outlet-type
		      :value newvalue
		      :to (:xmpp netlet)}]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (try
	(send-message-to (deref *xmpp-connection*) messagemap)
	(catch Exception e nil))
       (redirect "/overview")))
    (redirect "/login")))
	  
(defn configure-netlet
  [session params]
  (if (and (session :username)
	   (not (= (params "name") ""))
	   (not (= (params "namehash") "")))
    (let [userdata (@*users-map* (session :username))
	  usernetlets (:netlets userdata)
	  netlet (first (filter (fn [x] (= (md5-sum (:name x)) (params "namehash"))) usernetlets))
	  outlets (sort (flatten (map (fn [x] (if (set? (:outlet x)) (seq (:outlet x)) (:outlet x)))
				      (netlet-model-properties (:model netlet)))))
	  outlets (filter (fn [x] (and (params (str x))
				       (not (= "" (params (str x)))))) outlets)
	  outlet-type  (fn [n]
			 (let
			     [model-props (netlet-model-properties (:model netlet))]
			   (:switch-type
			    (first (filter (fn [x] (or (and (set? (:outlet x))
							    ((:outlet x) n))
						       (and (integer? (:outlet x))
							    (= (:outlet x) n))))
					   (seq model-props))))))
	  confmap (zipmap outlets (map (fn [x] {:outlet-name (params (str x))
						:value (if (and (:config netlet)
								((:config netlet) x)
								(:value ((:config netlet) x)))
							 (:value ((:config netlet) x))
							 0)
						:switch-type (outlet-type x)
						:data (if (and (:config netlet)
							       ((:config netlet) x)
							       (:data ((:config netlet) x)))
							(:data ((:config netlet) x))
							(sorted-set-by (fn [a b] (< (:timestamp a) (:timestamp b))) ))}) outlets))
	  newnetlet (if (empty? confmap)
		      (dissoc netlet :config)
		      (assoc netlet :config confmap))
	  newnetlet (assoc newnetlet :name (params "name"))
	  newnetlet (assoc newnetlet :xmpp (params "xmppid"))
	  newnetlet (assoc newnetlet :data (:data netlet))
	  newusernetlets (cons newnetlet (filter (fn [x] (not (= (md5-sum (:name x)) (params "namehash")))) usernetlets))
	  newuserdata (assoc userdata :netlets newusernetlets)]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect "/overview"))
    (redirect "/login"))))
	  

(defn authorize
  [session params]
  (let [user (params "name")]
    (if (or
	 (session :username)
	 (and user
	      (@*users-map* user)
	      (= (:passhash (@*users-map* user)) (md5-sum (params "password")))))
      (if (session :username)
	(index session params)
	(index (assoc session 
		 :username    (params "name")
		 :auth-level  (if (= (params "name") "jake.albert@gmail.com")
				:admin
				:user))
		 
	       (assoc params "page" "overview")))
      (redirect "/login"))))


(defn register
  [session params]
  (let [user (params "name")
	password (params "password")]
    (if (@*users-map* user)
      (if (= (:passhash (@*users-map* user)) (md5-sum password))
	(authorize session params)
	(redirect "/register"))
      (if (and user password
	       (not= user "")
	       (not= password ""))
	(dosync
	 (alter *users-map* assoc user {:passhash (md5-sum password)})
	 (authorize session params))
	(redirect "/register")))))
    


(defroutes admin-routes
  (GET "/admin/*" {session :session params :params}
       (index session params)))

(defn get-netlet-data
  ([username datatype n]
     nil)
  ([username datatype startdt enddt]
     nil)
  ([username datatype n netlet outlet]
     nil)
  ([username datatype startdt enddt netlet outlets]
     (let [usermap     ((var *users-map*) username)
	   usernetlets (usermap :netlets)
	   netlet      (cond
			(string? netlet)
			(first (filter (fn [nl] 
					 (= (md5-sum (:name nl))
					    netlet))
				       usernetlets))
			(nil? netlet)
			(first usernetlets))
	   nl-name     (:name netlet)
	   nl-conf     (:config netlet)
	   nl-keys     (cond 
			(or (nil? outlets)
			    (and
			     (seq? outlets)
			     (empty? outlets)))  (keys nl-conf)
			(seq? outlets) outlets
			(integer? outlets) (list outlets))
	   netlets      (vals (select-keys nl-conf nl-keys))
	   nl-datas     (map
			 (fn [nl]
			   {:name (:outlet-name nl) 
			    :data
			    (let [nl-data (:data nl)
				  nl-data (filter (fn [datum]
						    (and
						     (> (:timestamp datum) startdt)
						     (< (:timestamp datum) enddt)))
						  (seq nl-data))
				  nl-data (map (fn [datum]
						 [(:timestamp datum) (datatype datum)])
					       nl-data)]
			      nl-data)
			    :marker {:enabled false}})
			 netlets)]
       nl-datas)))

(defn json-response
  [request params datas]
    {:status 200
     :headers {"Content-Type" "application/json"
	       "Cache-Control" "no-cache, must-revalidate"
	       "Expires" "Mon, 26 Jul 1997 05:00:00 GMT"}
     :body (json/encode-to-str datas)})


(defn add-datasets-to-chart [datasets chart]
  (if (empty? datasets)
    chart
    (let [data (first datasets)
	  title (:name data)
	  datas (:data data)
	  times (map first datas)
	  vals  (map second datas)]
      (add-datasets-to-chart (rest datasets) (add-lines* chart times vals :series-label (str title))))))
	  

(defn png-response
  [request params datas]
  (let
      [firstdata (first datas)
       restdata  (rest datas)
       title (:name firstdata)
       datas (:data firstdata)
       times (map first datas)
       vals  (map second datas)
       chart (clear-background (xy-plot times vals
					:legend true
					:series-label title
					:title ""
					:y-label (let [charttype (keyword (params "charttype"))]
						   (cond (= charttype :current) "Current (A)"
							 (= charttype :power) "Power (kW)" :else "Measurement"))
					:x-label "Time"))
       chart (add-datasets-to-chart restdata chart)
       out-stream (ByteArrayOutputStream.)
       in-stream (do
		   (if (not= (params "size") "ss")
		     (save chart out-stream :width 674 :height 300)
		     (save chart out-stream :width 394 :height 200))
		   (ByteArrayInputStream.
		    (.toByteArray out-stream)))
       header {:status 200
	       :headers {"Content-Type" "image/png"}
	       :body in-stream}]
    header))


(defn chart-response
  [request session params]
  (if (:username session)
    (let [username (:username session)
	  datatype (if (params "charttype")
		     (keyword (params "charttype"))
		     :current)
	  startdt  (if (params "startdt")
		     (Integer/parseInt (params "startdt"))
		     0)
	  enddt    (if (params "enddt")
		     (Integer/parseInt (params "enddt"))
		     (get-now))
	  netlet   (if (params "netlet")
		     (params "netlet")
		     nil)
	  outlets  (if (params "outlets")
		     (filter integer?
			     (map #(try (Integer/parseInt %)
					(catch Exception e nil))
				  (seq (.split (params "outlets") ","))))
		     1)
	  chart-data (get-netlet-data username datatype startdt enddt netlet outlets)]
      (cond
       (= (params "extension") "png") (png-response request params chart-data)
       (= (params "extension") "json") (json-response request params chart-data)))
    (not-found "" session)))

(defroutes public-routes
  (GET "" {session :session params :params}
       (index session (assoc params "page" "overview")))
  (GET "/" {session :session params :params}
       (index session (assoc params "page" "overview")))
  (POST "/set-outlet" {session :session params :params}
	(set-outlet session params))
  (POST "/configure-netlet" {session :session params :params}
	(configure-netlet session params))
  (POST "/add-trigger" {session :session params :params}
	(create-trigger session params))
  (GET "/triggers/:namehash/:outlethash" {session :session params :params}
       (authorize session (assoc params "page" "triggers")))
  (GET "/add-trigger/:namehash/:outlethash" {session :session params :params}
       (authorize session (assoc params "page" "add-trigger")))
  (GET "/configure-netlet/:namehash" {session :session params :params}
       (authorize session (assoc params "page" "configure-netlet")))
  (GET "/delete-netlet/:namehash" {session :session params :params}
       (delete-netlet session params))
  (GET "/delete-trigger/:triggerhash" {session :session params :params}
       (delete-trigger session params))
  (POST "/add-netlet" {session :session params :params}
	(create-netlet session params))
  (GET "/users/:userpage/:subpage" {session :session params :params}
       (index session (assoc params "page" "users")))
  (GET "/users/:userpage" {session :session params :params}
       (index session (assoc params "page" "users")))
  (GET "/:page" {session :session params :params}
       (authorize session params))
  (GET "/charts/:charttype.:extension" {session :session params :params request :request}
       (chart-response request session params))
  (GET "/:page/:subpage" {session :session params :params}
       (authorize session params)))

(defroutes auth-routes
  (POST "/register" {session :session params :params}
	(register session params))
  (GET "/logout" {session :session params :params}
       (index (dissoc (dissoc session :username) :auth-level) (assoc params "page" "logout")))
  (POST "/login" {session :session params :params}
       (authorize session params))
  (GET "/register" {session :session params :params}
       (index session (assoc params "page" "register")))
  (GET "/login" {session :session params :params}
       (if (or (:username session)
	       (:auth-level session))
	 (redirect "/overview")
	 (index session (assoc params "page" "register")))))


(defroutes app
  auth-routes
  public-routes
  admin-routes
  (GET "/*" {session :session params :params}
       (index session (assoc params "page" "bummer"))))

(wrap! app
       :session)

(defservice app)

(defn -main [& args]
  (let [socketclass (str (try (Class/forName "javax.net.SocketFactory") (catch Exception e "no sockets")))]
    (cond 
     (= socketclass "class javax.net.SocketFactory") (start-server (var app))
     :else nil)))