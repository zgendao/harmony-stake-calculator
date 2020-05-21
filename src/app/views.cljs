(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]))

(def api (atom {:one-price 0.00326
                :network-stake 600000000}))

(def state (atom {:type "delegator"
                  :restake? false
                  :stake 10000
                  :time 12
                  :restake true
                  :fee 10
                  :delegated 0
                  :uptime 99.9
                  :median-stake 24000
                  :price-inc 10
                  :total-stake 60000000
                  :navbar-open false}))

(defn chart-data [tochart months]
  (let [multiplier (+ 1 (first tochart)) stake (@state :stake)]
    (if (@state :restake?)
      (reduce
       (fn [v r] (conj v (* (last v) multiplier)))
       [stake]
       (range months))
      (reduce
       (fn [v r] (conj v (+ (last v) (second tochart))))
       [stake]
       (range months)))))

(defn chart-component [tochart]
  (let [chartData {:xAxis {:categories []
                           :title {:text "Month"}}
                   :yAxis {:title {:text "Holding (ONE)"}}
                   :series [{:data (chart-data tochart (@state :time))}]
                   :tooltip {:pointFormat "<b>{point.y:.2f}</b> ONE <br>"
                             :headerFormat ""}
                   :plotOptions {:series {:color "#00ADE8"}}
                   :legend {:enabled false}
                   :credits {:enabled false}
                   :title {:style {:display "none"}}}]
    (.chart highcharts "rev-chartjs" (clj->js chartData))))

(defn stake-chart [tochart]
  (reagent/create-class
   {:component-did-mount  #(chart-component tochart)
    :component-did-update #(chart-component tochart)
    :display-name        "chartjs-component"
    :reagent-render      (fn []
                           @state
                           [:div#rev-chartjs])}))

(defn num-input [label value disabled class]
  [:div {:class class}
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
     [:a {:href "https://harmony.one/"} "PROJECT"]
     [:a {:href "https://staking.harmony.one/"} "STAKING"]
     [:p (str (@api :one-price)) "USD"]]
    [:button.navbar__togglr {:on-click #(swap! state assoc :navbar-open (not (@state :navbar-open)))}
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]]]])

(defn dashboard []
  (let [one-price (@api :one-price)
        network-stake (@api :network-stake)
        yearly-issuance 441000000

        fee (if (= (@state :type) "delegator") (- 1 (/ (@state :fee) 100)) (+ 1 (/ (* (@state :delegated) (/ (@state :fee) 100)) (@state :stake))))
        network-share (/ (* fee (@state :stake)) network-stake)
        holding (@state :stake)

        first-m-inc (/ (* yearly-issuance network-share) 12)
        m-rate (/ first-m-inc (@state :stake))
        tochart [m-rate first-m-inc]

        y-inc (- (last (chart-data tochart 12)) holding)
        m-inc (/ y-inc 12)
        d-inc (/ y-inc 365)

        y-rate (/ y-inc (@state :stake))

        reward-value (* m-inc (@state :time))
        reward-rate (/ reward-value (@state :stake))
        reward-frequency 0

        d-inc-usd (* one-price d-inc)
        m-inc-usd (* one-price m-inc)
        y-inc-usd (* one-price y-inc)
        holding-usd (* one-price holding)
        reward-value-usd (* one-price reward-value)]
    [:main.container
     [:div#settings.card
     [:h2.title "Staking settings"]
      [:form
       [:div
        [:input {:class [(when (= (@state :type) "delegator") "active")] :type "button" :value "Delegator" :on-click #(swap! state assoc :type "delegator")}]
        [:input {:class [(when (= (@state :type) "validator") "active")] :type "button" :value "Validator" :on-click #(swap! state assoc :type "validator")}]]
       [:div.u-fillLeft_fitRight
        [:label "Stake"
          [:input {:type "range"
                  :min 1
                  :max 1000000}]]
        [:div.showUnit.showUnit--one
          [:input {:type "number"
                  :min 1
                  :value (@state :stake)
                  :on-change #(swap! state assoc :stake (js/parseInt (-> % .-target .-:stake)))}]]]
       [num-input "Staking Time" :time false "showUnit showUnit--months"]
       [:div>label.switch "Auto restake"
        [:input#autorestake {:type "checkbox" :checked (@state :restake?) :on-click #(swap! state assoc :restake? (not (@state :restake?)))}]
        [:span.slider]]
       [:h3.title.title--secondary "Advanced"]
       [num-input "Fee" :fee false "showUnit showUnit--percentage"]
       [num-input "Delegated" :delegated (when (= (@state :type) "delegator") "disabled") "showUnit showUnit--one"]
       [num-input "Uptime (AVG)" :uptime "disabled" "showUnit showUnit--percentage"]
       [num-input "Effective Median Stake" :median-stake "disabled" "showUnit showUnit--one"]
       [num-input "Price Increase (Year)" :price-inc "disabled" "showUnit showUnit--percentage"]
       [num-input "Total Stake" :total-stake "disabled" "showUnit showUnit--one"]]]
     [:div#earnings_chart.card
     [:h2.title "Earnings"]
      [:div#earnings_chart__chartWrapper
       [stake-chart tochart]]
      [:div.dataBlock
       [:p "Daily Income"]
       [:strong "$" (vformat d-inc-usd)]
       [:p (vformat d-inc) " ONE"]]
      [:div.dataBlock
       [:p "Monthly Income"]
       [:strong "$" (vformat m-inc-usd)]
       [:p (vformat m-inc) " ONE"]]
      [:div.dataBlock
       [:p "Yearly Income"]
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
       [:strong (pformat network-share) "%"]]
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
       [:strong "- days"]]]]))

(defn app []
  [:<>
   [navbar]
   [dashboard]])
