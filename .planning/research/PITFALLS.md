# PITFALLS — ERC-8004 Agent Skill Document (JSON-RPC Verification)

**Research Date:** 2026-02-19
**Domain:** AI agent skill documents + Ethereum JSON-RPC + ERC-8004 signature verification
**Purpose:** Prevent critical mistakes when writing the ERC8004-SKILL.md verification instructions

---

## Summary

This document catalogs concrete pitfalls that affect two intersecting domains: (1) writing skill documents that AI agents can reliably follow, and (2) the specific technical hazards of on-chain ERC-8004 signature verification via raw JSON-RPC. Each pitfall is specific to this project — not generic writing advice.

---

## Pitfall 1 — personal_ecRecover Is Not a Standard JSON-RPC Method

**Domain:** JSON-RPC verification flow

**What goes wrong:** An agent reading the skill document issues a `personal_ecRecover` call and receives an error or empty result. Public RPC endpoints vary: the Arbitrum Foundation public RPC at `https://arb1.arbitrum.io/rpc` supports `personal_ecRecover`, but many third-party providers (Infura, Alchemy, QuickNode on default plans) disable the `personal_*` namespace entirely as a security measure. An agent that uses a different RPC endpoint than specified — perhaps from memory or because it substituted a "known Ethereum endpoint" — will get a method-not-found error.

**Warning signs:**
- Skill document says "call `personal_ecRecover`" without specifying the exact RPC URL
- Skill document lists `personal_ecRecover` as a fallback alongside `eth_call` without explaining which is primary
- Verification steps do not test the endpoint before instructing the agent to use it

**Prevention strategy:**
- Pin the exact RPC endpoint in the skill document: `https://arb1.arbitrum.io/rpc`
- Include a concrete curl test for `personal_ecRecover` against that exact endpoint so agents can self-verify
- State explicitly: "Do not substitute a different RPC endpoint — `personal_ecRecover` is not supported on all providers"
- Provide the raw JSON-RPC payload for `personal_ecRecover` so agents have no room to hallucinate the format

**Phase:** Skill document authoring — verification flow section

---

## Pitfall 2 — Agents Hallucinate JSON-RPC Request Structure

**Domain:** AI agent behavior + JSON-RPC

**What goes wrong:** An agent trained on many Ethereum codebases may confuse the JSON-RPC wire format with higher-level library abstractions. Common hallucinations include: omitting the `jsonrpc: "2.0"` field, using `method: "ecRecover"` instead of `method: "personal_ecRecover"`, passing parameters as a named object instead of a positional array, or using `id: null` in ways that cause some endpoints to reject the request. The agent generates a request that looks plausible but fails silently or returns a surprising error.

**Warning signs:**
- Skill document describes the request in prose ("call personal_ecRecover with the hash and signature") without showing the exact JSON body
- Skill document uses pseudocode or library-style notation (e.g., `web3.eth.personal.ecRecover(hash, sig)`) instead of raw JSON
- No example of the expected response structure is provided

**Prevention strategy:**
- Show the complete, verbatim request body including all required fields:
  ```json
  {
    "jsonrpc": "2.0",
    "method": "personal_ecRecover",
    "params": ["0x<hash>", "0x<signature>"],
    "id": 1
  }
  ```
- Show the complete expected response body:
  ```json
  {"jsonrpc":"2.0","id":1,"result":"0x<recovered-address>"}
  ```
- State parameter order explicitly: hash first, signature second — this is the opposite of some library conventions
- Provide a working curl example the agent can execute verbatim

**Phase:** Skill document authoring — all JSON-RPC call sections

---

## Pitfall 3 — ABI Encoding Is Opaque and Error-Prone Without Libraries

**Domain:** JSON-RPC verification flow + ABI encoding

**What goes wrong:** Reading `getAgentWallet(uint256)` and `ownerOf(uint256)` via `eth_call` requires manually constructing the `data` field: a 4-byte function selector followed by a 32-byte zero-padded uint256. Agents asked to "encode the call data" without explicit values will make mistakes: using the wrong selector, padding incorrectly (left-pad, not right-pad), omitting the `0x` prefix, or confusing the decimal agent ID with its hex representation.

**Warning signs:**
- Skill document says "encode the function call" without providing pre-computed selectors
- Skill document describes ABI encoding rules abstractly ("pad to 32 bytes") without a worked example
- No concrete data field values are provided for specific functions

**Prevention strategy:**
- Pre-compute and hard-code the exact function selectors in the skill document:
  - `getAgentWallet(uint256)`: selector `0x9a5b0c6b` (or the actual computed keccak256 of the signature)
  - `ownerOf(uint256)`: selector `0x6352211e`
  - `tokenURI(uint256)`: selector `0xc87b56dd`
