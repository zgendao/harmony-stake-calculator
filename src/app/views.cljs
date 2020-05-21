(ns app.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]))

(def state (atom {:one-price 0
                  :network-stake 1024015805
                  :type "delegator"
                  :restake? false
                  :stake 20000
                  :time 12
                  :restake true
                  :fee 10
                  :delegated 0
                  :uptime 99.9
                  :median-stake 24000
                  :price-inc 10
                  :navbar-open false}))

(defn- request []
  (go (let [stake-response (<! (http/post "https://api.s0.t.hmny.io" {:json-params {:jsonrpc "2.0" :method "hmy_getStakingNetworkInfo" :params [] :id 1} :with-credentials? false :headers {"Content-Type" "application/json"}}))
            price-response (<! (http/get "https://api.coingecko.com/api/v3/simple/price?ids=harmony&vs_currencies=usd" {:with-credentials? false :headers {"Content-Type" "application/json"}}))]
        (do (swap! state assoc :network-stake (Math/round (* 0.000000000000000001 (:total-staking (:result (stake-response :body))))))
            (swap! state assoc :one-price (:usd (:harmony (:body price-response))))))))

(defn chart-data [m-rate first-m-inc months]
  (let [multiplier (+ 1  m-rate) stake (@state :stake) _ (println m-rate first-m-inc)]
    (if (@state :restake?)
      (reduce
       (fn [v r] (conj v (* (last v) multiplier)))
       [stake]
       (range months))
      (reduce
       (fn [v r] (conj v (+ (last v) first-m-inc)))
       [stake]
       (range months)))))

(defn chart-component [m-rate first-m-inc]
  (let [chartData {:xAxis {:categories []
                           :title {:text "Month"}}
                   :yAxis {:title {:text "Holding (ONE)"}}
                   :series [{:data (chart-data m-rate first-m-inc (@state :time))}]
                   :tooltip {:pointFormat "<b>{point.y:.2f}</b> ONE <br>"
                             :headerFormat ""}
                   :plotOptions {:series {:color "#00ADE8"}}
                   :legend {:enabled false}
                   :credits {:enabled false}
                   :title {:style {:display "none"}}}]
    (.chart highcharts "rev-chartjs" (clj->js chartData))))

