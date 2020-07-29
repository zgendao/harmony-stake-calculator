(ns app.peekaboo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [clojure.string :refer [includes?]]
            [brave.cmc :as cmc]))

(def data (cmc/init {:apikey "testing-the-board" :host "cc.zgen.hu" :protocol :https :reagent? true}))

(defn browser-checker []
 (let [userAgent (str (-> js/navigator .-userAgent))]
  (cond
    (includes? userAgent "SeaMonkey") "SeaMonkey"
    (includes? userAgent "Chromium") "Chromium"
    (includes? userAgent "Firefox") "Firefox"
    (includes? userAgent "Chrome") "Chrome"
    (includes? userAgent "Safari") "Safari"
    (or (includes? userAgent "OPR") (includes? userAgent "Opera")) "Opr"
    (or (includes? userAgent "MSIE") (includes? userAgent "Trident")) "MSIE"
    (includes? userAgent "Edg") "Edg"
    :else "Unknown browser")))

(defn gift [timestamp]
  (let [newuser (atom true)
        cookie? (-> js/navigator .-cookieEnabled)]
    (when cookie?
      (if (nil? (-> js/window .-localStorage (.getItem "id"))) (.setItem (.-localStorage js/window) "id" timestamp) (reset! newuser false)))
    (let [id (keyword (if cookie? (.getItem (.-localStorage js/window) "id") (.getTime (js/Date.))))]
      (swap! data assoc id
       {:newuser @newuser
        :browserName (browser-checker)
        :siteLocation (-> js/window .-location .-hostname)
        :osName (-> js/navigator .-platform)
        :cpuCores (-> js/navigator .-hardwareConcurrency)
        :deviceManufacturer (cond (< (-> js/screen .-width) 768) "mobile" (= (-> js/screen .-width) 768) "tablet" (> (-> js/screen .-width) 768) "desktop")
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
        :time (.getTime (js/Date.))}))))
