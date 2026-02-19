# Research Summary: ERC-8004 Agent Skill Document

**Project:** ERC-8004 Verifier — Agent Skill Expansion (ERC8004-SKILL.md)
**Domain:** AI agent skill documents — machine-readable JSON-RPC verification instructions
**Researched:** 2026-02-19
**Confidence:** HIGH

---

## Executive Summary

This project has a single, tightly scoped deliverable: replace `public/ai.html` with `ERC8004-SKILL.md`, a markdown file that teaches AI agents how to sign and verify ERC-8004 content using only HTTP fetch. No new runtime code is required — the existing ClojureScript browser verifier is unchanged. The entire work surface is one markdown document, but its quality determines whether agents can successfully follow it. Research across all four areas converges on the same finding: the hardest problems are instructional design and completeness, not technical implementation.

The recommended approach is to write the skill document in execution order — each section providing exactly what the next step requires before it is needed. The document must embed all pre-computed constants (function selectors, RPC endpoints, ABI encoding examples), show verbatim JSON-RPC request/response pairs, and state the verification logic as explicit pseudocode rather than prose. The critical insight from architecture research is that verification reduces to two parallel HTTP POST calls and one case-insensitive string comparison — agents do not need crypto libraries, keccak256, or any web3 tooling. The `hash:` field in the signed block is passed directly to `personal_ecRecover`; no recomputation is needed.

The primary risk is not technical but instructional: a skill document that omits any of its blocking steps — signed block format spec, full ID parsing, function selectors, ABI encoding examples, address case normalization, or the two-check verification rule — produces an agent that either halts or silently produces wrong results. Pitfall research identifies 12 concrete failure modes, all preventable through completeness and precision. There are no ambiguous technical choices. Every technical decision is already resolved by the existing codebase; the task is to describe what the browser app already does, expressed for agents using raw HTTP.

---

## Key Findings

### Recommended Stack

The skill document is plain CommonMark markdown — no HTML, no JSON schema, no OpenAPI. LLMs ingest markdown natively without parsing overhead, it survives copy-paste, and it renders correctly in GitHub, Claude, and GPT interfaces. All agent interactions with the blockchain use JSON-RPC 2.0 over HTTPS POST to public Arbitrum endpoints that require no authentication. Two Ethereum methods cover the entire verification surface: `personal_ecRecover` (ECDSA recovery) and `eth_call` (contract reads). Agents cannot run keccak256 at runtime, so all selectors and encoding constants must be pre-computed and embedded as literals in the document.

**Core technologies:**
- Markdown (CommonMark): skill document format — LLMs parse natively, no tooling required, stable under copy-paste
- JSON-RPC 2.0: wire protocol — all public Arbitrum nodes speak it, no SDK or auth required, one POST per call
- `personal_ecRecover` (EIP-191): signer recovery — the inverse of the existing `personal_sign` operation, available on `arb1.arbitrum.io/rpc` without authentication
- `eth_call` with manual ABI encoding: contract reads — pure view calls, no wallet, no gas, no state change; only two functions needed (`getAgentWallet`, `ownerOf`)
- Pre-computed function selectors: embedded constants — agents cannot compute keccak256 at runtime; selectors must be literals in the document

### Expected Features

The feature research establishes a strict dependency chain for verification: format spec → full ID spec → ecRecover call spec → eth_call specs → address comparison rule → pass/fail definition. Every link is blocking; none can be skipped. The document must cover all of these for a verification-only agent to succeed.

**Must have (table stakes) — v1:**
- Explicit capability declaration at the top (web fetch only, no libraries)
- Signed block format spec with annotated real example (content, `---`, `Signed by`, `sig:`, `hash:`, `ts:`, `Verify:`)
- Agent identity format spec: `eip155:<chainId>:<registryAddress>:<agentId>` field-by-field
- Signing instructions preserved from `ai.html`
- `personal_ecRecover` call spec: exact JSON request/response pair, parameter types, endpoint URL
- `eth_call` specs for `getAgentWallet` and `ownerOf`: pre-computed selectors, ABI encoding pattern, address decoding
- RPC endpoint list: Arbitrum mainnet (42161) and Sepolia (421614), chain IDs
- Verification pass/fail rule: explicit boolean — `signer == wallet OR signer == owner`
- Address comparison rule: lowercase both before comparing
- Error conditions: three named failure cases with recommended agent responses

