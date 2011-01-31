(ns netlet.content
  (:use [netlet.templates]
	[netlet.util]
	[clj-time.core]
	[clj-time.format]
	[hiccup :only [html h]]))

(def site-title "Netlets")

(def outlet-set (partial sorted-set-by (fn [x y] (< (:outlet x) (:outlet y)))))

(def netlet-model-properties
     {"prototype1" (outlet-set
		    {:outlet 1
		     :switch-type :analog}
		    {:outlet 2
		     :switch-type :analog})
      "prototype2" (outlet-set
		    {:outlet 1
		     :switch-type :analog}
		    {:outlet 2
		     :switch-type :analog}
		    {:outlet 3
		     :switch-type :analog}
		    {:outlet 4
		     :switch-type :analog})
      "build1"     (outlet-set
		    {:outlet 1
		     :switch-type :ssr}
		    {:outlet 2
		     :switch-type :ssr}
		    {:outlet 3
		     :switch-type :analog}
		    {:outlet 4
		     :switch-type :analog})
      "build2"     (outlet-set
		    {:outlet 1
		     :switch-type :ssr}
		    {:outlet 2
		     :switch-type :ssr}
		    {:outlet 3
		     :switch-type :analog}
		    {:outlet 4
		     :switch-type :analog})})

(def netlet-switch-properties
     {:analog       {:min-value 0 :max-value 1}
      :ssr          {:min-value 0 :max-value 255}
      :triac        {:min-value 0 :max-value 255}})


(def lipsum
     "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras luctus ligula et ipsum suscipit ut pharetra metus luctus. Duis vestibulum arcu in diam sollicitudin vulputate. Cras rhoncus consectetur mauris, sit amet molestie nisi volutpat sit amet. Cras dictum, tortor ac auctor feugiat, sapien ante elementum risus, ut placerat mi mauris at quam. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque bibendum eros at lectus congue non sodales lectus auctor. Nulla aliquet tortor id felis placerat tincidunt. Cras et mauris fringilla risus vestibulum tristique a ut eros. Integer sit amet ante sit amet odio laoreet faucibus sed id nisi. Proin lacus lorem, feugiat vel euismod sit amet, facilisis sed quam. In sed nulla quam. Vivamus interdum, mauris id consectetur varius, velit augue fringilla urna, quis dignissim quam est sit amet leo. Fusce est lacus, viverra in dictum in, dapibus vitae sapien. Phasellus eleifend magna eros, ac faucibus arcu. Mauris nisi libero, rutrum laoreet egestas sed, consectetur a dolor. Ut lobortis sapien eget turpis dictum non egestas odio mattis. Duis vehicula faucibus eros id accumsan.")

   

