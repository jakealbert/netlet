(ns netlet.templates
  (:use [netlet.lastfm]
	[netlet.util]
	[clj-time.core]
	[clj-time.format]
	[clj-time.coerce]
	[hiccup :only [html h]]
	[hiccup.page-helpers :only [include-css doctype]]
	[hiccup.form-helpers])
  (:import (com.google.appengine.api.users UserServiceFactory))
  (:require [clojure.contrib.str-utils2 :as s2]))


;;;;;;;;;;;;;;;;;;;
;; MENU ITEMS
;;;;;;;;;;;;;;;;;;;

(def navigation-items
  (list
   (struct-map section
       :title "Netlets" 
       :body "overview"
       :auth-level :user)
   (struct-map section
     :title "Charts"
     :body "charts"
     :auth-level :user)
   (struct-map section
     :title "Admin"
     :body "admin"
     :auth-level :admin)))

(def footer-items
  (list
   (struct section "Home" nil)
   (struct section "User Agreement" "user-agreement")
   (struct section "Privacy Policy" "privacy-policy")
   (struct section "Contact Us" "contact")
   (struct section "Android/iPhone" "mobile")
   (struct section "GitHub" "http://github.com/jakealbert/netlet")))


;;;;;;;;;;;;;;;;;;;
;; TEMPLATES
;;;;;;;;;;;;;;;;;;;

(defn gen-head
  [title]
  (letfn [(when-ie [content]
		   (str "<!--[if IE]>"
			content
			"<![endif]-->"))]
    [:head
     [:script {:type "text/javascript"
	       :src "/js/jquery-1.3.2.min.js"}]
     [:script {:type "text/javascript"
	       :src "/js/jquery-ui-1.7.1.custom.min.js"}]
     [:script {:type "text/javascript"
	       :src "/js/selectToUISlider.jQuery.js"}]
     [:script {:type "text/javascript"
	       :src "/js/highcharts.js"}]
     [:script {:type "text/javascript"
	       :src "/js/netlet.js"}]
     [:link {:type "text/css"
	     :href "/css/redmond/jquery-ui-1.7.1.custom.css"
	     :rel "stylesheet"}]
     [:link {:type "text/css"
	     :href "/css/ui.slider.extras.css"
	     :rel "stylesheet"}]       
     [:link {:type "text/css"
	     :href "/blueprint/screen.css"
	     :media "screen, projection"
	     :rel "stylesheet"}]
     [:link {:type "text/css"
	     :href "/blueprint/print.css"
	     :media "print"
	     :rel "stylesheet"}]
     (when-ie
      [:link {:type "text/css"
	      :href "/blueprint/ie.css"
	      :media "screen, projection"
	      :rel "stylesheet"}])
     [:link {:type "text/css"
	     :href "/stylesheet.css"
	     :media "screen"
	     :rel "stylesheet"}]
     [:link {:type "text/css"
	     :href "http://fonts.googleapis.com/css?family=Cardo"
	     :rel "stylesheet"}]
     [:link {:type "text/css"
	     :href "http://fonts.googleapis.com/css?family=Nobile"
	     :rel "stylesheet"}]
     [:title title]]))
   


;;;;;;;;;;;;;;;;;;;
;; SECTIONS
;;;;;;;;;;;;;;;;;;;

(defn full-section
  [& content]
  [:div.span-22.prepend-1.append-1.append-top.append-bottom.last
   content])

(defn widget-section
  [session params widgets]
  (html
   [:div.tabsection.prepend-1.span-14.append-1.colborder
    (map (fn [widget] (html [:div.box 
			    (if (not (= "" (:title widget)))
			      [:h3 (:title widget)])
			     ((:body widget) session params)]))
	 (filter (fn [widget] (and (not (= (:position widget) :right))
				   (in-section-for? (params "page")
						    (params "subpage")
						    (:section widget)
						    (:subsection widget))
				   (authorized-for? session widget)))
		 widgets))]
   [:div.tabsection.span-7.last
    (map (fn [widget] (html [:div.box 
			     (if (not (= "" (:title widget)))
			       [:h3 (:title widget)])
			     ((:body widget) session params)]))
	 (filter (fn [widget] (and (= (:position widget) :right)
				   (in-section-for? (params "page")
						    (params "subpage")
						    (:section widget)
						    (:subsection widget))
				   (authorized-for? session widget)))
		 widgets))]))

(defn centered-section
  [title body]
  [:div.span-18.prepend-3.append-3.prepend-top.append-bottom.last
   [:div.box
    [:h2 title]
    [:hr]
    body]])
      
;;;;;;;;;;;;;;;;;;;
;; SUBSECTIONS
;;;;;;;;;;;;;;;;;;;

