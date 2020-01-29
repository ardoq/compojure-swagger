(ns ardoq-swagger.spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [ardoq-swagger.core :as swagger]
            [spec-tools.core :as st]))

(deftest spec-tools-test
  (testing "spec-tools/merge works"
    (s/def ::first-name string?)
    (s/def ::last-name string?)
    (s/def ::id int?)
    (s/def ::person-spec (s/keys :req [::first-name ::last-name]))
    (s/def ::identifiable (s/keys :req [::id]))
    (let [handler (swagger/with-swagger
                    (swagger/POST "/test1/:id" [])
                    {:summary  "it works"
                     :response {:spec (st/merge ::person-spec ::identifiable)}})]
      (is (= (swagger/swagify-route handler)
             {"/test1/:id" {:post {:summary "it works",
                                   :description "",
                                   :parameters [],
                                   :responses {200 {:schema {:type "object",
                                                             :properties {"first-name" {:type "string"},
                                                                          "last-name" {:type "string"},
                                                                          "id" {:type "integer", :format "int64"}},
                                                             :required ["first-name" "id" "last-name"]},
                                                    :description ""}}}}})))))
