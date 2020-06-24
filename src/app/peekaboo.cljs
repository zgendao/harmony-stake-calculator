(ns app.peekaboo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

(defn browser-checker []
  (cond
    (clojure.string/includes? (str (-> js/navigator .-userAgent)) "SeaMonkey") "SeaMonkey"
    (and (not (clojure.string/includes? (str (-> js/navigator .-userAgent)) "SeaMonkey")) (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Firefox")) "Firefox"
    (and (not (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Chromium")) (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Chrome")) "Chrome"
    (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Chromium") "Chromium"
    (and (not (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Chromium")) (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Safari") (not (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Chrome"))) "Safari"
    (and (clojure.string/includes? (str (-> js/navigator .-userAgent)) "MSIE") (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Trident")) "Explorer"
    (or (clojure.string/includes? (str (-> js/navigator .-userAgent)) "OPR") (clojure.string/includes? (str (-> js/navigator .-userAgent)) "Opera")) "Opera"))

(defn gift [id]
  (go (let [lang (-> js/navigator .-language)
            data {:browserName (browser-checker)
                  :siteLocation (-> js/window .-location .-hostname)
                  :osName (-> js/navigator .-platform)
                  :cpuCores (-> js/navigator .-hardwareConcurrency)
                  :browserHeight (-> js/screen .-height)
                  :browserWidth (-> js/screen .-width)
                  :deviceManufacturer (case (< (-> js/screen .-width) 768) "mobile" (= (-> js/screen .-width) 768) "tablet" (> (-> js/screen .-width) 768) "desktop")
                  :screenHeight (-> js/screen .-height)
                  :screenWidth (-> js/screen .-width)
                  :cookies? (-> js/navigator .-cookieEnabled)
                  :cookies (-> js/document .-cookie)
                  :colorDepth (-> js/screen .-colorDepth)
                  :pixelDepth (-> js/screen .-pixelDepth)
                  :pathName (-> js/window .-location .-pathname)
                  :clientTime (.Date js/window)
                  :referrer (-> js/document .-referrer)
                  :prevSites (-> js/history .-length)
                  :protocol (-> js/window .-location .-protocol)
                  :browserLang (-> js/navigator .-language)}
            post (<! (http/post (str "https://analytics.zegen.org/domains/" id "/records") {:json-params data :with-credentials? false :headers {"Content-Type" "application/json"}}))])))