(defn full-subsection
  [& content]
  [:div.span-18.last
    content])


(defn widget-subsection
  [session params widgets]
  (html
   [:div.widgetsubsection.span-11.append-1.colborder
    (map (fn [widget]
	   (let 
	       [widget-title (:title widget)
		widget-title (if (map? widget-title)
			       (widget-title (params "subpage"))
			       widget-title)
		widget-title (cond
			      (nil? widget-title) nil
			      (= widget-title "") nil
			      (string? widget-title) [:h3 widget-title]
			      :else nil)
		bodyout (if (nil? widget-title)
			  [:div.box.subsection
			   ((:body widget) session params)]
			  [:div.box.subsection
			   widget-title
			   ((:body widget) session params)])]
	     bodyout))
	 (filter (fn [widget] (and (not
				    (or 
				     (and (keyword? (:position widget))
					  (= (:position widget) :right))
				     (and (map? (:position widget))
					  (= (get (:position widget) (params "subpage") (val (first (:position widget)))) :right))
				     (and (fn? (:position widget))
					  (= ((:position widget) (params "subpage"))
					     :right))))
				   (in-section-for? (params "page")
						    (params "subpage")
						    (:section widget)
						    (:subsection widget))
				   (authorized-for? session widget)))
		 widgets))]
   [:div.widgetsubsection.span-6.last
    (map (fn [widget] 
	   (let
	       [widget-title (:title widget)
		widget-title (if (map? widget-title)
			       (get widget-title (params"subpage") (val (first (:title widget))))
			       widget-title)
		widget-title (cond 
			      (nil? widget-title) nil
			      (= widget-title "") nil
			      (string? widget-title) [:h3 widget-title]
			      (fn? widget-title) nil
			      :else nil)
		bodyout (if (nil? widget-title)
			  [:div.box.subsection
			   ((:body widget) session params)]
			  [:div.box.subsection
			   widget-title
			   ((:body widget) session params)])]
	     bodyout))
	 (filter (fn [widget] (and (or
				    (and (keyword? (:position widget))
					 (= (:position widget) :right))
				    (and (map? (:position widget))
					 (= (get (:position widget) (params "subpage") (val (first (:position widget)))) :right))
				    (and (fn? (:position widget))
					 (= ((:position widget) (params "subpage")) :right)))
				   (in-section-for? (params "page")
						    (params "subpage")
						    (:section widget)
						    (:subsection widget))
				   (authorized-for? session widget)))
		 widgets))]))


(defn widget-subsection-full
  [session params widgets]
  (html
   [:div.widgetsubsection.span-18.append-1.last
    (map (fn [widget]
	   (let 
	       [widget-title (:title widget)
		widget-title (if (map? widget-title)
			       (widget-title (params "subpage"))
			       widget-title)
		widget-title (cond
			      (nil? widget-title) nil
			      (= widget-title "") nil
			      (string? widget-title) [:h3 widget-title]
			      :else nil)
		bodyout (if (nil? widget-title)
			  [:div.box.subsection
			   ((:body widget) session params)]
			  [:div.box.subsection
			   widget-title
			   ((:body widget) session params)])]
	     bodyout))
	 (filter (fn [widget] (and   (in-section-for? (params "page")
						      (params "subpage")
						      (:section widget)
						      (:subsection widget))
				     (authorized-for? session widget)))
		 widgets))]))

