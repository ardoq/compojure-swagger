(ns ardoq-swagger.util
  (:require [clojure.walk :as walk]))

(defn update-some
  "Like `update` but only if `m` contains `k`."
  [m k f & args]
  (if (some? (k m))
    (apply update m k f args)
    m))

(defn transform-schema [schema transformer]
  (-> schema
      (update-some :required (partial mapv transformer))
      (update-some :properties (partial reduce-kv
                                   (fn [m k v]
                                     (assoc m (transformer k) v)) {}))))

(defn transform-contained-spec-names [transformer form]
  (if (map? form)
    (-> form (update-some :name transformer) (update-some :schema transform-schema transformer))
    form))


(defn strip-namespace [s] (if (string? s) (name (symbol s)) s))

(defn remove-spec-namespaces
  "Traverse a swagger spec removing leading namespaces in spec names"
  [swagger-spec]
  (walk/postwalk (partial transform-contained-spec-names strip-namespace) swagger-spec))