(def widgets
     (list
      (struct-map section
	:title "Welcome to Netlets!"
	:body (fn [s p]
		(html
		 [:p "Netlets put you in control."]
		 [:p "Netlets provide you with power."]
		 [:p "Netlets let you control your power."]))
	:section "overview"
	:auth-level :logged-out)
      (struct-map section
	:title ""
	:body (fn [s p]
		[:img {:src "/images/product-image.png"
		       :width "235"}])
	:section "overview"
	:position :right
	:auth-level :logged-out)
      (struct-map section
	:title "Nerd Facts"
	:body (fn [s p]
		[:ul
		 [:li "Powered by the ARM9260"]
		 [:li "Near-real-time push communication over XMPP."]
		 [:li "Industry-standard SSL ensures end-to-end encryption and security."]                                           [:li "WiFi model supports all standard authentication models: WEP, WPA-PSK, WPA2, WPA2 Enterprise."]])
	:section "overview"
	:position :right
	:auth-level :logged-out)
      (struct-map section
	:title "What can I do with Netlets?"
	:body (fn [s p]
		(html
		 [:ul
		  [:li "Check the power consumption of the devices in your home from anywhere"]
		  [:li "View your power and current usage history and find ``bad'' trends in your energy footprint."]
		  [:li "Virtually ``unplug'' devices when you're not using them."]
		  [:li "Create triggers based on time-of-day or change in current to change the state of any of your Netlet outlets."]
		  [:li "``Unplug'' your monitor, printer, scanner, and speakers when your computer is asleep."]
		  [:li "Dim your lights from your Android or iOS device."]]))
	:section "overview"
	:auth-level :logged-out)
		  
      (struct-map section
	:title (html "My Netlets " [:a {:href "/add-netlet"} "[+]"])
	:body (fn [session params]
		(let
		    [user (@*users-map* (session :username))
		     netlets (:netlets user)]
		  (html
		   (if netlets
		     [:ul
		      (map (fn [x] [:li 
				    [:h4 (h (:name x)) " " [:a {:href (str "/configure-netlet/" (md5-sum (:name x)))} "[edit]"]]
				    [:ul
				     (map (fn [y]
					    [:li
					     [:span (h (:outlet-name (second y)))
					      " "
					      [:a {:href (str "/triggers/" (md5-sum (:name x)) "/" (md5-sum (:outlet-name (second y))))} "[triggers]"]]
					     [:form {:id (str (md5-sum (str (:name x) (:outlet-name (second y)))) "-submit") :class "outlet" :method "post" :action "/set-outlet"}
					      [:select {:id (md5-sum (str (:name x) (:outlet-name (second y)))) :name "newvalue" }
					       (let [switch-props (netlet-switch-properties (:switch-type (second y)))
						     min-val (:min-value switch-props)
						     max-val (:max-value switch-props)]
						 (for [opt-val (range min-val (inc max-val))]
						   (if (= opt-val (:value (second y)))
						     [:option {:value (str opt-val) :selected "selected"}
						      (if (= (:switch-type (second y)) :analog)
							(if (= opt-val 0)
							  "Off"
							  "On")
							(str opt-val))]
						     [:option {:value (str opt-val)}
						      (if (= (:switch-type (second y)) :analog)
							(if (= opt-val 0)
							  "Off"
							  "On")
							(str opt-val))])))]
					      [:input {:type "hidden" :name "netlet" :value (md5-sum (:name x))}]
					      [:input {:type "hidden" :name "outlet" :value (h (str (first y)))}]
					      [:input {:type "submit" :class "update" :value "Update"}]]
						       ])
					  (reverse (seq (:config x))))]
				    [:br]])  (sort-by :name netlets))]
		     [:p.alt "You have no devices set up."]))))
	:section "overview"
	:auth-level :user)
      (struct-map section
	:title "Power Usage Ticker"
	:body (fn [session params]
		[:div#chart-container
		 ])
	:section "overview"
	:auth-level :user)
      (struct-map section
	:title  "Recent Triggers"
	:body (fn [s p]
		[:p (str (try (Class/forName "javax.net.SocketFactory") (catch Exception e "no sockets")))])
	:section "overview"
	:position :right
	:auth-level :user)
      (struct-map section
	:title (fn [] "Power Usage History" )
	:long-tile ""
	:body (fn [s p] [:div#power-chart-container {:style 
						     (if (not= (p "subpage") "power")
						       "width: 394px; height: 200px;"
						       "width: 674px; height: 300px")}
			 (if (not= (p "subpage") "power")
			   [:img {:src "/charts/power.png?size=ss"}]
			   [:img {:src "/charts/power.png"}])])
	:section "charts"
	:subsection #{"overview" "power"})
      (struct-map section
	:title "Current Draw History"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"overview" "current"})
      (struct-map section
	:title "Apparent Power History"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"overview" "ap"}
	:position :left)
      (struct-map section
	:title "Device Breakdown"
	:body (fn [s p] [:div])
	:section "charts"
	:subsection #{"overview" "devices"}
	:position {"overview" :right
		   "devices" :left})
      (struct-map section
	:title "Debug"
	:body (fn [session params]
		(html
		 [:p (str session)]
		 [:p (if (params "password")
		       (str (assoc params "password" "******"))
		       (str params))]))
	:position :right
	:auth-level :admin)))

