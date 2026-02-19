(ns verify.parse
  (:require [clojure.string :as str]))

(def ^:private agent-line-re #"Signed by [^(]*\(([^)]+)\)")
(def ^:private sig-re #"^sig:\s*(0x[0-9a-fA-F]+)")
(def ^:private hash-re #"^hash:\s*(0x[0-9a-fA-F]+)")
(def ^:private ts-re #"^ts:\s*(\S+)")
(def ^:private full-id-re #"eip155:(\d+):([^:]+):(\d+)")
(def ^:private separator-re #"^-{3,}\s*$")
(def ^:private noise-re #"^[-—\s]*$")

(defn parse-full-id
  "Parse eip155:<chainId>:<registry>:<agentId>"
  [full-id]
  (when-let [m (re-find full-id-re full-id)]
    {:chain-id (js/parseInt (nth m 1))
     :registry (nth m 2)
     :agent-id (js/parseInt (nth m 3))}))

(defn- find-signed-by-index
  "Find the index of the 'Signed by' line."
  [lines]
  (loop [i 0]
    (when (< i (count lines))
      (if (re-find agent-line-re (nth lines i))
        i
        (recur (inc i))))))

(defn- find-separator-before
  "Find the --- line immediately before the given index."
  [lines before-idx]
  (loop [i (dec before-idx)]
    (when (>= i 0)
      (let [line (str/trim (nth lines i))]
        (cond
          (re-matches separator-re line) i
          (= "" line) (recur (dec i)) ;; skip blank lines
          :else nil)))))

(defn parse-signed-content
  "Parse a signed content block into its components.
   Anchors on the 'Signed by' line and finds --- before it.
   Returns {:content :full-id :signature :hash :timestamp} or nil."
  [text]
  (let [text (-> text str/trim (str/replace #"\r\n?" "\n"))
        lines (vec (str/split-lines text))
        signed-idx (find-signed-by-index lines)]
    (when signed-idx
      (let [sep-idx (find-separator-before lines signed-idx)]
        (when sep-idx
          (let [;; Strip leading/trailing --- noise lines from content
                raw-content (subvec lines 0 sep-idx)
                stripped (vec (->> raw-content
                                   (drop-while #(re-matches noise-re %))
                                   reverse
                                   (drop-while #(re-matches noise-re %))
                                   reverse))
                content (str/trim (str/join "\n" stripped))
                footer (subvec lines signed-idx)
                agent-match (some #(re-find agent-line-re %) footer)
                sig-match (some #(re-find sig-re %) footer)
                hash-match (some #(re-find hash-re %) footer)
                ts-match (some #(re-find ts-re %) footer)]
            (when (and (seq content) agent-match sig-match hash-match)
              (let [full-id (second agent-match)
                    parsed-id (parse-full-id full-id)]
                (when parsed-id
                  {:content content
                   :full-id full-id
                   :chain-id (:chain-id parsed-id)
                   :registry (:registry parsed-id)
                   :agent-id (:agent-id parsed-id)
                   :signature (second sig-match)
                   :hash (second hash-match)
                   :timestamp (when ts-match (second ts-match))})))))))))
