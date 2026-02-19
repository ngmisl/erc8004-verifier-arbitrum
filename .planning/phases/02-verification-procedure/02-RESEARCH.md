# Phase 2: Verification Procedure - Research

**Researched:** 2026-02-19
**Domain:** Ethereum JSON-RPC verification procedure documentation for AI agents
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Procedure walkthrough structure**
- Cross-reference Phase 1's format spec by name (e.g., "use the `hash:` field from Signed Block Format") rather than repeating field descriptions — assumes agents read top-down through the document

**JSON-RPC example style**
- Show BOTH templates with placeholders AND worked examples with real on-chain data — template first for the pattern, then a concrete example that proves the procedure works
- Follow the existing show-then-explain annotation style from Phase 1 where it fits, but Claude has discretion on whether JSON-RPC examples benefit more from a different presentation

**ABI encoding presentation**
- Show complete worked examples for BOTH getAgentWallet and ownerOf — do not DRY them into "same pattern, different selector." Each function gets its own full copy-pasteable example
- Assume agents know how to read a hex address from an eth_call response — do not explain response decoding

### Claude's Discretion
- Overall structure: H3-per-step vs. numbered list, step labeling style, inline error notes vs. pure happy path
- JSON-RPC: full HTTP request vs. JSON body only, paired vs. grouped responses
- ABI encoding: byte-level walkthrough vs. formula + example, whether to reference ABI standard or be self-contained
- Selector resolution: whether to show keccak256 computation in the doc, whether to cross-reference against viem and/or live on-chain call
- QA-02: whether to anchor personal_ecRecover spec to viem behavior or the Ethereum JSON-RPC standard independently

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| QA-01 | Compute correct `getAgentWallet(uint256)` function selector from canonical ABI | RESOLVED: selector is `0x00339509` — computed via viem `getFunctionSelector` and verified against keccak256 of UTF-8 encoded signature bytes. All three prior research file values were wrong. |
| QA-02 | Verify `personal_ecRecover` parameter semantics match existing chain.cljs behavior | RESOLVED: pass the `hash:` field value directly as params[0]; the RPC node applies EIP-191 prefix internally. Confirmed via viem source (toPrefixedMessage.js) and OpenEthereum JSON-RPC docs. |
| VFY-01 | Numbered step-by-step verification walkthrough (parse → ecRecover → eth_call → compare → report) | Architecture is confirmed linear; steps can be presented as numbered H3 sections or a flat numbered list. Research recommends H3-per-step for agent navigability. |
| VFY-02 | `personal_ecRecover` JSON-RPC call spec — exact request body, params array order, response shape | Params: [hash, sig] in that order. Full request/response pair verified against OpenEthereum docs and chain.cljs behavior. |
| VFY-03 | `eth_call` spec for `getAgentWallet(uint256)` — pre-computed function selector, ABI-encoded argument, response decoding | Selector `0x00339509` confirmed. ABI encoding: agentId zero-padded to 64 hex chars. Response: last 40 hex chars of result are the address. |
| VFY-04 | `eth_call` spec for `ownerOf(uint256)` — pre-computed function selector (`0x6352211e`), same encoding pattern | Selector `0x6352211e` confirmed by all prior research and recomputed in this phase. |
| VFY-05 | Pre-computed ABI call data examples — worked example with a real agentId showing full `data` field construction | Worked example for agentId=5 provided in Code Examples section. Each function gets its own full copy-pasteable `data` field. |
| VFY-06 | JSON-RPC request/response pairs shown together for each call | Template-then-worked-example pattern is the locked decision. Research supports showing JSON body only (not full HTTP), paired per RPC method. |
| VFY-09 | RPC endpoint list — Arbitrum mainnet and Sepolia with chain IDs | Chain IDs and URLs confirmed in chain.cljs. Both endpoints support `personal_ecRecover` and `eth_call`. |
</phase_requirements>

---

## Summary

