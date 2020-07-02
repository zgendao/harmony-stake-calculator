(ns app.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            ["highcharts" :as highcharts]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]))

(def state (atom {:one-price 0
                  :network-stake 0
                  :median-stake 0
                  :type "delegator"
                  :restake? false
                  :stake 20000
                  :time 12
                  :restake true
                  :fee 5
                  :delegated 0
                  :network-stake-inc 1
                  :price-inc 0
                  :navbar-open false
                  :chart-open false
                  :first-m-inc
                  :m-rate}))

(defn- request []
  (go (let [stake-response (<! (http/post "https://api.s0.t.hmny.io" {:json-params {:jsonrpc "2.0" :method "hmy_getStakingNetworkInfo" :params [] :id 1} :with-credentials? false :headers {"Content-Type" "application/json"}}))
            price-response (<! (http/get "https://api.coingecko.com/api/v3/simple/price?ids=harmony&vs_currencies=usd" {:with-credentials? false :headers {"Content-Type" "application/json"}}))]
        (do (swap! state assoc :network-stake (Math/round (* 0.000000000000000001 (:total-staking (:result (stake-response :body))))))
            (swap! state assoc :median-stake (Math/round (* 0.000000000000000001 (:median-raw-stake (:result (stake-response :body))))))
            (swap! state assoc :one-price (:usd (:harmony (:body price-response))))))))

(defn chart-data [m-rate first-m-inc months]
  (let [holding (@state :stake) multiplier (+ 1  m-rate)]
    (if (@state :restake?)
      (reduce
       (fn [v r] (conj v (* (last v) multiplier)))
       [holding]
       (range months))
      (reduce
       (fn [v r] (conj v (+ (last v) first-m-inc)))
       [holding]
       (range months)))))

(defn chart-component []
  (let [chartData {:xAxis {:categories []
                           :title {:text "Month"}}
                   :yAxis {:title {:text "Holding (ONE)"}}
                   :series [{:data (chart-data (@state :m-rate) (@state :first-m-inc) (@state :time))}]
                   :tooltip {:pointFormat "<b>{point.y:.2f}</b> ONE <br>"
                             :headerFormat ""}
                   :plotOptions {:series {:color "#00ADE8"}}
                   :legend {:enabled false}
                   :credits {:enabled false}
                   :title {:style {:display "none"}}}]
    (.chart highcharts "rev-chartjs" (clj->js chartData))))

