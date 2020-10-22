(ns compojure-swagger.core-test
  (:require [clojure.test :refer :all]
            [compojure-swagger.core :refer [routes context GET POST PUT DELETE ANY] :as core]
            [clojure.spec.alpha :as s]
            [ring.mock.request :as mock]
            [spec-tools.swagger.core :as swagger]))

(def swagify-route #'core/swagify-route)
(def swagify-verb #'core/swagify-verb)

(deftest swagger-context-test
  (testing "context prepends path ok when swagifying"
    (let [ctx (core/context "/top" []
                            (core/with-swagger (core/GET "/bottom" [] nil)
                                               {:swagger-content {:description "simple get"}}))]
      (is (= (swagify-route ctx)
             {"/top/bottom" {:get {:description "simple get"}}})))))

(deftest swagify-nested-context-test
  (testing "nesting contexts works as expected"
    (let [ctx (core/context "/top" []
                            (core/context "/mid" []
                                          (core/with-swagger (core/GET "/bot" [] nil)
                                                             {:swagger-content {:description "nested get"}})))]
      (is (= (swagify-route ctx)
             {"/top/mid/bot" {:get {:description "nested get"}}})))))

(deftest swagger-routes-test
  (testing "swagifying routes works as expected"
    (let [handler (core/routes
                    (core/with-swagger (core/GET "/test1" [])
                                       {:swagger-content {:description "test 1"}})
                    (core/with-swagger (core/GET "/test2" [])
                                       {:swagger-content {:description "test 2"}}))]
      (is (= (swagify-route handler)
             {"/test1" {:get {:description "test 1"}}
              "/test2" {:get {:description "test 2"}}})))))

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
    (let [handler (core/context "/top" req
                                (core/context "/mid" []
                                              (core/GET "/bot" [] (:query-string req))))]
      (is (= "it=works"
             (:body (handler (mock/request :get "/top/mid/bot?it=works"))))))))

(deftest with-swagger-test
  (testing "with-swagger works as expected"
    (let [handler (core/with-swagger (core/context "/test1" []
                                                   (core/with-swagger (GET "test" [] nil)
                                                                      {:really "does"}))
                                     {:it "works"})]
      (is (= "works" (get-in handler [:swagger :it])))
      (is (= "does" (get-in (first (:children handler)) [:swagger :really])))))
  (testing "verbs without with-swagger do not get added to doc"
    (let [no-paths-swagger (core/swagger-spec
                             core/swagger-default
                             (core/context "/test1" []
                                           (GET "/test" [] nil)))
          one-path-swagger (core/swagger-spec
                             core/swagger-default
                             (core/context "/test1" []
                                           (core/with-swagger (GET "/doc" [] nil)
                                                              {:swagger-content {:description "simple get"}})
                                           (GET "/no-doc" [] nil)))]
      (is (= (count (:paths no-paths-swagger)) 0))
      (is (= (count (:paths one-path-swagger)) 1)))))


(deftest with-swagger-swagify-test
  (s/def ::first-name string?)
  (s/def ::last-name string?)
  (s/def ::id int?)
  (let [id-spec (s/keys :req [::id])
        test-spec (s/keys :req [::first-name] :opt [::last-name])
        handler (core/with-swagger (core/POST "/test1/:id" [])
                                   {:swagger-content {:summary             "it works"
                                                      ::swagger/parameters {:path id-spec
                                                                            :body test-spec}
                                                      ::swagger/responses  {200 {:schema test-spec}}}})
        expected-swagger {"/test1/:id" {:post {:summary    "it works",
                                               :parameters [{:in          "path",
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
                                                                                        "last-name"  {:type "string"}},
                                                                           :required   ["first-name"]}}],
                                               :responses  {200 {:schema      {:type       "object",
                                                                               :properties {"first-name" {:type "string"},
                                                                                            "last-name"  {:type "string"}},
                                                                               :required   ["first-name"]},
                                                                 :description ""}}}}}]
    (testing "with-swagger swagifies specs as expected"
      (is (= (swagify-route handler) expected-swagger)))
    (testing "swagify-verb swagifies verbs as expected"
      (is (= (swagify-verb handler) expected-swagger)))))

