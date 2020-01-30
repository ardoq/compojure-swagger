(ns ardoq-swagger.util-test
  (:require [clojure.test :refer :all]
            [ardoq-swagger.util :as util]))

(deftest remove-spec-namespaces-test
  (testing "remove-spec-namespaces works as expected"
    (is (= (util/remove-spec-namespaces
             {"/test1/:id" {:post {:summary     "it works",
                                   :description nil,
                                   :parameters  [{:in          "path",
                                                  :name        "ardoq-swagger.core-test/id",
                                                  :description "",
                                                  :type        "integer",
                                                  :required    true,
                                                  :format      "int64"}
                                                 {:in          "body",
                                                  :name        "",
                                                  :description "",
                                                  :required    true,
                                                  :schema      {:type       "object",
                                                                :properties {"ardoq-swagger.core-test/first-name" {:type "string"},
                                                                             "ardoq-swagger.core-test/last-name"  {:type "string"}
                                                                             "ardoq-swagger.core-test/address"    {:type                 "object"
                                                                                                                   :additionalProperties {:type       "object"
                                                                                                                                          :properties {"ardoq-swagger.core-test/street"   {:type "string"}
                                                                                                                                                       "ardoq-swagger.core-test/house-no" {:type "integer"}}}
                                                                                                                   :title "ardoq-swagger.core-test/address"}},
                                                                :required   ["ardoq-swagger.core-test/first-name"]}}],
                                   :responses   {200 {:schema      {:type       "object",
                                                                    :properties {"ardoq-swagger.core-test/first-name" {:type "string"},
                                                                                 "ardoq-swagger.core-test/last-name"  {:type "string"}},
                                                                    :required   ["ardoq-swagger.core-test/first-name"]},
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
