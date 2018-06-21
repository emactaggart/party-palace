(ns party-palace.handler
  (:require [compojure.core :refer [GET POST PUT defroutes context]]
            [compojure.route :refer [not-found resources]]
            [clojure.data.json :as json]
            [party-palace.middleware :refer [wrap-middleware]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.util.response :refer [file-response resource-response redirect content-type]]
            [environ.core :refer [env]]
            [clojure.walk :refer [keywordize-keys]]
            [party-palace.data :as party-data]
            [party-palace.clients.jenkins-client :as jenkins]
            [party-palace.util :as util]
            [party-palace.clients.ci-client :as ci])
  (:use [party-palace.clients.hue-client :as hue]))

(defn calc-mode [state]
  (let [job (get state :job "")
        effect (get state :effect "none")
        colormode (get state :colormode "hs")]
    (cond
        (not-empty job) :build
        (= effect "colorloop") :colorloop
        (= colormode "hs") :hs
        (= colormode "ct") :ct
        :else :hue)))

(defn shwrap-json-body [col]
  {:pre [(map? col)]}
  {:body (json/write-str col)})

(def test-mode (env :dev))

(def all-data (if test-mode
                (clojure.walk/keywordize-keys party-data/data-get-all-lights)
                (hue/get-all)))

(def jenkins-data (if test-mode
                    (party-data/slurp-jobs)
                    (jenkins/get-job-names-and-status)))


(def ci-data (ci/get-light-jobs))

(defn merge-lights-and-jobs [lights jobs]
  (let [duecer (fn [m [k v]] (assoc-in m [k :state :job] v))]
    (reduce duecer lights jobs)))

(def lights-and-jobs-data
  (merge-lights-and-jobs (:lights all-data) ci-data))

(defn merge-lights-and-modes [lights]
  (let [duecer (fn [m [k v]] (assoc-in m [k :state :light-mode] (calc-mode v)))]
    (reduce duecer lights lights)))

(def lights-and-jobs-and-modes-data (merge-lights-and-modes lights-and-jobs-data))


(def all-data-response
  (let [all-data-with-jobs (assoc-in all-data [:lights] lights-and-jobs-and-modes-data)]
    all-data-with-jobs))


(defn update-light [id light-state]
  (let [light-mode (-> light-state
                       :light-mode
                       keyword)
        light-request (case light-mode
                        :build (merge
                                (select-keys light-state [:on  :hue :sat :bri])
                                {:effect "none"})
                        :colorloop (merge
                                    (select-keys light-state [:on :hue :sat :bri])
                                    {:effect "colorloop"})
                        :hs (merge
                             (select-keys light-state [:on :hue :sat :bri])
                             {:effect "none"})
                        :ct (merge
                             (select-keys light-state [:on :ct :sat :bri])
                             {:effect "none"})
                        {})]
    (if (= :build light-mode)
      (ci/set-light-job! id (:job light-state))
      (ci/set-light-job! id ""))
    light-request
    ))


(defn handle-light-update [id light]
  (let [l (update-light id light)]
    (if test-mode
      l
      (hue/set-light l))))

;; (defn wrap-dir-index [handler]
;;   (fn [req]
;;     (handler
;;      (update-in req [:uri]
;;                 #(if (= "/" %) "/index.html" %)))))

(defroutes routes
  (context "/api" []
           (GET "/hello" [] (shwrap-json-body {:hello "hello"}))
           ;; (GET "/lights" [] (shwrap-json-body (hue/get-lights)))
           (GET "/all" [] (shwrap-json-body all-data-response))
           (GET "/lights" [] (shwrap-json-body (:lights all-data-response)))
           (PUT "/lights/:id" [id :as {body :body}]
                (let [light-state (keywordize-keys body)]
                   (handle-light-update id light-state)
                   {:status 200 :body nil}
                  ))
            ;(GET "/jenkins-jobs" []
              ;(
              ;                       -> jenkins-data
              ;                       (#(hash-map :jobs %))
              ;                       shwrap-json-body
              ;                       )
              ;                      (jenkins-data)

                                    )
           (GET "/jenkins-jobs" [] (-> (party-data/slurp-jobs)
                                       shwrap-json-body))
           ;FIXME
           ;(GET "/jenkins-jobs" [] (-> jenkins-data
           ;                            shwrap-json-body))
           )
  ;; (GET "/" [] (-> (resource-response "index.html" {:root "public"})
  ;;                 (content-type "text/html")))
  (GET "/" [] (redirect "/index.html"))
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
