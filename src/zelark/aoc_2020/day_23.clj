(ns zelark.aoc-2020.day-23
  (:require [clojure.java.io :as io]))

;; --- Day 23: Crab Cups ---
;; https://adventofcode.com/2020/day/23

(def input (slurp (io/resource "input_23.txt")))

(defn parse-input [input]
  (mapv #(Long/parseLong %) (re-seq #"\d" input)))

(defprotocol ICircleNode
  (value    [this])
  (get-next [this])
  (insert   [this node])
  (insert-n [this start end])
  (remove-n [this n]))

(defrecord CircleNode [value next]
  ICircleNode
  (value    [_] value)
  (get-next [_] (deref next))
  (insert [this node]
          (dosync (ref-set (:next node) (get-next this))
                  (ref-set (:next this) node)))
  (insert-n [this start end]
            (dosync (ref-set (:next end) (get-next this))
                    (ref-set (:next this) start)))
  (remove-n [this n]
            (dosync (ref-set (:next this) (first (drop (inc n) (iterate get-next this)))))))

(defmethod print-method CircleNode [^CircleNode object ^java.io.Writer writer]
  (.append writer (format "[%s]" (value object))))

(defn circle-node [value]
  (let [node (CircleNode. value (ref nil))
        _    (dosync (ref-set (:next node) node))]
    node))

(defn build-cups [labels]
  (first (reduce (fn [[acc prev] label]
                   (let [node (circle-node label)]
                     (when prev (insert prev node))
                     [(assoc acc label node) node]))
                 [{} nil] labels)))

(defn next-dest [^long n ^long curr]
  (let [dest (unchecked-dec curr)]
    (if (< dest 1) n dest)))

(defn play-game [{:keys [labels ^long limit result-fn]}]
  (let [cups      (build-cups labels)
        cnt       (count labels)
        next-dest (partial next-dest cnt)]
    (loop [curr (first labels) i 1]
      (when (<= i limit)
        (let [ccup (cups curr)
              a    (get-next ccup)
              b    (get-next a)
              c    (get-next b)
              abc  (set (map value [a b c]))
              dest (first (drop-while abc (rest (iterate next-dest curr))))
              dcup (cups dest)
              _    (remove-n ccup 3)
              _    (insert-n dcup a c)]
          (recur (value (get-next ccup)) (inc i)))))
    (result-fn (get-next (cups 1)))))

;; part 1
(play-game {:labels (parse-input input)
            :limit 100
            :result-fn #(reduce (fn [acc cup] (+ (* acc 10) (value cup)))
                                0
                                (take 8 (iterate get-next %)))}) ; 49725386

;; part 2
(play-game {:labels (into (parse-input input) (range 10 1000001))
            :limit  1e7
            :result-fn #(* (value %) (value (get-next %)))}) ; 538935646702
