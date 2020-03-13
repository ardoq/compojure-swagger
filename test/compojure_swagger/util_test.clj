(ns compojure-swagger.util-test
  (:require [clojure.test :refer :all]
            [compojure-swagger.util :as util]))

(deftest remove-spec-namespaces-test
  (testing "remove-spec-namespaces works as expected"
    (is (= (util/remove-spec-namespaces
             {"/test1/:id" {:post {:summary     "it works",
                                   :description nil,
                                   :parameters  [{:in          "path",
                                                  :name        "compojure-swagger.core-test/id",
                                                  :description "",
                                                  :type        "integer",
                                                  :required    true,
                                                  :format      "int64"}
                                                 {:in          "body",
                                                  :name        "",
                                                  :description "",
                                                  :required    true,
                                                  :schema      {:type       "object",
                                                                :properties {"compojure-swagger.core-test/first-name" {:type "string"},
                                                                             "compojure-swagger.core-test/last-name"  {:type "string"}
                                                                             "compojure-swagger.core-test/address"    {:type                 "object"
                                                                                                                   :additionalProperties {:type       "object"
                                                                                                                                          :properties {"compojure-swagger.core-test/street"   {:type "string"}
                                                                                                                                                       "compojure-swagger.core-test/house-no" {:type "integer"}}}
                                                                                                                   :title "compojure-swagger.core-test/address"}},
                                                                :required   ["compojure-swagger.core-test/first-name"]}}],
                                   :responses   {200 {:schema      {:type       "object",
                                                                    :properties {"compojure-swagger.core-test/first-name" {:type "string"},
                                                                                 "compojure-swagger.core-test/last-name"  {:type "string"}},
                                                                    :required   ["compojure-swagger.core-test/first-name"]},
                                                      :description ""}}}}})
           {"/test1/:id" {:post {:summary     "it works",
                                 :description nil,
                                 :parameters  [{:in          "path",
                                                :name        "id",
                                                :description "",
                                                :type        "integer",
                                                :required    true,
                                                :format      "int64"}
                                               {:in          "body",
                                                :name        "",
                                                :description "",
                                                :required    true,
                                                :schema      {:type       "object",
                                                              :properties {"first-name" {:type "string"},
                                                                           "last-name"  {:type "string"}
                                                                           "address"    {:type                 "object",
                                                                                         :additionalProperties {:type       "object",
                                                                                                                :properties {"street"  {:type "string"},
                                                                                                                             "house-no" {:type "integer"}}}
                                                                                         :title "address"}},
                                                              :required   ["first-name"]}}],
                                 :responses   {200 {:schema      {:type       "object",
                                                                  :properties {"first-name" {:type "string"},
                                                                               "last-name"  {:type "string"}},
                                                                  :required   ["first-name"]},
                                                    :description ""}}}}}))))
