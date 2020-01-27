(ns ardoq-swagger.util
  (:require [clojure.walk :as walk]))

(defn strip-namespace [s] (if (string? s) (name (symbol s)) s))

(defn remove-spec-namespaces
  "Traverse a swagger spec removing leading namespaces in spec names"
  [swagger-spec]
  (walk/postwalk (fn [form] (-> form
                                (#(if-let [name (:name %)]
                                    (assoc % :name (strip-namespace name)) %))
                                (#(if-let [required (get-in % [:schema :required])]
                                    (assoc-in % [:schema :required] (->> required (map strip-namespace) vec)) %))
                                (#(if-let [properties (get-in % [:schema :properties])]
                                    (assoc-in % [:schema :properties]
                                              (reduce-kv
                                                (fn [m k v]
                                                  (assoc m (strip-namespace k) v)) {} properties)) %))))
                 swagger-spec))