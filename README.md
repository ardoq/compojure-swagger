# compojure-swagger
[![Clojars Project](https://img.shields.io/clojars/v/com.ardoq/compojure-swagger.svg)](https://clojars.org/com.ardoq/compojure-swagger) [![cljdoc badge](https://cljdoc.org/badge/com.ardoq/compojure-swagger)](https://cljdoc.org/d/com.ardoq/copmpojure-swagger/CURRENT) [![CircleCI](https://circleci.com/gh/ardoq/compojure-swagger.svg?style=svg)](https://circleci.com/gh/ardoq/compojure-swagger)

A Clojure library designed to generate swagger documentation (openapi 2.0) for compojure API's.

Provides new macros for HTTP verbs (GET, POST, ...), routes and context.

Also provides a top-level function, `swagger-api`, to generate a swagger.json file + serve it via swagger-ui.

Based on compojure, spec-tools and ring-swagger.
This library aims to allow for swagger-docs generation like [compojure-api](https://github.com/metosin/compojure-api) does,
but without all the other functionality, such as coercion and bidirectional routing.

## Usage
### Simple example
```clojure
(ns hello-world.core
  (:require [compojure-swagger :refer :all]
            [hello-world.my-specs :refer [some-spec]]
            [hello-world.my-handlers :refer [some-handler]]))

(def app
  (routes
    (swagger-api
      {:path "/api-docs"
       :version "0.1"
       :title "The best API in the world"}
      (context "/api" []
        (with-swagger
          (GET "/" [] "<h1>Hello World</h1>")
          {:summary  "Example endpoint"})
        (with-swagger
          (POST "/" request (some-handler request)
          {:summary "Example point endpoint"
           :parameters {:body some-spec}}))))))
```

## Errors

This thing completely breaks if you create cross-namespace closures, or if your routes/contexts consist of variables.

ex:
```clojure
(defn verb (GET "test1" [] req))

(context "path" req verb)
```
Will not work, as verb will not be able to resolve the req reference. However, we don't think this works in regular compojure either, and we're not sure this is even a legitimate use case.


## License

Copyright © 2020 Ardoq

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
