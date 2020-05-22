(ns app.peekaboo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

(defn gift [id]
  (go (let [data {:browserName (-> js/navigator .-appCodeName) :siteReferrer "https://example.com/referrer.html"
                  :deviceName "iPad" :siteLocation "https://example.com/index.html"
                  :osName (-> js/navigator .-platform) :siteLanguage "en"
                  :browserHeight 900 :browserWidth 1000
                  :deviceManufacturer "Apple" :screenColorDepth 32
                  :screenHeight (-> js/screen .-width) :screenWidth (-> js/screen .-height)
                  :browserVersion "9.0.1" :osVersion "9.0.1"}
            post (<! (http/post (str "https://analytics.zegen.org/domains/" id "/records") {:json-params data :with-credentials? false :headers {"Content-Type" "application/json"}}))])))
