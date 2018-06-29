(ns party-palace.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery *anti-forgery-token*]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.json :refer [wrap-json-body]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]))

(def no-xsrf-site-defaults (assoc-in site-defaults [:security :anti-forgery] false))

;; TODO figure out xsrf
;; (def wrap-csrf-in-cookie [handler]
;;   (fn [request]
;;     ()))


(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults no-xsrf-site-defaults)
      ;; (wrap-defaults site-defaults)
      ;; (fn [request] (do (.println System/out request) request))
      ;; (fn [request] (do (println request) request))
      ;; wrap-anti-forgery

      ;; (fn [handler] (do (println *anti-forgery-token*)
      ;;                   (fn [request] (handler request))))
      ;; wrap-session
      wrap-json-body
      wrap-exceptions
      wrap-reload
      ))