Phase 2 is a document-authoring phase. The deliverable is the `## Verification Procedure` section of `ERC8004-SKILL.md`. All technical facts needed to write this section are now fully resolved — including the critical `getAgentWallet(uint256)` function selector which was inconsistent across all prior research files. No further investigation is required before writing.

The primary technical complexity in this phase is QA-01 (selector resolution) and QA-02 (ecRecover parameter semantics) — both now resolved. Everything else is transcription of known architecture patterns into the locked document style. The verified values are: `getAgentWallet(uint256)` selector = `0x00339509`, `ownerOf(uint256)` selector = `0x6352211e`, and `personal_ecRecover` params = `[hash_field_verbatim, sig_field_verbatim]`.

The document section must be entirely self-contained within the happy path (Phase 3 handles pass/fail logic and error conditions). Phase 2 stops after "make these three RPC calls and collect the addresses returned." The two plans split naturally: Plan 02-01 resolves QA items and writes the procedure overview + ecRecover spec (VFY-01, VFY-02), and Plan 02-02 writes the registry eth_call specs and endpoint table (VFY-03 through VFY-06, VFY-09).

**Primary recommendation:** Write the procedure as numbered H3 steps. Each step is one action with one code block (template) and one code block (worked example). Cross-reference Phase 1 by field name, never by copying field definitions. The happy path flows: parse (reference Phase 1) → ecRecover → getAgentWallet → ownerOf → [Phase 3 handles comparison]. The endpoint table stands alone as a reference at the end.

---

## QA-01: getAgentWallet Selector Resolution (CRITICAL)

**Status:** RESOLVED — HIGH confidence

**The conflict:** Three prior research files all had different values for `getAgentWallet(uint256)`:
- STACK.md: `0x45a3a8d5` (wrong)
- ARCHITECTURE.md: `0x3cef5e0f` (wrong)
- PITFALLS.md: `0x9a5b0c6b` (wrong)

**Computed value:**

```
keccak256("getAgentWallet(uint256)") = 0x00339509795e73c16200ac22b3525a599b2a063675c00885b2cfb3aa66bd6fc3
selector (first 4 bytes)            = 0x00339509
```

**Verification method:** Computed using `viem.getFunctionSelector('getAgentWallet(uint256)')` against the project's installed viem package, and cross-verified using `keccak256(toBytes('getAgentWallet(uint256)'))`. Both methods return `0x00339509`. The `ownerOf(uint256)` selector `0x6352211e` and `tokenURI(uint256)` selector `0xc87b56dd` are consistent with all prior research and were reconfirmed.

**Impact:** The skill document must use `0x00339509` as the `getAgentWallet(uint256)` selector. All three prior research file values are incorrect. Any example in the prior research that shows `data` field values using those wrong selectors must be recomputed.

**Corrected worked example for agentId=5:**
```
getAgentWallet call data:
0x003395090000000000000000000000000000000000000000000000000000000000000005
```

---

## QA-02: personal_ecRecover Parameter Semantics (CONFIRMED)

**Status:** RESOLVED — HIGH confidence

**What chain.cljs does:**
```clojure
(recoverMessageAddress #js {:message #js {:raw content-hash} :signature signature})
```

The `{:raw content-hash}` form in viem tells `toPrefixedMessage` to treat the value as raw bytes rather than a UTF-8 string. Specifically, from viem source (`toPrefixedMessage.js`):

```js
if (typeof message_.raw === 'string')
    return message_.raw;  // use the hex string directly as the message bytes
```

Then `hashMessage` calls `keccak256(toPrefixedMessage(message))`, which produces:
```
keccak256(EIP191_PREFIX + raw_hash_bytes)
```
where EIP191_PREFIX = `"\x19Ethereum Signed Message:\n32"` (32 = byte length of the 32-byte hash).

**JSON-RPC `personal_ecRecover` does the same thing:** It takes the message bytes (params[0]), applies `"\x19Ethereum Signed Message:\n" + len(message)` prefix, keccak256s the result, then runs ecrecover with params[1] (the signature).