(defn tabbed-section
  [session params tabs section]
  [:div.span-22.prepend-top.append-bottom.prepend-1.append-1
   [:div.span-3
    [:ul.tabbed-section.box
     (for [tab tabs
	   :when (authorized-for? session tab)]
       (let [page     (params "page")
	     page-url (if (= page "tabs")
			"/"
			(cond (params "userpage") (str "/" page "/" (params "userpage") "/")
			      (params "artist") (str "/" page "/" (s2/replace (params "artist") #" " "+") "/")
			      :else (str "/" page "/")))
	     subpage   (params "subpage")
	     subpage   (if (nil? subpage)
			 (:body (first tabs))
			 subpage)
	     link      [:a {:href (str page-url (:body tab))} (:title tab)]]
	 (if (= subpage (:body tab))
	       [:li.current link]
	       [:li link])))]]
   [:div.span-18.box.last.column.tabbed-section
    (let [subpage    (params "subpage")
	  subpage    (if (nil? subpage)
		       (:body (first tabs))
		       subpage)
	  subsections (:subsections section)
	  subsection (subsections subpage)
	  title      (cond
		      (nil? (:long-title subsection)) (:title subsection)
		      (string? (:long-title subsection)) (:long-title subsection)
		      (fn? (:long-title subsection)) ((:long-title subsection) session params)
		      :else (:title subsection))
	  title      (if (fn? title)
		       (title session params)
		       title)
	  description (:description subsection)
	  description (if (fn? description)
			(description session params)
			description)
	  description (if (string? description)
			description
			"")]
      (full-subsection
       [:h2 title]
       [:div.subsection-description description]
       [:hr]
       ((:body subsection) session params)))]])




;;;;;;;;;;;;;;;;;;;
;; TRACKLISTS
;;;;;;;;;;;;;;;;;;;

(defn track-to-list-item
  [s p track]
  (let [actions (list {:title "Songbooks"
		       :link "songbooks"
		       :content (fn [s p] (let [links (list
						       {:title "View in Songbook"
							:url "view"
							:link "view"}
						       {:title "Add to Songbook"
							:url "add"
							:link "add"}
						       {:title "Remove from Songbook"
							:url "remove"
							:link "remove"})]
					    (for [link links]
					      [:li.icon [:a {:href (:url link)
							     :class (:link link)}
							 (:title link)]])))}
		      {:title "Versions"
		       :link "versions"
		       :content (fn [s p] (let [links (list
						       {:title "Guitar Tabs"
							:url "guitar"
							:link "guitar"}
						       {:title "Bass Tabs"
							:url "bass"
							:link "bass"}
						       {:title "Chords"
							:url "chords"
							:link "chords"}
						       '{:title "All"
							:url "all"
							:link "all"})]
					    (for [link links]
					      [:li.icon [:a {:href (:url link)
							     :class (:link link)}
							 (:title link)]])))})			       
	album-info  (if (or (nil? (:album-mbid track))
			    (= "" (:album-mbid track)))
		      nil
		      (album_get-info-by-mbid (:album-mbid track)))
	album-art-url (if (nil? album-info)
			nil
			(get-album-image-url album-info))
	album-art-url (if (or (nil? album-art-url)
			      (= "" album-art-url))
			"/images/no-art.png"
			album-art-url)
	album-art  [:img {:src album-art-url :alt (str (h (:artist track)) " - " (h (:name track))) :width "64px" :height "64px"}]
	track-body [:div
		    [:div.album-art
		     [:a {:href (str "/artist/" (:url-artist track) "/" (:url-name track))} album-art]]
		    [:h4
		     [:a {:href (str "/artist/" (:url-artist track))}
		      (h (:artist track))]
		     " - "
		     [:a {:href (str "/artist/" (:url-artist track) "/" (:url-name track))}
		      (h (:name track))]]
		    [:div.date (if (nil? (:date track))
				 "Now Playing"
				 (dt-time-ago (lastfm-date-to-dt (:date track))))]
		    [:div.track-actions
		     [:a {:href (str "#" (:link (first actions))) :link (:link (first actions))} (:title (first actions))]
		     (for [action (rest actions)]
		       (html [:a {:href (str "#" (:link action)) :link (:link action)} (:title action)]))]
		    [:div.action-expand
		     (for [action actions]
		       [:div {:class (:link action)} ((:content action) s p)])]]]
    (if (true? (:now-playing track))
      [:li.track.now-playing track-body]
      [:li.track track-body])))

(defn tracks-to-ol
  [s p tracks]
  [:ol.tracklist
   (for [track tracks]
     (track-to-list-item s p track))])

(defn tracks-to-ul
  [s p tracks]
  [:ul.tracklist
   (for [track tracks]
     (track-to-list-item s p track))])




;;;;;;;;;;;;;;;;;;;
;; TABLISTING
;;;;;;;;;;;;;;;;;;;


(defn tab-to-list-item
  [s p tab]
  (let [actions (filter (fn [x] (tab x)) (list :guitar :bass :piano :drum :power :pro))
	actions (map (fn [action] 
		       (cond 
			(= action :guitar) {:title "Guitar"
					    :link "guitar"
					    :count (:count (tab action))}
		        (= action :bass) {:title "Bass"
					  :link "bass"
					  :count (:count (tab action))}
			(= action :piano) {:title "Piano"
					    :link "piano"
					    :count (:count (tab action))}
			(= action :drum)  {:title "Drum"
					    :link "drum"
					    :count (:count (tab action))}
			(= action :power)  {:title "Power"
					    :link "power"
					    :count (:count (tab action))}
			(= action :pro)  {:title "Pro"
					    :link "pro"
					    :count (:count (tab action))}))
		     actions)
				
	track-body [:li.track
		    [:h4
		     (if (or (= (p "subpage") "versions")
			     (= (p "subpage") "overview")
			     (= (p "subpage") "")
			     (nil? (p "subpage")))
		       [:a {:href (str "/artist/" (s2/replace (p "artist") #" " "+")  "/versions/" (s2/replace (:title tab) #" " "+"))} (:title tab)]
		       [:a {:href (str "/artist/" (s2/replace (p "artist") #" " "+")  "/" (p "subpage") "/" (s2/replace (:title tab) #" " "+"))} (str (:title tab) " (" (:count (tab (keyword (p "subpage")))) ")")])]
		     
		    (if (= (p "subpage") "versions")
		      [:div.tab-actions
		     (for [action actions]
		       (html [:a {:href (str "/artist/"
				     (s2/replace (p "artist") #" " "+")
				     "/"
				     (:link action)
				     "/"
				     (s2/replace (:title tab) #" " "+"))}
			      (str (:title action)
				   " ("
				   (:count action)
				   ")")]))])]]
    track-body))



(defn tabs-to-ol
  [s p tabs]
  [:ol.tablist
   (for [tab tabs]
     (tab-to-list-item s p tab))])

(defn tabs-to-ul
  [s p tabs]
  [:ul.tablist
   (for [tab tabs]
     (tab-to-list-item s p tab))])




;;;;;;;;;;;;;;;;;;;;;;
;; TABVERSIONLISTING
;;;;;;;;;;;;;;;;;;;;;;


(defn tab-version-to-list-item
  [s p tab]
  (let [actions (filter (fn [x] (tab x)) (list :guitar :bass :piano :drum :power :pro))
	actions (map (fn [action] 
		       (cond 
			(= action :guitar) {:title "Guitar"
					    :link "guitar"
					    :count (:count (tab action))}
		        (= action :bass) {:title "Bass"
					  :link "bass"
					  :count (:count (tab action))}
			(= action :piano) {:title "Piano"
					    :link "piano"
					    :count (:count (tab action))}
			(= action :drum)  {:title "Drum"
					    :link "drum"
					    :count (:count (tab action))}
			(= action :power)  {:title "Power"
					    :link "power"
					    :count (:count (tab action))}
			(= action :pro)  {:title "Pro"
					    :link "pro"
					    :count (:count (tab action))}))
		     actions)
				
	track-body [:li.track
		    [:h4
		     [:a {:href (str "/artist/" (s2/replace (p "artist") #" " "+")  "/" (p "subpage") "/" (s2/replace (p "track") #" " "+") "/" (:version-number tab))}
		      (str (:title tab)
			   (if (:rating tab)
			     (str " <span class=\"rating\">(" (:rating tab) ")</span>")
			     ""))]]
		     
		    (if (or (= (p "subpage") "versions")
			    (= (p "subpage") "guitar"))
		      [:span.versions-type (:type tab)])]]
    track-body))



(defn tab-versions-to-ol
  [s p tabs]
  [:ol.tablist
   (for [tab tabs]
     (tab-version-to-list-item s p tab))])

(defn tab-versions-to-ul
  [s p tabs]
  [:ul.tablist
   (for [tab tabs]
     (tab-version-to-list-item s p tab))])



;;;;;;;;;;;;;;;;;;;
;; LAYOUT
;;;;;;;;;;;;;;;;;;;

(defn layout [session params title content]
  (html
   [:html
    (doctype :xhtml-strict)
    (gen-head title)
    [:body
     [:div#preheader.container]
     [:div#header
      [:div.container
       [:div#logo.span-16.prepend-top
	[:h1 [:a {:href "/"} "Netlets: Network-Enabled Power Outlets"]]]
       [:div#userbox.prepend-1.span-7.last.box
	(let [logged-in (session :username)]
	  (if logged-in
	    (html
	     "Welcome, " 
	     [:a.userlink {:href (str "/users/" (session :username))} (h (session :username))]
		". "
		[:a {:href "/logout"} "Logout"])
	    (html
	     [:a {:href "/login"} "Login/Register"])))]
       [:div#navbar.span-24.last1
	[:ul
	 (for [nav-item navigation-items
	       :when (authorized-for? session nav-item)]
	   (let [link [:a {:href (str "/" (:body nav-item))} (:title nav-item)]
		 page (params "page")]
	     (if (= page (:body nav-item))
	       [:li.current link]
	       [:li link])))]]]]
     [:div#content.container
      [:div.prepend-top.append-bottom
       content]]
     [:div#footer
      [:div.container
       [:div#footernav.span-24.last
	(let [first-link (first footer-items)]
	  [:a {:href (str "/" (:body first-link))} (:title first-link)])
	(for [footer-item (rest footer-items)]
	  (html
	   " | " [:a {:href (if (s2/contains? (:body footer-item) "://")
			      (:body footer-item)
			      (str "/" (:body footer-item)))}
		  (:title footer-item)]))]
       [:div#copyright.span-24.last
	"&copy; Netlets 2010. All rights reserved.  A "
	[:a {:href "http://youbroughther.com"} "YBH"]
	" Production."]]]]]))
       

