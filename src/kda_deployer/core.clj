(ns kda-deployer.core
  (:use [clojure.java.shell])
  (:require [kda-deployer.aws :as aws]))

(defn git-pull [dir]
  (println "git pull:" dir)
  (let [r (:out (sh "git" "pull" :dir dir))]
    (println r)
    r))

(defn mvn-build [dir]
  (println "mvn-build:" dir)
  (let [result (sh "mvn" "clean" "package" :dir dir)]
    (println (:out result))
    (:exit result)))

(defn deploy-kda-job-jar[]
  (println (sh "aws" "s3" "cp" "target/kda-job-1.0.jar" "s3://ds-kda/kda-job/target/kda-job-1.0.jar" :dir "../kda-job"))
  )

(defn update-kda-job[]
  (let [appVersion (:ApplicationVersionId (:ApplicationDetail (aws/kda-describe-application "ds-kda")))
        objectVersion (:VersionId (aws/s3-head-object "ds-kda" "kda-job/target/kda-job-1.0.jar"))
        result (aws/kda-update-application "ds-kda" appVersion objectVersion)]))

(defn build-kda-processor[])

(defn build-kda-job[]
  (println "Building kda-job")
  (let [kda-job-dir "../kda-job"
        git-pull-result git-pull kda-job-dir
        mvn-build-result mvn-build kda-job-dir
        ]
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
