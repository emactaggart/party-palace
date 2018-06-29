(ns party-palace.data-service
  (:require [clojure.data.json :as json]
            [ring.util.response :refer [file-response resource-response redirect content-type]]
            [environ.core :refer [env]]
            [clojure.walk :refer [keywordize-keys]]
            [party-palace.test-data :as party-data]
            [party-palace.clients.jenkins-client :as jenkins]
            [party-palace.util :as util]
            [party-palace.clients.ci-client :as ci])
  (:use [party-palace.clients.hue-client :as hue]))

(def test-mode (env :dev))

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
                                 (select-keys light-state [:on :hue :sat :bri])
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
      (hue/set-light id l))))

