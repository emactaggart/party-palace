(ns party-palace.handler
  (:require [compojure.core :refer [GET POST PUT defroutes context]]
            [compojure.route :refer [not-found resources]]
            [clojure.data.json :as json]
            [party-palace.middleware :refer [wrap-middleware]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.util.response :refer [file-response resource-response redirect content-type]]
            [clojure.walk :refer [keywordize-keys]]
            [party-palace.data-service :as data]))

(defn shwrap-json-body [col]
  {:pre [(map? col)]}
  {:body (json/write-str col)})

;; (defn wrap-dir-index [handler]
;;   (fn [req]
;;     (handler
;;      (update-in req [:uri]
;;                 #(if (= "/" %) "/index.html" %)))))

(defroutes routes
           (context "/api" []
             (GET "/hello" [] (shwrap-json-body {:hello "hello"}))
             (GET "/all" [] (shwrap-json-body data/all-data-response))
             (GET "/lights" [] (shwrap-json-body (:lights data/all-data-response)))
             (PUT "/lights/:id" [id :as {body :body}]
               (let [light-state (keywordize-keys body)]
                 (data/handle-light-update id light-state)
                 {:status 200 :body nil}))
             ; TODO handle in data service
             (GET "/jenkins-jobs" [] (shwrap-json-body data/jenkins-data)))
           ;; (GET "/" [] (-> (resource-response "index.html" {:root "public"})
           ;;                 (content-type "text/html")))
           (GET "/" [] (redirect "/index.html"))
           (resources "/")
           (not-found "Not Found"))

(def app (wrap-middleware #'routes))
