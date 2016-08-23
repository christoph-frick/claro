(ns claro.engine.resolver
  (:require [claro.runtime.impl :as impl]
            [claro.data
             [protocols :as p]
             [tree :refer [wrap-tree]]]))

(defn- resolve-them-all!
  [impl adapter env [head :as batch]]
  (cond (p/batched-resolvable? head)
        (adapter impl #(p/resolve-batch! head env batch))

        (next batch)
        (let [deferreds (mapv
                          (fn [item]
                            (adapter impl #(p/resolve! item env)))
                          batch)]
          (impl/zip impl deferreds))

        :else
        (let [deferred (adapter impl #(p/resolve! head env))]
          (impl/chain1 impl deferred vector))))

(defn build
  "Generate a resolver function for `claro.runtime/run`, suitable for
   processing `claro.data.protocols/Resolvable` values."
  [impl adapter]
  {:pre [impl (fn? adapter)]}
  (fn [env batch]
    (impl/chain
      impl
      (resolve-them-all! impl adapter env batch)
      #(map wrap-tree %))))
