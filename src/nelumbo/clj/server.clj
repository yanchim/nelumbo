(ns nelumbo.clj.server
  (:require
   [nelumbo.clj.core :as core]
   [clojure.spec.alpha :as s]
   [clojure.tools.logging :as log]
   [muuntaja.core :as m]
   [reitit.coercion.spec :as rcs]
   [reitit.openapi :as openapi]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger-ui :as swagger-ui]
   [ring.adapter.jetty :as jetty]))

(s/def ::name string?)
(s/def ::tun boolean?)
(s/def ::ss boolean?)
(s/def ::cn boolean?)

(s/def ::subscribe
  (s/keys :req-un [::name]
          :opt-un [::tun ::ss ::cn]))

(comment
  (s/valid? ::subscribe {::name "tong" ::tun 1})
  :done)

(defn- ping [_]
  {:status 200
   :body {:hello "world"}})

(defn- subscribe-get [query]
  (let [{:keys [name tun ss cn]} query
        config ""
        password ""]
    {:status 200
     :body (-> (core/read-resource "vless-vision-reality/client.edn")
               clojure.edn/read-string)}
    ;; (if cn
    ;;   {:status 200
    ;;    :body {:config config}}
    ;;   {:status 200
    ;;    :body (update-in
    ;;           config
    ;;           [:outbounds]
    ;;           (fn [outbounds]
    ;;             (map (fn [outbound]
    ;;                    (if (= (:type outbound) "shadowtls")
    ;;                      (assoc outbound :password password)
    ;;                      outbound))
    ;;                  outbounds)))})
    ))

(def root-routes
  [""
   ["/"
    {:get {:responses {200 {:body {:hello string?}}}
           :handler ping}}]
   ["/openapi.json"
    {:get {:no-doc true
           :openapi {:info {:title       "nelumbo-api"
                            :description "openapi docs"
                            :version     "0.1.0"}
                     ;; used in /secure APIs
                     :components
                     {:securitySchemes {"auth" {:type :apiKey
                                                :in :header
                                                :name "x-csrf-token"}}}}
           :handler (openapi/create-openapi-handler)}}]])

(def api-routes
  ["/api" {:tags ["API"]}
   ["/subscribe" {:get {:parameters {:query ::subscribe}
                        :responses {200 {}}
                        :handler (fn [{{query :query} :parameters}]
                                   (subscribe-get query))}}]])

(def secure-routes
  ["/secure" {:tags ["SECURE"]}])

(def app
  (ring/ring-handler
   (ring/router
    [root-routes api-routes secure-routes]
    ;; router data affecting all routes
    {:data {:coercion   rcs/coercion
            :muuntaja   m/instance
            :middleware [openapi/openapi-feature
                         parameters/parameters-middleware ; decoding query & form params
                         muuntaja/format-middleware       ; content negotiation
                         exception/exception-middleware   ; converting exceptions to HTTP responses
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/api-docs"
      :config {:validatorUrl nil
               :urls [{:name "openapi", :url "/openapi.json"}]
               :urls.primaryName "openapi"
               :operationsSorter "alpha"}})
    (ring/create-default-handler))))

(defonce server (atom nil))

(defn stop! []
  (when-some [srv @server]
    (.stop srv)
    (reset! server nil)))

(defn start! [& [port dev]]
  (stop!)
  (let [port (or port 5477)
        srv (jetty/run-jetty #'app {:port port :join? false})]
    (log/info "Server started on port" port)
    (reset! server srv)))

(comment
  (-> (app {:request-method :get :uri "/"})
      (update :body slurp))
  :comment)

(comment
  (start!)
  (type @server)
  (clojure.reflect/reflect @server)
  (let [connector (first (.getConnectors @server))
        port      (.getLocalPort connector)]
    (format "Server running on port: %d" port))
  (.stop @server)
  (.start @server)
  (stop!)
  :comment)
