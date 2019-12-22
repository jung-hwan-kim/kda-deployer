(ns kda-deployer.aws
  (:use [clojure.java.shell])
  (:require [cheshire.core :as json]))

(defn kda-describe-application[name]
  (let [r (:out (sh "aws" "kinesisanalyticsv2" "describe-application" "--application-name" name))]
    (json/parse-string r true)))

(defn kda-describe-application2[name]
  (let [input {:ApplicationName name}
        r (:out (sh "aws" "kinesisanalyticsv2" "describe-application" "--cli-input-json" (json/generate-string input)))]
    (json/parse-string r true)))


(defn create-update-request[name version objectVersion]
  {:ApplicationName name
   :CurrentApplicationVersionId version
   :ApplicationConfigurationUpdate {:ApplicationCodeConfigurationUpdate {:CodeContentUpdate {:S3ContentLocationUpdate {:ObjectVersionUpdate objectVersion}}}}})

(defn kda-update-application[name version objectVersion]
  (let [input (create-update-request name version objectVersion)
        r (:out (sh "aws" "kinesisanalyticsv2" "update-application" "--cli-input-json" (json/generate-string input)))]
    (json/parse-string r true)))



(defn s3-head-object[bucket path]
  (json/parse-string (:out (sh "aws" "s3api" "head-object" "--bucket" bucket "--key" path)) true))