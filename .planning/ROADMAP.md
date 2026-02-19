# Roadmap: ERC-8004 Agent Skill Expansion

## Overview

Replace `public/ai.html` with `ERC8004-SKILL.md` — a machine-readable markdown skill document that teaches AI agents to sign and verify ERC-8004 content using only web fetch. The work moves in strict dependency order: format spec first (every downstream section depends on it), then the JSON-RPC call specs, then the pass/fail logic, then wiring the document into the build pipeline and index.html for discovery.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Skill Doc Foundation** - Capability declaration, signed block format spec, identity format, and signing instructions (completed 2026-02-19)
- [x] **Phase 2: Verification Procedure** - Numbered walkthrough, JSON-RPC call specs, pre-computed selectors, RPC endpoints (completed 2026-02-19)
- [ ] **Phase 3: Result Logic and Error Handling** - Pass/fail rule, address normalization, error conditions, trust forwarding
- [ ] **Phase 4: Build Pipeline** - Wire ERC8004-SKILL.md into build, remove ai.html, add "For Agents" discovery box

## Phase Details

### Phase 1: Skill Doc Foundation
**Goal**: Agents can read a complete, machine-navigable document that declares exactly what it teaches, specifies every field of the signed block format, and preserves signing instructions from ai.html
**Depends on**: Nothing (first phase)
**Requirements**: SKILL-01, SKILL-02, SKILL-03, FMT-01, FMT-02, FMT-03, SIGN-01
**Success Criteria** (what must be TRUE):
  1. An agent reading the top of ERC8004-SKILL.md knows in one sentence what it teaches, who it is for, and that only web fetch is required
  2. An agent can programmatically locate any section of the document using the H2/H3 heading structure
  3. An agent knows what the document does NOT cover (keccak256, registry deployment, wallet management) without having to infer it
  4. An agent can parse any signed block by following the format spec — separator, every field, required vs optional, annotated real example
  5. An agent knows the full `eip155:<chainId>:<registryAddress>:<agentId>` identity format broken down field by field
**Plans**: 2 plans

Plans:
- [ ] 01-01-PLAN.md -- Create ERC8004-SKILL.md with document structure, capability declaration, scope statement, and heading skeleton
- [ ] 01-02-PLAN.md -- Write signed block format spec (FMT-01, FMT-02, FMT-03) and signing instructions section (SIGN-01)

### Phase 2: Verification Procedure
**Goal**: Agents have a complete, step-by-step numbered verification procedure with verbatim JSON-RPC request/response pairs, pre-computed function selectors, ABI encoding examples, and a confirmed-correct getAgentWallet selector
**Depends on**: Phase 1
**Requirements**: QA-01, QA-02, VFY-01, VFY-02, VFY-03, VFY-04, VFY-05, VFY-06, VFY-09
**Success Criteria** (what must be TRUE):
  1. An agent following the numbered procedure (parse → ecRecover → eth_call x 2 → compare → report) can complete each step using only the information provided in that step's section
  2. An agent can copy the `personal_ecRecover` request body verbatim, substitute the hash and sig fields, and receive a valid signer address response
  3. An agent can construct the complete `data` field for `getAgentWallet(uint256)` and `ownerOf(uint256)` calls using the pre-computed selectors and the ABI encoding pattern shown
  4. The `getAgentWallet(uint256)` function selector in the document is the cryptographically correct value (resolved from QA-01)
  5. The RPC endpoint table lists both Arbitrum mainnet (42161) and Sepolia (421614) with their correct chain IDs
**Plans**: 2 plans

Plans:
- [ ] 02-01-PLAN.md -- Write procedure intro, Step 1 (extract fields), and Step 2 (personal_ecRecover spec with template + worked example)
- [ ] 02-02-PLAN.md -- Write Step 3 (getAgentWallet eth_call), Step 4 (ownerOf eth_call), and RPC Endpoints table

### Phase 3: Result Logic and Error Handling
**Goal**: Agents have an unambiguous pass/fail rule with explicit address normalization, three named error conditions with recommended responses, and conceptual trust forwarding guidance
**Depends on**: Phase 2
**Requirements**: VFY-07, VFY-08, VFY-10, TRUST-01
**Success Criteria** (what must be TRUE):
  1. An agent knows to lowercase both addresses before comparing and can apply the rule without any ambiguity about case sensitivity
  2. An agent knows verification passes if the recovered signer matches EITHER getAgentWallet OR ownerOf (the OR condition is explicit, not implied)
  3. An agent encountering a network error, address mismatch, or unregistered agent knows the recommended response for each case without having to decide
  4. An agent understands conceptually how to relay a verification result to downstream agents without being constrained to a prescribed format
**Plans**: TBD

Plans:
- [ ] 03-01: Write address comparison rule (VFY-07), pass/fail definition (VFY-08), error conditions (VFY-10), and trust forwarding guidance (TRUST-01)

### Phase 4: Build Pipeline
**Goal**: ERC8004-SKILL.md is served at the canonical URL, discoverable from index.html, ai.html is removed, and the build pipeline copies the skill doc correctly
**Depends on**: Phase 3
**Requirements**: BUILD-01, BUILD-02, BUILD-03, BUILD-04
**Success Criteria** (what must be TRUE):
  1. Running `bun run build` copies ERC8004-SKILL.md to dist/ (not ai.html)
  2. The index.html "For Agents" box is visible and contains a working fetch URL for the skill doc
  3. ai.html no longer exists in the project (public/ and dist/)
  4. An agent fetching the skill doc URL receives the markdown content, not an HTML wrapper
**Plans**: TBD

Plans:
- [ ] 04-01: Update build pipeline (BUILD-01, BUILD-02), remove ai.html (BUILD-04), add "For Agents" box to index.html (BUILD-03)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Skill Doc Foundation | 2/2 | Complete   | 2026-02-19 |
| 2. Verification Procedure | 2/2 | Complete    | 2026-02-19 |
| 3. Result Logic and Error Handling | 0/1 | Not started | - |
| 4. Build Pipeline | 0/1 | Not started | - |
