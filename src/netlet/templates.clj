(ns netlet.templates
  (:use [netlet.util]
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
	       :src "/js/jquery-1.4.4.min.js"}]
     [:script {:type "text/javascript"
	       :src "/js/jquery-ui-1.8.9.custom.min.js"}]
     [:script {:type "text/javascript"
	       :src "/js/selectToUISlider.jQuery.js"}]
     [:script {:type "text/javascript"
	       :src "/js/highcharts.js"}]
     [:script {:type "text/javascript"
	       :src "/js/netlet.js"}]
     [:link {:type "text/css"
	     :href "/css/custom-theme/jquery-ui-1.8.9.custom.css"
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
      (html
       [:link {:type "text/css"
	       :href "/blueprint/ie.css"
	       :media "screen, projection"
	       :rel "stylesheet"}]))
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
			      (html [:h3 (:title widget)]
				    [:hr]))

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
			       (html [:h3 (:title widget)]
				     [:hr]))
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
	[:h1 [:a {:href "/"} "Netlets"]]
	[:h2 "Network-Enabled Power Outlets"]]
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
       

