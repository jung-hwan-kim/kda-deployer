(defproject kda-deployer "0.1.0-SNAPSHOT"
  :description "AWS KDA Application Builder"
  :url "https://github.com/jungflykim/kda-deployer"
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :java-source-paths ["main/java"]
  :javac-options     ["-target" "1.8" "-source" "1.8"]
  :repl-options {:init-ns ds-stream-builder.core}
  :main kda-deployer.core/-main
  :aot :all)