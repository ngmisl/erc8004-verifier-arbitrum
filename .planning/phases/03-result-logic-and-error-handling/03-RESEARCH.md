# Phase 3: Result Logic and Error Handling - Research

**Researched:** 2026-02-19
**Domain:** Agent skill document authoring — pass/fail logic, error conditions, trust forwarding
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| VFY-07 | Address comparison rule — case-insensitive hex comparison, explicit lowercase normalization | Confirmed in chain.cljs lines 101-105: `.toLowerCase()` on both signer and each registry address before `=` comparison. Pattern well established in PITFALLS.md Pitfall 4. |
| VFY-08 | Pass/fail definition — verified if recovered signer matches either getAgentWallet OR ownerOf result | Confirmed in chain.cljs line 107: `(or wallet-match owner-match)`. PITFALLS.md Pitfall 10 documents the failure mode of checking only one. |
| VFY-10 | Three named error conditions with recommended agent response (network error, address mismatch, unregistered agent) | ARCHITECTURE.md documents error signal patterns. chain.cljs catch block (line 121-125) shows the network/recovery error path. PITFALLS.md Pitfall 6 catalogs the need for named error guidance. |
| TRUST-01 | Conceptual trust forwarding guidance — one paragraph explaining how agents can relay verification results without prescribing a format | No prior decisions constrain the approach. ARCHITECTURE.md does not cover trust forwarding. This is discretionary content: one paragraph, conceptual framing only, no format prescription. |
</phase_requirements>

---

## Summary

Phase 3 writes three sections into `public/ERC8004-SKILL.md`: `## Result`, `## Error Conditions`, and `## Trust Forwarding`. All three are currently `<!-- Phase 3 -->` placeholders. The technical facts for `## Result` and `## Error Conditions` are fully established by `chain.cljs` (the authoritative implementation). No new technical discoveries are needed.

The comparison rule (VFY-07) and pass/fail definition (VFY-08) are a direct transcription of `chain.cljs` lines 101-107 into agent-readable pseudocode and prose. The address normalization rule is explicit in the source: `.toLowerCase()` on both sides before `=`. The OR condition is explicit: `(or wallet-match owner-match)`.

The three error conditions (VFY-10) are derived from the `chain.cljs` catch block, the ARCHITECTURE.md error case table, and PITFALLS.md. The three conditions are: (1) network/RPC error preventing calls, (2) recovered address does not match either registry address (address mismatch), and (3) eth_call returns zero/empty meaning the agentId does not exist (unregistered agent). Each has a distinct recommended response.

Trust forwarding (TRUST-01) is pure authoring discretion — one conceptual paragraph, no code examples, no format prescription. The prior decisions establish terse spec tone and no "why" sections; the trust forwarding paragraph should follow the same voice while remaining conceptual enough not to constrain agent implementations.

**Primary recommendation:** Write all four requirements as a single plan (03-01-PLAN). The four items are closely coupled — Result references the three addresses from Steps 2-4, Error Conditions references the Result states, and Trust Forwarding follows naturally after Result. One coherent section set is better than two plans that split related content.

---

## Technical Facts — Source of Truth

### VFY-07 and VFY-08: Address Comparison and Pass/Fail (chain.cljs)

Source: `src/verify/chain.cljs` lines 101-107 (HIGH confidence — authoritative implementation)

```clojure
signer-lower (.toLowerCase signer)
wallet-match (= signer-lower (.toLowerCase agent-wallet))
owner-match  (= signer-lower (.toLowerCase agent-owner))
...
:verified (or wallet-match owner-match)
```

**What this means for the skill document:**

