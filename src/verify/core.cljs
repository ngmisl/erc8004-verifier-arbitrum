(ns verify.core
  (:require [verify.parse :as parse]
            [verify.schema :as schema]
            [verify.chain :as chain]
            [verify.state :as state]
            [clojure.string :as str]))

;; ─── DOM Helpers ──────────────────────────────────────────────────────

(defn $ [sel] (.querySelector js/document sel))

(defn set-html! [sel html]
  (when-let [el ($ sel)]
    (set! (.-innerHTML el) html)))

(defn set-class! [sel cls]
  (when-let [el ($ sel)]
    (set! (.-className el) cls)))

;; ─── Rendering ────────────────────────────────────────────────────────

(defn render-idle []
  (set-html! "#result" ""))

(defn render-verifying []
  (set-html! "#result"
    "<div class='status verifying'>
       <div class='spinner'></div>
       <span>Verifying on-chain...</span>
     </div>"))

(defn shorten [addr]
  (when addr
    (str (subs addr 0 6) "..." (subs addr (- (count addr) 4)))))

(defn format-timestamp [iso-str]
  (when iso-str
    (try
      (let [d (js/Date. iso-str)]
        (.toLocaleString d "en-US"
          #js {:year "numeric" :month "short" :day "numeric"
               :hour "2-digit" :minute "2-digit" :timeZoneName "short"}))
      (catch :default _ iso-str))))

(defn render-verified [result]
  (let [r (js->clj result :keywordize-keys true)
        ok (:verified r)]
    (set-html! "#result"
      (str
        "<div class='result-card " (if ok "verified" "failed") "'>"
        "<div class='result-header'>"
        (if ok
          "<span class='badge ok'>VERIFIED</span>"
          "<span class='badge fail'>FAILED</span>")
        (when (:agent-name r)
          (str "<span class='agent-name'>" (:agent-name r) "</span>"))
        "</div>"

        ;; Subject line
        (when (:subject r)
          (str "<p class='subject-line'>" (:subject r) "</p>"))

        "<table class='result-table'>"

        ;; Signature
        "<tr><td class='label'>Signature</td><td>"
        (if ok
          "<span class='check ok'>Valid</span>"
          "<span class='check fail'>Invalid</span>")
        "</td></tr>"

        ;; Timestamp
        (when (:timestamp r)
          (str "<tr><td class='label'>Signed at</td>"
               "<td>" (format-timestamp (:timestamp r)) "</td></tr>"))

        ;; Signer
        (when (:signer r)
          (str "<tr><td class='label'>Signer</td>"
               "<td><code>" (shorten (:signer r)) "</code>"
               (when (:wallet-match r)
                 " <span class='check ok'>Agent Wallet</span>")
               (when (and (not (:wallet-match r)) (:owner-match r))
                 " <span class='check ok'>Agent Owner</span>")
               "</td></tr>"))

        ;; Content hash (informational)
        (when (:content-hash r)
          (str "<tr><td class='label'>Content Hash</td>"
               "<td><code>" (shorten (:content-hash r)) "</code></td></tr>"))

        ;; Chain
        (when (:chain-name r)
          (str "<tr><td class='label'>Chain</td>"
               "<td>" (:chain-name r) "</td></tr>"))

        ;; Agent ID
        (str "<tr><td class='label'>Agent ID</td>"
             "<td>" (:agent-id r) "</td></tr>")

        ;; Registry
        (when (:registry r)
          (let [explorer (:explorer r)]
            (str "<tr><td class='label'>Registry</td>"
                 "<td><a href='" explorer "/address/" (:registry r)
                 "' target='_blank'><code>" (shorten (:registry r))
                 "</code></a></td></tr>")))

        "</table>"

        ;; Copy verify link button
        "<div class='copy-row'>"
        "<button class='copy-btn' onclick='window.__copyVerifyLink()'>"
        "Copy verify link"
        "</button>"
        "<span id='copy-status'></span>"
        "</div>"

        "</div>"))))

(defn render-error [msg]
  (set-html! "#result"
    (str "<div class='result-card failed'>"
         "<div class='result-header'>"
         "<span class='badge fail'>ERROR</span>"
         "</div>"
         "<p class='error-msg'>" msg "</p>"
         "</div>")))

(defn render [s]
  (let [status (.-status s)]
    (case status
      "idle"      (render-idle)
      "verifying" (render-verifying)
      "verified"  (render-verified (.-result s))
      "error"     (render-error (.-error s))
      nil)))

;; ─── Verification Flow ───────────────────────────────────────────────

(defn verify-parsed!
  "Run verification directly on parsed data (skips text parsing)."
  [parsed]
  (let [s         (state/get-state)
        set-vfy   (.-setVerifying s)
        set-res   (.-setResult s)
        set-err   (.-setError s)]
    (set-vfy)
    (let [validation (schema/validate parsed)]
      (if-not (:ok validation)
        (set-err "Invalid format. Check the signature and hash values.")
        (-> (chain/verify-all parsed)
            (.then  (fn [r] (set-res (clj->js r))))
            (.catch (fn [e] (set-err (str "Verification failed: " (.-message e))))))))))

(defn do-verify! []
  (let [s      (state/get-state)
        input  (.-input s)
        parsed (parse/parse-signed-content input)]
    (if-not parsed
      ((.-setError s) "Could not parse signed content. Make sure you paste the full block including the --- separator, sig, and hash lines.")
      (verify-parsed! parsed))))

;; ─── Unicode-safe Base64 ─────────────────────────────────────────────

(defn utf8->b64
  "Encode a UTF-8 string to base64 (handles Unicode)."
  [s]
  (let [bytes (.encode (js/TextEncoder.) s)]
    (js/btoa (.apply js/String.fromCharCode nil (js/Uint8Array. bytes)))))

(defn b64->utf8
  "Decode base64 to a UTF-8 string (handles Unicode)."
  [b64]
  (let [binary (js/atob b64)
        bytes (js/Uint8Array. (count binary))]
    (doseq [i (range (count binary))]
      (aset bytes i (.charCodeAt binary i)))
    (.decode (js/TextDecoder.) bytes)))

;; ─── URL Hash Decoding ───────────────────────────────────────────────

(defn decode-url-hash
  "Decode base64 JSON from URL hash fragment.
   Supports both content mode {c,s,h,a} and hash-only mode {s,h,a}.
   Returns {:text ... :parsed ...} with both display text and pre-parsed data, or nil."
  []
  (let [hash (.-hash js/location)]
    (when (and hash (> (count hash) 1))
      (try
        (let [b64 (subs hash 1)
              json (b64->utf8 b64)
              obj (js/JSON.parse json)
              c (aget obj "c") s (aget obj "s") h (aget obj "h") a (aget obj "a")
              t (aget obj "t") d (aget obj "d")]
          (when (and s h a)
            (let [parsed-id (parse/parse-full-id a)]
              (when parsed-id
                (let [agent-name (or (aget obj "n") "Agent")
                      ts-line (when t (str "ts: " t "\n"))]
                  {:text (str (when c (str c "\n\n"))
                              "---\n"
                              "Signed by " agent-name " (" a ")\n"
                              "sig: " s "\n"
                              "hash: " h "\n"
                              (or ts-line ""))
                   :parsed {:content c
                            :full-id a
                            :chain-id (:chain-id parsed-id)
                            :registry (:registry parsed-id)
                            :agent-id (:agent-id parsed-id)
                            :signature s
                            :hash h
                            :timestamp t
                            :subject d}})))))
        (catch :default _ nil)))))

;; ─── Init ────────────────────────────────────────────────────────────

(defn ^:export init []
  ;; Subscribe to state changes
  (state/subscribe render)

  ;; Textarea input binding
  (when-let [textarea ($ "#input")]
    (.addEventListener textarea "input"
      (fn [e]
        (let [set-input (.-setInput (state/get-state))]
          (set-input (.. e -target -value))))))

  ;; Verify button
  (when-let [btn ($ "#verify-btn")]
    (.addEventListener btn "click" (fn [_] (do-verify!))))

  ;; Paste anywhere triggers input
  (.addEventListener js/document "paste"
    (fn [e]
      (when-let [textarea ($ "#input")]
        (when (not= (.-activeElement js/document) textarea)
          (let [text (.getData (.-clipboardData e) "text")]
            (.preventDefault e)
            (set! (.-value textarea) text)
            (let [set-input (.-setInput (state/get-state))]
              (set-input text)))))))

  ;; Auto-verify on paste into textarea
  (when-let [textarea ($ "#input")]
    (.addEventListener textarea "paste"
      (fn [_]
        (js/setTimeout
          (fn []
            (let [set-input (.-setInput (state/get-state))]
              (set-input (.-value textarea)))
            (do-verify!))
          50))))

  ;; Copy verify link handler
  (set! (.-__copyVerifyLink js/window)
    (fn []
      (let [s (state/get-state)
            input (.-input s)
            parsed (parse/parse-signed-content input)]
        (when parsed
          (let [first-line (first (str/split-lines (:content parsed)))
                subj (when first-line
                        (if (> (count first-line) 80)
                          (str (subs first-line 0 77) "...")
                          first-line))
                proof (js/JSON.stringify
                        (clj->js (cond-> {:s (:signature parsed)
                                          :h (:hash parsed)
                                          :a (:full-id parsed)}
                                   (:timestamp parsed) (assoc :t (:timestamp parsed))
                                   subj (assoc :d subj))))
                url (str "https://erc8004.qstorage.quilibrium.com/#"
                         (utf8->b64 proof))]
            (-> (.writeText (.-clipboard js/navigator) url)
                (.then (fn []
                         (when-let [el ($ "#copy-status")]
                           (set! (.-textContent el) "Copied!")
                           (js/setTimeout #(set! (.-textContent el) "") 2000))))
                (.catch (fn []
                          (when-let [el ($ "#copy-status")]
                            (set! (.-textContent el) "Failed to copy"))))))))))

  ;; Auto-verify from URL hash if present (bypass parser — use exact data)
  (when-let [url-data (decode-url-hash)]
    (when-let [textarea ($ "#input")]
      (set! (.-value textarea) (:text url-data)))
    (let [set-input (.-setInput (state/get-state))]
      (set-input (:text url-data)))
    (verify-parsed! (:parsed url-data))))
