(ns app.core
  (:require [reagent.core :as r]
            [app.views :as views]
            [app.peekaboo :as peekaboo]))

(defn ^:dev/after-load start []
  (r/render-component [views/app] (.getElementById js/document "app")))

<<<<<<< HEAD
(defn ^:export main []
  (do
    (start)
    (peekaboo/gift "d572dd2c-8ffd-4ef0-b7f0-9ec5f58e5e0e")))
=======
(defn ^:export main
  []
  (do
    (start)
    (peekaboo/gift "1f8793f6-de72-4756-b7a7-8b4904c0babf")))
>>>>>>> 989ef819dece64cf95a6560636659c9524a57c55
