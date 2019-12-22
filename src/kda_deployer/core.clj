(ns kda-deployer.core
  (:use [clojure.java.shell]))

(defn build-kda-processor[]
  )

(defn build-kda-job[]
  (println "Building kda-job")
  (let [result (sh "mvn" "clean" "package" :dir "../kda-job")]
    (println (:exit result))
    (println (sh "ls" "-lh" "../kda-job/target/kda-job-1.0.jar"))
    ;;(println (:out result))
    (println (sh "aws" "s3" "cp" "target/kda-job-1.0.jar" "s3://ds-kda/kda-job/target/kda-job-1.0.jar" :dir "../kda-job"))
    (println (sh "aws" "s3" "ls" "ds-kda/kda-job/target/kda-job-1.0.jar"))
    )
  )

(defn -main
  "Deploying kda main"
  []
  (println "...")
  (build-kda-processor)
  (build-kda-job)
  (println "*** DONE ***")
  (shutdown-agents)
  )
