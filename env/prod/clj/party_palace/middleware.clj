(ns party-palace.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(assoc-in site-defaults [:security :session] false)

(get site-defaults :security)


(def less-secure-defaults
  (-> site-defaults
      (assoc :session false)
      (assoc-in [:security :anti-forgery] false)
      ))


(defn wrap-middleware [handler]
  (wrap-defaults handler less-secure-defaults))
