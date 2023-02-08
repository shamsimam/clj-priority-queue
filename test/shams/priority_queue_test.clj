(ns shams.priority-queue-test
  (:require [clojure.test :refer :all]
            [shams.priority-queue :refer :all])
  (:import shams.priority_queue.PersistentPriorityQueue))

(deftest test-new-priority-queue
  (testing "Invalid variant"
    (is (thrown-with-msg? IllegalArgumentException #"Illegal variant"
                          (priority-queue int :variant :unsupported))))
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (testing "no element->priority provided uses int for priorities"
        (testing "empty queue with no elements specified"
          (let [pq (priority-queue int)]
            (is (instance? PersistentPriorityQueue pq))
            (is (zero? (count pq)))
            (is (empty? pq))))
        (testing "empty queue with empty elements specified"
          (let [pq (priority-queue int :elements [])]
            (is (instance? PersistentPriorityQueue pq))
            (is (zero? (count pq)))
            (is (empty? pq))))
        (testing "non-empty queue"
          (let [pq (priority-queue int :elements [2 4 1 3])]
            (is (instance? PersistentPriorityQueue pq))
            (is (= 4 (count pq)))
            (is (= pq [4 3 2 1])))))
      (testing "element->priority provided explicitly"
        (testing "empty queue"
          (let [pq (priority-queue #(- 10 %) :elements [])]
            (is (instance? PersistentPriorityQueue pq))
            (is (zero? (count pq)))
            (is (empty? pq))))
        (testing "non-empty queue"
          (let [pq (priority-queue #(- 10 %) :elements [2 4 1 3])]
            (is (instance? PersistentPriorityQueue pq))
            (is (= 4 (count pq)))
            (is (= pq [1 2 3 4]))))
        (testing "matching priorities"
          (let [pq (priority-queue #(mod % 3) :elements [2 4 1 3 8 6 7 9 5])]
            (is (instance? PersistentPriorityQueue pq))
            (is (= 9 (count pq)))
            (is (= pq [2 8 5 4 1 7 3 6 9]))))
        (testing "shuffled 1000 element input"
          (let [source (range 1000)
                input (shuffle source)
                pq (priority-queue #(- 10 %) :elements input)]
            (is (instance? PersistentPriorityQueue pq))
            (is (= (count source) (count pq)))
            (is (= pq source))))))))

(deftest test-priority-queue->available-priorities
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (is (nil? (priority-queue->available-priorities nil)))
      (is (nil? (priority-queue->available-priorities (priority-queue int))))
      (is (= [4 3 2 1] (priority-queue->available-priorities (priority-queue int :elements [2 4 1 3]))))
      (is (= [9 8 7 6] (priority-queue->available-priorities
                         (priority-queue #(- 10 %) :elements [2 4 1 3])))))))

(deftest test-priority-queue->top-priority
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (is (nil? (priority-queue->top-priority nil)))
      (is (nil? (priority-queue->top-priority (priority-queue int))))
      (is (= 4 (priority-queue->top-priority (priority-queue int :elements [2 4 1 3]))))
      (is (= 9 (priority-queue->top-priority
                 (priority-queue #(- 10 %) :elements [2 4 1 3])))))))

(deftest test-priority-queue->element->priority
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (is (nil? (priority-queue->element->priority nil)))
      (is (= int (priority-queue->element->priority (priority-queue int))))
      (is (= int (priority-queue->element->priority (priority-queue int :elements [1]))))
      (is (= + (priority-queue->element->priority (priority-queue + :elements [1])))))))

(deftest test-priority-for
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (is (nil? (priority-for nil 1.5)))
      (is (= 1 (priority-for (priority-queue int) 1.5)))
      (is (= 1 (priority-for (priority-queue int :elements [1]) 1.5)))
      (is (= 1.5 (priority-for (priority-queue + :elements [1]) 1.5))))))

(deftest test-conj-peek-and-pop-fifo-ordering
  (let [pq (priority-queue #(mod % 3) :elements [2 4 1 3 8 7 9 5 6])]
    (testing "testing conj"
      (is (= [2 8 4 1 3]
             (-> (priority-queue #(mod % 3) :elements [3])
                 (conj 2) (conj 4) (conj 1) (conj 8))))
      (is (= [2 8 5 4 1 7 3 9 6]
             (-> (priority-queue #(mod % 3) :elements [3])
                 (conj 2) (conj 4) (conj 1) (conj 8)
                 (conj 7) (conj 9) (conj 5) (conj 6)))))
    (testing "testing FIFO order in pop"
      (is (-> pq (pop) (priority-queue?)))
      (is (-> pq (pop) (pop) (priority-queue?)))
      (is (-> pq (pop) (pop) (pop) (priority-queue?)))
      (is (-> pq (pop) (pop) (pop) (pop) (priority-queue?))))
    (testing "testing FIFO order in peek"
      (is (= 2 (peek pq)))
      (is (= 8 (-> pq (pop) (peek))))
      (is (= 5 (-> pq (pop) (pop) (peek))))
      (is (= 4 (-> pq (pop) (pop) (pop) (peek))))
      (is (= 1 (-> pq (pop) (pop) (pop) (pop) (peek)))))))

(deftest test-seq-interface
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (let [^PersistentPriorityQueue pq
            (priority-queue #(mod % 3)
                            :elements [2 4 1 3 8 7 9 5 6]
                            :variant variant)]
        (is (= (conj pq 0) (.cons pq 0)))
        (is (= (count pq) (.count pq)))
        (is (= (peek pq) (.first pq)))
        (is (= (pop pq) (.more pq)))
        (is (= (pop pq) (.next pq)))
        (when (= variant :queue)
          (is (= (list 2 8 5 4 1 7 3 9 6) (seq pq)))
          (is (= [2 8 5 4 1 7 3 9 6] (seq pq))))))

    (testing "large queues"
      (let [pq (priority-queue identity :elements (range 50000))]
        (is (seq pq))))))

(deftest test-print-priority-queue
  (doseq [variant [:queue :set]]
    (testing (str "Queue variant " (name variant))
      (is (= "()" (-> (priority-queue int :variant variant) print with-out-str)))
      (is (= "(1)" (-> (priority-queue int :variant variant) (conj 1) print with-out-str)))
      (is (= "(2 1)" (-> (priority-queue int :variant variant) (conj 1) (conj 2) print with-out-str)))
      (is (= "(1)" (-> (priority-queue int :variant variant) (conj 1) (conj 2) (pop) print with-out-str))))))
