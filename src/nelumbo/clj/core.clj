(ns nelumbo.clj.core
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]))

(defn read-resource
  "Read a classpath resource by joining path parts with '/'.
   Skips nil and empty strings. Throws if resource not found."
  [& parts]
  (let [clean-parts (->> parts (filter some?) (remove str/blank?))
        path        (str/join "/" clean-parts)]
    (if (seq clean-parts)
      (if-let [res (io/resource path)]
        (slurp res)
        (throw (ex-info (str "Resource not found: " path) {:path path})))
      (throw (ex-info "No valid path parts given" {:parts parts})))))

(defn -main [& args]
  (println :hello "world")
  :core)

(comment
  (-> (read-resource "proxy" "xray" "vless-vision-reality" "client.edn")
      clojure.edn/read-string)

  (->> (-> (read-resource "client.json")
           (clojure.data.json/read-str :key-fn keyword))
       (spit "client.edn"))
  :done)
