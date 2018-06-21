(ns party-palace.job-scheduler
  (:require [party-palace.clients.ci-client :as ci]
            [party-palace.clients.hue-client :as hue]
            [party-palace.clients.jenkins-client :as jenkins]
            [clojure.core.async :as async :refer [chan go <! >! go-loop timeout alt!]]
            [party-palace.util :refer [set-timeout set-interval]]))

(defn- status->hue-color
  [status]
  (case (keyword status)
    :blue hue/green
    :blue_anime hue/green
    :red hue/red
    :red_anime hue/red
    hue/blue))


(defn- jenkins-loop []
  (let [hue-jobs (ci/get-light-jobs)
        job-list (jenkins/get-job-names-and-status)]
     (for [[light-id job-name] hue-jobs]
       (if (not (empty? job-name))
         (let [status (jenkins/get-job-color-by-name job-list job-name)
               color (status->hue-color status)]
           (if status
             (hue/set-light
              (name light-id)
              (-> {}
                  (hue/set-sat 254)
                  (hue/set-hue color)))
             )
           [light-id job-name color])
         [light-id job-name nil]))))

(defn run-jenkins-loop []
  (set-interval jenkins-loop 5000))
