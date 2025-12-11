(ns nelumbo.clj.core
  (:require
   [clojure.java.io :as io]))

(defn read-resource [path]
  (if-let [resource (io/resource path)]
    (slurp resource)
    (throw (ex-info (str "Resource not found: " path) {:path path}))))

(defn -main [& args]
  (println :hello "world")
  :core)

(comment
  (-> (read-resource "vless-vision-reality/client.edn")
      clojure.edn/read-string)
  :done)
