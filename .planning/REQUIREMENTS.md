# Requirements: ERC-8004 Agent Skill Expansion

**Defined:** 2026-02-19
**Core Value:** AI agents can verify ERC-8004 signed messages using only web fetch

## v1 Requirements

### Skill Document Structure

- [x] **SKILL-01**: Skill doc opens with explicit capability declaration stating what it teaches, who it's for, and assumed tools (web fetch only)
- [x] **SKILL-02**: Consistent H2/H3 heading structure so agents can programmatically navigate sections
- [x] **SKILL-03**: Explicit scope statement — what the doc does NOT cover (keccak256, registry deployment, wallet management)

### Signed Block Format

- [x] **FMT-01**: Complete signed block format spec — exact grammar for separator, every field, required vs optional
- [x] **FMT-02**: Full ID format spec — `eip155:<chainId>:<registryAddress>:<agentId>` broken down field by field
- [x] **FMT-03**: Annotated real example — complete signed block with comments identifying each line's purpose

### Signing Instructions

- [x] **SIGN-01**: Signing instructions preserved from ai.html — tool name, input rules, output format, display rules

### Verification Procedure

- [x] **VFY-01**: Numbered step-by-step verification walkthrough (parse → ecRecover → eth_call → compare → report)
- [x] **VFY-02**: `personal_ecRecover` JSON-RPC call spec — exact request body, params array order, response shape
- [x] **VFY-03**: `eth_call` spec for `getAgentWallet(uint256)` — pre-computed function selector, ABI-encoded argument, response decoding
- [x] **VFY-04**: `eth_call` spec for `ownerOf(uint256)` — pre-computed function selector (`0x6352211e`), same encoding pattern
- [x] **VFY-05**: Pre-computed ABI call data examples — worked example with a real agentId showing full `data` field construction
- [x] **VFY-06**: JSON-RPC request/response pairs shown together for each call
- [x] **VFY-07**: Address comparison rule — case-insensitive hex comparison, explicit lowercase normalization
- [x] **VFY-08**: Pass/fail definition — verified if recovered signer matches either getAgentWallet OR ownerOf result
- [x] **VFY-09**: RPC endpoint list — Arbitrum mainnet and Sepolia with chain IDs
- [x] **VFY-10**: Three named error conditions with recommended agent response (network error, address mismatch, unregistered agent)

### Trust Forwarding

- [x] **TRUST-01**: Conceptual trust forwarding guidance — one paragraph explaining how agents can relay verification results without prescribing a format

### Build Pipeline

- [x] **BUILD-01**: Replace ai.html with ERC8004-SKILL.md in public/ directory
- [x] **BUILD-02**: Update dist script in package.json to copy ERC8004-SKILL.md instead of ai.html
- [x] **BUILD-03**: Add "For Agents" box to index.html with fetch URL for the skill doc
- [x] **BUILD-04**: Remove ai.html from the project

### Verification

- [x] **QA-01**: Compute correct `getAgentWallet(uint256)` function selector from canonical ABI (resolve research inconsistency)
- [x] **QA-02**: Verify `personal_ecRecover` parameter semantics match existing chain.cljs behavior

## v2 Requirements

### Extended Examples

- **EXT-01**: Worked example with real on-chain data (fully verified signed block as test case)
- **EXT-02**: Arbitrum Sepolia testnet verification example for agent developers

### Batch Verification

- **BATCH-01**: Guidance on verifying multiple signed blocks in sequence

### Agent Name Resolution

- **NAME-01**: tokenURI / agent name resolution via additional eth_call

## Out of Scope

| Feature | Reason |
|---------|--------|
| Keccak256 / content hash recomputation | Hash field is pre-computed in signed block; recomputing adds failure modes without benefit |
| EIP-191 prefix explanation | personal_ecRecover handles transparently; explaining causes double-prefix bugs |
| Library/SDK examples (ethers, viem, web3.py) | Target audience has web fetch only; library examples imply wrong runtime |
| Registry deployment instructions | Out of scope; agents verify, they don't deploy |
| Trust scoring / reputation weighting | Premature and domain-specific; agents decide their own trust models |
| Abstract security prose | Agents act on procedures, not generic caveats |
| HTTP API / server endpoint | Agents verify directly via RPC; no intermediary needed |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SKILL-01 | Phase 1 | Complete |
| SKILL-02 | Phase 1 | Complete |
| SKILL-03 | Phase 1 | Complete |
| FMT-01 | Phase 1 | Complete |
| FMT-02 | Phase 1 | Complete |
| FMT-03 | Phase 1 | Complete |
| SIGN-01 | Phase 1 | Complete |
| QA-01 | Phase 2 | Complete |
| QA-02 | Phase 2 | Complete |
| VFY-01 | Phase 2 | Complete |
| VFY-02 | Phase 2 | Complete |
| VFY-03 | Phase 2 | Complete |
| VFY-04 | Phase 2 | Complete |
| VFY-05 | Phase 2 | Complete |
| VFY-06 | Phase 2 | Complete |
| VFY-09 | Phase 2 | Complete |
| VFY-07 | Phase 3 | Complete |
| VFY-08 | Phase 3 | Complete |
| VFY-10 | Phase 3 | Complete |
| TRUST-01 | Phase 3 | Complete |
| BUILD-01 | Phase 4 | Complete |
| BUILD-02 | Phase 4 | Complete |
| BUILD-03 | Phase 4 | Complete |
| BUILD-04 | Phase 4 | Complete |

**Coverage:**
- v1 requirements: 24 total
- Mapped to phases: 24
- Unmapped: 0

---
*Requirements defined: 2026-02-19*
*Last updated: 2026-02-19 after roadmap creation*
