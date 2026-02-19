# Phase 1: Skill Doc Foundation - Research

**Researched:** 2026-02-19
**Domain:** Machine-readable markdown skill document authoring for AI agents
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Document voice**
- Terse spec tone — RFC-like, minimal prose, no hand-holding
- Include brief one-liner rationale where a field's purpose is non-obvious (e.g., "hash: the EIP-191 hash — used as input to ecRecover, not recomputed by verifier")
- No separate "why" sections or conceptual introductions

**Example strategy**
- Use synthetic but realistic data — plausible-looking values, not tied to a specific production message
- Multiple focused examples, not one monolithic block: minimal required-only example first, then a second showing optional fields
- Identity format (eip155:chainId:registry:agentId) gets its own worked example — show a complete string, then break it apart field by field

**ai.html migration**
- Rewrite all content for the new terse spec voice and markdown structure — ai.html is reference material, not source text
- Preserve the existing field names (hash, sig, agent, etc.) for backwards compatibility with existing signed blocks
- Block structure/separators can evolve if a cleaner format emerges — not locked to the exact ai.html delimiters
- ERC8004-SKILL.md is the sole authority — no mention of ai.html, no "replaces" note

### Claude's Discretion
- Field documentation format (table vs. definition list)
- Scope exclusions format (bullet list vs. inline in header)
- Example annotation style (inline markers vs. show-then-explain)
- Exact heading hierarchy and section ordering

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SKILL-01 | Skill doc opens with explicit capability declaration stating what it teaches, who it's for, and assumed tools (web fetch only) | Capability declaration pattern established in prior research; confirmed agents parse markdown natively |
| SKILL-02 | Consistent H2/H3 heading structure so agents can programmatically navigate sections | H2/H3 heading discipline is the only structural tool needed; confirmed from agent skill doc design research |
| SKILL-03 | Explicit scope statement — what the doc does NOT cover (keccak256, registry deployment, wallet management) | Out-of-scope list is fully defined in REQUIREMENTS.md; scope statement belongs at the top, close to the capability declaration |
| FMT-01 | Complete signed block format spec — exact grammar for separator, every field, required vs optional | All fields, regexes, and required/optional status are canonical in `parse.cljs`; separator, fields, and ordering confirmed |
| FMT-02 | Full ID format spec — `eip155:<chainId>:<registryAddress>:<agentId>` broken down field by field | Full ID regex and field extraction confirmed in `parse.cljs`; chain IDs and RPC endpoints confirmed in `chain.cljs` |
| FMT-03 | Annotated real example — complete signed block with comments identifying each line's purpose | Example strategy is locked (synthetic+realistic, minimal then optional); annotation approach is Claude's discretion |
| SIGN-01 | Signing instructions preserved from ai.html — tool name, input rules, output format, display rules | All signing content is fully available in `ai.html`; must be rewritten in terse spec voice, not copied verbatim |
</phase_requirements>

---

## Summary

Phase 1 is a document-authoring phase, not a code-writing phase. The deliverable is ERC8004-SKILL.md — a markdown file that teaches AI agents to recognize signed blocks and sign content. All technical facts for this phase are already established by the existing codebase (`parse.cljs`, `chain.cljs`, `ai.html`). No new discoveries are needed; the research task is to extract authoritative values and make authoring decisions about structure.

The two work items are logically independent: Plan 01-01 creates the document skeleton (capability declaration, scope statement, heading structure), and Plan 01-02 fills in the format spec and signing instructions. These can be sequential. The document skeleton must exist before the format content can be placed.

**Primary recommendation:** Write the document top-to-bottom in logical reading order. The H2 structure defines the sections; the H3 structure defines subsections within them. Establish the headings first (Plan 01-01), then fill in content (Plan 01-02).

---

## Signed Block Format — Authoritative Specification

Source: `parse.cljs` (canonical parser, HIGH confidence)

### Fields

| Field | Line Pattern | Required | Notes |
|-------|-------------|----------|-------|
| Separator | `---` (3+ dashes, optional trailing whitespace) | Yes | Anchors the footer; parser uses `separator-re #"^-{3,}\s*$"` |
| Agent line | `Signed by <name> (<fullId>)` | Yes | Name is any non-paren text; fullId is the eip155 string |
| `sig:` | `sig: 0x<hex>` | Yes | 65-byte ECDSA signature = 130 hex chars; `sig-re #"^sig:\s*(0x[0-9a-fA-F]+)"` |
| `hash:` | `hash: 0x<hex>` | Yes | 32-byte keccak256 hash = 64 hex chars; `hash-re #"^hash:\s*(0x[0-9a-fA-F]+)"` |
| `ts:` | `ts: <ISO 8601>` | No | Timestamp; `ts-re #"^ts:\s*(\S+)"` |
| `Verify:` | `Verify: <URL>` | No | Verification link; not parsed by verifier, display only |

