# Phase 1: Skill Doc Foundation - Context

**Gathered:** 2026-02-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Create ERC8004-SKILL.md — a machine-readable markdown skill document that declares what it teaches, specifies every field of the signed block format, defines the agent identity format, and provides signing instructions. Verification procedure, error handling, and build pipeline are separate phases.

</domain>

<decisions>
## Implementation Decisions

### Document voice
- Terse spec tone — RFC-like, minimal prose, no hand-holding
- Include brief one-liner rationale where a field's purpose is non-obvious (e.g., "hash: the EIP-191 hash — used as input to ecRecover, not recomputed by verifier")
- No separate "why" sections or conceptual introductions

### Example strategy
- Use synthetic but realistic data — plausible-looking values, not tied to a specific production message
- Multiple focused examples, not one monolithic block: minimal required-only example first, then a second showing optional fields
- Identity format (eip155:chainId:registry:agentId) gets its own worked example — show a complete string, then break it apart field by field

### ai.html migration
- Rewrite all content for the new terse spec voice and markdown structure — ai.html is reference material, not source text
- Preserve the existing field names (hash, sig, agent, etc.) for backwards compatibility with existing signed blocks
- Block structure/separators can evolve if a cleaner format emerges — not locked to the exact ai.html delimiters
- ERC8004-SKILL.md is the sole authority — no mention of ai.html, no "replaces" note

### Claude's Discretion
- Field documentation format (table vs. definition list)
- Scope exclusions format (bullet list vs. inline in header)
- Example annotation style (inline markers vs. show-then-explain)
- Exact heading hierarchy and section ordering

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

*Phase: 01-skill-doc-foundation*
*Context gathered: 2026-02-19*
