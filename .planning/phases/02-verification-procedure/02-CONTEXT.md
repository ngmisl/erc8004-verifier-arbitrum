# Phase 2: Verification Procedure - Context

**Gathered:** 2026-02-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Write the step-by-step verification procedure section of ERC8004-SKILL.md — a numbered walkthrough that teaches AI agents to verify ERC-8004 signed messages using only JSON-RPC calls via web fetch. Covers: procedure steps, personal_ecRecover spec, eth_call specs for getAgentWallet and ownerOf, pre-computed selectors, ABI encoding, and RPC endpoint table. Does NOT cover pass/fail logic, error handling, or trust forwarding (Phase 3).

</domain>

<decisions>
## Implementation Decisions

### Procedure walkthrough structure
- Cross-reference Phase 1's format spec by name (e.g., "use the `hash:` field from Signed Block Format") rather than repeating field descriptions — assumes agents read top-down through the document

### JSON-RPC example style
- Show BOTH templates with placeholders AND worked examples with real on-chain data — template first for the pattern, then a concrete example that proves the procedure works
- Follow the existing show-then-explain annotation style from Phase 1 where it fits, but Claude has discretion on whether JSON-RPC examples benefit more from a different presentation

### ABI encoding presentation
- Show complete worked examples for BOTH getAgentWallet and ownerOf — do not DRY them into "same pattern, different selector." Each function gets its own full copy-pasteable example
- Assume agents know how to read a hex address from an eth_call response — do not explain response decoding

### Claude's Discretion
- Overall structure: H3-per-step vs. numbered list, step labeling style, inline error notes vs. pure happy path
- JSON-RPC: full HTTP request vs. JSON body only, paired vs. grouped responses
- ABI encoding: byte-level walkthrough vs. formula + example, whether to reference ABI standard or be self-contained
- Selector resolution: whether to show keccak256 computation in the doc, whether to cross-reference against viem and/or live on-chain call
- QA-02: whether to anchor personal_ecRecover spec to viem behavior or the Ethereum JSON-RPC standard independently

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-verification-procedure*
*Context gathered: 2026-02-19*
