(ns kda-deployer.main
  (:use [clojure.java.shell])
  (:gen-class)
  (:require [kda-deployer.aws :as aws]))

(defn git-pull [dir]
  (println "git pull:" dir)
  (let [r (:out (sh "git" "pull" :dir dir))]
    (println r)
    r))

(defn lein-build [dir]
  (println "lein-build:" dir)
  (let [result (sh "lein" "do" "clean," "compile," "jar," "install" :dir dir)]
    (println (:out result))
    (if-not (= (:exit result) 0) (throw (:error result)))))

(defn mvn-build [dir]
  (println "mvn-build:" dir)
  (let [result (sh "mvn" "clean" "compile" "clojure:compile" "package" :dir dir)]
    (println (:out result))
    (if-not (= (:exit result) 0) (throw (:error result)))))

(defn upload-kda-job-jar [jar-path]
  (println "Upload " jar-path " to S3")
  (let [s3-path (str "s3://ds-kda/kda-job/" jar-path)
        result (sh "aws" "s3" "cp" jar-path s3-path :dir "../kda-job")
        ]
    (println (:out result))
    (if (= (:exit result) 0)
      (let [s3-desc (aws/s3-head-object "ds-kda" (str "kda-job/" jar-path))
            versionId (:VersionId s3-desc)]
        (println "Uploaded " versionId)
        versionId)
      (println "Failed"))))

(defn update-kda-job[]
  (let [appVersion (:ApplicationVersionId (:ApplicationDetail (aws/kda-describe-application "ds-kda")))
        objectVersion (:VersionId (aws/s3-head-object "ds-kda" "kda-job/target/kda-job-1.0.jar"))
        result (aws/kda-update-application "ds-kda" appVersion objectVersion)]))

(defn deploy-and-run-in-local[]
  ; ~/flink/bin/flink run target/kda-job-1.0.jar
  (println "* Running in local flink..")
  (let [cmd (str (System/getProperty "user.home") "/flink/bin/flink")
        result (sh cmd "run" "../kda-job/target/kda-job-1.0.jar")]
    (println (:out result))
    (if-not (= (:exit result) 0) (throw (:error result)))))

(defn deploy-job-to-aws[]
  (println "*** Deploying to AWS")
  (let [appVersion (:ApplicationVersionId (:ApplicationDetail (kda-deployer.aws/kda-describe-application "ds-kda")))
        objectVersion (upload-kda-job-jar "target/kda-job-1.0.jar")
        ]
    (println "deploying kda - appVer:" appVersion " objectVer:" objectVersion)
    (aws/kda-update-application "ds-kda" appVersion objectVersion)))

(defn build-task[]
  (println "Building kda-task")
  (let [kda-task-dir "../kda-task"]
    (git-pull kda-task-dir)
    (lein-build kda-task-dir)))

(defn build-job[]
  (println "Building kda-job")
  (let [kda-job-dir "../kda-job"]
     (git-pull kda-job-dir)
     (mvn-build kda-job-dir)))

    ;;(println (sh "ls" "-lh" "../kda-job/target/kda-job-1.0.jar"))
    ;;(println (:out result))

    ;;(println (sh "aws" "s3" "cp" "target/kda-job-1.0.jar" "s3://ds-kda/kda-job/target/kda-job-1.0.jar" :dir "../kda-job"))
    ;;(println (sh "aws" "s3" "ls" "ds-kda/kda-job/target/kda-job-1.0.jar"))

(defn do-build[args]
  (println "[BUILD]")
  (build-task)
  (build-job)
  (shutdown-agents)
  )

(defn do-deploy-local[args]
  (println "[DEPLOY LOCAL]")
  (build-task)
  (build-job)
  (deploy-and-run-in-local)
  (shutdown-agents)
  )
(defn do-deploy-aws[args]
  (println "[DEPLOY AWS")
  (build-task)
  (build-job)
  (deploy-job-to-aws)
  (shutdown-agents)
  )
(defn -main
  "Deploying kda main"
  [command & args]
  (case command
    "build" (do-build args)
    "deploy-local" (do-deploy-local args)
    "deploy-aws" (do-deploy-aws args)
    (println "Build & Deploy - options: deploy-local, deploy-aws, .. exiting without doing anything.")
    )
  )
