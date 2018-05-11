(ns ^:figwheel-no-load party-palace.dev
  (:require
    [party-palace.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
