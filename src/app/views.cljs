(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]
            [goog.string :as gstring :refer [format]]))

(def api (atom {:one-price 0.00328
                :network-stake 730000000}))

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
                  :total-stake 50000000}))

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
                           [:div#rev-chartjs {:style {:width "100%" :height "100%"}}])}))

(defn num-input [label value]
  [:div
   [:label label
    [:input {:type "number"
             :min 1
             :value (@state value)
             :on-change #(swap! state assoc value (js/parseInt (-> % .-target .-value)))}]]])

(defn vformat [value]
  (if (< 100 value) (format "%.0f" value) (format "%.2f" value)))
(defn pformat [percent]
  (format "%.2f" (* 100 percent)))

(defn header []
  [:div {:style {:display "flex" :height "80px" :width "100vw" :background "white"}}
   [:p "Project"]
   [:p "Staking"]
   [:p (str (@api :one-price))]])

(defn dashboard []
  (let [one-price (@api :one-price)
        network-stake (@api :network-stake)
        yearly-issuance 441000000

        fee (if (= (@state :type) "delegator") (- 1 (/ (@state :fee) 100)) (+ 1 (/ (* (@state :delegated) (/ (@state :fee) 100)) (@state :stake))))
        network-share (/ (* fee (@state :stake)) network-stake)
        holding (@state :stake)

        first-m-inc (/ (* yearly-issuance network-share) 12)
        mrate (/ first-first-m-inc (@state :stake))

        y-inc (* yearly-issuance network-share)
        dinc (/ (* yearly-issuance network-share) 365)

        yrate (/ yinc (@state :stake))

        reward-value (* minc (@state :time))
        reward-rate (/ reward-value (@state :stake))
        reward-frequency 0

        dinc-usd (* one-price dinc)
        minc-usd (* one-price minc)
        yinc-usd (* one-price yinc)
        holding-usd (* one-price holding)
        reward-value-usd (* one-price reward-value)

        tochart [mrate first-m-inc]]

    [:div.container
     [:h2.title "Staking settings"]
     [:div#settings.card
      [:form
       [:div
        [:input {:class [(when (= (@state :type) "delegator") "active")] :type "button" :value "Delegator" :on-click #(swap! state assoc :type "delegator")}]
        [:input {:class [(when (= (@state :type) "validator") "active")] :type "button" :value "Validator" :on-click #(swap! state assoc :type "validator")}]]
       [num-input "Stake (ONE)" :stake "full"]
       [num-input "Staking Time (Months)" :time]
       [:div>label.switch "Auto restake"
        [:input#autorestake {:type "checkbox" :checked (@state :restake?) :on-click #(swap! state assoc :restake? (not (@state :restake?)))}]
        [:span.slider]]
       [:h3.title "Advanced"]
       [num-input "Fee (%)" :fee]
       [num-input "Delegated (ONE)" :delegated]
       [num-input "Uptime (AVG) (%)" :uptime]
       [num-input "Effective Median Stake (ONE)" :median-stake]
       [num-input "Price Increase (Year) (%)" :price-inc]
       [num-input "Total Stake (ONE)" :total-stake]]]
     [:h2.title "Earnings"]
     [:div#earnings_chart.card
      [:div
       [stake-chart tochart]]
      [:div.dataBlock
       [:p "Daily Income"]
       [:strong "$" (vformat dinc-usd)]
       [:p (vformat dinc) " ONE"]]
      [:div.dataBlock
       [:p "Monthly Income"]
       [:strong "$" (vformat minc-usd)]
       [:p (vformat minc) " ONE"]]
      [:div.dataBlock
       [:p "Yearly Income"]
       [:strong "$" (vformat yinc-usd)]
       [:p (vformat yinc) " ONE"]]]
     [:div#earnings_more.card
      [:div.dataBlock
       [:p "Total Reward Rate"]
       [:strong (pformat reward-rate) "%"]]
      [:div.dataBlock
       [:p "Yearly Reward Rate"]
       [:strong (pformat yrate) "%"]]
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
       [:strong reward-frequency " days"]]]]))

(defn app []
  [:<>
   [header]
   [dashboard]])
