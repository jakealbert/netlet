(ns netlet.lastfm
  (:use compojure.core)
  (:require [clojure.contrib.str-utils2 :as s2])
  (:use [clojure-http.client])
  (:import (java.io Reader InputStream InputStreamReader ByteArrayInputStream IOException))
  (:import (java.security NoSuchAlgorithmException MessageDigest)
	   (java.math BigInteger))
  (:require [clojure.xml :as xml])
  (:require [clojure.zip :as zip])
  (:require [clojure.contrib.zip-filter.xml :as zf])
  (:require [clojure-http.resourcefully :as res]))

(def apikey  "95bbfb880f3495a0f13f4d29b524d4ef")
(def secret  "202ed3f4a070b39a4595db7e6e9758fb")

(def rooturl "http://ws.audioscrobbler.com/2.0/")
(def default-param-map (str "http://ws.audioscrobbler.com/2.0/?api_key=" apikey))
(def lastfm-auth-url (str "http://www.last.fm/api/auth/?api_key=" apikey))


(defn lastfm-getmethod
  [param-map]
  (let [param-map-url (apply str
			     (cons default-param-map
				   (map (fn [a b]
					  (str "&" a "=" b))
					(keys param-map)
					(vals param-map))))
	response (try (res/get param-map-url)
		      (catch IOException e
			nil))]
    (if (nil? response)
      nil
      (let
	  [xmlstr (apply str (:body-seq response))
	   input-stream (ByteArrayInputStream. (.getBytes xmlstr))
	   parsedxml (xml/parse input-stream)]
	parsedxml))))
	


(defn track-tag-to-map
  [track-tag]
  (let
      [track (:content track-tag)
       now-playing (if (:attrs track-tag)
		     (= "true" (:nowplaying (:attrs track-tag))))
       get-content-by-tag-name (fn [tag-name]
				 (first (for [y track
					  :when (= (:tag y) tag-name)]
					  (first (:content y)))))
       tag-names (list :artist :name :date :url :streamable)
       track-map (zipmap tag-names (map get-content-by-tag-name tag-names))
       track-map (assoc track-map :now-playing now-playing)
       album-mbid  (first (for [y track
				:when (= (:tag y) :album)]
			   (:mbid (:attrs y))))
       track-map (assoc track-map :album-mbid album-mbid)
       track-url (:url track-map)
       track-url (s2/split track-url #"/_/")
       name-url (second track-url)
       artist-url (second (s2/split (first track-url) #"/music/"))
       track-map (assoc track-map :url-name name-url :url-artist artist-url)]
    track-map))
		     

(defn user_get-weekly-track-chart
  [username]
   (lastfm-getmethod
    {"method" "user.getweeklytrackchart"
     "user" username}))

(defn user_get-weekly-top-10
  [username]
  (for [x (xml-seq (user_get-weekly-track-chart username))
	:when (and (:rank (:attrs x))
		   (< (Integer/parseInt (:rank (:attrs x))) 11))]
    (track-tag-to-map x)))

(defn user_get-recent-tracks
  ([username limit page]
     (lastfm-getmethod
      {"limit" limit
       "method" "user.getrecenttracks"
       "page" page
       "user" username}))
  ([username limit]
     (lastfm-getmethod
      {"limit" limit
       "method" "user.getrecenttracks"
       "user" username}))
  ([username]
     (lastfm-getmethod
      {"method" "user.getrecenttracks"
       "user" username})))


(defn get-recent-tracks
  ([username] (get-recent-tracks username 5))
  ([username num]
     (for [track-tag (:content
		      (first 
		       (:content 
			(first 
			 (xml-seq (user_get-recent-tracks username num))))))]
       (track-tag-to-map track-tag))))
       	 

(defn album_get-info-by-mbid
  ([mbid]
     (lastfm-getmethod
      {"method" "album.getinfo"
       "mbid" mbid}))
  ([mbid username]
     (lastfm-getmethod
      {"method" "album.getinfo"
       "mbid" mbid
       "username" username})))

(defn album_get-info-by-artist
  ([artist]
     (lastfm-getmethod
      {"method" "album.getinfo"
       "artist" artist}))
  ([artist username]
     (lastfm-getmethod
      {"method" "album.getinfo"
       "artist" artist
       "username" username})))

(defn get-album-image-url
  ([album-info] (get-album-image-url album-info "medium"))
  ([album-info size] (first
		(for [tag (:content
			   (first
			    (:content
			     (first
			      (xml-seq album-info)))))
		      :when (and (= (:tag tag) :image)
				 (= (:size (:attrs tag)) size))]
		  (first (:content tag))))))
       
    
(defn pad [n s]
  (let [padding (- n (count s))]
    (apply str (concat (apply str (repeat padding "0")) s))))

(defn md5-sum
  [#^String str]
  (let [alg (doto (MessageDigest/getInstance "MD5")
	      (.reset)
	      (.update (.getBytes str)))]
    (try
     (pad 32 (.toString (new BigInteger 1 (.digest alg)) 16))
     (catch NoSuchAlgorithmException e
       (throw (new RuntimeException e))))))


(defn auth_get-session
  [params]
  (let [token (params "token")
	api-unsigned (str "api_key" apikey "methodauth.getSessiontoken" token secret)
	api-sig (md5-sum api-unsigned)
	response (lastfm-getmethod {"method" "auth.getSession"
				    "token" token
				    "api_sig" api-sig})
	username   (first (for [x (xml-seq response)
				:when (= (:tag x) :name)]
			    (first (:content x))))
	key        (first (for [x (xml-seq response)
				:when (= (:tag x) :key)]
			    (first (:content x))))
	subscriber (first (for [x (xml-seq response)
				:when (= (:tag x) :subscriber)]
			    (first (:content x))))]
    {:username username :key key :subscriber subscriber}))
	

	