**Conclusion for skill document:** Pass the `hash:` field value verbatim as `params[0]`. Pass the `sig:` field value verbatim as `params[1]`. No pre-processing. The RPC node handles the EIP-191 prefix internally.

**Parameter types:**
- `params[0]`: `hash:` field value — 0x-prefixed 32-byte hex string (66 chars total: `0x` + 64 hex chars)
- `params[1]`: `sig:` field value — 0x-prefixed 65-byte hex string (132 chars total: `0x` + 130 hex chars)

**Order:** hash first, signature second. This is the same order as `personal_sign` args (message then signature for recovery). Some library conventions reverse this order; the JSON-RPC spec does not.

---

## Verified Technical Facts

### Function Selectors

| Function | Canonical Signature | Selector | Confidence |
|----------|-------------------|----------|------------|
| `getAgentWallet` | `getAgentWallet(uint256)` | `0x00339509` | HIGH — computed via viem getFunctionSelector |
| `ownerOf` | `ownerOf(uint256)` | `0x6352211e` | HIGH — consistent in all research, recomputed |
| `tokenURI` | `tokenURI(uint256)` | `0xc87b56dd` | HIGH — out of scope for Phase 2 |

### RPC Endpoints

Source: `chain.cljs` (HIGH confidence)

| Chain ID | Network | RPC Endpoint | Supports personal_ecRecover |
|----------|---------|-------------|---------------------------|
| 42161 | Arbitrum One | `https://arb1.arbitrum.io/rpc` | Yes |
| 421614 | Arbitrum Sepolia | `https://sepolia-rollup.arbitrum.io/rpc` | Yes |

### ABI Encoding Rule for uint256

Rule: convert the integer to hex, zero-pad left to 64 hex characters.

| agentId | ABI-encoded (64 hex chars) |
|---------|---------------------------|
| 1 | `0000000000000000000000000000000000000000000000000000000000000001` |
| 5 | `0000000000000000000000000000000000000000000000000000000000000005` |
| 42 | `000000000000000000000000000000000000000000000000000000000000002a` |
| 100 | `0000000000000000000000000000000000000000000000000000000000000064` |
| 1337 | `0000000000000000000000000000000000000000000000000000000000000539` |

### eth_call Response Decoding

Source: ARCHITECTURE.md (HIGH confidence), confirmed by ABI spec.

An address return value is ABI-encoded as 32 bytes: 12 zero bytes followed by the 20-byte address.

```
result: "0x000000000000000000000000d8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
                        ^^^^^^^^^^^^^^^^^^^^^^^^ ← 24 zero hex chars (12 bytes)
                                                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ← 40 hex chars = 20-byte address
```

Per user decision (CONTEXT.md): "Assume agents know how to read a hex address from an eth_call response — do not explain response decoding." The skill document should show the raw response and state the address value without explaining the decoding process.

---

## Architecture Patterns

### Verification Procedure Structure

The section to write is `## Verification Procedure` in ERC8004-SKILL.md. It currently contains only `<!-- Phase 2 -->`.