(def sections {})
	       
(def sections
  {"overview"     (struct section
		      ""
		      (fn [session params]
			(widget-section session params widgets)))
     "charts"   (struct-map section
		:title "Charts"
		:body (fn [session params]
			(tabbed-section session params 
					(list
					 (struct section "Overview" "overview")
					 (struct section "Current" "current")
					 (struct section "Power" "power")
					 (struct section "Apparent Power" "ap")
					 (struct section "Devices" "devices"))
					((var sections) "charts")))
		:subsections
		(let [calendar-description-fn (fn [s p]
						(let [dt (now)
						      pastsun (minus dt (days (day-of-week dt)))
						      pastsun2 (minus pastsun (weeks 4))
						      fmt (formatter "dd-MM-YYYY")
						      date1       (html
								   (get-calendar-html "startdate"
										      (unparse fmt pastsun2)))
						      date2       (html
								   (get-calendar-html "enddate"
										      (unparse fmt pastsun)))]
						 (html
						  [:form {:method "POST" :action "#"}
						   [:formset
						    [:label {:for "startdate"} "From "]
						    date1
						    [:label {:for "enddate"} " to "]
						    date2]])))
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
				:long-title "Weekly Current, Power, and AP Charts"
				:description overview-description-fn
				:body (fn [session params] 
					(widget-subsection
					 session
					 params widgets)))
		   "current"   (struct-map section
				:title "Current Draw History"
				:description calendar-description-fn
				:body (fn [session params] 
					(widget-subsection-full
					 session
					 params widgets)))
		   "power"       (struct-map section
				   :title "Power Usage History"
				   :description calendar-description-fn
				   :body (fn [session params] 
					   (widget-subsection-full
					    session
					    params widgets)))
		   "ap"  (struct-map section
				   :title "Apparent Power History"
				   :description calendar-description-fn
				   :body  (fn [session params] 
					   (widget-subsection-full
					    session
					    params widgets)))
		   "devices"       (struct-map section
				   :title "Device Breakdown"
				   :description calendar-description-fn
				   :body (fn [session params] 
					   (widget-subsection-full
					    session
					    params widgets)))}))
   "admin"    (struct section
		      "Admin"
		      (fn [s p]
			(full-section
			 [:p (map str @*users-map*)])))
   "configure-netlet" (struct section
		       "Configure"
		       (fn [s p]
			 (let [userdata (@*users-map* (s :username))
			       usernetlets (:netlets userdata)
			       netlet (first (filter (fn [x] (= (md5-sum (:name x)) (p "namehash"))) usernetlets))]
			   
			 (centered-section
			  (html "Configure Netlet " 			   [:a {:href (str "/delete-netlet/" (p "namehash"))} "[-]"])
			  [:div.prepend-6
			   [:h3 (h (:name netlet))]
			   [:form {:method "post" :action "/configure-netlet"}
			    [:input {:name "name" :type "hidden" :size 30 :value (h (:name netlet))}]
			    [:p
			     [:label {:for "xmppid"} "XMPP ID:"]
			     [:br]
			     [:input {:name "xmppid" :type "text" :size 30 :value (h (:xmpp netlet))}]]
			    (map (fn [y]
				   (if (set? (:outlet y))
				     (map (fn [z]
					    [:p
					     [:label {:for (str (h z))} (str "Outlet " (h z) " / " (:switch-type y) ":")]
					     [:br]
					     [:input {:name (str (h z)) :type "text"
						      :value (if (:config netlet)
							       (h (:outlet-name ((:config netlet) z)))
							       "")
						      :size 30}]])
					  (:outlet y))
				     [:p
				      [:label {:for (str (h (:outlet y)))} (str "Outlet " (h (:outlet y)) " / " (:switch-type y) ":")]
				      [:br]
				      [:input {:name (str (h (:outlet y))) :type "text"
					       :value (if (:config netlet)
							(h (:outlet-name ((:config netlet) (:outlet y))))
							"")
					       :size 30}]]))
				 (netlet-model-properties (:model netlet)))
			    [:input {:type "hidden" :name "namehash" :value (p "namehash")}]
			    [:input {:type "submit" :value "Save"}]]]))))
   "triggers" (struct-map section
		      :title "Outlet Triggers"
		      :auth-level :user
		      :body (fn [s p]
			      (let [userdata (@*users-map* (s :username))
				    usernetlets (:netlets userdata)
				    netlet (first (filter (fn [x] (= (md5-sum (:name x)) (p "namehash"))) usernetlets))
				    netletname (:name netlet)
				    configs (vals (:config netlet))
				    outlet (first (filter (fn [x] (= (md5-sum (:outlet-name x)) (p "outlethash"))) configs))
				    outletnum (first (first (filter (fn [x] (= (md5-sum (:outlet-name (second x))) (p "outlethash"))) (seq (:config netlet)))))
				    outletname (:outlet-name outlet)]
				(centered-section
				 (html (str (h outletname) " Triggers ")
				       [:a {:href (str "/add-trigger/"
						       (p "namehash")
						       "/"
						       (p "outlethash")) } "[+]"])
				 [:div.prepend-1
				  [:ul
				   (for [trigger  (filter (fn [x] (and
								   (= (:netlet x) (p "namehash"))
								   (= (:outlet x) outletnum)))
							  (:triggers userdata))]
				     (let [triggernetlet (first (filter (fn [x] (= (md5-sum (:name x)) (:trigger-netlet trigger))) usernetlets))
					   triggernname (:name triggernetlet)
					   triggeroutlet ((:config triggernetlet) (:trigger-outlet trigger))
					   triggeroname (:outlet-name triggeroutlet)]
				       [:li 
					"Set this outlet to "
					(if (> (:newval trigger) 0) [:strong "on"] [:strong "off"])
					" when "
					[:strong triggernname]
					"/"
					[:strong triggeroname]
					" spikes "
					(if (= :rising (:edge trigger)) [:strong "up"] [:strong "down"])
					". "
					[:a {:href (str "/delete-trigger/" (md5-sum (str (:netlet trigger)
										       (:outlet trigger)
										       (:trigger-netlet trigger)
										       (:trigger-outlet trigger)
										       (:edge trigger))))} "[-]"]]))]]))))
   "add-trigger" (struct-map section
		   :title "New Outlet Trigger"
		   :auth-level :user
		   :body (fn [s p]
			      (let [userdata (@*users-map* (s :username))
				    usernetlets (:netlets userdata)
				    netlet (first (filter (fn [x] (= (md5-sum (:name x)) (p "namehash"))) usernetlets))
				    netletname (:name netlet)
				    configs (vals (:config netlet))
				    outlet (first (filter (fn [x] (= (md5-sum (:outlet-name x)) (p "outlethash"))) configs))
				    outletname (:outlet-name outlet)]
				(centered-section
				 (str "New " (h outletname) " Trigger")
				 [:div.prepend-1
				  [:form {:method "post" :action "/add-trigger"}
				   [:p
				    "Set this outlet to "
				    [:select {:name "onoff"}
				     [:option {:value "on"} "on"]
				     [:option {:value "off"} "off"]]
				    " when "
				    [:select {:name "outlet"}
				     
				       (let [othernetlets (filter
							   (fn [ntl]
							     (not (empty?
								   (filter
								    (fn [otl]
								      (not= (:outlet-name otl) outletname))
								    (vals (:config ntl))))))
							   usernetlets)]
					 (for [anetlet othernetlets]
					   (html
					    [:optgroup {:label (:name anetlet)}
					     (for [aoutlet (sort (filter
								  (fn [x]
								    (not= x outletname))
								  (map :outlet-name (vals (:config anetlet)))))]
					       (html
						[:option {:value (str (md5-sum (:name anetlet)) (md5-sum aoutlet))} aoutlet]))])))]
				    " turns "
				    [:select {:name "triggeronoff"}
				     [:option {:value "on"} "on"]
				     [:option {:value "off"} "off"]]
				    "."
				    ]
				   [:p
				    [:input {:name "namehash"
					     :type "hidden"
					     :value (p "namehash")}]
				    [:input {:name "outlethash"
					     :type "hidden"
					     :value (p "outlethash")}]
				    [:input {:name "submit"
					     :type "submit"
					     :value "Add Trigger"}]]]]))))
   "login"    (struct section
		      "Login"
		      (fn [s p]
			(centered-section
			 "Come on in!"
			 [:div.prepend-6
			  [:form {:method "post" :action "/login"}
			   [:p
			    [:label {:for "name"} "Username / E-mail:"]
			    [:br]
			    [:input {:name "name" :type "text" :size 30}]]
			   [:p
			    [:label {:for "password"} "Password:"]
			    [:br]
			    [:input {:name "password" :type "password" :size 30 }]]
			   [:p
			    [:input {:type "submit" :value "Login"}]
			    "  "
			    [:a {:href "/register"} "Need an account?"]
			    "."]]])))
   "register" (struct section
		      "Register"
		      (fn [s p]
			(centered-section
			 "Join now!"
			 [:div.prepend-6
			  [:form {:method "post" :action "/register"}
			   [:p
			    [:label {:for "name"} "Username / E-mail:"]
			    [:br]
			    [:input {:name "name" :type "text" :size 30}]]
			   [:p 
			    [:label {:for "password"} "Password:"]
			    [:br]
			    [:input {:name "password" :type "password" :size 30}]]
			   [:p
			    [:input {:type "submit" :value "Register"}]]]])))
   "add-netlet" (struct section
			"Add Netlet"
			(fn [s p]
			  (centered-section
			   "Add a new Netlet to your account."
			   [:div.prepend-6
			    [:form {:method "post" :action "/add-netlet"}
			     [:p
			      [:label {:for "name"} "Netlet Name:"]
			      [:br]
			      [:input {:name "name" :type "text" :size 30}]]
			     [:p
			      [:label {:for "model"} "Netlet Model:"]
			      [:br]
			      [:select {:name "model"}
			       [:option {:value "prototype1"} "Prototype / 2 Outlets"]
			       [:option {:value "prototype2"} "Prototype / 4 Outlets"]
			       [:option {:value "build1"} "Netlet Home"]
			       [:option {:value "build2"} "Netlet Air"]]]
			     [:p
			      [:input {:type "submit" :value "Create"}]]]])))
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
					   [:div.viewmore
					     [:a.prev {:href "prev"} "Previous 25"]
					     [:a.next {:href "next"} "Next 25"]]))})
					       ;; check if user is in DS
   "user-agreement"      (struct section
				 "User Agreement"
				 (fn [s p]
				   (full-section
				    [:div.box 
				     [:h2 "User Agreement"]])))
   "privacy-policy"      (struct section
				 "Privacy Policy"
				 (fn [s p]
				   (full-section
				    [:div.box
				     [:h2 "Privacy Policy"]])))
   "contact"  (struct section
		      "Contact Us"
		      (fn [s p]
			(centered-section
			 "Contact Us"
			 [:p ""])))
   
   "bummer"   (struct section
		      "404.  Say Wha?"
		      (fn [s p]
			(centered-section 
			 "404: There's nothing to see here..."
			 [:p "&#8220;total bummer dude.&#8221;"])))})