(deftest swagger-api-filter-test
  (s/def ::first-name string?)
  (s/def ::last-name string?)
  (s/def ::id int?)
  (let [id-spec (s/keys :req [::id])
        test-spec (s/keys :req [::first-name] :opt [::last-name])
        privatize-filter (fn [data]
                           (update data :paths (partial
                                                map
                                                (fn [[path method-map]]
                                                  (select-keys method-map (for [[k v] method-map :when (not (:private v))] k))))))
        routes (routes
                (core/with-swagger (core/POST "/test1/:id" [])
                  {:swagger-content {:summary "public method 1"
                                     ::swagger/parameters {:path id-spec
                                                           :body test-spec}
                                     ::swagger/responses  {200 {:schema test-spec}}}})
                (core/with-swagger (core/POST "/test2/:id" [])
                  {:swagger-content {:summary "public method 2"
                                     ::swagger/parameters {:path id-spec
                                                           :body test-spec}
                                     ::swagger/responses  {200 {:schema test-spec}}}})
                (core/with-swagger (core/POST "/test3/:id" [])
                  {:swagger-content {:summary "private method"
                                     :private true
                                     ::swagger/parameters {:path id-spec
                                                           :body test-spec}
                                     ::swagger/responses  {200 {:schema test-spec}}}}))
        unfiltered-handler (core/swagger-api
                            {:meta-options {:path "/api-docs"}
                             :swagger-options {:info {:title "REST api"}
                                               :host "localhost"}}
                            routes)
        filtered-handler (core/swagger-api
                          {:meta-options {:path "/api-docs"}
                           :swagger-options {:info {:title "REST api"}
                                             :host "localhost"}
                           :route-filter privatize-filter}
                          routes)]

    (testing "with-swagger swagifies specs as expected"
      (let [unfiltered-response (:body (unfiltered-handler (mock/request :get "/swagger.json")))
            filtered-response (:body (filtered-handler (mock/request :get "/swagger.json")))]
        (is (re-find #"public method 1" unfiltered-response))
        (is (re-find #"public method 2" unfiltered-response))
        (is (re-find #"private method" unfiltered-response))

        (is (re-find #"public method 1" filtered-response))
        (is (re-find #"public method 2" filtered-response))
        (is (not (re-find #"private method" filtered-response)))))))

(deftest swagger-category-swagify-test
  (testing "swagger-category assigns tag as expected"
    (let [handler (core/swagger-category (core/context "/model_trains" []
                                                        (core/with-swagger
                                                          (core/POST "/test1/:id" [])
                                                          {:swagger-content {:summary "it works"}})
                                                        (core/GET "/test2" []))
                                          "model trains")
          expected-swagger {"/model_trains/test1/:id" {:post {:summary    "it works",
                                                              :tags '("model trains")}}
                            "/model_trains/test2" {:get {:tags '("model trains")}}}]
      (is (= (swagify-route handler) expected-swagger)))))

(deftest swagger-categories-swagify-test
  (testing "swagger-categories (plural) assigns tags as expected"
    (let [handler (core/swagger-categories (core/context "/model_trains" []
                                                        (core/with-swagger
                                                          (core/POST "/test1/:id" [])
                                                          {:swagger-content {:summary "it works"}})
                                                        (core/GET "/test2" []))
                                          ["model trains" "stamp collecting"])
          expected-swagger {"/model_trains/test1/:id" {:post {:summary    "it works",
                                                              :tags '("stamp collecting" "model trains")}}
                            "/model_trains/test2" {:get {:tags '("stamp collecting" "model trains")}}}]
      (is (= (swagify-route handler) expected-swagger)))))

(deftest with-swagger-get-test
  (testing "with-swagger doesn't create a required body when none is specified"
    (let [handler (core/with-swagger (core/GET "/test1" [])
                                     {:swagger-content {:summary "it works"}})]
      (is (= (swagify-route handler)
             {"/test1" {:get {:summary "it works"}}})))))

(deftest transformer-test
  (testing "with-swagger transformer works as expected"
    (s/def ::first-name string?)
    (let [test-spec (s/keys :req [::first-name])
          transformer (fn camelize-first-name [x] (-> x
                                                      (assoc-in ["/test1" :get :responses 200 :schema :properties]
                                                                {"firstName" {:type "string"}})
                                                      (assoc-in ["/test1" :get :responses 200 :schema :required]
                                                                ["firstName"])))
          handler (core/with-swagger (core/GET "/test1" [])
                                     {:transformer     transformer
                                      :swagger-content {:summary  "it works"
                                                        ::swagger/responses {200 {:schema test-spec}}}})]
      (is (= (swagify-route handler)
             {"/test1" {:get {:summary     "it works",
                              :responses   {200 {:schema      {:type       "object",
                                                               :properties {"firstName" {:type "string"}},
                                                               :required   ["firstName"]},
                                                 :description ""}}}}})))))

(deftest fix-params-in-path-test
  (testing "fix-params-in-path works as expected"
    (let [path "/test/:id/hmm/:name"]
      (is (= (core/fix-params-in-path path)
             "/test/{id}/hmm/{name}")))))

(deftest swagger-spec-test
  (testing "top-level swagger-spec function works as expected"
    (let [handler (core/routes
                    (core/with-swagger (core/GET "/test2" [])
                                       {:swagger-content {:description "a simple get"}})
                    (core/with-swagger (core/GET "/test1/:id" [])
                                       {:swagger-content {:description "another get"}}))]
      (is (= (core/swagger-spec core/swagger-default handler)
             {:swagger "2.0",
              :info {:version "0.0.1", :title "API Docs example", :description "Docs for the API"},
              :paths {"/test2" {:get {:description "a simple get"}}
                      "/test1/{id}" {:get {:description "another get"}}}})))))
