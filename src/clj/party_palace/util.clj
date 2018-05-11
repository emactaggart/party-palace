(ns party-palace.util
  (:require [clj-http.client :as http]
            [clojure.core.async :as async :refer [chan go <! >! go-loop timeout alt!]]
            [clojure.data.json :as json])
  (:import [java.net ConnectException SocketTimeoutException UnknownHostException]))


(def request-socket-timeout-ms 1000)
(def request-connection-timeout-ms 1000)

(defn valmap [f m]
  (reduce
   (fn [a [k v]] (conj a [k (f v)]))
   {}
   m))

(defn set-interval
  [f ms]
  (let [stop (chan)]
    (go-loop []
      (alt!
        (timeout ms) (do (f)
                         (recur))
        stop :stop))
    stop))

(defn set-timeout
  [f ms]
  (let [stop (chan)]
    (go
      (alt!
        (timeout ms) (f)
        stop :stop))
    stop))

(defn safe-json-read [json-str]
  (try
    (json/read-str json-str)
    (catch Exception e
      ;;TODO (some-logging)
      {})))

(defn- wrap-try-http-request [f & args]
  (try
    (apply f args)
    (catch ConnectException e {})
    (catch SocketTimeoutException e {})
    (catch UnknownHostException e {})))

(defn- request-wrapper
  ([req-method url body]
   (let [request (fn [url body] (req-method url {:socket-timeout request-socket-timeout-ms
                                                 :conn-timeout request-connection-timeout-ms
                                                 :body (json/write-str body)}))
         safe-request (fn [] (wrap-try-http-request request url body))]
     (-> (safe-request)
         (get :body (json/write-str {}))
         safe-json-read)))
  ([req-method url]
   (request-wrapper req-method url {})))

(defn get-request [url] (request-wrapper http/get url))

(defn put-request [url body] (request-wrapper http/put url body))

(defn post-request [url body] (request-wrapper http/post url body))


;; (defn- put-request [url body] (-> (http/put url {:socket-timeout request-socket-timeout-ms
;;                                                  :conn-timeout request-connection-timeout-ms
;;                                                  :body (json/write-str body)})
;;                                   (get :body)
;;                                   json/read-str))


;; (defn- post-request [url body] (-> (http/post url {:socket-timeout request-socket-timeout-ms
;;                                                    :conn-timeout request-connection-timeout-ms
;;                                                    :body (json/write-str body)})
;;                                    (get :body)
;;                                    json/read-str))

(defn safe-slurp [filename]
  (let [default-slurp nil]
      (try
        (slurp filename)
        (catch java.io.FileNotFoundException e
          ;;TODO (some-logging)
          default-slurp)
        (catch IllegalArgumentException e
          ;;TODO (some-logging)
          default-slurp))))

(defn safe-spit [filename contents]
  (try
    (spit filename contents)
    (catch java.io.FileNotFoundException e
      ;;TODO (some-logging)
      nil)))
