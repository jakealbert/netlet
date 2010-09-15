(ns local-dev
  "tools for local development.
   enables app engine API on the repl and in jetty instance"
  (:use ring.adapter.jetty
	[ring.middleware file file-info])
  (:import [java.io File]
	   [java.util HashMap]
	   [com.google.apphosting.api ApiProxy ApiProxy$Environment]
	   [com.google.appengine.tools.development ApiProxyLocalFactory 
	    LocalServerEnvironment]))

(defonce *server* (atom nil))
(def *port* 8080)

(defn- set-app-engine-environment []
  "sets up the app engine environment for the current thread."
  (let [att (HashMap. {"com.google.appengine.server_url_key"
			(str "http://localhost:" *port*)})
	env-proxy (proxy [ApiProxy$Environment] []
		    (isLoggledIn [] false)
		    (getRequestNamespace [] "")
		    (getDefaultNamespace [] "")
		    (getAttributes [] att)
		    (getAppId [] "_local_"))]
    (ApiProxy/setEnvironmentForCurrentThread env-proxy)))

(defn- set-app-engine-delegate [dir]
  "initialize app engine services.  run once per jvm"
  (let [local-env (proxy [LocalServerEnvironment] []
		    (getAppDir [] (File. dir))
		    (getAddress [] "localhost")
		    (getPort [] *port*)
		    (waitForServerToStart [] nil))
	api-proxy (.create (ApiProxyLocalFactory.) local-env)]
    (ApiProxy/setDelegate api-proxy)))

(defn init-app-engine
  "initializes the app-engine services"
  ([] (init-app-engine "/tmp"))
  ([dir]
     (set-app-engine-delegate dir)
     (set-app-engine-environment)))

(defn wrap-local-app-engine [app]
  "wraps a ring app to enable AE services"
  (fn [req]
    (set-app-engine-environment)
    (app req)))


(defn start-server [app]
  "Initializes AE services and (re)starts a jetty server instance. 
   wraps with the AE API"
  (set-app-engine-delegate "/tmp")
  (swap! *server* (fn [instance]
		    (when instance
		      (.stop instance))
		    (let [app (-> app
				  (wrap-local-app-engine)
				  (wrap-file "./war")
				  (wrap-file-info))]
		      (run-jetty app {:port *port*
				      :join? false})))))

(defn stop-server []
  "stops the local jetty server"
  (swap! *server* #(when % (.stop %))))
