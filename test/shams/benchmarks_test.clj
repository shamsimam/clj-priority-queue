(ns shams.benchmarks-test
  (:require [clojure.data.priority-map :as pm]
            [clojure.test :refer :all]
            [criterium.core :as cc]
            [shams.priority-queue :refer :all])
  (:import clojure.lang.PersistentQueue))

(defn driver
  "Driver to run the benchmark on the provided data structure."
  [inital-data insert-fn remove-fn operations xform-fn]
  (loop [loop-data inital-data
         [[mode data] & remaining-operations] operations]
    #_(println (.toString loop-data))
    (case mode
      :insert (recur (insert-fn loop-data (if xform-fn (xform-fn data) data)) remaining-operations)
      :remove (recur (remove-fn loop-data) remaining-operations)
      loop-data)))

(defn insert-elements
  [initial-data insert-fn elements xform-fn]
  (loop [loop-data initial-data
         [element & remaining] elements]
    (if element
      (recur (insert-fn loop-data (xform-fn element)) remaining)
      loop-data)))

(defn bench-data-structures
  [variant-name elements operations element->priority
   & {:keys [list ordered-set priority-map priority-queue-fifo priority-queue-random queue sorted-map unordered-set vector]}]
  (let [num-elements (count elements)]
    (when list
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into List")
      (cc/quick-bench (driver list conj rest operations nil)))
    (when vector
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into Vector")
      (cc/quick-bench (driver vector conj #(subvec % 1) operations nil)))
    (when unordered-set
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into Unordered-Set")
      (cc/quick-bench (driver unordered-set conj #(clojure.set/difference %1 #{(first %1)}) operations nil)))
    (when ordered-set
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into Ordered-Set")
      (cc/quick-bench (driver ordered-set conj #(clojure.set/difference %1 #{(apply min %1)}) operations nil)))
    (when sorted-map
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into Sorted-Map")
      (cc/quick-bench (driver sorted-map conj (fn [m] (dissoc m (-> m first key))) operations (fn [data] [data (element->priority data)]))))
    (when queue
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into Queue")
      (cc/quick-bench (driver queue conj pop operations nil)))
    (when priority-queue-fifo
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into PriorityQueue:FIFO")
      (cc/quick-bench (driver priority-queue-fifo conj pop operations nil)))
    (when priority-queue-random
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into PriorityQueue:Random")
      (cc/quick-bench (driver priority-queue-random conj pop operations nil)))
    (when priority-map
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into PriorityMap")
      (cc/quick-bench (driver priority-map conj pop operations (fn [data] [data (element->priority data)]))))
    (println "\n======================================================\n")))

(deftest ^:benchmark test-insert-only-comparison-with-fixed-priority
  (println "test-insert-only-comparison-with-fixed-priority")
  (let [num-elements 1000
        elements (-> num-elements range shuffle)
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)
        element->priority (constantly 0)]
    (bench-data-structures "Insert at fixed priority" elements operations element->priority
                           :list (list)
                           :ordered-set #{}
                           :priority-map (pm/priority-map)
                           :priority-queue-fifo (priority-queue element->priority :variant :queue)
                           :priority-queue-random (priority-queue element->priority :variant :set)
                           :queue PersistentQueue/EMPTY
                           :sorted-map (sorted-map)
                           :unordered-set #{}
                           :vector [])))

(deftest ^:benchmark test-remove-only-comparison-with-fixed-priority
  (println "test-remove-only-comparison-with-fixed-priority")
  (let [num-elements 1000
        elements (-> num-elements range shuffle)
        operations (repeatedly num-elements (constantly [:remove]))
        element->priority (constantly 0)]
    (bench-data-structures "Remove at fixed priority" elements operations element->priority
                           :list (insert-elements (list) conj elements identity)
                           :ordered-set (insert-elements #{} conj elements identity)
                           :priority-map (insert-elements (pm/priority-map) conj elements (fn [data] [data (element->priority data)]))
                           :priority-queue-fifo (insert-elements (priority-queue element->priority :variant :queue) conj elements identity)
                           :priority-queue-random (insert-elements (priority-queue element->priority :variant :set) conj elements identity)
                           :queue (insert-elements PersistentQueue/EMPTY conj elements identity)
                           :sorted-map (insert-elements (sorted-map) conj elements (fn [data] [data (element->priority data)]))
                           :unordered-set (insert-elements #{} conj elements identity)
                           :vector (insert-elements [] conj elements identity))))


(deftest ^:benchmark test-insert-remove-comparison-with-fixed-priority
  (println "test-insert-remove-comparison-with-fixed-priority")
  (let [num-elements 1000
        elements (-> num-elements range shuffle)
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)
        insert-head-start (-> num-elements (* 0.25) int)
        operations (concat
                     (take insert-head-start operations)
                     (interleave
                       (repeatedly (- num-elements insert-head-start) (constantly [:remove]))
                       (drop insert-head-start operations))
                     (repeatedly insert-head-start (constantly [:remove])))
        element->priority (constantly 0)]
    (bench-data-structures "Insert and Remove at fixed priority" elements operations element->priority
                           :list (list)
                           :ordered-set #{}
                           :priority-map (pm/priority-map)
                           :priority-queue-fifo (priority-queue element->priority :variant :queue)
                           :priority-queue-random (priority-queue element->priority :variant :set)
                           :queue PersistentQueue/EMPTY
                           :sorted-map (sorted-map)
                           :unordered-set #{}
                           :vector [])))

(deftest ^:benchmark test-insert-only-of-ints-comparison-with-changing-priority
  (println "test-insert-only-of-ints-comparison-with-changing-priority")
  (let [num-elements 1000
        elements (-> num-elements range shuffle)
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)]
    (doseq [level [50 250 500]]
      (let [element->priority #(mod %1 level)]
        (bench-data-structures (str "Insert Only ints at dynamic priority-" level) elements operations element->priority
                               :priority-map (pm/priority-map)
                               :priority-queue-fifo (priority-queue element->priority :variant :queue)
                               :priority-queue-random (priority-queue element->priority :variant :set))))
    (println "------------------------------------------------------")))

(deftest ^:benchmark test-remove-only-of-ints-comparison-with-changing-priority
  (println "test-remove-only-of-ints-comparison-with-changing-priority")
  (let [num-elements 1000
        elements (->> num-elements range shuffle)
        operations (repeatedly num-elements (constantly [:remove]))]
    (doseq [level [50 250 500]]
      (let [element->priority #(mod %1 level)]
        (bench-data-structures (str "Remove Only ints at dynamic priority-" level) elements operations element->priority
                               :priority-map (insert-elements (pm/priority-map) conj elements (fn [data] [data (element->priority data)]))
                               :priority-queue-fifo (insert-elements (priority-queue element->priority :variant :queue) conj elements identity)
                               :priority-queue-random (insert-elements (priority-queue element->priority :variant :set) conj elements identity))))
    (println "------------------------------------------------------")))

(deftest ^:benchmark test-insert-only-of-maps-comparison-with-changing-priority
  (println "test-insert-only-of-maps-comparison-with-changing-priority")
  (let [num-elements 1000
        elements (->> num-elements range shuffle (map (fn [elem] {:data elem :type :map})))
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)]
    (doseq [level [50 250 500]]
      (let [element->priority #(mod (:data %1) level)]
        (bench-data-structures (str "Insert Only maps at dynamic priority-" level) elements operations element->priority
                               :priority-map (pm/priority-map)
                               :priority-queue-fifo (priority-queue element->priority :variant :queue)
                               :priority-queue-random (priority-queue element->priority :variant :set))))
    (println "------------------------------------------------------")))

(deftest ^:benchmark test-remove-only-of-maps-comparison-with-changing-priority
  (println "test-remove-only-of-maps-comparison-with-changing-priority")
  (let [num-elements 1000
        elements (->> num-elements range shuffle (map (fn [elem] {:data elem :type :map})))
        operations (repeatedly num-elements (constantly [:remove]))]
    (doseq [level [50 250 5000]]
      (let [element->priority #(mod (:data %1) level)]
        (bench-data-structures (str "Remove Only maps at dynamic priority-" level) elements operations element->priority
                               :priority-map (insert-elements (pm/priority-map) conj elements (fn [data] [data (element->priority data)]))
                               :priority-queue-fifo (insert-elements (priority-queue element->priority :variant :queue) conj elements identity)
                               :priority-queue-random (insert-elements (priority-queue element->priority :variant :set) conj elements identity))))
    (println "------------------------------------------------------")))

(deftest ^:benchmark test-insert-remove-comparison-of-ints-with-changing-priority
  (println "test-insert-remove-comparison-of-ints-with-changing-priority")
  (let [num-elements 10000
        elements (-> num-elements range shuffle)
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)
        insert-head-start (-> num-elements (* 0.25) int)
        operations (concat
                     (take insert-head-start operations)
                     (interleave
                       (repeatedly (- num-elements insert-head-start) (constantly [:remove]))
                       (drop insert-head-start operations)))]
    (doseq [level [50 250 500]]
      (let [element->priority #(mod %1 level)]
        (bench-data-structures (str "Insert and Remove ints at dynamic priority-" level) elements operations element->priority
                               :priority-map (pm/priority-map)
                               :priority-queue-fifo (priority-queue element->priority :variant :queue)
                               :priority-queue-random (priority-queue element->priority :variant :set))))
    (println "------------------------------------------------------")))

(deftest ^:benchmark test-insert-remove-comparison-of-maps-with-changing-priority
  (println "test-insert-remove-comparison-of-maps-with-changing-priority")
  (let [num-elements 10000
        elements (->> num-elements range shuffle (map (fn [elem] {:data elem :type :map})))
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)
        insert-head-start (-> num-elements (* 0.25) int)
        operations (concat
                     (take insert-head-start operations)
                     (interleave
                       (repeatedly (- num-elements insert-head-start) (constantly [:remove]))
                       (drop insert-head-start operations)))]
    (doseq [level [50 250 500]]
      (let [element->priority #(mod (:data %1) level)]
        (bench-data-structures (str "Insert and Remove maps at dynamic priority-" level) elements operations element->priority
                               :priority-map (pm/priority-map)
                               :priority-queue-fifo (priority-queue element->priority :variant :queue)
                               :priority-queue-random (priority-queue element->priority :variant :set))))
    (println "------------------------------------------------------")))
