(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]))

(def api (atom {:one-price 0.00035
                :network-stake 550000000}))

(def state (atom {:type "delegator"
                  :stake 10000
                  :time 12
                  :restake true
                  :fee 10
                  :delegated 0
                  :uptime 99.9
                  :median-stake 24000
                  :price-inc 10
                  :total-stake 50000000}))

(defn chart-data [mrate]
  (let [multiplier (+ 1 mrate) months (@state :time) stake (@state :stake)]
    (reduce
     (fn [v r] (conj v (* (last v) multiplier)))
     [stake]
     (range months))))

(defn chart-component [mrate]
  (let [chartData {:xAxis {:categories [1 2 3 4 5 6 7 8 9 10]
                           :title {:text "Month"}}
                   :yAxis {:title {:text "Stake (ONE)"}}
                   :series [{:data (chart-data mrate)}]
                   :tooltip {:pointFormat "<b>{point.y:.2f}</b> ONE"
                             :headerFormat ""}
                   :legend {:enabled false}
                   :credits {:enabled false}
                   :title {:style {:display "none"}}}]
    (.chart highcharts "rev-chartjs" (clj->js chartData))))

(defn stake-chart [mrate]
  (fn []
    (reagent/create-class
     {:component-did-mount  #(chart-component mrate)
      :component-did-update #(chart-component mrate)
      :display-name        "chartjs-component"
      :reagent-render      (fn []
                             (@state :stake)
                             [:div#rev-chartjs {:style {:width "600px" :height "360px"}}])})))

(defn num-input [label value]
  (fn []
    [:div
     [:label label]
     [:input {:type "number"
              :min 1
              :value (@state value)
              :on-change #(swap! state assoc value (js/parseInt (-> % .-target .-value)))}]]))

(defn app []
  ;restake? (.-checked (.getElementById js/document "autorestake"))]
  [:div {:style {:display "flex" :justify-content "center"}}
   [:form
    [:h2 "Staking settings"]
    [:div
     [:input {:type "button" :value "Delegator" :on-click #(swap! state assoc :type "delegator")}]
     [:input {:type "button" :value "Validator" :on-click #(swap! state assoc :type "validator")}]
     [:span (@state :type)]]
    [num-input "Stake (ONE)" :stake]
    [num-input "Staking Time (Months)" :time]
    [:div>label.switch
     [:input#autorestake {:type "checkbox"}]
     [:span.slider.round]]
    [:h3 "Advanced"]
    [num-input "Fee (%)" :fee]
    [num-input "Delegated (ONE)" :delegated]
    [num-input "Uptime (AVG) (%)" :uptime]
    [num-input "Effective Median Stake (ONE)" :median-stake]
    [num-input "Price Increase (Year) (%)" :price-inc]
    [num-input "Total Stake (ONE)" :total-stake]]

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

         dinc (if (< 100 dinc) (format "%.0f" dinc) (format "%.2f" dinc))
         minc (if (< 100 minc) (format "%.0f" minc) (format "%.2f" minc))
         yinc (if (< 100 yinc) (format "%.0f" yinc) (format "%.2f" yinc))
         reward-rate (format "%.2f" (* 100 reward-rate))
         yrate (format "%.2f" (* 100 yrate))
         network-share (format "%.4f" (* 100 network-share))
         reward-value (if (< 100 reward-value) (format "%.0f" reward-value) (format "%.2f" reward-value))]
     [:div
      [:h3 "Earnings"]
      [:p (str fee)]
      [stake-chart mrate]
      [:p "Daily Income: " dinc]
      [:p "Monthly Income: " minc]
      [:p "Yearly Income: " yinc]
      [:p "Total Reward Rate: " reward-rate "%"]
      [:p "Yearly Reward Rate: " yrate "%"]
      [:p "Network Share: " network-share "%"]
      [:p "Current Holdings: " holding]
      [:p "Total Rewards Value: " reward-value]
      [:p "Reward Frequency: " reward-frequency]])])