**Should have (differentiators) — v1:**
- Pre-computed complete `data` field values for both `getAgentWallet` and `ownerOf` with worked example for agentId=5
- Step-by-step numbered procedure (1-parse, 2-ecRecover, 3-getAgentWallet, 4-ownerOf, 5-compare, 6-report)
- JSON-RPC request/response shown as paired code blocks (not interleaved with prose)
- Explicit scope statement — what this document does NOT cover
- Machine-readable heading structure (H2/H3 consistent naming)

**Defer (v1.x, after validation):**
- Trust forwarding concept section (add once basic verification loop is tested)
- Worked example with real on-chain data
- Explicit scope statement (add once we see what agents attempt incorrectly)

**Defer (v2+):**
- Arbitrum Sepolia testnet verification example
- Batch verification guidance
- `tokenURI` / agent name resolution

### Architecture Approach

The verification architecture is a linear, forward-only data flow with no circular dependencies and one parallelizable segment. The agent owns three concerns: (1) parsing the signed block to extract five fields, (2) making two independent HTTP POST calls to the Arbitrum RPC, and (3) comparing the resulting addresses. The RPC node owns ECDSA recovery and contract state reads. The registry contract owns the authoritative agentId → wallet/owner mapping. Steps 2 and 3 (ecRecover and eth_call) can execute concurrently — they share only the parsed inputs from step 1.

**Major components:**
1. Signed block parser — regex-based field extraction from the `---`-delimited footer block; inputs: raw text; outputs: hash, sig, chainId, registryAddress, agentId
2. `personal_ecRecover` caller — HTTP POST with `[hash, sig]` params; outputs: recovered signer address or error
3. `eth_call` caller (x2) — HTTP POST with pre-computed calldata to registry contract; outputs: agent wallet address, NFT owner address
4. Address comparator — lowercase normalization + OR comparison; outputs: VERIFIED or FAILED with reason
5. Error classifier — maps RPC error codes and empty responses to named failure states; outputs: actionable agent response

### Critical Pitfalls

1. **`personal_ecRecover` is not universally supported** — pin the exact endpoint (`https://arb1.arbitrum.io/rpc`) and state explicitly that substituting another provider will fail; include a working curl test the agent can run to confirm support
2. **Agents hallucinate JSON-RPC request structure** — show the complete verbatim request body including `jsonrpc`, `method`, `params` (as array, not object), and `id`; show the response body; state parameter order (hash first, signature second)
3. **ABI encoding without libraries requires exact pre-computed values** — embed the function selectors as literals (`getAgentWallet`: `0x3cef5e0f`, `ownerOf`: `0x6352211e`); show the complete `data` field for agentId=5; state the padding rule once concretely
4. **Address comparison is case-sensitive in naive implementations** — state the rule explicitly: lowercase both addresses before comparing; show pseudocode with `.toLowerCase()`
5. **The `hash:` field is used directly, not recomputed** — lead the verification section with "use the `hash:` field verbatim; do not recompute it"; if the construction formula appears anywhere, segregate it with a clear "informational only" label
6. **Verification logic requires both registry checks** — `getAgentWallet` AND `ownerOf`; the OR condition is required because agents may sign with either the operational wallet or the owner key; documenting only one check produces false negatives

---

## Implications for Roadmap

Based on research, this project has one active milestone with a clean dependency order. The build sequence is determined by the feature dependency chain: you cannot write verification steps until the format spec is complete, and you cannot write the comparison logic until both RPC call specs are written.

### Phase 1: Signed Block Format and Signing Instructions

**Rationale:** Every downstream section depends on the signed block format being fully specified. This is also the section carried over from `ai.html` with the lowest risk — the format is stable and fully documented in the existing codebase. Completing this first makes all remaining sections unambiguous.