**Phase 2 scope (write this):**
1. Parse fields (reference Phase 1's Signed Block Format)
2. Call `personal_ecRecover` → get recovered signer address
3. Call `eth_call` → `getAgentWallet(agentId)` → get agent wallet address
4. Call `eth_call` → `ownerOf(agentId)` → get NFT owner address
5. Collect results (Phase 3 handles the comparison and pass/fail)

**Phase 3 scope (do not write):**
- Address comparison rule
- Pass/fail definition
- Error conditions

### Recommended Section Structure

This is Claude's discretion. Recommendation: **numbered H3 steps** with a brief intro sentence.

Rationale: H3 per step gives agents a navigation anchor for each discrete action. A flat numbered list is harder to reference ("go back to the ecRecover step" requires counting; "see Step 2" can be scanned via heading). Phase 1 uses show-then-explain for format fields; for JSON-RPC calls, the pattern is: template JSON first, worked example second, no inline annotation needed (JSON bodies are self-explanatory when labels match the signed block field names).

Suggested subsection structure:

```
## Verification Procedure

[intro: what these steps produce; note Steps 2-4 can run concurrently]

### Step 1: Extract Fields
### Step 2: Recover Signer (personal_ecRecover)
### Step 3: Read Agent Wallet (eth_call → getAgentWallet)
### Step 4: Read NFT Owner (eth_call → ownerOf)
### RPC Endpoints
```

Step 5 (compare + report) belongs to Phase 3. Steps 2, 3, and 4 are independent and can execute concurrently — note this for agents that support parallel requests.

### JSON-RPC Example Format

Per locked decision: template first (with `<placeholder>` notation), then worked example.

Per Claude's discretion on JSON body vs. full HTTP request: use **JSON body only** (not the full HTTP transport wrapper). The `Content-Type: application/json` and POST URL are one-time facts that can be stated in the step intro or the endpoint table. Repeating the HTTP transport in every example adds noise without information.

Exception: the `personal_ecRecover` step should include the POST URL in the step intro because it is the first time agents see where to send JSON-RPC calls.

### Cross-Reference Style

Per locked decision: reference Phase 1 by field name, not by repeating content. Pattern:

```
Use the `hash:` field from [Signed Block Format] and the `sig:` field from [Signed Block Format].
```

Do not reproduce the field table. Do not re-explain what the hash is. Agents read top-to-bottom; Phase 1 already established these.

---

## Code Examples

All examples use agentId=5, registry from Phase 1 synthetic example. Per locked decision, templates first, then worked examples.

### Step 2: personal_ecRecover — Template

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "personal_ecRecover",
  "params": [
    "<hash: field value>",
    "<sig: field value>"
  ]
}
```

**Response template:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "<recovered signer address>"
}
```

### Step 3: getAgentWallet — Template and Worked Example

**Template:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "eth_call",
  "params": [
    {
      "to": "<registryAddress from full ID>",
      "data": "0x00339509<agentId zero-padded to 64 hex chars>"
    },
    "latest"
  ]
}
```

**Worked example (agentId=5, registry=0x8004b3A873394d8B0Af8fD5D9C5D5a432):**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "eth_call",
  "params": [
    {
      "to": "0x8004b3A873394d8B0Af8fD5D9C5D5a432",
      "data": "0x003395090000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ]
}
```

**Response (contains the agent wallet address):**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": "0x000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"
}
```

### Step 4: ownerOf — Template and Worked Example

**Template:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "eth_call",
  "params": [
    {
      "to": "<registryAddress from full ID>",
      "data": "0x6352211e<agentId zero-padded to 64 hex chars>"
    },
    "latest"
  ]
}
```

