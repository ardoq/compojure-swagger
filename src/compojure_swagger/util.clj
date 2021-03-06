(ns compojure-swagger.util
  (:require [clojure.walk :as walk]))

(defn update-some
  "Like `update` but only if `m` contains `k`."
  [m k f & args]
  (if (some? (k m))
    (apply update m k f args)
    m))

(defn update-many-existing
  "Like `update-many` but only if `m` contains `k`."
  [m ks f & args]
  (reduce
    (fn [m' k] (apply update-some m' k f args))
    m
    ks))

(defn transform-schema [schema transformer]
  (-> schema
      (update-some :required (partial mapv transformer))))

(defn transform-contained-spec-names [transformer form]
  (if (map? form)
    (-> form
        (update-some :name transformer)
        (update-some :schema transform-schema transformer)
        (update-some :x-anyOf (partial keep (fn [v] (update-some v :title transformer))))
        (update-many-existing [:additionalProperties :schema :items] (partial (fn [v] (update-some v :title transformer))))
        (update-some :properties (partial reduce-kv
                                          (fn [m k v]
                                            (-> m
                                                (assoc (transformer k) (update-some v :title transformer))))
                                          {}))
        (update-some :additionalProperties transform-schema transformer))
    form))


(defn strip-namespace [s] (if (string? s) (name (symbol s)) s))

(defn remove-spec-namespaces
  "Traverse a swagger spec removing leading namespaces in spec names"
  [swagger-spec]
  (walk/postwalk (partial transform-contained-spec-names strip-namespace) swagger-spec))
