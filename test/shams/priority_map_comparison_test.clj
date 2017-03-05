(ns shams.priority-map-comparison-test
  (:require [clojure.data.priority-map :as pm]
            [clojure.test :refer :all]
            [shams.priority-queue :refer :all]))

(defn- assert-functional-equivalence-of-priority-queue-with-priority-map
  [variant]
  (let [num-elements 100
        element->priority #(mod % 11)
        elements (-> num-elements range shuffle)
        modes (repeatedly num-elements (constantly :insert))
        operations (map vector modes elements)
        insert-head-start (-> num-elements (* 0.15) int)
        operations (concat
                     (take insert-head-start operations)
                     (interleave
                       (repeatedly (- num-elements insert-head-start) (constantly [:remove]))
                       (drop insert-head-start operations)))]
    (loop [pq (priority-queue element->priority :priority-comparator compare :variant variant)
           pm (pm/priority-map)
           [[mode data] & remaining-operations] operations]
      (case mode
        :insert (let [pq' (conj pq data)
                      pm' (conj pm [data (element->priority data)])]
                  (is (= (count pq') (count pm')))
                  (is (= (priority-queue->top-priority pq') (second (peek pm'))))
                  (recur pq' pm' remaining-operations))
        :remove (let [pq' (pop pq)
                      pm' (pop pm)
                      pq-top (peek pq)
                      pm-top (peek pm)]
                  (is (= (count pq') (count pm')))
                  (is (= (priority-queue->top-priority pq') (second (peek pm'))))
                  (is (= (element->priority pq-top) (second pm-top)))
                  (recur pq' pm' remaining-operations))
        (do
          (is (= (count pq) (count pm))))))))

(deftest test-functional-equivalence-of-priority-queue-fifo-with-priority-map
  (assert-functional-equivalence-of-priority-queue-with-priority-map :queue))

(deftest test-functional-equivalence-of-priority-queue-random-with-priority-map
  (assert-functional-equivalence-of-priority-queue-with-priority-map :set))