- Provide a complete worked example showing how to construct `data` for agent ID 5:
  ```
  data = 0x9a5b0c6b + 0000000000000000000000000000000000000000000000000000000000000005
  ```
- State explicitly: "The uint256 argument is left-padded with zeros to 32 bytes (64 hex characters)"
- State explicitly: "Convert the agent ID from decimal to hex before padding"

**Phase:** Skill document authoring — on-chain registry read section

---

## Pitfall 4 — Address Comparison Is Case-Sensitive in Naive Implementations

**Domain:** JSON-RPC verification flow

**What goes wrong:** The recovered signer address from `personal_ecRecover` is typically returned in EIP-55 checksummed mixed-case format (e.g., `0xAbCd...`). The `getAgentWallet` response may return a different checksum or all-lowercase. An agent performing a direct string comparison will report a false negative — "signer does not match wallet" — when the addresses are actually identical.

The existing verifier handles this correctly via `.toLowerCase()` on both sides (see `chain.cljs` lines 101-105), but a naive agent following incomplete instructions will not.

**Warning signs:**
- Skill document says "compare the recovered address to the agent wallet address" without specifying case normalization
- No mention of EIP-55 checksumming in the verification instructions
- Example values in the document use inconsistent case between recovered address and wallet address

**Prevention strategy:**
- State the comparison rule explicitly: "Normalize both addresses to lowercase before comparing"
- Show the comparison in pseudocode:
  ```
  match = (recovered_address.toLowerCase() === agent_wallet.toLowerCase())
  ```
- Include an example where the two addresses differ only in checksum casing to make the rule concrete

**Phase:** Skill document authoring — address comparison section

---

## Pitfall 5 — The Hash Field Is Used Directly, Not Recomputed From Content

**Domain:** Skill document design + agent misunderstanding

**What goes wrong:** The signed content block includes a `hash:` field. The actual hash is `keccak256(abi.encode(keccak256(content), timestamp))`. An agent reading the skill document may assume it needs to recompute this hash from the content in order to verify, and will attempt to call keccak256 — which it cannot do without crypto libraries. Alternatively, it may misread the hash as the keccak256 of just the content text, leading to a wrong verification path.

The correct approach for agents is to use the `hash:` field directly in `personal_ecRecover` — the hash is self-attesting once the signer is confirmed on-chain.

**Warning signs:**
- Skill document mentions the hash construction formula without immediately clarifying agents do not need to recompute it
- The formula `keccak256(abi.encode(keccak256(content), timestamp))` appears without a "you don't need this" caveat
- Verification steps include "compute the hash from the content" before the `personal_ecRecover` call

**Prevention strategy:**
- Lead with the verification path that agents actually use: take `hash:` from the signed block, pass it directly to `personal_ecRecover`
- Explicitly state: "You do not need to recompute the hash. The `hash:` field in the signed block is the value to pass to `personal_ecRecover` directly."
- If the formula is included for completeness, segregate it into a separate "How signing works" section clearly labeled as informational only

**Phase:** Skill document authoring — verification overview section

---

## Pitfall 6 — Skill Documents Describe the Happy Path Only

**Domain:** Skill document design

**What goes wrong:** A skill document that only describes the success case leaves agents unable to handle the common failure conditions they will actually encounter: RPC errors, rate limits, agent not found in registry, signature recovery returning an unexpected address, tokenURI returning a non-data URI. An agent that receives `{"error": {"code": -32603, "message": "Internal error"}}` has no guidance and will either halt or hallucinate a recovery strategy.

**Warning signs:**
- No section on error conditions or error response formats
- Skill document has no "what to do if X fails" instructions
- JSON-RPC error response format not documented
- Rate limiting not mentioned

**Prevention strategy:**
- Include a dedicated error handling section covering:
  - RPC error response format: `{"error": {"code": ..., "message": "..."}}`
  - Common error codes: -32601 (method not found), -32603 (internal/rate limit), -32000 (execution reverted)
  - What "execution reverted" means for registry reads (agent ID does not exist)
  - What to do on rate limit: retry with exponential backoff, or try the Sepolia testnet endpoint for testing
- Specify the difference between "signature recovery succeeded but address does not match" (invalid signature, not an error) vs. "RPC call failed" (infrastructure problem)

**Phase:** Skill document authoring — error handling section; also relevant during testing/validation phase

---

## Pitfall 7 — Inconsistent Parameter Types for personal_ecRecover

**Domain:** JSON-RPC verification flow

