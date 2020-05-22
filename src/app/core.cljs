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
    (peekaboo/gift "1f8793f6-de72-4756-b7a7-8b4904c0babf")))