**Delivers:** Section 1 (capability declaration), Section 2 (signed block format with annotated example), Section 3 (agent identity format spec `eip155:...`), Section 4 (signing instructions from `ai.html`)

**Addresses features:** Explicit capability declaration, signed block format spec, full ID format spec, signing instructions preserved

**Avoids pitfalls:** Pitfall 5 (hash recomputation confusion — state in this section that the `hash:` field is used directly), Pitfall 8 (registry address parsing — show the full ID breakdown here), Pitfall 12 (machine-readable structure — establish heading conventions from the start)

### Phase 2: Verification Procedure and JSON-RPC Call Specs

**Rationale:** With the format spec complete, the verification procedure can be written in strict dependency order. The three RPC call specs (`personal_ecRecover`, `getAgentWallet`, `ownerOf`) are independent of each other but all depend on Phase 1 outputs. This phase is the technical core and the highest risk — pitfall research identifies 8 of 12 pitfalls concentrated here.

**Delivers:** Section 5 (numbered verification procedure), Section 6 (`personal_ecRecover` call spec with full JSON and worked example), Section 7 (registry reads via `eth_call` — both functions with pre-computed selectors, ABI encoding, address decoding), Section 8 (RPC endpoints table)

**Uses:** JSON-RPC 2.0 over HTTPS POST, pre-computed selectors `0x3cef5e0f` and `0x6352211e`, ABI uint256 encoding pattern

**Avoids pitfalls:** Pitfall 1 (endpoint specificity), Pitfall 2 (verbatim JSON structure), Pitfall 3 (pre-computed selectors), Pitfall 7 (parameter types and byte lengths), Pitfall 9 (no library assumptions)

### Phase 3: Verification Result Logic and Error Handling

**Rationale:** The pass/fail rule and error conditions are written last because they reference the exact RPC response shapes established in Phase 2. The two-check OR condition (Pitfall 10) and the address case normalization (Pitfall 4) belong here as explicit rules.

**Delivers:** Section 9 (verification result definition — explicit boolean rule with pseudocode), Section 10 (error conditions — three named failure cases, RPC error format, retry guidance)

**Addresses features:** Address comparison rule, verification pass/fail definition, error conditions named

**Avoids pitfalls:** Pitfall 4 (case normalization), Pitfall 6 (happy-path-only documents), Pitfall 10 (both registry checks required), Pitfall 11 (rate limiting and retry guidance)

### Phase 4: Build Pipeline and Discovery Integration

**Rationale:** Once the skill document content is complete, it needs to be served and discoverable. This phase is entirely mechanical — update build scripts to copy `ERC8004-SKILL.md` instead of (or alongside) `ai.html`, add the "For Agents" box to `index.html` pointing to the fetch URL, and verify the document is served as `text/plain` or `text/markdown`.

**Delivers:** ERC8004-SKILL.md in `public/` and `dist/`; updated `bun run build` pipeline; "For Agents" box on `index.html` with the skill doc URL; correct MIME type for `.md` serving

**Addresses features:** Stable fetch URL, discovery via index.html

**Avoids pitfalls:** Serving as `text/html` would cause agents to receive HTML-wrapped content; must verify MIME type

### Phase Ordering Rationale

- Phase 1 before Phase 2: format spec is a hard dependency for every verification step — you cannot describe what fields to extract until the format is specified
- Phase 2 before Phase 3: the pass/fail rule references specific RPC response fields; the error conditions reference specific RPC error codes — both require Phase 2 to be concrete
- Phase 3 before Phase 4: the document must be complete before wiring up delivery
- Phases 1-3 are document-authoring phases; Phase 4 is infrastructure — no overlap in work surface

### Research Flags

Phases with standard patterns (skip additional research):
- **Phase 1:** Format is fully specified in `parse.cljs` and `ai.html`; no research needed, only transcription and annotation
- **Phase 4:** Build pipeline changes are mechanical; shadow-cljs copy pattern is documented in AGENTS.md

