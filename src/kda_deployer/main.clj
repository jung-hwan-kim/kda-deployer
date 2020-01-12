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
  (let [result (sh "mvn" "clean" "compile" "package" :dir dir)]
    (println (:out result))
    (if-not (= (:exit result) 0) (throw (:error result)))))

(defn upload-kda-job-jar [app-name jar-path]
  (println "Upload " jar-path " to S3")
  (let [s3-path (str "s3://ds-kda/artifacts/" jar-path)
        result (sh "aws" "s3" "cp" jar-path s3-path :dir (str "../" app-name))
        ]
    (println (:out result))
    (if (= (:exit result) 0)
      (let [s3-desc (aws/s3-head-object "ds-kda" (str "artifacts/" jar-path))
            versionId (:VersionId s3-desc)]
        (println "Uploaded " versionId)
        versionId)
      (println "Failed"))))

(defn update-kda-job[]
  (let [appVersion (:ApplicationVersionId (:ApplicationDetail (aws/kda-describe-application "ds-kda")))
        objectVersion (:VersionId (aws/s3-head-object "ds-kda" "kda-job/target/kda-job-1.0.jar"))
        result (aws/kda-update-application "ds-kda" appVersion objectVersion)]))

(defn run-in-local[app-name]
  (println "* Running in local flink.." app-name)
  (let [cmd (str (System/getProperty "user.home") "/flink/bin/flink")
        result (sh cmd "run" (str "../" app-name "/target/" app-name "-1.0.jar"))]
    (println (:out result))
    (if-not (= (:exit result) 0) (throw (:error result)))))

(defn deploy-job-to-aws[app-name]
  (println "*** Deploying to AWS" app-name)
  (let [appVersion (:ApplicationVersionId (:ApplicationDetail (kda-deployer.aws/kda-describe-application app-name)))
        objectVersion (upload-kda-job-jar app-name (str "target/" app-name "-1.0.jar"))]
    (println "deploying kda - appVer:" appVersion " objectVer:" objectVersion)
    (aws/kda-update-application app-name appVersion objectVersion)))

(defn build-task[]
  (println "Building kda-task")
  (let [kda-task-dir "../kda-task"]
    (git-pull kda-task-dir)
    (lein-build kda-task-dir)))

(defn build-job[app-name]
  (println "Building " app-name)
  (let [kda-job-dir (str "../" app-name)]
     (git-pull kda-job-dir)
     (mvn-build kda-job-dir)))

    ;;(println (sh "ls" "-lh" "../kda-job/target/kda-job-1.0.jar"))
    ;;(println (:out result))

    ;;(println (sh "aws" "s3" "cp" "target/kda-job-1.0.jar" "s3://ds-kda/kda-job/target/kda-job-1.0.jar" :dir "../kda-job"))
    ;;(println (sh "aws" "s3" "ls" "ds-kda/kda-job/target/kda-job-1.0.jar"))

(defn do-build[app-name]
  (println "[BUILD]")
  (if (= app-name "kda-job")
    (build-task))
  (build-job app-name)
  (shutdown-agents)
  )

(defn do-deploy-local[app-name]
  (println "[DEPLOY LOCAL]")
  (if (= app-name "kda-job")
    (build-task))
  (build-job app-name)
  (run-in-local app-name)
  (shutdown-agents)
  )
(defn do-deploy-aws[app-name]
  (println "[DEPLOY AWS")
  (if (= app-name "")
    (build-task))
  (build-job app-name)
  (deploy-job-to-aws app-name)
  (shutdown-agents)
  )
(defn do-stuff[app-name]
  (println app-name)
  (println (str "target/" app-name ".jar"))
  )
(defn -main
  "Deploying kda main"
  [command & args]
  (case command
    "build" (do-build (or (first args) "kda-job"))
    "deploy-local" (do-deploy-local (or (first args) "kda-job"))
    "deploy-aws" (do-deploy-aws (or (first args) "kda-job"))
    "test" (do-stuff  (or (first args) "kda-job"))
    (println "Build & Deploy - options: deploy-local, deploy-aws, .. exiting without doing anything.")
    )
  )
