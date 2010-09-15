(ns netlet.content
  (:use [netlet.lastfm]
	[netlet.templates]
	[netlet.util]
	[netlet.tabsucker]
	[clj-time.core]
	[clj-time.format]
	[hiccup :only [html h]]))

(def site-title "Netlet")

(def lipsum
     "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras luctus ligula et ipsum suscipit ut pharetra metus luctus. Duis vestibulum arcu in diam sollicitudin vulputate. Cras rhoncus consectetur mauris, sit amet molestie nisi volutpat sit amet. Cras dictum, tortor ac auctor feugiat, sapien ante elementum risus, ut placerat mi mauris at quam. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque bibendum eros at lectus congue non sodales lectus auctor. Nulla aliquet tortor id felis placerat tincidunt. Cras et mauris fringilla risus vestibulum tristique a ut eros. Integer sit amet ante sit amet odio laoreet faucibus sed id nisi. Proin lacus lorem, feugiat vel euismod sit amet, facilisis sed quam. In sed nulla quam. Vivamus interdum, mauris id consectetur varius, velit augue fringilla urna, quis dignissim quam est sit amet leo. Fusce est lacus, viverra in dictum in, dapibus vitae sapien. Phasellus eleifend magna eros, ac faucibus arcu. Mauris nisi libero, rutrum laoreet egestas sed, consectetur a dolor. Ut lobortis sapien eget turpis dictum non egestas odio mattis. Duis vehicula faucibus eros id accumsan.")

   

(def widgets
     (list
      (struct-map section
	:title "My Netlets"
	:body (fn [session params]
		(html
		 [:p.alt "You have no devices set up."]
		 [:p [:a {:href "#"} "Add a Netlet"]]))
	:section "overview"
	:auth-level :user)
      (struct-map section
	:title "Recent Power Usage"
	:body (fn [session params]
		[:ul
		 [:li "current"]
		 [:li "voltage"]
		 [:li "apparent power"]])
	:section "overview"
	:auth-level :user)
      (struct-map section
	:title  "Recently Activated Triggers"
	:body (fn [s p]
		[:p "Want more, eh?"])
	:section "overview"
	:auth-level :user)
      (struct-map section
	:title "Top Artists"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"overview" "artist"})
      (struct-map section
	:title "Most Jammed"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"artist" "track"}
	:position :right)
      (struct-map section
	:title "Top Tracks"
	:body (fn [s p] [:div])
	:section #{"charts" "tabs"}
	:subsection #{"overview" "track"})
      (struct-map section
	:title "Hyped Artists"
	:body (fn [s p] [:div])
	:section #{"charts" "tabs"}
	:subsection #{"overview" "hyped-artist"}
	:position {"overview" :right
		   "hyped-artist" :left})
      (struct-map section
	:title "Hyped Tracks"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"overview" "hyped-track"}
	:position {"overview" :right
		   "hyped-track" :left})
      (struct-map section
	:title "Loved Tracks"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"overview" "loved"}
	:position {"overview" :right
		   "loved" :left})
      (struct-map section
	:title {"overview" "Tracks"}
	:body (fn [s p]
		(if (p "track")
		  (tab-versions-to-ol s p
				      (get-tabs-by-track (p "artist") (p "track") (p "subpage")))
		  (tabs-to-ul s p 
			      (filter
			       (fn [tab]
				 (let [tab-type (p "subpage")
				       tab-type (if (or (nil? tab-type)
							(= "" tab-type)
							(= "overview" tab-type))
						  :versions
						  (keyword tab-type))]
				   (tab tab-type)))
			       (sort-by :title (get-tabs-by-artist (p "artist")))))))
	:section "artist"
	:subsection #{"overview" "versions" "guitar" "bass" "drum" "piano" "power" "guitar-pro"}
	:position (fn [x] (if (or (= x "overview")
				  (= x "")
				  (nil? x))
			    :right
			    :left)))
      (struct-map section
	:title "Debug"
	:body (fn [session params]
		(html
		 [:p (str session)]
		 [:p (str params)]))
	:position :right
	:auth-level :admin)))

(def sections {})
	       
