(ns claro.data.ops.chain
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [blocking-composition :refer [->BlockingComposition]]
             [composition :refer [->ResolvableComposition]]
             [leaf :refer [->ResolvableLeaf]]]
            [claro.data.tree :refer [wrap-tree]]))

;; ## Helpers

(defn- matches?
  [value predicate]
  (and (p/resolved? value)
       (or (not predicate)
           (predicate value))))

;; ## Chains

(defn chain-when
  "Apply the given function to the (potentially not fully-resolved) value
   once `predicate` is fulfilled."
  [value predicate f]
  (let [f' (comp wrap-tree f)]
    (cond (p/resolvable? value)
          (->ResolvableComposition (->ResolvableLeaf value) predicate f')

          (p/wrapped? value)
          (->ResolvableComposition value predicate f')

          (matches? value predicate)
          (f' value)

          :else
          (let [tree (wrap-tree value)]
            (if (p/resolved? tree)
              (throw
                (IllegalStateException.
                  (format "'predicate' does not hold for fully resolved: %s"
                          (pr-str value))))
              (->ResolvableComposition tree predicate f'))))))

(defn chain-blocking
  "Apply the given function once `value` is fully resolved."
  [value f]
  (let [f' (comp wrap-tree f)]
    (if (p/resolvable? value)
      (->BlockingComposition (->ResolvableLeaf value) f')
      (let [tree (wrap-tree value)]
        (if (p/resolved? tree)
          (f' tree)
          (->BlockingComposition tree f'))))))

(defn chain-eager
  "Apply the given function once the value is no longer a `Resolvable` or
   wrapped."
  [value f]
  (chain-when value nil f))