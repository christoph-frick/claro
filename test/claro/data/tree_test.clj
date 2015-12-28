(ns claro.data.tree-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [clojure.set :as set]
            [claro.data.resolvable :as r]
            [claro.data.tree :as tree]))

;; ## Generator

(defrecord R [x]
  r/Resolvable)

(def gen-resolvables
  (gen/fmap
    (fn [length]
      (set (repeatedly length #(->R (rand-int 1000)))))
    gen/s-pos-int))

(defn gen-resolvable-tree
  [rs]
  (gen/recursive-gen
    (fn [g]
      (gen/one-of
        [(gen/list g)
         (gen/set g)
         (gen/vector g)
         (gen/map g g)]))
    (gen/one-of [(gen/elements rs) gen/string-ascii])))

(def gen-tree
  (->> (fn [rs]
         (gen/tuple
           (gen/return rs)
           (gen-resolvable-tree rs)))
       (gen/bind gen-resolvables)
       (gen/fmap
         (fn [[rs tree]]
           [rs (tree/wrap-tree tree)]))))

;; ## Test

(defn- attempt-resolution
  [tree resolvables]
  (or (empty? resolvables)
      (let [resolvable->resolved (->> #(rand-nth (seq resolvables))
                                      (repeatedly (quot (count resolvables) 2))
                                      (map (juxt identity :x))
                                      (into {}))
            resolved (set (keys resolvable->resolved))
            tree' (tree/apply-resolved-values tree resolvable->resolved)
            rs (tree/resolvables tree')]
        (and (is (not-any? resolved rs))
             (is (= rs (set/difference resolvables resolved)))))))

(defspec t-tree 200
  (prop/for-all
    [[available-resolvables tree] gen-tree]
    (let [rs (tree/resolvables tree)]
      (and (is (or (set? rs) (nil? rs)))
           (is (every? r/resolvable? rs))
           (is (every? #(contains? available-resolvables %) rs))
           (attempt-resolution tree rs)))))