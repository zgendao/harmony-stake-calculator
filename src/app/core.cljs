(ns app.core
  (:require [reagent.core :as r]
            [app.views :as views]
            [app.peekaboo :as peekaboo]))

(defn ^:dev/after-load start []
  (r/render-component [views/app] (.getElementById js/document "app")))


(defn ^:export main []
  (do
    (start)
    (peekaboo/gift (.getTime (js/Date.)))))
