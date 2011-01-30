(ns netlet.util
  (:use [clj-time.core]
	[clj-time.format]
	[clj-time.coerce])
  (:import (java.security NoSuchAlgorithmException MessageDigest)
	   (java.math BigInteger))
  (:require [clojure.contrib.str-utils2 :as s2]))

(def *users-map* (ref {}))

(defstruct section :title :body)

(defn xor
  [a b]
  "Logical Two-Input Exclusive-OR"
  (or (and a (not b))
      (and b (not a))))

    
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


;;;;;;;;;;;;;;;
;; PREDICATES
;;;;;;;;;;;;;;;

(defn authorized-for?
  [session section]
  "Is the user of the current session's auth-level
    above the auth-level in the section map?"
  (let [user (session :username)
	user-auth-level (session :auth-level)
	section-auth-level (section :auth-level)]
    (cond
     (nil? user-auth-level)      (or (nil? section-auth-level)
				     (= section-auth-level :logged-out))
     (= user-auth-level :user)   (or (nil? section-auth-level)
				     (= section-auth-level :user))
     (= user-auth-level :admin)  (or (nil? section-auth-level)
				     (= section-auth-level :user)
				     (= section-auth-level :admin)))))

(defn in-section-for?
  [page subpage section subsection]
  "Returns whether the :section and :subsection of a widget/map contain
   the current page and subpage (http://tabs.fm/page/subpage)" 
   (or (nil? section)
       (and
	(nil? page)
	(or 
	 (and
	  (set? section)
	  (section "overview"))
	 (and
	  (string? section)
	  (= section "overview"))))
       (and
	(or
	 (and
	  (set? section)
	  (section page))
	 (and
	  (string? section)
	  (= section page)))
	(or
	 (nil? subsection)
	 (and (nil? subpage) 
	      
	      (subsection "overview"))
	      
	       
	 (and
	  (set? subsection)
	  (subsection subpage))
	 (and
	  (string? subsection)
	  (= subsection subpage))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DATE SELECTION/CALENDAR FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unixtime
  [dt]
  (* 1000 (in-secs (interval (epoch) dt))))

(defn get-calendar-select
  [alod selected-date] 
  (if (empty? alod)
    '()
    (cons [:optgroup {:label (str (year (first alod)))}
	   (map (fn [dt] 
		  (let [option-attrs {:value (unixtime dt)}
			option-attrs (if (= (unparse (formatter "dd-MM-YYYY")
						     dt)
					    selected-date)
				       (assoc option-attrs :selected "selected")
				       option-attrs)]
		    
		    [:option option-attrs
		     (unparse (formatter "EEEE dd MMMM YYYY") dt)]))
		
		
		(filter (fn [dt]
			  (= (year dt) (year (first alod)))) alod))]
	  (get-calendar-select (filter (fn [dt]
				(not (= (year dt) (year (first alod))))) alod)
		      selected-date))))

(defn get-calendar-dates
  ([now-dt] (get-calendar-dates (minus now-dt (days (day-of-week now-dt))) '()))
  ([enddate acc]
     (let [startdate (date-time 2010 6 01)]
       (if (after? enddate startdate)
	 (get-calendar-dates
	  (minus enddate (weeks 1))
	  (cons enddate acc))
	 acc))))



(defn get-calendar-html
  [name selected-date]
  (vec
    (concat
     [:select.calendar {:name name
			:id name
		        :style "width: auto !important"}]
     (get-calendar-select (get-calendar-dates (now)) selected-date))))



(defn lastfm-date-to-dt
  [lastfm-date]
  (let
      [split-lfm-date (s2/split lastfm-date #" ")
       year (Integer/parseInt (s2/butlast (nth split-lfm-date 2) 1))
       day  (Integer/parseInt (first split-lfm-date))
       hmsplit (map (fn [x] (Integer/parseInt x)) (s2/split (nth split-lfm-date 3) #":"))
       hour (first hmsplit)
       minute (second hmsplit)
       monthmap {"Jan" 1
		 "Feb" 2
		 "Mar" 3
		 "Apr" 4
		 "May" 5
		 "Jun" 6
		 "Jul" 7
		 "Aug" 8
		 "Sep" 9
		 "Oct" 10
		 "Nov" 11
		 "Dec" 12}
       month (monthmap (second split-lfm-date))]
    (date-time year month day hour minute)))
  
(defn dt-time-ago
  [dt]
  (let
      [now-dt (now)
       dur (interval dt now-dt)
       dur-min (in-minutes dur)
       dur-hour (int (/ dur-min 60))
       dur-day (int (/ dur-hour 24))]
    (cond (= 0 dur-min) "Now Playing"
	  (= 1 dur-min) "1 Minute Ago"
	  (< dur-min 60) (str dur-min " Minutes Ago")
	  (= dur-hour 1) "1 Hour Ago"
	  (< dur-hour 24) (str dur-hour " Hours Ago")
	  (= dur-day 1) "1 Day Ago"
	  :else (str dur-day " Days Ago"))))
       
(defn flatten [x]
  (let [s? #(instance? clojure.lang.Sequential %)]
    (filter (complement s?) (tree-seq s? seq x))))

(defn get-now
  []
  (* 1000 (in-secs (interval (epoch) (now)))))
