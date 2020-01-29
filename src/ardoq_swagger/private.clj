(ns ardoq-swagger.private)
  
(defrecord Route [path method info children])

(defmacro routes [& handlers]
  `(map->Route
     {:path nil
      :children (list ~@handlers)
      }))

(defmacro context
  [path args & body]
  `(map->Route {:path     ~path
                :children (list ~@body)
                }))

(defn- make-verb-route
  [verb path args body]
  `(map->Route {:path    ~path
                :method  ~verb
                })) 

(defmacro GET
  [path args & body]
  (make-verb-route :get path args body))

(defmacro POST
  [path args & body]
  (make-verb-route :post path args body))

(defmacro PUT
  [path args & body]
  (make-verb-route :put path args body))

(defmacro DELETE
  [path args & body]
  (make-verb-route :delete path args body))

(defmacro ANY
  [path args & body]
  (make-verb-route :any path args body))
