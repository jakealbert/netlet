(ns netlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use [netlet.response :only [success not-found]]
	[netlet.templates :only [layout]]
	[netlet.content :only [site-title sections]]
	[netlet.util :only [section *users-map* flatten]]
	[hiccup :only [html]]
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
       subsections (:subsections section)
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
	  newuserdata (if usernetlets
			(assoc userdata :netlets (cons {:name netletname :model netletmodel} usernetlets))
			(assoc userdata :netlets (cons {:name netletname :model netletmodel} #{})))]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect "/overview")))
    (redirect "/login")))

(defn delete-netlet
  [session params]
  (if (and (session :username)
	   (not (= (params "namehash") "")))
    (let [userdata (@*users-map* (session :username))
	  usernetlets (:netlets userdata)
	  filterednetlets (filter (fn [x] (not (= (md5-sum (:name x))
						  (params "namehash"))))
				  usernetlets)
	  newuserdata (assoc userdata :netlets filterednetlets)]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect "/overview")))
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
	  newoutlet (assoc outlet :value newvalue)
	  newconf (assoc config outletnum newoutlet)
	  newnetlet (assoc netlet :config newconf)
	  newnetlets (cons newnetlet (filter (fn [x] (not (= (md5-sum (:name x)) (params "netlet")))) usernetlets))
	  newuserdata (assoc userdata :netlets newnetlets)]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
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
	  confmap (zipmap outlets (map (fn [x] {:outlet-name (params (str x)) :value 0 :switch-type (outlet-type x)}) outlets))
	  newnetlet (if (empty? confmap)
		      (dissoc netlet :config)
		      (assoc netlet :config confmap))
	  newnetlet (assoc newnetlet :name (params "name"))
	  newusernetlets (cons newnetlet (filter (fn [x] (not (= (md5-sum (:name x)) (params "namehash")))) usernetlets))
	  newuserdata (assoc userdata :netlets newusernetlets)]
      (dosync
       (alter *users-map* assoc (session :username) newuserdata)
       (redirect "/overview")))
    (redirect "/login")))
	  

(defn register
  [session params]
  (let [user (params "name")
	password (params "password")]
    (if (@*users-map* user)
      (if (= (:passhash (@*users-map* user)) (md5-sum password))
	(authorize session params)
	(redirect "/register"))
      (dosync
       (alter *users-map* assoc user {:passhash (md5-sum password)})
       (authorize session params)))))
    

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
	(index (assoc session :username (params "name")
		      :auth-level (if (= (params "name") "jake.albert@gmail.com")
				    :admin
				    :user))
	       (assoc params "page" "overview")))
      (redirect "/login"))))

(defroutes admin-routes
  (GET "/admin/*" {session :session params :params}
       (index session params)))

(defroutes public-routes
  (GET "/" {session :session params :params}
       (index session (assoc params "page" "overview")))
  (POST "/set-outlet" {session :session params :params}
	(set-outlet session params))
  (POST "/configure-netlet" {session :session params :params}
	(configure-netlet session params))
  (GET "/configure-netlet/:namehash" {session :session params :params}
       (authorize session (assoc params "page" "configure-netlet")))
  (GET "/delete-netlet/:namehash" {session :session params :params}
       (delete-netlet session params))
  (POST "/add-netlet" {session :session params :params}
	(create-netlet session params))
  (GET "/users/:userpage/:subpage" {session :session params :params}
       (index session (assoc params "page" "users")))
  (GET "/users/:userpage" {session :session params :params}
       (index session (assoc params "page" "users")))
  (GET "/:page" {session :session params :params}
       (index session params))
  (GET "/:page/:subpage" {session :session params :params}
       (index session params)))

(defroutes auth-routes
  (POST "/register" {session :session params :params}
	(register session params))
  (GET "/logout" {session :session params :params}
       (index (dissoc (dissoc session :username) :auth-level) (assoc params "page" "logout")))
  (POST "/login" {session :session params :params}
       (authorize session params))
  (GET "/login" {session :session params :params}
       (if (or (:username session)
	       (:auth-level session))
	 (redirect "/overview")
	 (index session (assoc params "page" "login")))))


(defroutes app
  auth-routes
  public-routes
  admin-routes
  (GET "/*" {session :session params :params}
       (index session (assoc params "page" "bummer"))))

(wrap! app
       :session)

(defservice app)

