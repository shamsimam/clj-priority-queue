(ns shams.benchmarks-test
  (:require [clojure.data.priority-map :as pm]
            [clojure.test :refer :all]
            [criterium.core :as cc]
            [shams.priority-queue :refer :all])
  (:import clojure.lang.PersistentQueue
           java.util.Comparator
           java.util.PriorityQueue))

(defn driver
  "Driver to run the benchmark on the provided data structure."
  [inital-data insert-fn remove-fn operations xform-fn]
  (loop [loop-data inital-data
         [[mode data] & remaining-operations] operations]
    (case mode
      :insert (recur (insert-fn loop-data (if xform-fn (xform-fn data) data)) remaining-operations)
      :remove (recur (remove-fn loop-data) remaining-operations)
      loop-data)))

(defn bench-data-structures
  [variant-name elements operations element->priority
   & {:keys [java-priority-queue priority-map priority-queue-fifo priority-queue-random]}]
  (let [num-elements (count elements)]
    (when java-priority-queue
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into java.util.priority-queue queue")
      (cc/quick-bench (driver java-priority-queue #(do (.offer %1 %2) %1) #(do (.poll %1) %1) operations nil)))
    (when priority-queue-fifo
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into shams/priority-queue queue")
      (cc/quick-bench (driver priority-queue-fifo conj pop operations nil)))
    (when priority-queue-random
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into shams/priority-queue set")
      (cc/quick-bench (driver priority-queue-random conj pop operations nil)))
    (when priority-map
      (println "------------------------------------------------------")
      (println variant-name num-elements "elements into clojure.data/priority-map")
      (cc/quick-bench (driver priority-map conj pop operations (fn [data] [data (element->priority data)]))))
    (println "\n======================================================\n")))

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
                               :java-priority-queue (PriorityQueue. 64 (reify Comparator
                                                                         (compare [_ e1 e2]
                                                                           (- (element->priority e1) (element->priority e2)))))
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
                               :java-priority-queue (PriorityQueue. 64 (reify Comparator
                                                                         (compare [_ e1 e2]
                                                                           (- (element->priority e1) (element->priority e2)))))
                               :priority-map (pm/priority-map)
                               :priority-queue-fifo (priority-queue element->priority :variant :queue)
                               :priority-queue-random (priority-queue element->priority :variant :set))))
    (println "------------------------------------------------------")))
