(ns claro.projection.objects
  (:require [claro.projection.protocols :refer [Projection]]
            [claro.data.ops.chain :refer [chain-eager]]))

;; ## Helpers

(defn- leaf?
  [value]
  (not (coll? value)))

(defn- assert-leaf
  [value]
  (when-not (leaf? value)
    (throw
      (IllegalStateException.
        (str "leaf projection template can only be used for non-collection "
             "values.\n"
             "value: " (pr-str value)))))
  value)

;; ## Templates

(defrecord LeafProjection []
  Projection
  (project [_ value]
    (chain-eager value assert-leaf)))

(def leaf
  "Projection template for leaf values (equivalent to `nil` but preferable
   since more explicit)."
  (->LeafProjection))

(extend-protocol Projection
  nil
  (project [_ value]
    (chain-eager value assert-leaf)))

;; ## Printing

(defmethod print-method LeafProjection
  [value ^java.io.Writer writer]
  (.write writer "<leaf>"))
