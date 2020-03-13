(ns compojure-swagger.spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [compojure-swagger.core :as core]
            [spec-tools.core :as st]
            [spec-tools.swagger.core :as swagger]))

(deftest spec-tools-test
  (testing "spec-tools/merge works"
    (s/def ::first-name string?)
    (s/def ::last-name string?)
    (s/def ::id int?)
    (s/def ::person-spec (s/keys :req [::first-name ::last-name]))
    (s/def ::identifiable (s/keys :req [::id]))
    (let [handler (core/with-swagger
                    (core/POST "/test1/:id" [])
                    {:swagger-content {:summary            "it works"
                                       ::swagger/responses {200 {:schema (st/merge ::person-spec ::identifiable)}}}})]
      (is (= (core/swagify-route handler)
             {"/test1/:id" {:post {:summary     "it works",
                                   :responses   {200 {:schema      {:type       "object",
                                                                    :properties {"first-name" {:type "string"},
                                                                                 "last-name"  {:type "string"},
                                                                                 "id"         {:type "integer", :format "int64"}},
                                                                    :required   ["first-name" "id" "last-name"]},
                                                      :description ""}}}}})))))
