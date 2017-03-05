# shams/priority-queue

An unbounded priority queue implementation written in Clojure. 
It relies on Clojure's persistent data structures, so the priority queue is itself also persistent.
It supports regular `seq` operations along with the `conj`/`peek`/`pop` operations.

Each element in the priority queue has a priority, computed by invoking `(element->priority element)`.
The elements of the priority queue are ordered according to the ordering of the priorities. 
The head of the queue is the element with the **highest value** of priority. 

If multiple elements are tied for highest priority value, then the first element can be determined in one of two ways. If the `:queue` variant was used, the tie is broken using FIFO arrival order of the elements. 
If the `:set` variant is used, the tie is broken randomly.
Note: the `:set` variant does not allow duplicate values into the priority queue.


## Releases and Dependency Information

Releases are published on [Clojars].
Latest stable release is 0.1.0.

[Leiningen] dependency information:

    [shams/priority-queue "0.1.0"]

### Dependencies

The priority queue is written entirely in Clojure and has no runtime dependencies other than Clojure itself.

I have tested this library with Clojure 1.8.0. You can run the tests with `lein test`.


## Usage

```clojure
(ns examples
  (:require [shams.priority-queue :as pq]))
```  

The standard way to construct a priority queue is with `priority-queue`.
This creates a priority queue with a default `variant` of `:queue` and `element->priority` function of `#(mod % 5)`:
```clojure
user=> (pq/priority-queue #(mod % 5))
() ;; creates an empty priority queue
```

Elements can be pre-populated into the queue using the `:elements` optional argument in `priority-queue`.
The default implementation uses higher priority values as higher priority.
```clojure
user=> (pq/priority-queue #(mod % 5) :elements [1 2 3 4 5 6])
(4 3 2 1 6 5)
```

Inserting an element is done using the `conj` function:
```clojure
user=> (-> (pq/priority-queue #(mod % 5)) (conj 6) (conj 2) (conj 5) (conj 7))
(2 7 6 5)
```
or by using `into`
```clojure
user=> (into (pq/priority-queue #(mod % 5)) [6 2 5 7])
(2 7 6 5)
```

The `peek` operation returns the highest priority element in the queue.
The `pop` operation removes the highest priority element from the queue and returns a new priority queue.
```clojure
user=> (def p (pq/priority-queue #(mod % 5) :elements [1 2 3 4 5 6]))
#'user/p
user=> (peek p)
4
user=> (pop p)
(3 2 1 6 5)
```

It is possible to use a custom comparator to determine the highest priority in the queue using the `:priority-comparator` option.
The following example uses smaller priority values to represent higher priority:
```clojure
user=> (def p (pq/priority-queue #(mod % 5) :elements [1 2 3 4 5 6] :priority-comparator compare))
user=> p
(5 1 6 2 3 4)
#'user/p
user=> (peek p)
5
user=> (pop p)
(1 6 2 3 4)
```

Priority queues are countable and can be tested for emptiness:
```clojure
user=> (count (pq/priority-queue #(mod % 5)))
0
user=> (count (pq/priority-queue #(mod % 5) :elements [1 2]))
2
user=> (empty? (pq/priority-queue #(mod % 5)))
true
user=> (empty? (pq/priority-queue #(mod % 5) :elements [1 2]))
false
```

Whether a reference is a priority queue can be checked using the `priority-queue?` function:
```clojure
user=> (priority-queue? (pq/priority-queue #(mod % 5)))
true
user=> (priority-queue? [])
false
```

The available priorities of the elements in the queue can be queried using the `priority-queue->available-priorities` function. 
The top priority of the elements in the priority queue can be retrieved using the `priority-queue->top-priority` function.
```clojure
user=> (priority-queue->available-priorities (pq/priority-queue #(mod % 5) :elements [1 2 3 6]))
(3 2 1)
user=> (priority-queue->top-priority (pq/priority-queue #(mod % 5) :elements [1 2 3 6]))
3
```

The `element->priority` can be retrieved using the `priority-queue->element->priority` function:
```clojure
user=> (priority-queue->element->priority (pq/priority-queue int))
#object[clojure.core$int 0xe8110ef "clojure.core$int@e8110ef"]
```

To look up the priority of a given element, use the `priority-for` function:
```clojure
user=> (priority-for (pq/priority-queue #(mod % 5)) 9)
4
```

### Priority queue variants

If multiple elements are tied for highest priority value, then the first element can be determined in one of two ways. If the `:queue` variant was used, the tie is broken using FIFO arrival order of the elements. 
If the `:set` variant is used, the tie is broken randomly.
Note: the `:set` variant drops duplicate values inserted into the priority queue.

```clojure
user=> (def p1 (pq/priority-queue #(mod % 2) :elements [3 4 5 6 7 5] :variant :queue))
#'user/p1
user=> p1
(3 5 7 5 4 6)
user=> (peek p1)
3
user=> (pop p1)
(5 7 5 4 6)

user=> (def p2 (pq/priority-queue #(mod % 2) :elements [3 4 5 6 7 5] :variant :set))
#'user/p2
user=> p2
(7 3 5 4 6)
user=> (peek p2)
7
user=> (pop p2)
(3 5 4 6)
```


## Bugs and Enhancements

Please open issues against the [official priority-queue repo on Github](https://github.com/shamsimam/clj-priority-queue/issues).


## Change Log

* Version 0.1.0 released on 05-Mar-2017.


## Performance

The priority queue implementation is about twice as fast as the [clojure.data.priority-map](https://github.com/clojure/data.priority-map) implementation as it
* (mainly) uses a single sorted map to maintain the priorities, 
* provides no support for mutating element priority once in the queue, and
* functions as a queue rather than a map.

Check out the benchmarks in `shams.benchmarks-test`; or run `lein benchmark`. 
If you have scenarios where the priority queue is not performing as well as expected, please let me know.

Some numbers measured using the [Criterium] benchmarking library on a Mac OS 10.12.3 with 2.8 GHz Intel Core i7 quad-core processor:
```
Insert and Remove ints at dynamic priority-500 10000 elements execution time mean
PriorityQueue-FIFO: 15.971518 ms
PriorityQueue-Random: 18.875325 ms
PriorityMap: 25.676095 ms

Insert and Remove maps at dynamic priority-500 10000 elements execution time mean
PriorityQueue-FIFO: 14.942025 ms
PriorityQueue-Random: 19.038114 ms
PriorityMap: 26.438824 ms
```


## License

Released under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html).


[Clojars]: http://clojars.org/
[Criterium]: https://github.com/hugoduncan/criterium
[Leiningen]: http://leiningen.org/