- Both sides lowercased before comparison — this must be stated as an explicit rule, not implied
- Two independent comparisons: signer vs agent-wallet, and signer vs agent-owner
- Pass condition: OR of the two comparisons — `wallet-match OR owner-match`
- Fail condition: neither comparison true AND no RPC error (addresses are valid but don't match)

**Pseudocode for the skill doc:**

```
signer_lower  = <recovered address from Step 2>.toLowerCase()
wallet_lower  = <agent wallet from Step 3>.toLowerCase()
owner_lower   = <NFT owner from Step 4>.toLowerCase()

verified = (signer_lower == wallet_lower) OR (signer_lower == owner_lower)
```

**Why two addresses:** The ERC-8004 registry has two authorized addresses per agent — the `agentWallet` (operational signing key) and the NFT `owner` (controlling account). Either may sign content. PITFALLS.md Pitfall 10 identifies checking only one as a source of false negatives.

**Address case context:** `personal_ecRecover` returns a checksummed EIP-55 address (mixed case). The `eth_call` responses from the worked examples in Phase 2 are all-lowercase (the ABI-encoded 32-byte return is decoded as all-lowercase hex). Lowercasing both sides is the correct normalization regardless of which direction case differs. PITFALLS.md Pitfall 4 confirms this as a known failure mode.

---

### VFY-10: Three Named Error Conditions

Source: `chain.cljs` lines 83-87 (unsupported chain), lines 121-125 (catch block), `ARCHITECTURE.md` error case table, `PITFALLS.md` Pitfall 6 (HIGH confidence)

The three error conditions that require named treatment are:

**Condition 1: Network Error** (RPC call fails before returning a result)

- Signal: HTTP error (timeout, 5xx), or JSON-RPC error code `-32603` (internal), or `-32602` (invalid params)
- chain.cljs behavior: `.catch` block resolves with `{:verified false :error "Signature recovery failed: ..."}` — does NOT return a positive "failed" result, treats it as an infrastructure problem
- Recommended agent response: Do not report the content as unverified. Report that verification was unable to complete due to an RPC error. The signature itself may be valid — inability to contact the network is not evidence of tampering.
- Distinction from address mismatch: this is an infrastructure failure, not a cryptographic outcome

**Condition 2: Address Mismatch** (all RPC calls succeed, but recovered signer matches neither registry address)

- Signal: `verified = false` AND no RPC error (all three steps returned valid addresses)
- chain.cljs behavior: resolves with `{:verified false}` with `:wallet-match false :owner-match false`
- Recommended agent response: Report the content as NOT VERIFIED. This is a cryptographic outcome — the signature does not correspond to an authorized key for this agent. Treat as a definitive negative result, not an error.
- Distinction from network error: the RPC calls succeeded; the failure is in the comparison, not the infrastructure

**Condition 3: Unregistered Agent** (eth_call returns zero address or empty for the agentId)

- Signal: `eth_call` for `getAgentWallet` or `ownerOf` returns `0x` (empty result) or the zero address `0x0000000000000000000000000000000000000000`
- ARCHITECTURE.md: "Contract not deployed at address → `eth_call` returns `0x` empty result" and "Agent ID does not exist (ownerOf reverts) → `eth_call` returns error or empty"
- chain.cljs: does not special-case this — a zero address comparison will always fail, so it falls through to `{:verified false}`. The skill doc should name this explicitly so agents know why they got a false result.
- Recommended agent response: Report that the agent identity could not be found in the registry. The agentId or registry address may be incorrect, or the agent may not be registered. Do not conflate with address mismatch — this is a registry lookup failure, not a signature failure.

**Error condition table (for skill doc):**

| Condition | What Happened | Recommended Response |
|-----------|---------------|---------------------|
| Network error | An RPC call failed before returning an address | Report: unable to complete verification due to network error. Do not report as unverified. |
| Address mismatch | All calls succeeded; recovered signer matches neither registry address | Report: NOT VERIFIED. The signature is not from an authorized key for this agent. |
| Unregistered agent | eth_call returned zero address or empty for getAgentWallet/ownerOf | Report: agent identity not found in registry. Verification cannot be completed. |

---

### TRUST-01: Trust Forwarding Guidance

Source: No prior decisions constrain the approach. This is Claude's discretion (LOW constraint, HIGH authoring latitude).

**What the requirement says:** One paragraph explaining how agents can relay verification results to downstream agents without prescribing a format.

**What it must NOT do:**
- Prescribe a specific data structure, field names, or schema
- Suggest a specific transport or protocol
- Add multiple paragraphs or a separate "why" rationale section (per terse spec voice convention)

**What it should accomplish:**
- Convey the conceptual principle: a verified result from a trusted verifier can be relayed as attestation
- Distinguish between the verification result (VERIFIED/NOT VERIFIED/ERROR) and the content itself
- Imply that the verifier agent's own identity is part of the trust chain (a relay is only as trustworthy as the relay agent)
- Not make promises about what downstream agents will accept

**Recommended approach:** One short paragraph, terse spec tone, conceptual only. Something like: An agent that has verified a signed block may communicate the result to downstream agents by stating the outcome (verified, not verified, or error), the verified identity (full ID), and the content hash. The verifying agent's own identity should accompany the attestation — downstream agents evaluate trust based on who is relaying the result, not the format of the relay.

---

## Document Context

Phase 3 writes into three `<!-- Phase 3 -->` placeholders in the current ERC8004-SKILL.md:

```
## Result

<!-- Phase 3 -->

## Error Conditions

<!-- Phase 3 -->

## Trust Forwarding

<!-- Phase 3 -->
```

The `## Result` section is the natural culmination of the Verification Procedure — the five steps (1-4 from Phase 2) produced three addresses; `## Result` defines what to do with them. Cross-references should point to "Step 2", "Step 3", "Step 4" by name (the heading format already established: `### Step N: ...`).

**Phase 3 must not re-explain the steps.** It receives the three addresses as given and states what to do with them.

---

## Architecture Patterns

### Section Sequencing

`## Result` → `## Error Conditions` → `## Trust Forwarding` is the correct order. Result defines the success path first; Error Conditions defines what to do when the success path cannot be reached; Trust Forwarding is a downstream concern that presupposes a result exists to relay.

### Style Decisions to Carry Forward

All prior phase conventions apply unchanged to Phase 3:

| Convention | Source | Application in Phase 3 |
|-----------|--------|------------------------|
| Terse spec tone — imperatives, minimal prose | 01-CONTEXT locked | All three sections: short, direct statements |
| Show-then-explain | 01-02 decision | Pseudocode block for comparison rule, then bullet explanation |
| Synthetic data disclaimer as blockquote | 01-02 decision | Any worked example with synthetic addresses |
| No separate "why" sections | 01-CONTEXT locked | Rationale is one-liner inline, never a separate heading |
| Cross-reference by section name | 02-01 decision | "from Step 2", "from Step 3", "from Step 4" |
| JSON body only for JSON-RPC examples | 02-01 decision | N/A for Phase 3 — no new JSON-RPC calls in this phase |

### Pseudocode Format for Comparison Rule

The comparison rule is the only procedural content in `## Result`. It should use the same pattern established for Phase 2: a short intro sentence, then a pseudocode block, then a bullet annotation list.

```
signer_lower  = <recovered address from Step 2>.toLowerCase()
wallet_lower  = <agent wallet from Step 3>.toLowerCase()
owner_lower   = <NFT owner from Step 4>.toLowerCase()

verified = (signer_lower == wallet_lower) OR (signer_lower == owner_lower)
```

- `signer_lower`: recovered signer from personal_ecRecover, lowercased
- `wallet_lower`: agent wallet from getAgentWallet, lowercased
- `owner_lower`: NFT owner from ownerOf, lowercased
- `verified`: true if either comparison matches — the OR is explicit

### Error Conditions Format

A named table (matching the document's established table style) is appropriate for three named conditions. Following the table with one sentence per condition's recommended response mirrors the style already used for the Fields table in Phase 1 (table + prose note) and the RPC endpoint table in Phase 2 (table + intentional-duplication note).

Alternative: three H3 subsections (one per condition). This is heavier but provides navigation anchors. Given that there are exactly three conditions and they are short, a table with inline guidance is preferred — consistent with the document's spare style.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead |
|---------|-------------|-------------|
| Address normalization logic | Custom bitwise comparison | State the lowercase rule explicitly and show it in pseudocode — agents implement it natively |
| Error classification | Multi-level error hierarchy | Exactly three named conditions, no more — this matches the implementation (chain.cljs has exactly three resolution paths) |
| Trust forwarding format | A prescribed JSON schema or field names | One conceptual paragraph — agents choose their own relay format |
| Verification result data structure | A required output format | State the outcome (VERIFIED/NOT VERIFIED/ERROR) as a concept, not a schema |

---

## Common Pitfalls

### Pitfall: Ambiguous comparison rule (Pitfall 4 from PITFALLS.md)

**What goes wrong:** Saying "compare the addresses" without specifying case normalization. `personal_ecRecover` returns checksummed (mixed-case) addresses; `eth_call` responses may be all-lowercase. Direct string comparison fails for identical addresses.

**Prevention:** State explicitly: "Lowercase both addresses before comparing." Show in pseudocode. Do not say "normalize" without showing what normalization means.

### Pitfall: Implied OR condition (Pitfall 10 from PITFALLS.md)

**What goes wrong:** Writing "verified if the signer matches the agent wallet" implies that matching the owner is insufficient, or that ownerOf is optional. The OR must be stated explicitly.

**Prevention:** Write the pass condition with the literal word "OR" (or `OR` in pseudocode). Do not write "compare against the agent wallet (and optionally the owner)" — the owner check is not optional for correct verification.

### Pitfall: Conflating error conditions

**What goes wrong:** Reporting "NOT VERIFIED" for all three error types causes an agent to treat a network outage the same as a bad signature. An agent that sees "NOT VERIFIED" assumes the content is tampered with, not that the RPC timed out.

**Prevention:** The three conditions MUST have distinct recommended responses. Only "address mismatch" should result in a NOT VERIFIED report. "Network error" and "unregistered agent" are separate failure modes that should not be reported as NOT VERIFIED.

### Pitfall: Trust forwarding paragraph becoming prescriptive

**What goes wrong:** Adding suggested field names ("include `verified: true` and `agentId` in your relay message") prescribes a format that constrains downstream agent implementations. TRUST-01 explicitly requires NOT prescribing a format.

**Prevention:** Write conceptually. Use "the outcome", "the verified identity", "the content hash" as concepts, not as suggested field names. No JSON or data structure examples in this section.

### Pitfall: Missing the boundary between Result and Error Conditions

**What goes wrong:** Putting error handling inside the `## Result` section (e.g., adding "if no match, see error handling below"). `## Result` should define the success path cleanly. `## Error Conditions` is a separate H2 section.

**Prevention:** `## Result` ends with the verified/not-verified outcome. The error table lives entirely in `## Error Conditions`. Cross-references go forward (not backward): `## Error Conditions` may reference result states, but `## Result` should not reference the error section.

---

## Code Examples

### Comparison Rule Pseudocode (for ## Result)

```
signer_lower  = <recovered address from Step 2>.toLowerCase()
wallet_lower  = <agent wallet from Step 3>.toLowerCase()
owner_lower   = <NFT owner from Step 4>.toLowerCase()

verified = (signer_lower == wallet_lower) OR (signer_lower == owner_lower)
```

Source: `src/verify/chain.cljs` lines 101-107 (HIGH confidence)

### Case Normalization Example (optional illustration)

If an example would help prevent the case-sensitivity pitfall, a minimal inline example could show two addresses that differ only in checksum case resolving to equal after lowercase. This is optional — the pseudocode may be sufficient. If used, apply synthetic data disclaimer.

```
0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045  (EIP-55 checksummed)
0xd8da6bf26964af9d7eed9e03e53415d37aa96045  (all lowercase)
→ identical after .toLowerCase()
```

> These values are synthetic. Do not use for on-chain verification.

---

## Plan Structure Recommendation

### One plan is correct for Phase 3

The ROADMAP already specifies: "03-01: Write address comparison rule (VFY-07), pass/fail definition (VFY-08), error conditions (VFY-10), and trust forwarding guidance (TRUST-01)."

All four requirements are closely coupled content that writes into a single section group (Result + Error Conditions + Trust Forwarding). They are short sections — `## Result` is ~10 lines, `## Error Conditions` is a table + one line per condition (~15 lines), `## Trust Forwarding` is one paragraph (~3-4 lines). One plan is appropriate.

**Plan 03-01 task:** Replace all three `<!-- Phase 3 -->` placeholders with the final content in a single atomic write to `public/ERC8004-SKILL.md`.

---

## What Phase 3 Must NOT Include

- No new JSON-RPC call specs (all calls are established in Phase 2)
- No repetition of Step 2, 3, or 4 content — reference by step name only
- No hash recomputation (established as out of scope from Phase 1)
- No trust scoring, reputation weights, or format prescription for TRUST-01
- No library-specific syntax (ethers, viem, web3.py) in any pseudocode
- No explanations of WHY the comparison is case-insensitive (one-liner inline at most)
- No mention of `tokenURI` or agent name resolution (out of scope)
- No response decoding explanation (Phase 2 established agents can read hex addresses)

---

## Open Questions

1. **Address mismatch: display the addresses for diagnosis?**
   - What we know: chain.cljs returns `:signer`, `:agent-wallet`, `:agent-owner` in the result. Showing the addresses helps debugging.
   - What's unclear: Whether the skill doc should instruct agents to display all three addresses when NOT VERIFIED, or just report the outcome.
   - Recommendation: State the outcome only. The skill doc is instruction for verification, not for diagnostic display. Agents can decide on their own to display addresses; the doc should not prescribe it. Keep NOT VERIFIED as a clean outcome statement.

2. **Should `## Result` name the step outcome or just give the pseudocode?**
   - What we know: The document established H3 headings per step in Phase 2. Phase 3's `## Result` is not a step — it's the synthesis.
   - What's unclear: Whether `## Result` should have any H3 subsections or just flat content.
   - Recommendation: No H3 subsections in `## Result`. It is a conclusion section, not a procedural step. Flat content: one sentence intro, pseudocode block, annotation bullets. `## Error Conditions` similarly gets no H3 subsections — use a table instead.

---

## Sources

### Primary (HIGH confidence)

- `/home/teresa/dev/erc8004-arbitrum/src/verify/chain.cljs` lines 78-125 — `verify-all` function: complete authoritative implementation of comparison rule, pass/fail definition, and error paths
- `/home/teresa/dev/erc8004-arbitrum/.planning/research/PITFALLS.md` — Pitfall 4 (case sensitivity), Pitfall 6 (happy-path-only failure), Pitfall 10 (OR condition omission)
- `/home/teresa/dev/erc8004-arbitrum/.planning/research/ARCHITECTURE.md` — Error case table (Step 5 section), address comparison pseudocode
- `/home/teresa/dev/erc8004-arbitrum/public/ERC8004-SKILL.md` — current document state; Phase 3 fills `## Result`, `## Error Conditions`, `## Trust Forwarding` placeholders

### Secondary (MEDIUM confidence)

- `/home/teresa/dev/erc8004-arbitrum/.planning/phases/01-skill-doc-foundation/01-CONTEXT.md` — locked decisions: terse spec tone, no separate "why" sections
- `/home/teresa/dev/erc8004-arbitrum/.planning/STATE.md` — accumulated decision log: all prior phase decisions that constrain Phase 3 style
- `/home/teresa/dev/erc8004-arbitrum/.planning/phases/02-verification-procedure/02-VERIFICATION.md` — confirms Phase 3 placeholders are present and correct; confirms Phase 2 is clean at its boundary

---

## Metadata

**Confidence breakdown:**
- VFY-07 (address comparison rule): HIGH — directly read from chain.cljs source
- VFY-08 (pass/fail definition): HIGH — directly read from chain.cljs source
- VFY-10 (error conditions): HIGH — derived from chain.cljs catch block + ARCHITECTURE.md error table + PITFALLS.md; three conditions are unambiguous
- TRUST-01 (trust forwarding): MEDIUM — no prior art in project files; authoring discretion with clear constraints from the requirement itself and terse spec tone convention

**Research date:** 2026-02-19
**Valid until:** Stable — comparison rule and error conditions are locked by chain.cljs behavior; trust forwarding is authoring judgment not tied to any volatile dependency
