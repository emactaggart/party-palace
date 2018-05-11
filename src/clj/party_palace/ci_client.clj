(ns party-palace.ci-client
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [party-palace.util :as util]))

(def ^:private jobs-file (env :jobs-file))

(def ^:private default-light-jobs {:1 "" :2 "" :3 "" :4 "" :5 ""})

(defn get-light-jobs []
  (let [jobs-json (util/safe-slurp jobs-file)]
    (if jobs-json
      (util/safe-json-read jobs-json)
      default-light-jobs)))

(defn reset-jobs! []
  (util/safe-spit jobs-file
                  (json/write-str default-light-jobs)))

(defn set-light-job! [id name]
  {:pre [integer? id
         string? name]}
  (let [builds (merge default-light-jobs (get-light-jobs))
        updated-builds (assoc-in builds [(keyword (str id))] name)
        json-builds (json/write-str updated-builds)]
    (util/safe-spit jobs-file json-builds)))
