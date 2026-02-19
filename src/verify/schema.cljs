(ns verify.schema
  (:require ["zod" :refer [z]]))

(def hex-pattern #"^0x[0-9a-fA-F]+$")

(def SignedContent
  (.object z
    #js {:content   (-> z .string (.min 1) .optional .nullable)
         :fullId    (-> z .string (.min 1 "Agent ID is required"))
         :signature (-> z .string (.regex hex-pattern "Invalid signature hex"))
         :hash      (-> z .string (.regex hex-pattern "Invalid hash hex"))
         :chainId   (-> z .number (.int) (.positive))
         :registry  (-> z .string (.regex hex-pattern "Invalid registry address"))
         :agentId   (-> z .number (.int) (.nonnegative))}))

(defn validate
  "Validate parsed data against the schema.
   Returns {:ok true :data ...} or {:ok false :error ...}"
  [parsed]
  (let [result (.safeParse SignedContent
                 (clj->js {:content   (:content parsed)
                           :fullId    (:full-id parsed)
                           :signature (:signature parsed)
                           :hash      (:hash parsed)
                           :chainId   (:chain-id parsed)
                           :registry  (:registry parsed)
                           :agentId   (:agent-id parsed)}))]
    (if (.-success result)
      {:ok true :data parsed}
      {:ok false :error (-> result .-error .format)})))
