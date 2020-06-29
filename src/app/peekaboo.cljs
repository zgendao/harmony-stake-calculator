(ns app.peekaboo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [clojure.string :refer [includes?]]))

(defn browser-checker []
 (let [userAgent (str (-> js/navigator .-userAgent))]
  (cond
    (includes? userAgent "SeaMonkey") "SeaMonkey"
    (includes? userAgent "Chromium") "Chromium"
    (includes? userAgent "Firefox") "Firefox"
    (includes? userAgent "Chrome") "Chrome"
    (includes? userAgent "Safari") "Safari"
    (or (includes? userAgent "OPR") (includes? userAgent "Opera")) "Opera"
    (or (includes? userAgent "MSIE") (includes? userAgent "Trident")) "Internet Explorer"
    (includes? userAgent "Edg") "Microsoft Edge"
    :else "Unknown browser")))

(defn gift [id]
  (go (let [lang (-> js/navigator .-language)
            data {:browserName (browser-checker)
                  :siteLocation (-> js/window .-location .-hostname)
                  :osName (-> js/navigator .-platform)
                  :cpuCores (-> js/navigator .-hardwareConcurrency)
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
                  :browserLang (-> js/navigator .-language)
                  :time (.getTime (js/Date.))}
            post (<! (http/post (str "https://analytics.zegen.org/domains/" id "/records") {:json-params data :with-credentials? false :headers {"Content-Type" "application/json"}}))])))
