(ns ardoq-swagger.core-test
  (:require [clojure.test :refer :all]
            [ardoq-swagger.core :refer [routes context GET POST PUT DELETE ANY] :as swagger]
            [clojure.spec.alpha :as s]
            [ring.mock.request :as mock]))

(def swagify-route #'swagger/swagify-route)

;; TODO: With-swagger test
(deftest swagger-context-test
  (testing "context prepends path ok when swagifying"
    (let [ctx (swagger/context "/top" []
                               (swagger/GET "/bottom" [] nil))]
      (is (= (swagify-route ctx)
             {"/top/bottom" {:get {:summary                            nil,
                                   :description                        nil,
                                   :parameters [{:in "body", :name "", :description "", :required true, :schema {}}],
                                   :responses  {200 {:schema nil, :description ""}}}}})))))

(deftest swagify-nested-context-test
  (testing "nesting contexts works as expected"
    (let [ctx (swagger/context "/top" []
                               (swagger/context "/mid" []
                                                (swagger/GET "/bot" [] nil)))]
      (is (= (swagify-route ctx)
             {"/top/mid/bot" {:get {:summary                            nil,
                                    :description                        nil,
                                    :parameters [{:in "body", :name "", :description "", :required true, :schema {}}],
                                    :responses  {200 {:schema nil, :description ""}}}}})))))

(deftest swagger-routes-test
  (testing "swagifying routes works as expected"
    (let [handler (swagger/routes
                    (swagger/GET "/test1" [])
                    (swagger/GET "/test2" []))]
      (is (= (swagify-route handler)
             {"/test1" {:get {:summary                            nil,
                              :description                        nil,
                              :parameters [{:in "body", :name "", :description "", :required true, :schema {}}],
                              :responses  {200 {:schema nil, :description ""}}}},
              "/test2" {:get {:summary                            nil,
                              :description                        nil,
                              :parameters [{:in "body", :name "", :description "", :required true, :schema {}}],
                              :responses  {200 {:schema nil, :description ""}}}}})))))

(deftest routes-test
  (testing "routes works as expected"
    (let [handler (routes
                    (GET "/test1" [] nil)
                    (POST "/test2" [] nil))]
      (is (seq? (:children handler)))
      (is (= (count (:children handler)) 2)))))

(deftest context-test
  (testing "context works as expected"
    (let [ctx (context "/top" []
                       (GET "/test1" [] nil)
                       (POST "/test2" [] nil))]
      (is (seq? (:children ctx)))
      (is (= (count (:children ctx)) 2)))))

(deftest verb-args-test
  (testing "verb args closure works as expected"
    (let [handler (GET "/test" req (:query-string req))]
      (is (= "it=works"
             (:body (handler (mock/request :get "/test?it=works"))))))))

(deftest context-args-test
  (testing "context args closure works as expected"
    (let [handler (swagger/context "/top" req
                                   (swagger/context "/mid" []
                                                    (swagger/GET "/bot" [] (:query-string req))))]
      (is (= "it=works"
             (:body (handler (mock/request :get "/top/mid/bot?it=works"))))))))

(deftest with-swagger-test
  (testing "with-swagger works as expected"
    (let [handler (swagger/with-swagger (swagger/context "/test1" []
                                                         (swagger/with-swagger (GET "test" [] nil)
                                                           {:really "does"}))
                    {:it "works"})]
      (is (= "works" (get-in handler [:swagger :it])))
      (is (= "does" (get-in (first (:children handler)) [:swagger :really]))))))


(deftest with-swagger-swagify-test
  (testing "with-swagger swagifies specs as expected"
    (s/def ::first-name string?)
    (s/def ::last-name string?)
    (s/def ::id int?)
    (let [id-spec (s/keys :req-un [::id])
          test-spec (s/keys :req [::first-name] :opt [::last-name])
          handler (swagger/with-swagger (swagger/GET "/test1" []) {:summary     "it works"
                                                                   :parameters  {:path-par id-spec
                                                                                 :body     test-spec}
                                                                   :response    {:spec test-spec}})]

      (is (= (swagify-route handler)
             {"/test1" {:get {:summary "it works",
                              :description nil,
                              :parameters [{:in "path",
                                            :name "id",
                                            :description "",
                                            :type "integer",
                                            :required true,
                                            :format "int64"}
                                           {:in "body",
                                            :name "",
                                            :description "",
                                            :required true,
                                            :schema {:type "object",
                                                     :properties {"ardoq-swagger.core-test/first-name" {:type "string"},
                                                                  "ardoq-swagger.core-test/last-name" {:type "string"}},
                                                     :required ["ardoq-swagger.core-test/first-name"]}}],
                              :responses {200 {:schema {:type "object",
                                                        :properties {"ardoq-swagger.core-test/first-name" {:type "string"},
                                                                     "ardoq-swagger.core-test/last-name" {:type "string"}},
                                                        :required ["ardoq-swagger.core-test/first-name"]},
                                               :description ""}}}}})))))

(deftest transformer-test
  (testing "with-swagger transformer works as expected"
    (s/def ::first-name string?)
    (let [test-spec (s/keys :req [::first-name])
          transformer (fn camelize-first-name [x] (-> x
                                                      (assoc-in ["/test1" :get :responses 200 :schema :properties]
                                                                {"firstName" {:type "string"}})
                                                      (assoc-in ["/test1" :get :responses 200 :schema :required]
                                                                ["firstName"])))
          handler (swagger/with-swagger (swagger/GET "/test1" []) {:summary "it works"
                                                                   :response {:spec test-spec}
                                                                   :transformer transformer})]
      (is (= (swagify-route handler)
             {"/test1" {:get {:summary "it works",
                              :description nil,
                              :parameters [{:in "body", :name "", :description "", :required true, :schema {}}],
                              :responses {200 {:schema {:type "object",
                                                        :properties {"firstName" {:type "string"}},
                                                        :required ["firstName"]},
                                               :description ""}}}}})))))
