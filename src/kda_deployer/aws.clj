(ns kda-deployer.aws
  (:use [clojure.java.shell])
  (:require [cheshire.core :as json]))

(defn kda-describe-application[name]
  (let [r (:out (sh "aws" "kinesisanalyticsv2" "describe-application" "--application-name" name))]
    (json/parse-string r true)))


(defn create-update-request[name version objectVersion]
  {:ApplicationName name
   :CurrentApplicationVersionId version
   :ApplicationConfigurationUpdate {:ApplicationCodeConfigurationUpdate {:CodeContentTypeUpdate "ZIPFILE"
                                                                         :CodeContentUpdate {:S3ContentLocationUpdate {:ObjectVersionUpdate objectVersion}}}}})
