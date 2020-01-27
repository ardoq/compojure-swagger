(ns ardoq-swagger.core
  (:require [liberator.core :refer [resource]]
            [compojure.core :as cc]
            [ring.swagger.common :as rsc]
            [ring.swagger.swagger-ui :as ui]
            [ardoq-swagger.private]
            [spec-tools.swagger.core :as swagger])
  (:import (clojure.lang IFn AFn)))

(def swagger-private "ardoq-swagger.private")
(def compojure-core "compojure.core")

(defrecord Route [path method info children handler]
  compojure.response/Renderable
  (render [_ request]
    (handler request))

  IFn
  (invoke [_ request]
    (handler request))
  (invoke [_ request respond raise]
    (handler request respond raise))

  (applyTo [this args]
    (AFn/applyToHelper this args)))

(def swagger-default
  {:info  {:version     "0.0.1"
           :title       "API Docs example"
           :description "Docs for the API"}})

(defn with-swagger [route swagger]
  (assoc route :swagger swagger))
(defn- make-verb-route
  [verb path args body]
  `(map->Route {:path    ~path
                :method  ~verb
                :handler ~(cc/compile-route verb path args body)}))
(defmacro GET
  [path args & body]
  (make-verb-route :get path args body))
(defmacro POST
  [path args & body]
  (make-verb-route :post path args body))
(defmacro PUT
  [path args & body]
  (make-verb-route :put path args body))
(defmacro DELETE
  [path args & body]
  (make-verb-route :delete path args body))
(defmacro ANY
  [path args & body]
  (make-verb-route :any path args body))

(defn- is-verb?
  "is elem HTTP verb"
  [elem]
  (some #(= elem %) ["GET" "POST" "PUT" "DELETE" "ANY"])) ; XXX: Symbol or string?

(defn- unhandle-children
  "If child is valid, don't let it have a handler"
  [children]
  (map #(cond
          (-> % first name (= "routes")) (apply list (->> % first name (symbol swagger-private)) (unhandle-children (rest %))); Nested body
          (-> % first name (= "context")) (apply list (->> % first name (symbol swagger-private)) (nth % 1) (nth % 2) (unhandle-children (nthrest % 3))); Nested body
          (-> % first name is-verb?) (-> % vec
                                            (assoc 0 (->> % first name (symbol swagger-private)))
                                            seq)
          (= "with-swagger" (-> % first name)) (list (first %) (-> % second list unhandle-children first) (last %))
          :else %) children))

(defn- handlerify
"Recursively swap out valid forms for compojure versions"
  [handlers]
  (map #(cond
          (-> % first name (= "routes")) (apply list (->> % first name (symbol compojure-core)) (handlerify (rest %))); Nested body
          (-> % first name (= "context")) (apply list (->> % first name (symbol compojure-core)) (nth % 1) (nth % 2) (handlerify (nthrest % 3))); Nested body
          (is-verb? (-> % first name)) (-> % vec
                                           (assoc 0 (->> % first name (symbol compojure-core)))
                                           seq)
          (= "with-swagger" (-> % first name)) (-> % second list handlerify first)
          :else %) handlers))

(defmacro routes [& handlers]
  `(map->Route
     {:path nil
      :children (list ~@(unhandle-children handlers))
      :handler  (cc/routes ~@(handlerify handlers))}))

(defmacro context
  [path args & body]
  `(map->Route {:path     ~path
                :children (list ~@(unhandle-children body))
                :handler  (cc/context ~path ~args ~@(handlerify body))}))

; TODO: Clean up destructuring
(defn- swagify-verb [verb]
  (let [{{desc :description summary :summary transformer :transformer
          {:keys [spec description]} :response
          {:keys [path-par body]} :parameters} :swagger
         :keys [path method]} verb
        transformer (if transformer transformer identity)]
    (transformer
      (swagger/swagger-spec {(str path)
                             {method
                              {:summary             summary :description desc
                               ::swagger/parameters (merge {:path path-par}
                                                           (if body {:body body} {}))
                               ::swagger/responses  {200 {:schema      spec
                                                          :description description}}}}}))))

;; TODO: Handle with-swagger for routes and context
(defn- swagify-route [route]
  (if-let [children (:children route)]
    ;; Children -> Routes or context
    (let [new-route (assoc route :children (map swagify-route children))
          ;; Merge children which share paths
          new-children (apply rsc/deep-merge (:children new-route))]
      (if-let [context-path (:path route)]
        ;; Context, prepend to path
        (into {} (map (fn [[path body]] {(str context-path path) body}) new-children))
        ;; Routes
        new-children))
    ;; No children -> HTTP verb
    (swagify-verb route)))

(defn- swagify-options [options]
  (let [{:keys [version title description]} options]
     {:info
      (into {} (filter second ; Remove nil values
        {:version version
         :title title
         :description description}))
          }))

(defn fix-params-in-path
  "Replace a path param such as /:id with corresponding /{id}"
  [path]
  (clojure.string/replace path #"\/\:([^\/]*)" "/{$1}"))

(defn fix-paths
  "Replace all path params under :paths such as /:id/:name with corresponding /{id}/{name}"
  [swagger-spec]
  (assoc swagger-spec :paths (reduce-kv
                               (fn [m k v]
                                 (assoc m (fix-params-in-path k) v))
                               {} (:paths swagger-spec))))

(defn swagger-spec [options swag-routes]
  (-> (swagger/swagger-spec
        (merge
          {:swagger "2.0"}
          options
          {:paths (swagify-route swag-routes)}))
      fix-paths))

(defn swagger-api
  "Create an endpoint which contains swagger docs for swag-routes"
  [options swag-routes]
  (cc/routes
    swag-routes
    (ui/swagger-ui options)
    (cc/GET "/swagger.json" {}
            (resource :available-media-types ["application/json"]
                      :handle-ok
                      (swagger-spec
                        (rsc/deep-merge swagger-default (swagify-options options))
                        swag-routes)))))
