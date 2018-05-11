(ns party-palace.server
  (:require [party-palace.handler :refer [app]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            )
  (:gen-class))

(defn -main [& args]
  (let [pstr (if-not (empty? (env :port))
             (env :port)
             "3000")
        port (Integer/parseInt pstr)]
    (run-jetty app {:port port :join? false})))
