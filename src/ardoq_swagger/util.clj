(ns ardoq-swagger.util
  (:require [clojure.walk :as walk]))

(defn transform-spec-names
  "Traverse a swagger spec removing leading namespaces in spec names"
  [swagger-spec transformer]
  (walk/postwalk (fn [form] (-> form
                                (#(if-let [name (:name %)]
                                    (assoc % :name (transformer name)) %))
                                (#(if-let [required (get-in % [:schema :required])]
                                    (assoc-in % [:schema :required] (->> required (map transformer) vec)) %))
                                (#(if-let [properties (get-in % [:schema :properties])]
                                    (assoc-in % [:schema :properties]
                                              (reduce-kv
                                                (fn [m k v]
                                                  (assoc m (transformer k) v)) {} properties)) %))))
                 swagger-spec))

(defn strip-namespace [s] (if (string? s) (name (symbol s)) s))

(defn remove-spec-namespaces
  "Traverse a swagger spec removing leading namespaces in spec names"
  [swagger-spec]
  (transform-spec-names swagger-spec strip-namespace))
