(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]
            [goog.string :as gstring :refer [format]]))

(def api (atom {:one-price 0.0035
                :network-stake 550000000}))

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

(defn chart-data [tochart]
  (let [multiplier (+ 1 (first tochart)) months (@state :time) stake (@state :stake)]
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
                   :series [{:data (chart-data tochart)}]
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
  ;restake? (.-checked (.getElementById js/document "autorestake"))]
  (let [one-price (@api :one-price)
        network-stake (@api :network-stake)
        yearly-issuance 441000000

        fee (if (= (@state :type) "delegator") (- 1 (/ (@state :fee) 100)) (+ 1 (/ (* (@state :delegated) (/ (@state :fee) 100)) (@state :stake))))
        network-share (/ (* fee (@state :stake)) network-stake)
        dinc (/ (* yearly-issuance network-share) 365)
        minc (/ (* yearly-issuance network-share) 12)
        yinc (* yearly-issuance network-share)
        holding (@state :stake)
        reward-value (* minc (@state :time))
        yrate (/ yinc (@state :stake))
        mrate (/ minc (@state :stake))
        reward-rate (/ reward-value (@state :stake))
        reward-frequency 0
        tochart [mrate minc]

        dinc-usd (* one-price dinc)
        minc-usd (* one-price minc)
        yinc-usd (* one-price yinc)
        holding-usd (* one-price holding)
        reward-value-usd (* one-price reward-value)

        dinc (vformat dinc) minc (vformat minc) yinc (vformat yinc) reward-value (vformat reward-value) holding (vformat holding)
        dinc-usd (vformat dinc-usd) minc-usd (vformat minc-usd) yinc-usd (vformat yinc-usd) reward-value-usd (vformat reward-value-usd) holding-usd (vformat holding-usd)
        reward-rate (pformat reward-rate) yrate (pformat yrate) network-share (pformat network-share)]
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
       [:strong "$" dinc-usd]
       [:p dinc " ONE"]]
      [:div.dataBlock
       [:p "Monthly Income"]
       [:strong "$" minc-usd]
       [:p minc " ONE"]]
      [:div.dataBlock
       [:p "Yearly Income"]
       [:strong "$" yinc-usd]
       [:p yinc " ONE"]]]
     [:div#earnings_more.card
      [:div.dataBlock
       [:p "Total Reward Rate"]
       [:strong reward-rate "%"]]
      [:div.dataBlock
       [:p "Yearly Reward Rate"]
       [:strong yrate "%"]]
      [:div.dataBlock
       [:p "Network Share"]
       [:strong network-share "%"]]
      [:div.dataBlock
       [:p "Current Holdings"]
       [:strong "$" holding-usd]
       [:p holding " ONE"]]
      [:div.dataBlock
       [:p "Total Rewards Value"]
       [:strong "$" reward-value-usd]
       [:p reward-value " ONE"]]
      [:div.dataBlock
       [:p "Reward Frequency"]
       [:strong reward-frequency " days"]]]]))

(defn app []
  [:<>
   [header]
   [dashboard]])
