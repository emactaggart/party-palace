(ns party-palace.prod
  (:require [party-palace.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
