(ns verify.state
  (:require ["zustand/vanilla" :refer [createStore]]))

(def store
  (createStore
    (fn [set _get]
      #js {:input    ""
           :status   "idle"    ;; idle | verifying | verified | error
           :result   nil
           :error    nil
           :setInput (fn [v] (set #js {:input v}))
           :setVerifying (fn [] (set #js {:status "verifying" :result nil :error nil}))
           :setResult (fn [r] (set #js {:status "verified" :result r}))
           :setError (fn [msg] (set #js {:status "error" :error msg :result nil}))
           :reset (fn [] (set #js {:input "" :status "idle" :result nil :error nil}))})))

(defn get-state [] (.getState store))
(defn subscribe [listener] (.subscribe store listener))