**What goes wrong:** The `personal_ecRecover` method expects both parameters as hex strings prefixed with `0x`. If an agent passes the hash as raw bytes, as a decimal integer, or without the `0x` prefix, the RPC endpoint may return an error or silently return a wrong address. Additionally, some documents describe the message as the "content" text rather than the hash — leading agents to pass the human-readable content string to `personal_ecRecover` instead of the 32-byte hash.

**Warning signs:**
- Skill document refers to the first parameter as "the message" without specifying it is the hex-encoded hash, not the content text
- No explicit statement that both parameters must be `0x`-prefixed hex strings
- No mention of the expected byte length of the hash parameter (32 bytes = 64 hex chars + `0x` prefix = 66 chars total)

**Prevention strategy:**
- State parameter types explicitly:
  - Parameter 1: `"0x" + 64 hex characters` (the 32-byte keccak256 hash from the `hash:` field)
  - Parameter 2: `"0x" + 130 hex characters` (the 65-byte ECDSA signature from the `sig:` field)
- Validate that the hash and sig fields from the parsed block already have the correct format — the parser only accepts `0x`-prefixed hex, so agents can use them verbatim
- Show a concrete example with real-length values (truncated with `...` to communicate expected length)

**Phase:** Skill document authoring — personal_ecRecover call section

---

## Pitfall 8 — eth_call Requires the `to` Field; Missing or Wrong Registry Address Fails Silently

**Domain:** JSON-RPC verification flow

**What goes wrong:** `eth_call` requires a `to` field containing the contract address. The registry address is embedded in the agent's full ID (`eip155:<chainId>:<registryAddress>:<agentId>`). An agent that misparses the full ID — treating the registry address as the chain ID, or dropping the `0x` prefix — will call the wrong address or no address, receiving `0x` (empty return data) which decodes as address zero (`0x0000...0000`). This looks like a valid response and the agent proceeds to compare the signer against the zero address, producing a spurious "not verified" result with no clear error.

**Warning signs:**
- Skill document does not show how to parse the full ID format
- No explicit statement that the registry address comes from the full ID, not from a hardcoded constant
- eth_call examples use a placeholder that might be confused with the chain's native RPC address

**Prevention strategy:**
- Show the full ID parsing explicitly with a labeled example:
  ```
  eip155:42161:0x8004...a432:5
        ─────  ─────────────── ─
        chainId  registryAddress  agentId
  ```
- Show the complete `eth_call` params including the `to` field populated from the parsed registry address
- Warn: "If `eth_call` returns `0x` or the zero address, the registry address or agent ID is likely wrong"

**Phase:** Skill document authoring — on-chain registry read section

---

## Pitfall 9 — Skill Document Implicitly Assumes Crypto Library Availability

**Domain:** Skill document design + agent capability assumptions

**What goes wrong:** The existing `ai.html` mentions the hash formula `keccak256(abi.encode(keccak256(content), timestamp))`. If a verification skill document includes similar formulas without qualification, agents may attempt to use them, fail because they have no keccak256 implementation, and report verification as impossible rather than following the simpler `personal_ecRecover` path that avoids crypto entirely.

Additionally, a document that assumes `ethers.js` or `web3.py` idioms in pseudocode creates a false dependency. Agents may search for those tools or attempt to install them rather than using raw HTTP.

**Warning signs:**
- Skill document uses library-specific syntax in examples: `ethers.utils.verifyMessage()`, `web3.eth.accounts.recover()`, `recoverMessageAddress()`
- Pseudocode references `keccak256` or `abi.encode` in the verification path (not just the "how signing works" context)
- No explicit statement of the agent capability assumption: "You only need HTTP fetch — no libraries required"

**Prevention strategy:**
- Open the skill document with an explicit capability statement: "This procedure requires only the ability to make HTTP POST requests. No cryptographic libraries are needed."
- Use only raw JSON-RPC in all examples
- Remove or clearly segregate any formula that involves keccak256 from the verification procedure itself
- Test the document by mentally executing it as an agent that has no Ethereum libraries

**Phase:** Skill document design — capability model, must be established before any procedure is written

---

## Pitfall 10 — Verification Result Is Ambiguous Without Both Registry Checks

**Domain:** Skill document design + verification logic

**What goes wrong:** The ERC-8004 registry supports two valid authorized addresses per agent: the `agentWallet` (the agent's operational key) and the NFT `owner` (the agent's controlling account). A skill document that only instructs checking one of these will produce false negatives — for example, if a user signs with their owner key directly, a document that only checks `getAgentWallet` will report the signature as unverified even though it is legitimate.

This mirrors the existing verifier behavior (see `chain.cljs` lines 102-107: `wallet-match` OR `owner-match`).

