(ns party-palace.clients.jenkins-client
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [party-palace.util :refer [get-request]]
            [environ.core :refer [env]]))

(def jenkins-ball-colors [:red :red_anime
                          :yellow :yellow_anime
                          :blue :blue_anime
                          :grey :grey_anime
                          :disabled :disabled_anime
                          :aborted :aborted_anime
                          :notbuilt :notbuilt_anime])

(def ci-server (env :ci-server-uri))

(defn- get-jenkins [] (get-request (str ci-server "/api/json")))

(defn- get-view-details [name] (get-request (str ci-server "/view/" name "/api/json")))

(defn- get-job-details [name] (get-request (str ci-server "/job/" name "/api/json")))

(defn- get-views [] (get (get-jenkins) "views"))

(defn- get-gross-jobs [] (get (get-jenkins) "jobs"))

(defn- find-by-name [v name] (first (filter #(= (get % "name") name) v)))

(defn- get-color [name] (let [job (find-by-name (get-gross-jobs) name)]
                         (get job "color")))

(defn- get-jobs [s]
  (let [c #(select-keys % [:name
                           ;; :url
                           :color])]
    (-> s
        clojure.walk/keywordize-keys
        (#(map c %))
        )))

(defn get-job-names-and-status []
  (get-jobs (get-gross-jobs)))

(defn get-job-color-by-name [jobs name]
  (->> jobs
       (filter #(= name (:name %)))
       first
       :color))