**Worked example (agentId=5, same registry):**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "eth_call",
  "params": [
    {
      "to": "0x8004b3A873394d8B0Af8fD5D9C5D5a432",
      "data": "0x6352211e0000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ]
}
```

**Response (contains the NFT owner address):**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": "0x000000000000000000000000e41d2489571d322189246dafa5ebde1f4699f498"
}
```

Note: The response values in the worked examples are synthetic. The registry address `0x8004b3A873394d8B0Af8fD5D9C5D5a432` is the same synthetic example used in Phase 1. Real on-chain data should be used for the final worked examples if available, but synthetic data with a disclaimer is acceptable per Phase 1 precedent.

---

## Common Pitfalls for Phase 2

These are the most relevant pitfalls from PITFALLS.md for the content being written in this phase.

### Pitfall: Wrong getAgentWallet Selector

**What goes wrong:** Any prior research value is used verbatim — all three are wrong.
**Correct value:** `0x00339509`
**Detection:** The call data for agentId=5 should be exactly 72 bytes (4-byte selector + 32-byte arg):
`0x003395090000000000000000000000000000000000000000000000000000000000000005`

### Pitfall: Explaining ecRecover Parameter Semantics Incorrectly

**What goes wrong:** Describing params[0] as "the EIP-191 prefixed hash" or "the pre-hashed content" causes confusion. The correct description is: the `hash:` field value from the signed block, verbatim.
**Correct framing:** "Pass the `hash:` field value as params[0]. The RPC node applies the EIP-191 prefix internally."

### Pitfall: Showing response decoding steps

**User decision explicitly prohibits this.** The ABI encoding presentation decision says "Assume agents know how to read a hex address from an eth_call response — do not explain response decoding." Show the raw response, identify what the address is, do not explain how to extract it.

### Pitfall: Reversing params[0] and params[1] in personal_ecRecover

**What goes wrong:** Some library conventions pass (sig, hash); the JSON-RPC standard is (hash, sig) = (message, signature). The skill document must state the order explicitly in the template.

### Pitfall: Using the registry address as a constant

**What goes wrong:** Using a hardcoded registry address in the template instead of cross-referencing the full ID. The registry address comes from the agent's full ID — it varies per agent/chain.
**Correct pattern:** Template uses `<registryAddress from full ID>` with explicit reference to Phase 1's `## Agent Identity` section.

### Pitfall: Omitting concurrent execution note

The three RPC calls (Steps 2, 3, 4) can run concurrently. An agent that runs them sequentially is correct but slow. The skill document should note parallelism is possible, matching the actual behavior of chain.cljs (`js/Promise.all`).

---

## Discretion Recommendations

For areas marked as Claude's discretion:

### Overall Structure

**Recommendation:** H3 per step. Each step has: one-sentence description, template JSON, worked example JSON. No inline error notes — Phase 3 handles errors. Pure happy path as required by phase boundary.

### JSON-RPC Presentation

**Recommendation:** JSON body only (not full HTTP wrapper). State the POST URL and `Content-Type: application/json` header once in the intro to `## Verification Procedure`, not repeated per step. This keeps each example to a single clean JSON block.

### ABI Encoding Style

**Recommendation:** Formula + example (not byte-level walkthrough). State the rule once before the first eth_call step:

```
Call data = selector (8 hex chars) + agentId zero-padded to 64 hex chars
```

Then show both worked examples. No ABI spec reference needed — the rule is simple enough to state inline. This approach is self-contained without external dependencies.

### Selector Resolution in the Document

**Recommendation:** Show only the pre-computed value. Do not show the keccak256 computation. Agents cannot compute keccak256 — showing the derivation provides no actionable benefit and adds length. State: "Pre-computed selector for `getAgentWallet(uint256)`: `0x00339509`"

### QA-02 Anchoring

**Recommendation:** Anchor to behavior, not to viem or the JSON-RPC standard. State: "Pass the `hash:` field value directly as params[0]. Pass the `sig:` field value directly as params[1]. The RPC node handles EIP-191 prefix internally." This is accurate, actionable, and doesn't require agents to understand either viem internals or the full JSON-RPC spec.

---

## What Phase 2 Must NOT Include

Per the roadmap and phase boundary:

- No address comparison rule (Phase 3)
- No pass/fail definition (Phase 3)
- No error conditions (Phase 3)
- No trust forwarding (Phase 3)
- No response decoding explanation (user decision: agents can read hex addresses)
- No keccak256 computation steps (agents cannot run keccak256)
- No library-specific examples (ethers, viem, web3.py)
- No repetition of field definitions from Phase 1's Signed Block Format
- No mention of tokenURI (out of scope for verification)
- No mention of the `Verify:` URL or how it works

---

## Document Context

Phase 2 writes into an existing document. The current state of `public/ERC8004-SKILL.md`:

```
## Verification Procedure

<!-- Phase 2 -->

## Result

<!-- Phase 3 -->
```

The `<!-- Phase 2 -->` placeholder is replaced with the full verification procedure section. The `## Result` and subsequent sections are not touched.

The verification procedure section cross-references (by name):
- `## Signed Block Format` → for the `hash:` and `sig:` field names
- `## Agent Identity` → for the `registryAddress` and `agentId` components of the full ID

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead |
|---------|-------------|-------------|
| Function selector computation | Any in-doc derivation or formula | Pre-computed literals: `0x00339509` (getAgentWallet), `0x6352211e` (ownerOf) |
| ABI encoding explanation | Full ABI spec reference | One rule + worked example inline |
| EIP-191 prefix explanation | Inline prefix derivation | "RPC node handles internally" — one sentence |
| Response address decoding explanation | Step-by-step extraction | Show raw response + state the address value — per user decision, no explanation |

---

## Open Questions

1. **Real on-chain worked examples**
   - What we know: Phase 1 used synthetic data with a disclaimer; user decision says "show BOTH templates AND worked examples with real on-chain data"
   - What's unclear: What real on-chain data to use — the project doesn't specify a known real agentId+registry combination
   - Recommendation: Use synthetic data (same approach as Phase 1) with a disclaimer, or use the same synthetic agentId=5 from Phase 1. Real data would require knowing a live on-chain agent. If the planner/executor has access to real data, use it; otherwise synthetic with disclaimer is acceptable and consistent with Phase 1.

2. **Concurrent execution note**
   - What we know: Steps 2, 3, 4 can run concurrently (chain.cljs uses Promise.all)
   - What's unclear: Whether to include this as a note or leave it as an optimization agents discover themselves
   - Recommendation: Include a brief note at the top of the procedure: "Steps 2, 3, and 4 are independent and may run concurrently." This is factually correct and helps agents that support parallel requests.

---

## Sources

### Primary (HIGH confidence)
- `/home/teresa/dev/erc8004-arbitrum/src/verify/chain.cljs` — canonical verification behavior; `recoverMessageAddress` with `{:raw hash}` semantics; `getAgentWallet`/`ownerOf` as the two reads; wallet-match OR owner-match logic
- `node_modules/viem/_cjs/utils/signature/toPrefixedMessage.js` — viem source confirming `{raw: hexString}` behavior: passes hex string directly as message bytes, EIP-191 prefix applied by `hashMessage`
- `node_modules/viem/_cjs/utils/signature/hashMessage.js` — viem source confirming `hashMessage` calls `keccak256(toPrefixedMessage(message))`
- `viem.getFunctionSelector('getAgentWallet(uint256)')` — returned `0x00339509`, computed live in project environment
- `viem.getFunctionSelector('ownerOf(uint256)')` — returned `0x6352211e`, consistent with all prior research
- `/home/teresa/dev/erc8004-arbitrum/.planning/research/ARCHITECTURE.md` — complete JSON-RPC request/response examples, ABI encoding table, response decoding rule
- `/home/teresa/dev/erc8004-arbitrum/.planning/research/PITFALLS.md` — 12 documented pitfalls, 8 relevant to Phase 2
- `/home/teresa/dev/erc8004-arbitrum/public/ERC8004-SKILL.md` — current document state; Phase 2 writes into `## Verification Procedure` placeholder

### Secondary (MEDIUM confidence)
- OpenEthereum `personal_ecRecover` documentation (https://openethereum.github.io/JSONRPC-personal-module) — confirms params order (message, signature), confirms node applies EIP-191 prefix internally
- Ethereum JSON-RPC specification (https://ethereum.org/en/developers/docs/apis/json-rpc/) — confirms `eth_call` params format `[{to, data}, blockTag]`

---

## Metadata

**Confidence breakdown:**
- QA-01 selector resolution: HIGH — computed live in project environment via viem getFunctionSelector; two independent methods agree
- QA-02 parameter semantics: HIGH — traced through viem source to toPrefixedMessage.js; confirmed by OpenEthereum docs
- JSON-RPC request format: HIGH — from prior research (ARCHITECTURE.md) and Ethereum spec; stable, well-documented
- ABI encoding: HIGH — simple rule, verified with multiple agentId examples
- Discretion recommendations: MEDIUM — authoring judgment calls; multiple valid approaches exist

**Research date:** 2026-02-19
**Valid until:** Stable — function selectors are derived from ABI signatures which cannot change without breaking the contract; JSON-RPC formats are stable specs; RPC endpoints are official Offchain Labs endpoints