**Warning signs:**
- Skill document describes only one registry read (either `getAgentWallet` OR `ownerOf`, not both)
- No explanation that the signature is valid if either address matches
- Verification result logic says "compare recovered address to agent wallet" without mentioning owner

**Prevention strategy:**
- Instruct the agent to perform both reads: `getAgentWallet(agentId)` and `ownerOf(agentId)`
- State the verification logic explicitly: "The signature is valid if the recovered address matches EITHER the agent wallet OR the NFT owner"
- Explain the two-key model briefly: wallet = operational signing key, owner = controlling account key

**Phase:** Skill document authoring — verification logic section

---

## Pitfall 11 — Rate Limiting on Public RPC Causes Non-Obvious Failures

**Domain:** JSON-RPC verification flow + production reliability

**What goes wrong:** The public Arbitrum RPC endpoints (`arb1.arbitrum.io/rpc`) have undocumented rate limits. An agent running automated verification (e.g., verifying many signatures in sequence) may hit these limits. The error response is a generic `{"error": {"code": -32603, "message": "..."}}` or an HTTP 429, not a clear "rate limited" message. Agents without explicit guidance will not know to back off, retry, or use an alternative endpoint.

Additionally, CORS restrictions mean that browser-based agents cannot always fall back to alternative endpoints that do not include permissive CORS headers.

**Warning signs:**
- Skill document has no mention of rate limits
- No retry guidance
- No mention of Arbitrum Sepolia as a testing alternative
- No fallback endpoint listed

**Prevention strategy:**
- Note that the public RPC has rate limits; recommend adding delays between calls for bulk verification
- List the Sepolia endpoint (`https://sepolia-rollup.arbitrum.io/rpc`) as the correct endpoint for development and testing
- Specify that agents should treat HTTP 429 or JSON-RPC error code -32603 as potentially rate-limit-related
- Recommend a simple retry: wait 2 seconds, retry once before reporting failure

**Phase:** Skill document authoring — prerequisites/constraints section and error handling section

---

## Pitfall 12 — Skill Document Format Is Not Machine-Readable

**Domain:** Skill document design

**What goes wrong:** A skill document written primarily for humans — with dense narrative prose, no structured sections, no labeled code blocks, and no clear "do this, then do that" imperative structure — is hard for agents to follow reliably. Agents parse natural language ambiguously. An instruction like "then you'd want to check the address against what's on-chain" is vague; "Step 3: Call `eth_call` with the following body and compare the `result` field to the recovered address" is not.

Common failures: agents skip steps because they misread prose as explanation rather than instruction, agents mistake an example as something they should execute rather than a reference, agents cannot locate the relevant section for the step they are on.

**Warning signs:**
- No numbered steps in the verification procedure
- Steps mixed with background explanation in the same paragraph
- Code examples not labeled as "request" vs. "response" vs. "expected value"
- No explicit "SUCCESS criterion" statement at the end of each step

**Prevention strategy:**
- Use numbered lists for all procedural content
- Separate background explanation from procedure: put explanation in a "How it works" section, not inline with steps
- Label every code block: `REQUEST`, `RESPONSE`, `EXPECTED FORMAT`
- End each step with a success criterion: "If `result` contains a 42-character hex string starting with `0x`, proceed to step N"
- Avoid idioms like "you might want to" or "typically you would" — every instruction should be imperative and unambiguous

**Phase:** Skill document authoring — applies to the entire document structure

---

## Cross-Cutting Observations

**Encoder/decoder mismatch risk:** The signed block parser (`parse.cljs`) requires a very specific format: content, then `---` separator, then "Signed by" line, then `sig:`, `hash:`, `ts:` in order. Any agent-generated verification attempt that also involves re-constructing or re-formatting these blocks risks producing a block the parser rejects. The skill document should treat the signed block as immutable input — agents must not modify any field.

**Chain ID matters for RPC selection:** The full ID encodes the chain ID. An agent that hardcodes the Arbitrum mainnet RPC but encounters a Sepolia full ID (`eip155:421614:...`) will query the wrong chain. The skill document must map chain IDs to RPC endpoints explicitly.

**`tokenURI` is optional:** `tokenURI` is used only to retrieve the agent name for display purposes. Failure to retrieve it must not fail the verification. The skill document must label this step as optional and state that verification can succeed without it.

**BigInt/integer encoding for uint256:** The agent ID in the full ID is a decimal integer (e.g., `5`). In ABI encoding for `eth_call`, it must become a 32-byte zero-padded hex value. In `personal_ecRecover`, it is not used at all. Conflating these contexts is a common source of encoding errors — the skill document must clearly state which format is required at each step.

---

*Pitfalls research: 2026-02-19*