(def sections
  {"overview"     (struct section
		      ""
		      (fn [session params]
			(widget-section session params widgets)))
   "songbook" (struct-map section
		      :title "Songbook"
		      :body (fn [session params]
			      (let [user (session :lastfm-user)
				    logged-in (and (:username user)
						   (:key user))]
				(if logged-in
				  (tabbed-section session params
						  (list
						   (struct section
							   "Library"
							   "library")
						   (struct section
							   "Favorite Tracks"
							   "favorites")
						   (struct section
							   "Songs in Gm"
							   "songs-in-gm"))
						  ((var sections) "songbook"))
				  (centered-section "All the songs you play, all in one place." [:p "Songbook is your own collection of the tracks you love, the tabs you've written, and chords you're still learning.  Search your library for the by tuning, key, artists, or &#8220;jam count&#8221; with Smart Songbooks."]))))
		      :subsections {"library" (struct section
						      (fn [session p]
							(str
							 (:username
							  (:lastfm-user session))
							 "'s Library"))
						      (fn [s p ]
							"library"))
				    "favorites" (struct section
							(fn [session p]
							  (str
							   (:username
							    (:lastfm-user session))
							   "'s Favorite Tracks"))
							(fn [s p]
							  "favorites"))
				    "songs-in-gm" (struct section
							  "Songs in Gm"
							  (fn [s p]
							    "smart songbook"))})
     "charts"   (struct-map section
		:title "Charts"
		:body (fn [session params]
			(tabbed-section session params 
					(list
					 (struct section "Overview" "overview")
					 (struct section "Top Artists" "artist")
					 (struct section "Hyped Artists" "hyped-artist")
					 (struct section "Top Tracks" "track")
					 (struct section "Hyped Tracks" "hyped-track")
					 (struct section "Loved Tracks" "loved")) 
					((var sections) "charts")))
		:subsections
		(let [calendar-description-fn (fn [s p]
						(let [dt (now)
						      pastsun (minus dt (days (day-of-week dt)))
						      pastsun2 (minus pastsun (weeks 1))
						      fmt (formatter "dd-MM-YYYY")
						      datestr (str "For the week ending on "
								   (html
								    (get-calendar-html "enddate"
										       (unparse fmt pastsun))) )]
						  datestr))
		      overview-description-fn (fn [s p]
						(let [dt (now)
						      pastsun (minus dt (days (day-of-week dt)))
						      pastsun2 (minus pastsun (weeks 1))
						      fmt (formatter "EEEE dd MMMM YYYY")
						      datestr (str "For the week of "
								   (unparse fmt pastsun2)
								   " to "
								   (unparse fmt pastsun))]
						  datestr))]
		  {"overview" (struct-map section
				:title "Overview"
				:long-title "Weekly Artist, Track, and Tab Charts"
				:description overview-description-fn
				:body (fn [session params] 
					(widget-subsection
					 session
					 params widgets)))
		   "artist"   (struct-map section
				:title "Top Artists"
				:description calendar-description-fn
				:body (fn [session params] 
					(widget-subsection
					 session
					 params widgets)))
		   "hyped-artist" (struct-map section 
				   :title  "Hyped Artists" 
				   :description calendar-description-fn
				   :body (fn [session params] 
					   (widget-subsection
					    session
					    params widgets)))
		   "track"       (struct-map section
				   :title "Top Tracks"
				   :description calendar-description-fn
				   :body (fn [session params] 
					   (widget-subsection
					    session
					    params widgets)))
		   "hyped-track"  (struct-map section
				   :title "Hyped Tracks"
				   :description calendar-description-fn
				   :body  (fn [session params] 
					   (widget-subsection
					    session
					    params widgets)))
		   "loved"       (struct-map section
				   :title "Loved Tracks"
				   :description calendar-description-fn
				   :body (fn [session params] 
					   (widget-subsection
					    session
					    params widgets)))}))
   "admin"    (struct section
		      "Admin"
		      (fn [s p]
			(full-section
			 [:p "not implemented yet"])))
   "logout"   (struct section
		      ""
		      (fn [s p]
			(centered-section
			 "You are now logged out."
			 [:p "&#8220;later dude.&#8221;"])))
  	   
 "users"     (struct-map section
		       :title "Users"
		       :body (fn [s p]
			       (let [userpage (p "userpage")]
				 (tabbed-section s p 
						 (list 
						  (struct section
							  "Profile"
							  "profile")
						  (struct section
							  "Tracks"
							  "recent")
						 (struct section
							  "Library"
							  "library")
						  (struct section
							  "Charts"
							  "charts")
						  (struct section
							  "Friends"
							  "friends"))
						 ((var sections) "users"))))
		       :subsections 
		       {"profile" (struct-map section
				    :title "Profile"
				    :long-title (fn [s p] (str (p "userpage") "'s Profile"))
				    :body (fn [s p]
					    "user profile"))
			"library" (struct-map section
				    :title (fn [s p] (str (p "userpage") "'s Library"))
				    :body (fn [s p]
					    "user library"))
			"charts" (struct-map section
				   :title (fn [s p] (str (p "userpage") "'s Charts"))
				   :body (fn [s p]
					   "user charts"))
			"friends" (struct-map section
				    :title (fn [s p] (str (p "userpage") "'s Friends"))
				    :body (fn [s p]
					    "user friends"))
			"recent" (struct-map section
				   :title (fn [s p] (str (p "userpage") "'s Tracks"))
				   :body (fn [s p]
					   (list
					    (tracks-to-ol s p
							  (get-recent-tracks (p "userpage") 25))
					    [:div.viewmore
					     [:a.prev {:href "prev"} "Previous 25"]
					     [:a.next {:href "next"} "Next 25"]])))})
					       ;; check if user is in DS
   "user-agreement"      (struct section
				 "User Agreement"
				 (fn [s p]
				   (full-section
				    [:div.box 
				     [:h2 "User Agreement"]
				     [:hr]
				     [:p "Last Revised June 26 2010"]
				     [:p "The following User Agreement (\"Agreement\") governs the use of www.tabs.fm (\"Website\"), including without limitation participation in its bulletin boards, forums, personal ads, chats, and all other areas (except to the extent stated otherwise on a specific page) as provided by You Brought Her Productions. (\"Service Provider,\" \"we,\" or \"our\")."]
				     [:p "Please read the rules contained in this Agreement carefully.  You can access this Agreement any time at "
				      [:a {:href "http://tabs.fm/user-agreement"} "http://tabs.fm/user-agreement"]
				      ". "
				      [:strong "Your use of and/or registration on any aspect of the Website will constitute your agreement to comply with these rules."]
				      " If you cannot agree with these rules, please do not use the Website."]
				     [:p "In addition to reviewing this Agreement, please read our "
				      [:a {:href "http://tabs.fm/privacy-policy"} "Privacy Policy"]
				      ". Your use of the Website constitues agreement to its terms and conditions as well."]
				     [:p "The Agreement may be modified from time to time; the date of the most recent revision will appear on this page, so check back often.  Continued access of the Website by you will constitute your acceptance of any changes or revisions to the Agreement."]
				     [:p "Your failure to follow these rules, whether listed below or in bulletins posted at various points in the Website, may result in suspension or termination of your access to the Website, without notice, in addition to Service Provider's other remedies."]
				     [:h3 "Monitoring"]
				     [:p "We strive to provide an enjoyable online experience for our users, so we may monitor activity on the Website, including in the bulletin boards, forums, personal ads, and chats, to foster compliance with this Agreement.  All users of the Website hereby specifically agree to such monitoring.  Nevertheless, we do not make any warranties or guarantees that: (1) the Website, or any portion thereof, will be monitored for accuracy or unacceptable use, (2) apparent statements of fact will be authenticated, or (3) we will take any specific action (or any action at all) in the event of a dispute regarding compliance or non-compliance with this Agreement"]
				     [:h3 "Medical Information Disclaimer"]
				     [:h3 "Registration and Account Creation"]
				     [:h4 "Registration Information:"]
				     [:h4 "Use of User ID/Password"]
				     [:h4 "Fees and Payments"]
				     [:h3 "Rules of Usage"]
				     [:h4 "Use of the Service by You"]
				     [:h4 "Comments by Others Are Not Endorsed by Service Provider"]
				     [:h4 "User of Material Supplied by You:"]
				     [:h4 "Copyright Complaints:"]
				     [:h4 "Merchandise Sold ON OR THROUGH the Website:"]
				     [:h4 "Indemnification:"]
				     [:h4 "Editing and Deletions:"]
				     [:h4 "Additional Rules:"]
				     [:h4 "Disclaimer of Warranty and Limitation of Liability:"]
				     [:h4 "Termination or Suspension of Access to the Website:"]
				     [:h4 "Jurisdiction:"]
				     [:h4 "Auctions:"]
				     [:h4 "Associated Press:"]
				     [:h4 "Mobile Terms and Conditions"]])))
   "privacy-policy"      (struct section
				 "Privacy Policy"
				 (fn [s p]
				   (full-section
				    [:div.box
				     [:h2 "Privacy Policy"]
				     [:hr]
				     [:p "Last Revised June 26 2010"]
				     [:p "The following Privacy Policy summarizes the various ways that You Brought Her Productions (\"Service Provider,\" \"we,\" or \"our\") treats the information you provide while using www.tabs.fm (\"Website\").  It is our goal to bring you information that is tailored to your individual needs and, at the same time, protect your privacy."]
				     [:p "Please read this Privacy Policy carefully.  You can access the Privacy Policy at any time at "
				      [:a {:href "http://tabs.fm/privacy-policy"} "http://tabs.fm/privacy-policy"]
				      ". "
				      [:strong "Your use of and/or registration on any aspect of the Website will constitute your agreement to this Privacy Policy."]
				      " If you cannot agree with the terms and conditions of this Privacy Policy, please do not use the Website.  This Privacy Policy does not cover information collected elsewhere, including without limitation offline and on sites linked to from the Website."]
				     [:p "In addition to reviewing this Privacy Policy, please read our "
				      [:a {:href "http://tabs.fm/user-agreement"} "User Agreement"]
				      ". Your use of the Website constitues agreement to its terms and conditions as well."]
				     [:p "The Privacy Policy may be modified from time to time; the date of the most recent revision will appear on this page, so check back often.  Continued access of the Website by you will constitute your acceptance of any changes or revisions to the privacy Policy."]
				     [:h3 "The Type of Information the Website Collects"]
				     [:h3 "How the Website Uses Information Provided by You"]
				     [:h3 "Cookies"]
				     [:h3 "Information Security and Notification"]
				     [:h3 "Kids and Parents"]
				     [:h3 "Privacy Policy Coordinator"]])))
   "contact"  (struct section
		      "Contact Us"
		      (fn [s p]
			(centered-section
			 "Contact Us"
			 [:p "not implemented yet"])))
   
   "bummer"   (struct section
		      "404.  Say Wha?"
		      (fn [s p]
			(centered-section 
			 "404: There's nothing to see here..."
			 [:p "&#8220;total bummer dude.&#8221;"])))})