(defn stake-chart [m-rate first-m-inc]
  (reagent/create-class
   {:component-did-mount #(chart-component m-rate first-m-inc)
    :component-did-update #(chart-component m-rate first-m-inc)
    :display-name "chartjs-component"
    :reagent-render (fn []
                      @state
                      [:div#rev-chartjs {:style {:width "100%" :height "100%"}}])}))

(defn num-input [label value disabled]
  [:div
   [:label label
    [:input {:type "number"
             :disabled disabled
             :min 1
             :value (@state value)
             :on-change #(swap! state assoc value (js/parseInt (-> % .-target .-value)))}]]])

(defn vformat [value]
  (if (< 100 value) (format "%.0f" value) (format "%.2f" value)))
(defn pformat [percent]
  (format "%.2f" (* 100 percent)))

(defn navbar []
  [:nav
   [:div.container
    [:div.navbar__brand
     [:img {:src "./images/logo.png" :width "150px"}]
     [:p "Calculator"]]
    [:div.collapse {:class [(when (not (@state :navbar-open)) "u-hideOnMobile")]}
     [:a {:href "https://harmony.one/" :target "_blank"} "PROJECT"]
     [:a {:href "https://staking.harmony.one/" :target "_blank"} "STAKING"]
     [:p (format "%.6f" (@state :one-price)) " USD"]]
    [:button.navbar__togglr {:on-click #(swap! state assoc :navbar-open (not (@state :navbar-open)))}
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]]]])

(defn dashboard []
  (let [one-price (@state :one-price)
        network-stake (@state :network-stake)
        yearly-issuance 441000000

        price-inc (@state :price-inc)
        holding (@state :stake)
        time (@state :time)

        fee (if (= (@state :type) "delegator") (- 1 (/ (@state :fee) 100)) (+ 1 (/ (* (@state :delegated) (/ (@state :fee) 100)) (@state :stake))))
        network-share (/ (* fee holding) network-stake)

        first-m-inc (/ (* yearly-issuance network-share) 12)
        m-rate (/ first-m-inc holding)

        avg-m-inc (/ (- (reduce + (chart-data m-rate first-m-inc time)) (* holding (+ 1 time))) time)
        y-inc (- (last (chart-data m-rate first-m-inc 12)) holding)
        m-inc (/ y-inc 12)
        d-inc (/ y-inc 365)

        y-rate (/ y-inc (@state :stake))

        reward-value (* m-inc (@state :time))
        reward-rate (/ reward-value (@state :stake))
        reward-frequency-sec (/ 86400 d-inc)

        d-inc-usd (* one-price d-inc)
        m-inc-usd (* one-price m-inc)
        y-inc-usd (* one-price y-inc)
        holding-usd (* one-price holding)
        reward-value-usd (* one-price reward-value)]
    [:main.container
     [:h2.title "Staking settings"]
     [:div#settings.card
      [:form
       [:div
        [:input {:class [(when (= (@state :type) "delegator") "active")] :type "button" :value "Delegator" :on-click #(swap! state assoc :type "delegator")}]
        [:input {:class [(when (= (@state :type) "validator") "active")] :type "button" :value "Validator" :on-click #(swap! state assoc :type "validator")}]]
       [num-input "Stake (ONE)" :stake]
       [num-input "Staking Time (Months)" :time]
       [:div>label.switch "Auto restake"
        [:input#autorestake {:type "checkbox" :checked (@state :restake?) :on-click #(swap! state assoc :restake? (not (@state :restake?)))}]
        [:span.slider]]
       [:h3.title "Advanced"]
       [num-input "Fee (%)" :fee]
       [num-input "Delegated (ONE)" :delegated (when (= (@state :type) "delegator") "disabled")]
       [num-input "Uptime (AVG) (%)" :uptime "disabled"]
       [num-input "Effective Median Stake (ONE)" :median-stake "disabled"]
       [num-input "Price Increase (Year) (%)" :price-inc]
       [num-input "Total Stake (ONE)" :network-stake]]]
     [:h2.title "Earnings"]
     [:div#earnings_chart.card
      [:div
       [stake-chart m-rate first-m-inc]]
      [:div.dataBlock
       [:p "Daily Income (AVG)"]
       [:strong "$" (vformat d-inc-usd)]
       [:p (vformat d-inc) " ONE"]]
      [:div.dataBlock
       [:p "Monthly Income (AVG)"]
       [:strong "$" (vformat m-inc-usd)]
       [:p (vformat m-inc) " ONE"]]
      [:div.dataBlock
       [:p "Yearly Income (AVG)"]
       [:strong "$" (vformat y-inc-usd)]
       [:p (vformat y-inc) " ONE"]]]
     [:div#earnings_more.card
      [:div.dataBlock
       [:p "Total Reward Rate"]
       [:strong (pformat reward-rate) "%"]]
      [:div.dataBlock
       [:p "Yearly Reward Rate"]
       [:strong (pformat y-rate) "%"]]
      [:div.dataBlock
       [:p "Network Share"]
       [:strong (format "%.3f" network-share) "%"]]
      [:div.dataBlock
       [:p "Current Holdings"]
       [:strong "$" (vformat holding-usd)]
       [:p (vformat holding) " ONE"]]
      [:div.dataBlock
       [:p "Total Rewards Value"]
       [:strong "$" (vformat reward-value-usd)]
       [:p (vformat reward-value) " ONE"]]
      [:div.dataBlock
       [:p "Reward Frequency"]
       [:strong (cond
                  (> 8 reward-frequency-sec) "every 8 sec"
                  (> 60 reward-frequency-sec) (str (format "%.1f" reward-frequency-sec) " sec")
                  (> 3600 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 60)) " min")
                  (> 86400 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 3600)) " hour")
                  (> 604800 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 86400)) " day")
                  (> 18144000 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 604800)) " week")
                  (< 18144000 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 18144000)) " month"))]]]]))

(defn app []
  (request)
  ;(post-request)
  [:<>
   [navbar]
   [dashboard]])
