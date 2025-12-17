#!/usr/bin/env bb

(def owner "SagerNet")
(def repo "sing-box")
(def download-dir "var/releases")

(def platform-configs
  {"android-armv8" {:prefix "SFA" :suffix "arm64-v8a.apk"}
   "darwin-arm64"  {:prefix "sing-box" :suffix "darwin-arm64.tar.gz"}
   "darwin-amd64"  {:prefix "sing-box" :suffix "darwin-amd64.tar.gz"}
   "linux-amd64"   {:prefix "sing-box" :suffix "linux-amd64.tar.gz"}
   "windows-amd64" {:prefix "sing-box" :suffix "windows-amd64.zip"}})

(require
 '[babashka.http-client :as http]
 '[cheshire.core :as json]
 '[clojure.java.io :as io]
 '[clojure.string :as str])

(defn latest-release []
  (let [url (str "https://api.github.com/repos/"
                 owner "/" repo "/releases/latest")
        {:keys [status body]} (http/get url {:headers {"User-Agent" "bb-sync"}})]
    (when-not (= status 200)
      (throw (ex-info (str "GitHub API error: " status) {:status status})))
    (json/parse-string body true)))

(defn get-filenames [version]
  (map (fn [[_ {:keys [prefix suffix]}]]
         (str/join "-" [prefix version suffix]))
       platform-configs))

(defn find-asset-by-name [assets filename]
  (some #(when (= (:name %) filename) %) assets))

(defn download-file! [url dest-path]
  (println "-> Streaming from" url "...")
  (io/make-parents dest-path)
  (let [input-stream (:body (http/get url {:stream? true}))]
    (with-open [out (io/output-stream dest-path)]
      (io/copy input-stream out))))

(defn ensure-dir []
  (clojure.java.io/make-parents (str download-dir "/.tmp")))

(def release (latest-release))

(let [;release (latest-release)
      version (:name release)
      expected-files (get-filenames version)
      assets (:assets release)]
  (ensure-dir)
  (doseq [filename expected-files]
    (let [dest-path (str download-dir "/" filename)
          asset (find-asset-by-name assets filename)]
      (if (.exists (io/file dest-path))
        (println "Already have:" filename)
        (if asset
          (do
            (println "Downloading:" filename)
            (download-file! (:browser_download_url asset) dest-path)
            (println "Saved:" dest-path))
          (println "Asset not found on GitHub:" filename))))))

(System/exit 0)
