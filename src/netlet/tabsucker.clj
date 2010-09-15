(ns netlet.tabsucker
  (:import (java.io Reader InputStream InputStreamReader ByteArrayInputStream IOException))
  (:require [clojure.contrib.str-utils2 :as s2])
  (:require [clojure-http.resourcefully :as res]))



(defn get-pre-from-url
   [url]
   (letfn [(response-process [response]
			     (if (nil? response)
			       nil
			       (let [bodyseq (:body-seq response)
				     bodystr (apply str bodyseq)
				     [bodystr prebody] (re-find #".*<pre(.*)</pre>.*" bodystr)
				     [presplit pre] (s2/split prebody #">" 2)
				     pre (if (or (s2/contains? pre "<br>")
						 (s2/contains? pre "<br />")
						 (s2/contains? pre "<br/>")
						 (s2/contains? pre "<BR>")
						 (s2/contains? pre "<BR />")
						 (s2/contains? pre "<BR/>"))
					   (s2/split pre #"(<br>)|(<br />)|(<br/>)|(<BR>)|(<BR/>)|(BR />)")
					   pre)]
						 
				 
				     pre)))
	   (response []
		    (let [response-try (try
					(res/get url)
					(catch IOException e
					  nil))
			  processed (response-process response-try)]
		      processed))]
    (response)))



(defn get-url-from-911link
   [link]
   (letfn [(response-process [response]
			     (if (nil? response)
			       nil
			       (let [bodyseq (:body-seq response)
				     bodystr (apply str bodyseq)
				     [bodystr tablink] (re-find #".*<iframe src=\"(.*)\" id=\"LinkFrame\".*" bodystr)]
				 tablink)))
	   (response []
		    (let [response-try (try
					(res/get (str "http://www.911tabs.com" link))
					(catch IOException e
					  nil))
			  processed (response-process response-try)]
		      processed))]
    (response)))
				     
				     

(defn get-tabs-by-link
  [link]
  (letfn [(response-process [response]
			    (if (nil? response)
			      nil
			      (let [bodyseq (:body-seq response)
				    bodystr (apply str bodyseq)
				    splitone (s2/split bodystr #"video lessons")
				    splittwo (s2/split (nth splitone 2) #"<td colspan=2 height=80 align=center>")
				    splittr (s2/split (first splittwo) #"(<tr class=tr>)|(<tr class=\"tr1\">)")
				    splittr (drop 1 (drop-last 0 splittr))]
				(map (fn [tr]
				       (let [tds (first (s2/split tr #"</tr>"))
					     tds (s2/replace tds #"\t" "")
					     [tds version-number title part rating type] (re-find #"<td class=small>(.*)\.</td><td>(.*)</td><td align=center class=small>(.*)</td><td align=center class=smallg>(.*)</td><td align=center class=small>(.*)</td>" tds)
					     type-cond (cond
							(= type "guitar tab") :guitar
							(= type "chords") :chords
							(= type "bass tab") :bass
							(= type "drum tab") :drum
							(= type "power tab") :power
							(= type "guitar pro tab") :guitar_pro
							(= type "piano tab") :piano
							:else type)
					     part-cond (cond
							(= part "intro") :intro
							(= part "solo") :solo
							(= part "") nil
							:else part)
					     rating (if (= rating "")
						      nil
						      (Double/parseDouble rating))
					     version-number (Integer/parseInt version-number)
					     [title-anchor title-link title-name] (re-find
										 #"<a href=\"(.*)\" rel=\"nofollow\">(.*)</a>"
										 title)
					     newmap {:title title-name
						     :version-number version-number
						     :911link title-link
						     :part part
						     :rating rating
						     :type type-cond}]
					 newmap))
				     splittr))))
	  (response []
		    (let [response-try (try
					(res/get (str "http://www.911tabs.com/" link))
					(catch IOException e
					  nil))
			  processed (response-process response-try)]
		      processed))]
    (response)))

 

(defn get-tabs-by-track
  ([artist name] (get-tabs-by-track artist name ""))
  ([artist name type]
     (let [link-type (if (= type "versions")
		       ""
		       type)
	   link-append (if (= link-type "")
			 "_tab.htm"
			 (str  "_" link-type "_tab.html"))
	   link-prepend (if (= link-type "")
			  ""
			  (str "/" link-type "_tabs"))
	   link-artist (s2/replace (s2/lower-case artist) #" " "_")
	   link-firstlet (s2/take link-artist 1)
	   link-name (s2/replace (s2/lower-case name) #" " "_")]
       (get-tabs-by-link (str "/tabs/" link-firstlet "/" link-artist
			      link-prepend "/" link-name link-append)))))

(defn get-tabs-by-artist
  [artist]
  (let [artist-911tabs-url (s2/lower-case (s2/replace (s2/replace artist #" " "_") #"\+" "_"))
	first-letter (s2/lower-case (s2/take artist 1))]
    (letfn [(response-process [response]
				(if (nil? response)
				  #{}
				  (let [bodyseq (:body-seq response)
					bodystr (apply str bodyseq)
					splitone (s2/split bodystr #"video lessons")
					splittwo (s2/split (nth splitone 2) #"<td colspan=2 height=80 align=center>")
					splittr (s2/split (first splittwo) #"(<tr class=tr>)|(<tr class=\"tr1\">)")
					splittr (drop 1 (drop-last 0 splittr))]
				    (map (fn [tr]
					   (let [tds (first (s2/split tr #"</tr>"))
						 tds (s2/butlast (s2/drop (s2/replace tds #"\t" "") 4) 5)
						 [title & tabs] (s2/split tds #"</td><td align=\"center\">")
						 [title-anchor versions-link title] (re-find
										     #"<a href=\"(.*)\"  title=\".*\"  >(.*) tab</a>"
										     title)
						 [guitar bass drum piano power pro] (map (fn [tab]
											   (if (or (nil? tab)
												   (= "" tab))
											     nil
											     (let [[tab-anchor tab-link tab-count foo] (re-find #"<a href=\"(.*)\" class=\"ta\"  title=\".*\">(.*) (tab|tabs)</a>" tab)
												   tab-count (Integer/parseInt tab-count)]
											       {:link tab-link :count tab-count}))) tabs)
						 newmap {:title title
							 :versions {:link versions-link
								    :count (reduce + 0 (filter (fn [x] x) (map :count (list guitar bass drum piano power pro))))}
							 :guitar guitar
							 :bass bass
							 :drum drum
							 :piano piano
							 :power power
							 :guitar_pro pro}]
					     newmap))
					 splittr))))
	    (response [n acc]
		      (let [response-try (try
					  (res/get (str "http://www.911tabs.com/tabs/" 
							first-letter
							"/"
							artist-911tabs-url
							"/index" n ".html"))
					  (catch IOException e
					    nil))
			    processed (response-process response-try)]
			(if (empty? processed)
			  acc
			  (response (inc n) (concat acc processed)))))]
      (response 1 #{}))))
