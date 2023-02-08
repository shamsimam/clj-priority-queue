(ns shams.priority-queue
  (:import clojure.lang.Counted
           clojure.lang.IPersistentCollection
           clojure.lang.IPersistentList
           clojure.lang.ISeq
           clojure.lang.PersistentHashSet
           clojure.lang.PersistentQueue
           clojure.lang.Seqable))

(defn- to-seq
  "Helper function to convert the priority->elements into a seq."
  [sorted-priorities priority->elements]
  (->> sorted-priorities
       (mapcat priority->elements)
       (seq)))

(defprotocol Bufferable
  (insert-into [elements element] "Inserts an element into the elements buffer.")
  (remove-from [elements] "Removes an element from the elements buffer."))

(extend-protocol Bufferable
  PersistentHashSet
  (insert-into [elements element]
    (conj elements element))
  (remove-from [elements]
    (disj elements (first elements)))
  PersistentQueue
  (insert-into [elements element]
    (conj elements element))
  (remove-from [elements]
    (pop elements)))

(defn- push-element
  "Inserts an element into the priority->elements structure based on the variant.
   If the variant is :queue, the element is inserted into a queue.
   Else (the variant is :set) the element is inserted into a set."
  [priority->elements priority seed-value element]
  (let [elements (or (priority->elements priority) seed-value)]
    (assoc priority->elements priority (insert-into elements element))))

(defn- pop-element
  "Inserts an element into the priority->elements structure based on the variant.
   If the variant is :queue, the element is removed using pop.
   Else (the variant is :set) the element is removed using disj."
  [priority->elements]
  (let [[priority priority-elements] (first priority->elements)
        priority-elements-count (count priority-elements)]
    (if (= 1 priority-elements-count)
      (dissoc priority->elements priority)
      (assoc priority->elements priority (remove-from priority-elements)))))

(deftype PersistentPriorityQueue
  [element->priority seed-value ^int num-elements priority->elements]

  Seqable
  (seq [_] (to-seq (-> priority->elements keys vec) priority->elements))

  ISeq
  (first [this] (.peek this))
  (next [this] (.pop this))
  (more [this] (.pop this))

  Counted
  (count [_] num-elements)

  IPersistentCollection
  (empty [_] (PersistentPriorityQueue. element->priority seed-value 0 (sorted-map)))
  (equiv [this other] (and (sequential? other) (= (.seq this) (seq other))))

  IPersistentList
  (cons [_ element]
    (PersistentPriorityQueue.
      element->priority
      seed-value
      (inc num-elements)
      (push-element priority->elements (element->priority element) seed-value element)))
  (peek [_]
    (when (pos? num-elements)
      (-> priority->elements first val first)))
  (pop [_]
    (PersistentPriorityQueue.
      element->priority
      seed-value
      (dec num-elements)
      (pop-element priority->elements))))

(defn priority-queue
  "Factory function to create a priority queue populated by elements.
   The element->priority is invoked on an element to compute the priority.
   The priority-comparator (defaults to larger values having higher priority) is used to order priorities.
   The variant (defaults to :queue) is used to determine how to break ties for equal priority elements.
   Valid values for variant are :queue (FIFO ordering) and :set (random tie-breaking)."
  ([element->priority & {:keys [elements priority-comparator variant]
                         :or   {priority-comparator #(compare %2 %1), variant :queue}}]
   (let [seed-values {:queue PersistentQueue/EMPTY, :set #{}}
         seed-value (get seed-values variant)]
     (when-not seed-value
       (throw (IllegalArgumentException. ^String (str "Illegal variant" variant
                                                      ", supported variants are :queue and :set"))))
     (into
       (PersistentPriorityQueue. element->priority seed-value 0 (sorted-map-by priority-comparator))
       elements))))

(defn priority-queue?
  "Returns true if the reference passed in is an instance of a priority queue."
  [reference]
  (instance? PersistentPriorityQueue reference))

(defn priority-queue->available-priorities
  "Returns a sorted vector of available priorities in the queue."
  [priority-queue]
  (when (priority-queue? priority-queue)
    (-> (.priority->elements ^PersistentPriorityQueue priority-queue) keys seq)))

(defn priority-queue->top-priority
  "Returns the highest priority of the available elements in the priority queue."
  [priority-queue]
  (-> priority-queue priority-queue->available-priorities first))

(defn priority-queue->element->priority
  "Returns the function used to compute the priority of elements in the priority queue."
  [priority-queue]
  (when (priority-queue? priority-queue)
    (.element->priority ^PersistentPriorityQueue priority-queue)))

(defn priority-for
  "Returns the priority of the element if it were to be inserted into the priority queue."
  [priority-queue element]
  (when-let [element->priority (priority-queue->element->priority priority-queue)]
    (element->priority element)))