**Parser behavior:** The parser anchors on the `Signed by` line (first occurrence), then finds the nearest `---` before it, skipping blank lines. Content is everything before the separator, stripped of leading/trailing noise lines. The footer is the "Signed by" line plus everything after it. Field lines can appear in any order in the footer, but convention follows: agent line, sig, hash, ts, Verify.

**Required minimum for verification:** `---` separator + `Signed by` line (with valid fullId) + `sig:` + `hash:`. The `ts:` field is optional for parsing but included by all known signing implementations. Content may be empty (edge case), but in practice is always present.

### Full Block Structure

```
<content>

---
Signed by <AgentName> (<fullId>)
sig: 0x<130 hex chars>
hash: 0x<64 hex chars>
ts: <ISO 8601 timestamp>
Verify: <URL>
```

### Full ID Format

Source: `parse.cljs` `parse-full-id` and `full-id-re`, `chain.cljs` chains map

Pattern: `eip155:<chainId>:<registryAddress>:<agentId>`

| Component | Type | Example | Notes |
|-----------|------|---------|-------|
| `eip155` | literal prefix | `eip155` | Fixed; identifies EIP-155 namespace |
| `chainId` | decimal integer | `42161` | Arbitrum One; `421614` = Arbitrum Sepolia |
| `registryAddress` | `0x`-prefixed hex, 20 bytes | `0x8004...a432` | ERC-8004 registry contract on the specified chain |
| `agentId` | decimal integer | `5` | NFT token ID of the agent in the registry |

**Supported chain IDs:**

| chainId | Network | RPC |
|---------|---------|-----|
| 42161 | Arbitrum One | `https://arb1.arbitrum.io/rpc` |
| 421614 | Arbitrum Sepolia | `https://sepolia-rollup.arbitrum.io/rpc` |

### Verify URL Format

The `Verify:` URL is `https://erc8004.orbiter.website/#<base64>` where the fragment is base64-encoded JSON. JSON fields:

| Key | Value | Required |
|-----|-------|----------|
| `s` | signature (0x hex) | Yes |
| `h` | content hash (0x hex) | Yes |
| `a` | agent full ID string | Yes |
| `n` | agent name | No |
| `t` | ISO 8601 timestamp | No |
| `d` | subject — first line of content, max 80 chars (77 + "..." if truncated) | No |

Source: `core.cljs` `__copyVerifyLink` implementation (HIGH confidence).

---

## Signing Instructions — Content from ai.html

Source: `ai.html` (HIGH confidence — must be rewritten in terse spec voice)

### Tool interface (from ai.html)

- Tool name: `sign_content`
- Input: raw content text only — no labels, commentary, formatting, or preamble; strip everything that is not the message text
- Output: copy-paste ready block — content followed by the signature footer

### Display rules (from ai.html)

- Present the entire signed block as a single code block
- No preamble, commentary, or explanation before or after the block
- Do not modify, reformat, or truncate any part of the footer
- Verify link must remain intact and clickable

These rules apply to the agent that calls `sign_content` and displays the result. They should be stated as imperatives in the skill doc.

---

## Document Structure Recommendations

This is Claude's discretion per CONTEXT.md. Recommendations:

### Recommended H2 Heading Structure

```
# ERC8004-SKILL: Agent Signing and Verification

## Capability
## Scope
## Signed Block Format
## Agent Identity
## Signing Instructions
```

Rationale:
- `## Capability` — satisfies SKILL-01; one-sentence declaration + tool assumption
- `## Scope` — satisfies SKILL-03; explicit "does not cover" list
- `## Signed Block Format` — satisfies FMT-01 and FMT-03; format spec + examples
- `## Agent Identity` — satisfies FMT-02; full ID breakdown
- `## Signing Instructions` — satisfies SIGN-01; tool interface + display rules

H3 subsections within `## Signed Block Format`:
```
### Fields
### Full Block Example (Required Only)
### Full Block Example (With Optional Fields)
```

