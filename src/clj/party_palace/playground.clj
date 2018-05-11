(ns party-palace.playground
  (:require
   [party-palace.data :as data]
   [party-palace.hue-client :as hue]
   [party-palace.jenkins-client :as jenkins]
   [party-palace.ci-client :as ci]
   [party-palace.util :as u]
   [party-palace.handler :as hand]
   [clj-http.client :as http]
   [clojure.core.async :as a]
   ))


;; (ci/get-light-jobs)
;; (hue/get-lights)
;; ()
;; (hand/test-lights-response)


 ;; (let [lights hand/test-lights-response
 ;;       jobs (ci/get-light-jobs)
 ;;       job-maps (valmap #(hash-map :jobs %) jobs)]
 ;;   (->
 ;;    (merge-with merge lights job-maps)))
