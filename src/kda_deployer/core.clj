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


(defn deploy-kda-job[]
  (println "deploying..")
  (let [appVersion (:ApplicationVersionId (:ApplicationDetail (kda-deployer.aws/kda-describe-application "ds-kda")))
        objectVersion (upload-kda-job-jar "target/kda-job-1.0.jar")
        ]
    (println "deploying kda - appVer:" appVersion " objectVer:" objectVersion)
    (aws/kda-update-application "ds-kda" appVersion objectVersion)))

(defn build-kda-processor[])

(defn build-kda-job[]
  (println "Building kda-job")
  (let [kda-job-dir "../kda-job"
        git-pull-result (git-pull kda-job-dir)
        mvn-build-result (mvn-build kda-job-dir)
        ]
    (if (= mvn-build-result 0)
      (let [result (deploy-kda-job)]
        (println result))
      (println "BUILD FAILED"))))

    ;;(println (sh "ls" "-lh" "../kda-job/target/kda-job-1.0.jar"))
    ;;(println (:out result))

    ;;(println (sh "aws" "s3" "cp" "target/kda-job-1.0.jar" "s3://ds-kda/kda-job/target/kda-job-1.0.jar" :dir "../kda-job"))
    ;;(println (sh "aws" "s3" "ls" "ds-kda/kda-job/target/kda-job-1.0.jar"))


(defn -main
  "Deploying kda main"
  []
  (println "...")
  (build-kda-processor)
  (build-kda-job)
  (println "*** DONE ***")
  (shutdown-agents)
  )
