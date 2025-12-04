(ns nelumbo.cljs.app)

(defn init
  "Initialize."
  []
  (.log js/console "Init app")
  :init)

(defn ^:dev/before-load stop []
  (js/console.log "stop"))

(defn ^:dev/after-load start []
  (js/console.log "start"))