(defn stake-chart []
  (reagent/create-class
   {:component-did-mount #(chart-component)
    :component-did-update #(chart-component)
    :display-name "chartjs-component"
    :reagent-render (fn []
                      @state
                      [:div#rev-chartjs])}))

(defn num-input [label value disabled max class]
  [:div
   [:label {:class class} label
    [:input {:type "number"
             :disabled disabled
             :min 1
             :value (if max (min max (@state value)) (@state value))
             :on-change #(swap! state assoc value (js/parseInt (-> % .-target .-value)))}]]])

(defn vformat [value]
  (if (< 100 value) (format "%.0f" value) (format "%.2f" value)))
(defn pformat [percent]
  (format "%.2f" (* 100 percent)))

(defn navbar []
  [:nav
   [:div.container
    [:div.navbar__brand
     [:a {:href "https://harmony.one/" :target "_blank"}
      [:img {:src "./images/logo1.png" :width "25px"}]
      [:p "Harmony Calculator"]]]
    [:div.collapse {:class [(when (not (@state :navbar-open)) "u-hideOnMobile")]}
     [:a {:href "https://staking.harmony.one/"  :target "_blank"} "STAKING"]
     [:a {:href "https://docs.harmony.one/home/validators"  :target "_blank"} "BECOME A VALIDATOR"]
     [:p (format "%.6f" (@state :one-price)) " USD"]]
    [:button.navbar__togglr {:on-click #(swap! state assoc :navbar-open (not (@state :navbar-open)))}
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]]]])

(defn dashboard []
  (let [yearly-issuance 441000000
        one-price (@state :one-price)
        network-stake (@state :network-stake)
        price-inc (@state :price-inc)
        holding (@state :stake)
        time (@state :time)
        type (@state :type)

        future-one-price (* (@state :one-price) (+ 1 (/ price-inc 100)))
        fee (if (= type "delegator") (- 1 (/ (@state :fee) 100)) (+ 1 (/ (* (@state :delegated) (/ (@state :fee) 100)) (@state :stake))))

        avg-network-stake (/ (reduce + (reduce (fn [v r] (conj v (* (last v) (+ 1 (/ (@state :network-stake-inc) 100))))) [network-stake] (range time))) (inc time))
        network-share (/ (* fee holding) avg-network-stake)

        first-m-inc (/ (* yearly-issuance network-share) 12) _ (swap! state assoc :first-m-inc first-m-inc)
        m-rate (/ first-m-inc holding) _ (swap! state assoc :m-rate m-rate)

        m-inc (/ (- (last (chart-data m-rate first-m-inc time)) holding) time)
        y-inc (* m-inc 12)
        d-inc (/ y-inc 365)

        y-rate (/ y-inc holding)

        reward-value (* m-inc time)
        reward-rate (/ reward-value holding)
        reward-frequency-sec (/ 86400 d-inc)

        d-inc-usd (* future-one-price d-inc)
        m-inc-usd (* future-one-price m-inc)
        y-inc-usd (* future-one-price y-inc)
        holding-usd (* one-price holding)
        future-holding-usd (* future-one-price holding)
        reward-value-usd (+ (* future-one-price reward-value) (- future-holding-usd holding-usd))

        del-max 1000000
        val-max 50000000
        range-percent (if (= "delegator" type) (* 100 (/ holding del-max)) (* 100 (/ holding val-max)))]
    [:main
     [:div.container
      [:div#settings.card
       [:h2.title "Staking settings"]
       [:form
        [:div
         [:input {:class [(when (= type "delegator") "active")] :type "button" :value "Delegator" :on-click #(swap! state assoc :type "delegator")}]
         [:input {:class [(when (= type "validator") "active")] :type "button" :value "Validator" :on-click #(swap! state assoc :type "validator")}]]
        [:div.u-fillLeft_fitRight
         [:label "Stake"
          [:input {:style {:background (str "linear-gradient(to right, #00ADE8 0%, #00ADE8 " range-percent "%, #fff " range-percent "%, white 100%)")}
                   :type "range"
                   :min (if (= type "delegator") 1 del-max)
                   :max (if (= type "delegator") del-max val-max)
                   :value holding
                   :on-change #(swap! state assoc :stake (js/parseInt (-> % .-target .-value)))}]]
         [:div
          [:label.showUnit.showUnit--one
            [:input {:type "number"
                     :min 1
                     :value holding
                     :on-change #(swap! state assoc :stake (js/parseInt (-> % .-target .-value)))}]]]]
        [num-input "Staking Time" :time false 120 "showUnit showUnit--months"]
        [:div>label.switch "Auto restake"
         [:input#autorestake {:type "checkbox" :checked (@state :restake?) :on-click #(swap! state assoc :restake? (not (@state :restake?)))}]
         [:span.slider]]
        [:h3.title.title--secondary "Advanced"]
        [num-input "Fee" :fee false 100 "showUnit showUnit--percentage"]
        [num-input "Delegated" :delegated (when (= type "delegator") "disabled") false "showUnit showUnit--one"]
        [num-input "Price Increase" :price-inc false false "showUnit showUnit--percentage"]
        [num-input "Effective Median Stake" :median-stake "disabled" false "showUnit showUnit--one"]
        [num-input "Monthly Network Increase" :network-stake-inc false 100 "showUnit showUnit--percentage"]
        [num-input "Network Stake" :network-stake false false "showUnit showUnit--one"]]]
      [:div#earnings_chart.card {:class [(when (@state :chart-open) "earnings_chart--showChart")]}
       [:h2.title "Earnings"]
       [:div#earnings_chart__chartWrapper
        [stake-chart]]
       [:div.dataBlock {:on-click #(swap! state assoc :chart-open (not (@state :chart-open)))}
        [:p "Daily Income"]
        [:strong "$" (vformat d-inc-usd)]
        [:p (vformat d-inc) " ONE"]]
       [:div.dataBlock {:on-click #(swap! state assoc :chart-open (not (@state :chart-open)))}
        [:p "Monthly Income"]
        [:strong "$" (vformat m-inc-usd)]
        [:p (vformat m-inc) " ONE"]]
       [:div.dataBlock {:on-click #(swap! state assoc :chart-open (not (@state :chart-open)))}
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
        [:strong (format "%.4f" (* 100 network-share)) "%"]]
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
                   (> 8 reward-frequency-sec) "8 sec"
                   (> 60 reward-frequency-sec) (str (format "%.1f" reward-frequency-sec) " sec")
                   (> 3600 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 60)) " min")
                   (> 86400 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 3600)) " hour")
                   (> 604800 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 86400)) " day")
                   (> 18144000 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 604800)) " week")
                   (< 18144000 reward-frequency-sec) (str (format "%.1f"  (/ reward-frequency-sec 18144000)) " month"))]]]
      [:div#about
       [:p "Made by " [:a {:href "https://zgen.hu"} "ZGEN DAO"] " the bureaucracy-free online guild."]
       [:p "Send your feature requests to: " [:a {:href "mailto:contact@zgen.hu"} "crypto@zgen.hu"]]
       [:p "Source: " [:a {:href "https://github.com/zgendao/harmony-stake-calculator"} "zgendao/harmony-stake-calculator"]]]]]))

(defn app []
  (request)
  [:<>
   [navbar]
   [dashboard]])
