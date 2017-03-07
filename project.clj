(defproject shams/priority-queue "0.1.1"
  :description "Provides a persistent priority queue implementation, written entirely in Clojure."
  :url "https://github.com/shamsimam/clj-priority-queue"
  :license {:name "Eclipse Public License - Version 1.0"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  
  :aliases {"benchmark" ["test" ":benchmark"]}
  :aot [shams.priority-queue]
  :deploy-repositories [["releases" :clojars]]
  :global-vars {*unchecked-math* true
                *warn-on-reflection* true}
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-server" "-Xmx4g" "-Xms2g" "-XX:+UseParallelGC"]
  :lein-release {:scm :git
                 :deploy-via :clojars}
  :profiles {:dev  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :test {:dependencies [[criterium "0.4.4"]
                                   [org.clojure/data.priority-map "0.0.7"]]}}
  :test-selectors {:default (fn [m] (not (:benchmark m)))
                   :benchmark :benchmark
                   :all (constantly true)})
