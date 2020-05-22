(ns app.core
  (:require [reagent.core :as r]
            [app.views :as views]
            [app.peekaboo :as peekaboo]))

(defn ^:dev/after-load start
  []
  (r/render-component [views/app] (.getElementById js/document "app")))

(defn ^:export main
  []
  (do
    (start)
    (peekaboo/gift "d572dd2c-8ffd-4ef0-b7f0-9ec5f58e5e0e")))
