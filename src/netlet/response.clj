(ns netlet.response)
  
(defn respond
  ([status body]
     {:status status :headers {"Content-Type" "text/html"} :body body})
  ([status body session]
     {:status status :headers {"Content-Type" "text/html"} :body body :session session}))


(def success
     (partial respond 200))

(def not-found
     (partial respond 404))