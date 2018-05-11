(ns party-palace.hue-client
  (:require [party-palace.util
             :refer [get-request put-request post-request]]
            [environ.core :refer [env]]))

(def da-boys-zone-id (java.time.ZoneId/of "America/Regina"))
(def da-boys-date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss"))

(def red 0)
(def yellow 12750)
(def green 25500)
(def blue 46920)
(def pink 56100)

(def initech 2)
(def scorpio 4)
(def wolfpack 3)
(def chaosmonkeys 1)
(def admins 5)

(def ct-min 153)
(def ct-max 500)
(def bri-min 0)
(def bri-max 254)
(def hue-min 0)
(def hue-max 65535)
(def sat-min 0)
(def sat-max 254)
(def effect-none "none")
(def effect-loop "colorloop")
(def effect-strobe "strobe")
(def alert-none "none")
(def alert-select "select")
(def alert-lselect "lselect")
(def transition-time-min 0)
(def transition-time-max 65534)

(def default-schedule {})

;; TODO
;; SCHEDULE API
;; SCHENE API``
;; EFFECTS / ANIMATIONS
;; AMOUNT OF CALLS TIMES TRANSITION TIME

(def hue-api (env :hue-api-uri))

(def default-state {:on true
                    :effect "none"})

(def color-loop-state {:on true
                       :sat 254
                       :effect "colorloop"})

(defn- bound [min-val max-val] (fn [val] (min max-val (max min-val val))))

(def ^:private ct-bound (bound ct-min ct-max))
(def ^:private hue-bound (bound hue-min hue-max))
(def ^:private sat-bound (bound sat-min sat-max))
(def ^:private bri-bound (bound bri-min bri-max))
(def ^:private tt-bound (bound transition-time-min transition-time-max))

(defn set-ct [state ct] (assoc state :ct (ct-bound ct)))
(defn set-sat [state sat] (assoc state :sat (sat-bound sat)))
(defn set-hue [state hue] (assoc state :hue (hue-bound hue)))
(defn set-bri [state bri] (assoc state :bri (bri-bound bri)))
(defn set-tt [state tt] (assoc state :transitiontime (tt-bound tt)))
(defn set-colorloop [state loop?] (assoc state :effect (if loop? effect-loop effect-none)))
(defn set-on [state on?] (assoc state :on on?))
(defn set-alert [state] (assoc state :alert alert-select))
(defn set-alert-long [state] (assoc state :alert alert-lselect))
(defn set-alert-off [state] (assoc state :alert alert-none))


(defn get-all [] (get-request hue-api))

(defn get-sensor [id] (let [url (str hue-api "/sensors/" id)]
                        (get-request url)))

(defn get-light [id] (let [url (str hue-api "/lights/" id)]
                       (get-request url)))


(defn set-light [id state] (let [url (str hue-api "/lights/" id "/state")]
                             (put-request url state)))

(defn get-lights [] (let [url (str hue-api "/lights")]
                      (get-request url)))

(defn get-group [id] (let [url (str hue-api "/groups/" id)]
                       (get-request url)))

(defn set-group [id state] (let [url (str hue-api "/groups/" id "/action")]
                             (put-request url state)))

(defn get-all-schedules [] (let [url (str hue-api "/schedules")]
                             (get-request url)))

(defn get-schedule [id] (let [url (str hue-api "/schedules/" id)]
                          (get-request url)))

(defn update-schedule [id schedule] (let [url (str hue-api "/schedules/" id)]
                                      (put-request url schedule)))

(defn create-schedule [schedule] (let [url (str hue-api "/schedules")]
                                   (post-request url schedule)))

(defn get-config [] (let [url (str hue-api "/config")]
                      (get-request url)))

(defn set-config [config] (let [url (str hue-api "/config")]
                            (put-request url config)))

(defn set-config-utc-time [time-str] (let [url (str hue-api "/config")]
                                       (put-request url {:UTC time-str})))

(defn set-time-now [] (let [url (str hue-api "/config")
                            right-now (java.time.LocalDateTime/now da-boys-zone-id)
                            right-now-formatted (format right-now da-boys-date-formatter)]
                        (print right-now right-now-formatted)
                        (set-config-utc-time right-now-formatted)))

(def formatter java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME)
(def formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss"))

(def right-now (java.time.LocalDateTime/now da-boys-zone-id))
(def right-now-formatted (. right-now (format formatter)))

;; right-now-formatted

;; BEGIN_SAMPLE_CONFIG
;; {
;;     "name": "Philips hue",
;;     "zigbeechannel": 20,
;;     "bridgeid": "001788FFFE213069",
;;     "mac": "00:17:88:21:30:69",
;;     "dhcp": true,
;;     "ipaddress": "169.254.5.88",
;;     "netmask": "255.255.0.0",
;;     "gateway": "0.0.0.0",
;;     "proxyaddress": "none",
;;     "proxyport": 0,
;;     "UTC": "1970-01-12T06:46:08",
;;     "localtime": "1970-01-12T00:46:08",
;;     "timezone": "America/Regina",
;;     "modelid": "BSB002",
;;     "swversion": "01028090",
;;     "apiversion": "1.10.0",
;;     "swupdate": {
;;         "updatestate": 0,
;;         "checkforupdate": false,
;;         "devicetypes": {
;;             "bridge": false,
;;             "lights": [],
;;             "sensors": []
;;         },
;;         "url": "",
;;         "text": "",
;;         "notify": false
;;     },
;;     "linkbutton": false,
;;     "portalservices": true,
;;     "portalconnection": "disconnected",
;;     "portalstate": {
;;         "signedon": false,
;;         "incoming": false,
;;         "outgoing": false,
;;         "communication": "disconnected"
;;     },
;;     "factorynew": false,
;;     "replacesbridgeid": null,
;;     "backup": {
;;         "status": "idle",
;;         "errorcode": 0
;;     },
;;     "whitelist": {
;;         "ffffffffc932820648d0764348d07643": {
;;             "last use date": "2015-11-14T18:35:20",
;;             "create date": "2015-11-14T17:47:28",
;;             "name": "Hue#Samsung SM-G925W8"
;;         },
;;         "lightbuild": {
;;             "last use date": "1970-01-12T06:46:08",
;;             "create date": "2015-11-14T18:54:57",
;;             "name": "lightbuild"
;;         }
;;     }
;; }
;; END_SAMPLE_CONFIG


;; BEGIN_SAMPLE_SCHEDULE
;; {
;; "name": "Recurring Party Time",
;; "description": "Initech Party Time",
;; "command": {
;;             "address": "/api/lightbuild/lights/2/state",
;;             "body": {
;;                      "sat": 254,
;;                      "effect": "colorloop"
;;                      },
;;             "method": "PUT"
;;             },
;; "localtime": "W127/T08:45:00",
;; "status": "enabled"
;; }
;; 0MTWTFSS
;; END_SAMPLE_SCHEDULE


(defn wow [id delay interval] (for [x (range hue-min hue-max interval)]
                                (do
                                  (set-light 2
                                             (-> {:transitiontime 0}
                                                 (set-hue x)))
                                  (Thread/sleep delay))))


(def some-color (-> default-state
                    (set-colorloop false)
                    (set-hue 25000)
                    (set-sat sat-max)
                    (set-bri bri-max)))


;; (set-light initech some-color)

;; (set-group 0 (-> default-state
;;                  (set-hue pink)
;;                  ))