Phases that need attention during execution (not research, but validation):
- **Phase 2:** The `getAgentWallet` selector appears as `0x45a3a8d5` in STACK.md and as `0x3cef5e0f` in ARCHITECTURE.md — these values are inconsistent. **This must be resolved before writing Phase 2.** Compute from the actual deployed contract or from `keccak256("getAgentWallet(uint256)")`. PITFALLS.md uses yet another value (`0x9a5b0c6b`). Only one can be correct. See Gaps section.
- **Phase 3:** Verify that the retry recommendation (2-second wait, retry once) is appropriate for the public Arbitrum RPC — this is a judgment call not testable from research alone.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All technology choices validated against existing codebase; JSON-RPC and markdown formats are stable specs; no version ambiguity |
| Features | HIGH | Feature list derived from existing `ai.html`, `parse.cljs`, `chain.cljs`, and `AGENTS.md`; no speculation — every feature maps to existing behavior |
| Architecture | HIGH | Verification flow mirrors the existing browser app exactly; data flow is simple and linear; RPC call formats verified against Ethereum JSON-RPC spec |
| Pitfalls | HIGH | Pitfalls derived from direct analysis of existing code paths and known agent failure modes; all 12 are concrete and preventable |

**Overall confidence:** HIGH

### Gaps to Address

- **Function selector conflict (blocking):** The `getAgentWallet(uint256)` function selector appears as three different values across research files:
  - STACK.md: `0x45a3a8d5`
  - ARCHITECTURE.md: `0x3cef5e0f`
  - PITFALLS.md: `0x9a5b0c6b`

  Only one value is correct. This must be resolved before writing the verification section. Resolution approach: compute `keccak256("getAgentWallet(uint256)")` directly and take the first 4 bytes. The `ownerOf(uint256)` selector `0x6352211e` is consistent across all three files and can be used as-is.

- **Skill doc hosted URL:** PROJECT.md specifies `http://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md` but STACK.md specifies `https://erc8004.orbiter.website/ERC8004-SKILL.md`. Confirm the correct hosting URL before writing the discovery section. This affects the "For Agents" box and any self-referential URLs in the document.

- **`personal_ecRecover` hash parameter semantics:** STACK.md states "The hash field in the signed block is already the prefixed personal_sign hash" while also saying "Do not double-prefix." ARCHITECTURE.md says "The RPC node applies EIP-191 prefix internally during recovery." These descriptions are consistent but potentially confusing. The skill document must state the rule with a single, unambiguous sentence — confirm against the actual `chain.cljs` behavior (`recoverMessageAddress` usage) before writing.

---

## Sources

### Primary (HIGH confidence)
- `/home/teresa/dev/erc8004-arbitrum/src/verify/chain.cljs` — confirms `recoverMessageAddress` = `personal_ecRecover` inverse, `getAgentWallet`/`ownerOf` as the only needed reads, wallet-match OR owner-match logic
- `/home/teresa/dev/erc8004-arbitrum/src/verify/parse.cljs` — confirms signed block format, field names, and that `hash:` field is the pre-computed value passed directly to recovery
- `/home/teresa/dev/erc8004-arbitrum/public/ai.html` — source for signing instructions to preserve
- `/home/teresa/dev/erc8004-arbitrum/AGENTS.md` — contract ABI (three functions), supported chains, RPC endpoints
- Ethereum JSON-RPC specification: https://ethereum.org/en/developers/docs/apis/json-rpc/
- EIP-191 personal_sign: https://eips.ethereum.org/EIPS/eip-191
- Arbitrum public RPC docs: https://docs.arbitrum.io/build-decentralized-apps/public-chains

### Secondary (MEDIUM confidence)
- ERC-8004 draft: https://eips.ethereum.org/EIPS/eip-8004 — spec for agent identity format
- ABI encoding spec: https://docs.soliditylang.org/en/latest/abi-spec.html — function selector and uint256 encoding rules

### Tertiary (requires validation)
- Pre-computed `getAgentWallet(uint256)` selector — inconsistent across research files; must be verified before use

---

*Research completed: 2026-02-19*
*Ready for roadmap: yes (pending resolution of getAgentWallet selector gap)*
