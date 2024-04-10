(ns overseer.app
  (:require [overseer.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)

