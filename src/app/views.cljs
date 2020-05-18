(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]))

(def amount (atom 1))

(defn chart-data []
  (let [multiplier 1.2 years 10]
    (reduce
     (fn [v r] (conj v (* (last v) multiplier)))
     [@amount]
     (range (- years 1)))))

(defn chart-component []
  (let [chartData {:xAxis {:categories [1 2 3 4 5 6 7 8 9 10]
                           :title {:text "Year"}}
                   :yAxis {:title {:text "Stake"}}
                   :series [{:data (chart-data)}]
                   :tooltip {:pointFormat "<b>{point.y:.2f}</b> ONE"
                             :headerFormat ""}
                   :legend {:enabled false}
                   :credits {:enabled false}
                   :title {:style {:display "none"}}}]
    (.chart highcharts "rev-chartjs" (clj->js chartData))))

(defn stake-chart []
  (fn []
    (reagent/create-class
     {:component-did-mount  #(chart-component)
      :component-did-update #(chart-component)
      :display-name        "chartjs-component"
      :reagent-render      (fn []
                             @amount
                             [:div#rev-chartjs {:style {:width "600px" :height "360px"}}])})))

(defn app []
  ;(let [restake? (.-checked (.getElementById js/document "autorestake"))]
  [:div {:style {:display "flex" :justify-content "center"}}
   [:form
    [:h3 "Starter stake:"]
    [:input {:type "number"
             :min 1
             :value @amount
             :on-change #(reset! amount (js/parseInt (-> % .-target .-value)))}]
    [:div>label.switch
     [:input#autorestake {:type "checkbox"}]
     [:span.slider.round]]]
   [stake-chart]])
