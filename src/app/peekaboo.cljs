(ns app.peekaboo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

(defn gift [id]
  (go (let [lang (-> js/navigator .-language)
            data {:browserName (-> js/navigator .-appName)
                  :siteLocation (-> js/window .-location .-hostname)
                  :osName (-> js/navigator .-platform)
                  :browserHeight (-> js/screen .-height)
                  :browserWidth (-> js/screen .-width)
                  :deviceManufacturer (case (< (-> js/screen .-width) 768) "mobile" (= (-> js/screen .-width) 768) "tablet" (> (-> js/screen .-width) 768) "desktop")
                  :screenHeight (-> js/screen .-height)
                  :screenWidth (-> js/screen .-width)
                  :browserVersion (-> js/navigator .-appVersion)}
            post (<! (http/post (str "https://analytics.zegen.org/domains/" id "/records") {:json-params data :with-credentials? false :headers {"Content-Type" "application/json"}}))])))