H3 subsections within `## Agent Identity`:
```
### Format
### Field Breakdown
### Supported Chains
```

H3 subsections within `## Signing Instructions`:
```
### Tool Interface
### Output Format
### Display Rules
```

This structure keeps all verification-related content (VFY-* requirements) out of Phase 1 — they belong in Phase 2's sections, which will be appended to the same document.

### Field Documentation Format

Recommendation: **tables** for structured data (fields with types, required flags, patterns), **definition-list style prose** for behavioral rules that have a single condition+consequence. Rationale: tables are unambiguous for agents to parse; inline definition lists work when the relationship is 1:1 rather than 1:N.

### Example Annotation Style

Recommendation: **show-then-explain** for the full block examples. Show the complete synthetic example first as a fenced code block, then list each line with a one-line description below. This keeps the example visually intact (agents can see the complete pattern) while the follow-up list explains each component. Inline comments (`# comment`) inside the example block would break copy-paste fidelity if an agent tries to use the example directly.

For the identity format worked example, the decision is locked: show complete string, then break apart field by field.

### Scope Exclusions Format

Recommendation: **bullet list** under a `## Scope` heading that begins with a positive statement ("This document covers:") followed by a negative statement ("This document does not cover:"). Two-list approach avoids ambiguity. Agents can quickly locate the negative list by heading.

---

## Common Pitfalls for Phase 1 Specifically

From prior research (PITFALLS.md), pitfalls relevant to Phase 1 content:

### Pitfall 5 — Hash field explained as "informational" in Phase 1

The `hash:` field appears in the signed block format spec. Phase 1 MUST include a one-liner clarifying that the hash is used directly in verification, not recomputed. This prevents agents reading Phase 1 only from attempting to compute the hash. Exact language: "hash: pre-computed EIP-191 hash — passed directly to ecRecover; do not recompute from content."

### Pitfall 8 — Registry address parsing must be explicit

The full ID breakdown in `## Agent Identity` is the primary prevention for an agent misconstruing where the registry address comes from. The field breakdown example with labeled components is required.

### Pitfall 9 — Capability declaration must precede all technical content

The `## Capability` section must come first and must explicitly state "web fetch only — no cryptographic libraries required." Agents that skip to format content may assume library availability. The capability statement guards against this.

### Pitfall 12 — Heading structure must be consistent

Every H2 must have a clear imperative title, not a descriptive one. "Signed Block Format" (noun phrase, navigable) is correct. "Understanding the Signed Block" (gerund, harder to locate programmatically) is wrong. Consistent naming allows agents to build a mental index of the document from headings alone.

---

## What Phase 1 Must NOT Include

Per the roadmap and phase boundary:

- No verification procedure (belongs to Phase 2)
- No JSON-RPC call specs (Phase 2)
- No function selectors or ABI encoding (Phase 2)
- No pass/fail rule or address comparison (Phase 3)
- No error conditions or error handling (Phase 3)
- No trust forwarding (Phase 3)
- No build pipeline changes (Phase 4)
- No mention of ai.html in the document itself (locked decision)
- No hash recomputation formula — if the hash construction is mentioned at all, it must be clearly marked as informational and separate from the verification procedure. For Phase 1 scope, the safer choice is to omit it entirely and leave it for Phase 2's context.

---

## Synthetic Example Values

For use in FMT-03 annotated examples (synthetic but realistic):

```
Content (minimal):
  "Approved: invoice #4872 for $2,400. Payment authorized."

AgentName: Keystone
fullId: eip155:42161:0x8004b3A873394d8B0Af8fD5D9C5D5a432:5
sig: 0xf13bd8a9c7e4d2f0b1a5c8e3f7b2d9a6e4c1f8b0d3a2e5f7c9b4d6e8a1f3c2e5d7b9a0c3f6e8b1d4a2e5c7f9b0d3a6
hash: 0x57caa12b4f8c3d9e2a1b7f4c6e8d0a3b5f7c9e1d3a5b7c9e1f3a5b7c9e1f3a5
ts: 2026-02-19T14:32:15.000Z
Verify: https://erc8004.orbiter.website/#eyJzIjoiMHhmMTNiZDhhOWM3ZTRkMmYwYjFhNWM4ZTNmN2IyZDlhNmU0YzFmOGIwZDNhMmU1ZjdjOWI0ZDZlOGExZjNjMmU1ZDdiOWEwYzNmNmU4YjFkNGEyZTVjN2Y5YjBkM2E2IiwiaCI6IjB4NTdjYWExMmI0ZjhjM2Q5ZTJhMWI3ZjRjNmU4ZDBhM2I1ZjdjOWUxZDNhNWI3YzllMWYzYTViN2M5ZTFmM2E1IiwiYSI6ImVpcDE1NTo0MjE2MToweDgwMDRiM0E4NzMzOTRkOEIwQWY4ZkQ1RDVDN0Q1YTQzMjo1IiwibiI6IktleXN0b25lIiwidCI6IjIwMjYtMDItMTlUMTQ6MzI6MTUuMDAwWiJ9
```

