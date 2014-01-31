(ns nsscreencast-fetcher.core
  (:use [clojure.java.io :only [output-stream as-file input-stream]])
  (:require [clojure.xml :as xml]
            [clj-http.client :as client]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.zip :as zip]))

(defn make-feed [uri]
  (-> uri
      input-stream
      xml/parse
      zip/xml-zip))

(def feed (make-feed "https://www.nsscreencast.com/private_feed/secret_key?video_format=mp4"))

(defn link-file-pairs []
  (->> (zip-xml/xml-> feed
                      :entry
                      :link
                      [(zip-xml/attr= :rel "enclosure")]
                      (zip-xml/attr :href))
       (map #(re-seq #".*/(.*)\?.*" %))
       (map first)))

(defn zero-pad [n number-string]
  (->> (concat (reverse number-string) (repeat \0))
       (take n)
       reverse
       (apply str)))

(defn prefix [file-name]
  (clojure.string/replace file-name #"^\d+" #(zero-pad 3 %)))

(defn fullname [file]
  (str "C:\\Users\\bjball\\Videos\\NSScreencasts\\ns" (prefix file)))

(defn copy-file [[uri file]]
  (with-open [w (output-stream (fullname file))]
    (.write w (:body (client/get uri {:as :byte-array})))))

(defn not-downloaded [[uri file]]
  (-> file
      fullname
      as-file
      .exists
      not))

(defn -main []
  (->> (link-file-pairs)
       (filter not-downloaded)
       (map copy-file)
       (doall)))
