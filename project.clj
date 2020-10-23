(defproject com.ardoq/compojure-swagger "0.1.2"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; FIXME: Cleanup deps
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [liberator "0.15.3"]
                 [ring "1.8.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [compojure "1.5.0"]
                 [metosin/ring-swagger "0.26.2"]
                 [metosin/ring-swagger-ui "3.24.3"]
                 [metosin/spec-tools "0.10.0"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