Note: The sig and hash values above are synthetic placeholders of approximately correct length. Do not use these as real verification test cases. They will not pass on-chain verification.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead |
|---------|-------------|-------------|
| Signed block format spec | Custom grammar notation | Use regex patterns already in `parse.cljs` as the authoritative spec |
| Field names | New names or aliases | Existing field names from `ai.html`: `sig`, `hash`, `ts`, `Verify` — locked for backwards compatibility |
| Full ID format | New format | Existing `eip155:chainId:registry:agentId` — already in use in all signed blocks |

---

## Open Questions

1. **Verify URL base64 encoding — standard or URL-safe?**
   - What we know: `core.cljs` uses `js/btoa` for encoding and `js/atob` for decoding with a custom UTF-8 wrapper
   - What's unclear: Whether the skill doc should specify "base64" or "URL-safe base64" — `btoa` produces standard base64 which may contain `+`, `/`, and `=` characters
   - Recommendation: For Phase 1, the `Verify:` URL is a display field only (not parsed by verifier); the signing instructions just say to include the URL as provided by the tool. Exact encoding details matter for Phase 2+ if verification via URL is documented. Low impact for Phase 1; note it for Phase 2 research.

2. **Should Phase 1 include the hash construction formula as informational context?**
   - What we know: ai.html includes `keccak256(abi.encode(keccak256(content), timestamp))`; PITFALLS.md recommends omitting or clearly segregating it
   - What's unclear: Whether an agent reading only the Phase 1 sections (format + signing) would benefit from or be confused by seeing the formula
   - Recommendation: Omit entirely from Phase 1. The formula is relevant only to signing tool implementors, not to agents using or verifying the tool. Phase 1 scope is "how to recognize and produce signed blocks," not "how the hash is computed."

---

## Sources

### Primary (HIGH confidence)
- `/home/teresa/dev/erc8004-arbitrum/src/verify/parse.cljs` — canonical signed block format; all field regexes, separator logic, required/optional status, full ID parsing
- `/home/teresa/dev/erc8004-arbitrum/src/verify/chain.cljs` — supported chain IDs, RPC endpoints, supported functions, verification logic
- `/home/teresa/dev/erc8004-arbitrum/src/verify/core.cljs` — Verify URL construction, base64 encoding, subject truncation rule (80 chars, 77 + "...")
- `/home/teresa/dev/erc8004-arbitrum/public/ai.html` — signing instructions content: tool name, input rules, output format, display rules
- `/home/teresa/dev/erc8004-arbitrum/.planning/research/SUMMARY.md` — prior project research summary
- `/home/teresa/dev/erc8004-arbitrum/.planning/research/PITFALLS.md` — 12 documented pitfalls for skill doc authoring

### Secondary (MEDIUM confidence)
- `/home/teresa/dev/erc8004-arbitrum/.planning/REQUIREMENTS.md` — requirement definitions, out-of-scope table
- `/home/teresa/dev/erc8004-arbitrum/.planning/ROADMAP.md` — phase boundaries and dependency rationale

---

## Metadata

**Confidence breakdown:**
- Signed block format spec: HIGH — derived directly from `parse.cljs` regex definitions; these are the authoritative parsing rules
- Full ID format: HIGH — derived from `parse.cljs` `full-id-re` and `chain.cljs` chain map; both sources agree
- Signing instructions content: HIGH — directly available in `ai.html`; only voice/structure transformation required
- Document structure recommendations: MEDIUM — these are authoring judgment calls, not technical facts; multiple valid approaches exist
- Synthetic example values: MEDIUM — length and format are correct; specific hex values are placeholders

**Research date:** 2026-02-19
**Valid until:** Stable — format and signing instructions are locked by backwards compatibility; document structure is a one-time authoring decision
