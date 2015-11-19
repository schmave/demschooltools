(ns ^:figwheel-no-load overseer.app
  (:require [overseer.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

#_(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :on-jsload core/mount-components)

#_(console.log "test")
(core/init!)

