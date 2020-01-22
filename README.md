# ardoq-swagger

A Clojure library designed to generate swagger documentation for compojure API's.

Provides new macros for HTTP verbs (GET, POST, ...), routes, context.

Also provides a top-level function, `swagger-api`, to generate a swagger.json file + serve it via swagger-ui.

Based on compojure, spec-tools and ring-swagger. This library aims to allow for swagger-dos generation like compojure-api does, but without all the other functionality.


## Usage

Don't


## Errors

This thing completely breaks if you create cross-namespace closures, or even if your routes/contexts consist of variables.

ex:
```clojure
(defn verb (GET "test1" [] req))

(context "path" req verb)
```
Will not work, as verb will not be able to resolve the req reference. This is pretty awful and makes the library kind of completely unusuable as of right now. We're probably gonna use `delay` or something to fix it. 


## License

Copyright © 2020 Ardoq boys

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
