(ns compojure-swagger.core
  (:require [liberator.core :refer [resource]]
            [compojure.core :as cc]
            [ring.swagger.common :as rsc]
            [ring.swagger.swagger-ui :as ui]
            [compojure-swagger.private]
            [spec-tools.swagger.core :as swagger]
            [clojure.string :as str]
            [compojure-swagger.util :as util])
  (:import (clojure.lang IFn AFn)))

(def swagger-private "compojure-swagger.private")
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
  {:info {:version     "0.0.1"
          :title       "API Docs example"
          :description "Docs for the API"}})

(defn with-swagger [route swagger]
  (assoc route :swagger swagger))

(defn swagger-category [route category-name]
  (if (:children route)
    (assoc route :children (map #(swagger-category % category-name) (:children route)))
    (update-in route [:swagger :swagger-content :tags] conj category-name)))

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

(def supported-verbs #{"GET" "POST" "PUT" "DELETE" "ANY"})

(defn- verb?
  "is elem HTTP verb"
  [elem]
  (supported-verbs elem))

(defn- swap-namespace
  "Recursively swap out valid forms for new-ns versions"
  [children new-ns]
  (map #(cond
          (-> % first name (= "routes")) (apply list (->> % first name (symbol new-ns)) (swap-namespace (rest %) new-ns)) ; Nested body
          (-> % first name (= "context")) (apply list (->> % first name (symbol new-ns)) (nth % 1) (nth % 2) (swap-namespace (nthrest % 3) new-ns)) ; Nested body
          (-> % first name verb?) (-> % vec
                                      (assoc 0 (->> % first name (symbol new-ns)))
                                      seq)
          ;; We want to keep/discard with-swagger depending on namespace
          (#{"with-swagger" "swagger-category"} (-> % first name)) (cond
                                                 (= new-ns swagger-private) (list (first %) (-> % second list (swap-namespace new-ns) first) (last %))
                                                 (= new-ns compojure-core) (-> % second list (swap-namespace new-ns) first)
                                                 :else (throw (IllegalArgumentException. "Unknown namespace. How did this happen?")))
          :else %) children))

(defmacro routes [& handlers]
  `(map->Route
     {:path     nil
      :children (list ~@(swap-namespace handlers swagger-private))
      :handler  (cc/routes ~@(swap-namespace handlers compojure-core))}))

(defmacro context
  [path args & body]
  `(map->Route {:path     ~path
                :children (list ~@(swap-namespace body swagger-private))
                :handler  (cc/context ~path ~args ~@(swap-namespace body compojure-core))}))


(defn swagify-verb [verb]
  (let [{:keys [swagger path method]} verb
        {:keys [swagger-content transformer]} swagger
        transformer (if transformer transformer identity)]
    (if (some? swagger-content) (-> (swagger/swagger-spec {(str path)
                                                   {method swagger-content}})
                            util/remove-spec-namespaces
                            transformer))))

;; TODO: Handle with-swagger for routes and context
(defn swagify-route [route]
  (if-let [children (:children route)]
    ;; Children -> Routes or context
    (let [new-route (assoc route :children (keep swagify-route children))
          ;; Merge children which share paths
          new-children (apply rsc/deep-merge (:children new-route))]
      (if-let [context-path (:path route)]
        ;; Context, prepend to path
        (into {} (keep (fn [[path body]] {(str context-path path) body}) new-children))
        ;; Routes
        new-children))
    ;; No children -> HTTP verb
    (swagify-verb route)))

(defn fix-params-in-path
  "Replace a path param such as /:id with corresponding /{id}"
  [path]
  (str/replace path #"\/\:([^\/]*)" "/{$1}"))

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
    (ui/swagger-ui (:meta-options options))
    (cc/GET "/swagger.json" {}
      (resource :available-media-types ["application/json"]
                :handle-ok
                (swagger-spec
                  (rsc/deep-merge swagger-default (:swagger-options options))
                  swag-routes)))))
