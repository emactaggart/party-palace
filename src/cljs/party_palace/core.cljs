(ns party-palace.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom cursor]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [clojure.string :as string]
            [clojure.core :refer [count keys]]
            [clojure.core.async :as async :refer [chan <! >! put!]]
            [cljs-http.client :as http]
            [reagent.cookies :as cookies]
            ))

(enable-console-print!)

(defn set-light [id state]
  (let [url (str "/api/lights/" id)]
    (http/put url {:json-params state})))

(defn map-values
  [m keys f]
  (reduce #(update-in %1 [%2] f) m keys))

(defn str->js->clj [str]
  (-> str
      js/JSON.parse
      (js->clj :keywordize-keys true)
      ))

(defn safe-str->clj [s]
  (try (str->js->clj s)
       (catch js/SynytaxError e
         (do (println (str "SyntaxError parsing: " s))
                                {}))))

(defn response->clj [c]
  (-> c
      :body
      safe-str->clj))

(defn get-app-state-request [] (http/get "/api/all"))

(defn get-app-state []
  (let [c (get-app-state-request)]
    (async/map response->clj [c])))



(defn get-jenkins-jobs-request [] (http/get "/api/jenkins-jobs"))

(defn get-jenkins-jobs [] (async/map response->clj [(get-jenkins-jobs-request)]))

(defn tar-val [e] (-> e .-target .-value))
(def hue-gradient "linear-gradient(to right, #ff0000, #FF7900, #ffff00, #0070c0, #ff00ff, #ff0000)")
(def hue2-gradient "linear-gradient(to right, red, yellow, green, blue, magenta, red)")
(def bri-gradient "linear-gradient(to right, #222222, #ffffff)")
(def sat-gradient "linear-gradient(to right, rgba(255,0,255,0), rgba(255,0,255,1))")
(def ct-gradient "linear-gradient(to right, #A1BFFF,#DFE7FF,#FFF9FF,#FFF5F6,#FFF0E8,#FFE4CC,#FFE4CC,#FFBF7E,#FFBA75,#FFB369,#FFA855,#FFA24A,#FF9C3E,#FF8100,#FF7900)")

;; (defn get-token [] (clojure.string/replace (cookies/get-raw "XSRF-TOKEN") #"-" ""))

(print "BEGIN")

;; Application State

(defonce app-state (atom {:hue-state {}
                          :jenkins-state {:jobs []}}))

(defn load-app-state! []
  (do
    (go (let [app-chan (get-app-state)
              app (<! app-chan)]
          (swap! app-state assoc-in [:hue-state] app)))
    (go (let [jenkins-chan (get-jenkins-jobs)
              jenkins (<! jenkins-chan)]
          (swap! app-state assoc-in [:jenkins-state] jenkins)))
      ))

;; Input Components

(defn inside-app-state []
  [:div
   (str @app-state)
   ])

(defn on-off-button [status on-click]
  [:button.btn {:class (if status "btn-success" "btn-danger")
                :on-click on-click}
   (if status "On" "Off")])

(def x (if status "on" "off"))




(defn hue-button [status on-hue-click]
  [:button.btn
   {:type "button"
    :class (if status "btn-info" "btn-default")
    :on-click on-hue-click}
   "Hue"])

(defn ct-button [status on-ct-click]
  [:button.btn
   {:type "button"
    :class (if status "btn-warning" "btn-default")
    :on-click on-ct-click}
   "Color Temperature"])

(defn colorloop-button [status on-click]
  [:button.btn.btn-default
   {:style {:font-weight (if status :bold)
            :background (if status hue-gradient)}
    :on-click on-click
    }
   "Colorloop"])

(defn fuzzy-search
  ([search v]
   (let [q (-> search
               clojure.string/lower-case
               re-pattern)]
     (filter #(->> %
                   clojure.string/lower-case
                   (re-find q))
             v)
     ))

  ([search v select]
   {:pre [(string? search)
          (vector? v)
          (vector? select)]}
   (let [q (-> search
               clojure.string/lower-case
               re-pattern)]
     (filter
      (fn [m] (-> m
                  (select-keys select)
                  vals
                  (#(map clojure.string/lower-case %))
                  (#(some (partial re-find q) %))
                  ))
      v)
     )
   )
  )


(defn build-button [build-status on-click on-cancel-click]
  [:button.btn.dropdown-toggle
   {:on-click on-click
    :class (if build-status "btn-primary" "btn-default")}
   "Build"
   ])

(defn build-input [search on-input-change on-focus on-click-clear]
  [:span.input-group
   [:input.form-control
    {:type :text
     :on-change on-input-change
     :on-focus on-focus
     :value @search
     }
    ]
   [:div.input-group-btn
    [:button.btn.btn-default
     {:on-click on-click-clear
      :style {:height :34px}}
     [:span.glyphicon.glyphicon-remove]]]])

(defn build-list [jobs search on-select]
  (let [search-results (fuzzy-search search jobs)]
    [:ul.dropdown-menu.show
     (for [j search-results]
       ^{:key j}
       [:li
        [:a.dropdown-item
         {:on-click #(on-select j)}
         j]])
     (if (empty? search-results) [:li>a.dropdown-item "No Results"] [:span])
     ]))

(defn build-search [options job]
  (let [search (atom @job)
        display-dropdown (atom false)
        has-focus (atom false)]
    (fn [options job]
      (let [options @options
            names (map :name options)
            on-select (fn [j] (do (reset! job j)
                                  (reset! search j)
                                  (reset! display-dropdown false)))
            on-input-focus #(reset! display-dropdown true)
            on-input-blur #(reset! display-dropdown false)
            on-blur-dropdown #(when-not
                                  @has-focus
                                (reset! display-dropdown false)
                                (reset! search @job))
            on-click-clear #(do (reset! search ""))
            ]
        [:div.dropdown
         {:on-blur on-blur-dropdown
          :on-mouse-enter #(reset! has-focus true)
          :on-mouse-leave #(reset! has-focus false)}
         [build-input search #(reset! search (tar-val %)) on-input-focus on-click-clear]
         [:div
          (if @display-dropdown
            [build-list names @search on-select]
            [:span])
          ]]))))


(defn mode-buttons [state on-hue-click on-ct-click on-colorloop-click on-build-click]
  (let [light-mode (keyword (:light-mode state))
        ]
       [:span.btn-group
        [hue-button (= light-mode :hs) on-hue-click]
        [ct-button (= light-mode :ct) on-ct-click]
        [colorloop-button (= light-mode :colorloop) on-colorloop-click]
        [build-button (= light-mode :build) on-build-click]
        ]))

(defn set-button [on-click]
  [:button.btn.btn-danger {:on-click on-click
                :style {:background :red
                        :font-weight :bold}}
   "🚀Launch Missile🚀"])

(defn slider [value min max on-change background]
  (let []
    (fn [value min max on-change background]
      [:div
            {:style {:background background
                     :padding "1em"
                     :border-radius "4px"
                     :border "solid black 1px"
                     }}
            [:input {:type :range
                     :default-value @value
                     :min min
                     :max max
                     :on-change on-change
                     }]])))

(defn sliders [state-cursor on-hue-change on-sat-change on-ct-change on-bri-change options job-cursor]
  (fn [state-cursor on-hue-change on-sat-change on-ct-change on-bri-change options job-cursor]
      (let [hue-cursor (cursor state-cursor [:hue])
            sat-cursor (cursor state-cursor [:sat])
            ct-cursor (cursor state-cursor [:ct])
            bri-cursor (cursor state-cursor [:bri])
            light-mode (keyword (get @state-cursor :light-mode))
            ]
        [:div
         (case light-mode
           :build [:span
                   [build-search options job-cursor]
                   [slider sat-cursor 0 254 on-sat-change sat-gradient]]
           :colorloop [:span
                       [slider sat-cursor 0 254 on-sat-change sat-gradient]]
           :hs [:span
                [slider hue-cursor 0 65535 on-hue-change hue-gradient]
                [slider sat-cursor 0 254 on-sat-change sat-gradient]]
           :ct [:span
                [slider ct-cursor 153 500 on-ct-change ct-gradient]]
           [:span])
         [slider bri-cursor 0 254 on-bri-change bri-gradient]
         ])))


;; Application Components

(defn show-light [id light-cursor options]
  (let [
        state-cursor (cursor light-cursor [:state])
        job-cursor (cursor state-cursor [:job])
        ]
    (fn [id light-cursor options]
      (let [light @light-cursor
            state @state-cursor
            light-mode (cursor state-cursor [:light-mode])

            colormode (:colormode state)

            on-off-click (fn [] (swap! light-cursor update-in [:state :on] not))

            on-hue-click #(reset! light-mode "hs")
            on-ct-click #(reset! light-mode "ct")
            on-colorloop-click #(reset! light-mode "colorloop")
            on-build-click #(reset! light-mode "build")

            on-hue-change (fn [e] (swap! light-cursor assoc-in [:state :hue] (js/parseInt (tar-val e))))
            on-sat-change (fn [e] (swap! light-cursor assoc-in [:state :sat] (js/parseInt (tar-val e))))
            on-ct-change (fn [e] (swap! light-cursor assoc-in [:state :ct] (js/parseInt (tar-val e))))
            on-bri-change (fn [e] (swap! light-cursor assoc-in [:state :bri] (js/parseInt (tar-val e))))
        ]
      [:div.well.col-md-8
       [:h3 (:name light)]
       [:div
        [:span
         [on-off-button (:on state) on-off-click]
         [mode-buttons state on-hue-click on-ct-click on-colorloop-click on-build-click]
         ]
        [:span.pull-right
         [set-button #(set-light (name id) state)]
         ]
        [:div
         [sliders state-cursor on-hue-change on-sat-change on-ct-change on-bri-change options job-cursor]
         ]
        ]]))))

(defn show-lights [lights options]
  [:ul
   (for [[id light] @lights]
     ^{:key id}
     [show-light id (cursor lights [id]) options]
     )])


(defn app-playground []
  (let []
    (fn []
      [:div
       [show-lights (cursor app-state [:hue-state :lights]) (cursor app-state [:jenkins-state :jobs])]
       ]))
  )


(defn init-async! []
  (load-app-state!)
  )

;; -------------------------
;; Views

(defn home-page []
  [:div.container
   [app-playground]
   ])

(defn about-page []
  [:div [:h2 "About party-palace"]
   [:div [:a {:href "/"} "go to the home page"]]])

;; -------------------------
;; Routes

(defonce page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (init-async!)
  (mount-root)
  )
