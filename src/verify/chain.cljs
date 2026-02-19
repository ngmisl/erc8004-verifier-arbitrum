(ns verify.chain
  (:require ["viem" :refer [createPublicClient http recoverMessageAddress]]))

;; Minimal ABI — only the read functions we need
(def identity-abi
  #js [#js {:name "ownerOf"
            :type "function"
            :stateMutability "view"
            :inputs  #js [#js {:name "tokenId" :type "uint256"}]
            :outputs #js [#js {:name "" :type "address"}]}
       #js {:name "getAgentWallet"
            :type "function"
            :stateMutability "view"
            :inputs  #js [#js {:name "agentId" :type "uint256"}]
            :outputs #js [#js {:name "" :type "address"}]}
       #js {:name "tokenURI"
            :type "function"
            :stateMutability "view"
            :inputs  #js [#js {:name "tokenId" :type "uint256"}]
            :outputs #js [#js {:name "" :type "string"}]}])

(def chains
  {42161   {:name "Arbitrum"
            :rpc "https://arb1.arbitrum.io/rpc"
            :explorer "https://arbiscan.io"}
   421614  {:name "Arbitrum Sepolia"
            :rpc "https://sepolia-rollup.arbitrum.io/rpc"
            :explorer "https://sepolia.arbiscan.io"}})

(defn make-client [chain-id]
  (let [chain-info (get chains chain-id)]
    (when chain-info
      (createPublicClient
        #js {:transport (http (:rpc chain-info))}))))

(defn recover-signer
  "Recover the signer address from content hash + signature.
   The signature is over the raw hash bytes (not the content text)."
  [content-hash signature]
  (recoverMessageAddress #js {:message #js {:raw content-hash} :signature signature}))

(defn read-agent-wallet
  "Read the agent's wallet address from the on-chain registry."
  [client registry agent-id]
  (.readContract client
    #js {:address  registry
         :abi      identity-abi
         :functionName "getAgentWallet"
         :args     #js [(js/BigInt agent-id)]}))

(defn read-agent-owner
  "Read the agent's NFT owner from the on-chain registry."
  [client registry agent-id]
  (.readContract client
    #js {:address  registry
         :abi      identity-abi
         :functionName "ownerOf"
         :args     #js [(js/BigInt agent-id)]}))

(defn read-agent-uri
  "Read the agent's metadata URI."
  [client registry agent-id]
  (.readContract client
    #js {:address  registry
         :abi      identity-abi
         :functionName "tokenURI"
         :args     #js [(js/BigInt agent-id)]}))

(defn decode-data-uri
  "Decode a base64 data URI to JSON."
  [uri]
  (when (and uri (.startsWith uri "data:"))
    (let [b64 (second (.split uri ","))]
      (try
        (js/JSON.parse (js/atob b64))
        (catch :default _ nil)))))

(defn verify-all
  "Run full verification: recover signer from signature, check against on-chain agent."
  [{:keys [signature hash chain-id registry agent-id timestamp subject]}]
  (js/Promise.
    (fn [resolve _reject]
      (let [client (make-client chain-id)]
        (if-not client
          (resolve {:verified false
                    :error (str "Unsupported chain: " chain-id)})
          (-> (recover-signer hash signature)
              (.then
                (fn [signer]
                  (-> (js/Promise.all
                        #js [(read-agent-wallet client registry agent-id)
                             (read-agent-owner client registry agent-id)
                             (-> (read-agent-uri client registry agent-id)
                                 (.catch (fn [_] nil)))])
                      (.then
                        (fn [results]
                          (let [agent-wallet (aget results 0)
                                agent-owner (aget results 1)
                                agent-uri   (aget results 2)
                                metadata    (decode-data-uri agent-uri)
                                signer-lower (.toLowerCase signer)
                                wallet-match (= signer-lower
                                                (.toLowerCase agent-wallet))
                                owner-match  (= signer-lower
                                                (.toLowerCase agent-owner))]
                            (resolve
                              {:verified (or wallet-match owner-match)
                               :content-hash hash
                               :signer signer
                               :agent-wallet agent-wallet
                               :agent-owner agent-owner
                               :wallet-match wallet-match
                               :owner-match owner-match
                               :agent-name (when metadata (.-name metadata))
                               :chain-name (:name (get chains chain-id))
                               :explorer (:explorer (get chains chain-id))
                               :registry registry
                               :agent-id agent-id
                               :timestamp timestamp
                               :subject subject})))))))
              (.catch
                (fn [err]
                  (resolve {:verified false
                            :error (str "Signature recovery failed: "
                                        (.-message err))})))))))))
