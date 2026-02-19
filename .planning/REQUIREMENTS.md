# Requirements: ERC-8004 Agent Skill Expansion

**Defined:** 2026-02-19
**Core Value:** AI agents can verify ERC-8004 signed messages using only web fetch

## v1 Requirements

### Skill Document Structure

- [ ] **SKILL-01**: Skill doc opens with explicit capability declaration stating what it teaches, who it's for, and assumed tools (web fetch only)
- [ ] **SKILL-02**: Consistent H2/H3 heading structure so agents can programmatically navigate sections
- [ ] **SKILL-03**: Explicit scope statement — what the doc does NOT cover (keccak256, registry deployment, wallet management)

### Signed Block Format

- [ ] **FMT-01**: Complete signed block format spec — exact grammar for separator, every field, required vs optional
- [ ] **FMT-02**: Full ID format spec — `eip155:<chainId>:<registryAddress>:<agentId>` broken down field by field
- [ ] **FMT-03**: Annotated real example — complete signed block with comments identifying each line's purpose

### Signing Instructions

- [ ] **SIGN-01**: Signing instructions preserved from ai.html — tool name, input rules, output format, display rules

### Verification Procedure

- [ ] **VFY-01**: Numbered step-by-step verification walkthrough (parse → ecRecover → eth_call → compare → report)
- [ ] **VFY-02**: `personal_ecRecover` JSON-RPC call spec — exact request body, params array order, response shape
- [ ] **VFY-03**: `eth_call` spec for `getAgentWallet(uint256)` — pre-computed function selector, ABI-encoded argument, response decoding
- [ ] **VFY-04**: `eth_call` spec for `ownerOf(uint256)` — pre-computed function selector (`0x6352211e`), same encoding pattern
- [ ] **VFY-05**: Pre-computed ABI call data examples — worked example with a real agentId showing full `data` field construction
- [ ] **VFY-06**: JSON-RPC request/response pairs shown together for each call
- [ ] **VFY-07**: Address comparison rule — case-insensitive hex comparison, explicit lowercase normalization
- [ ] **VFY-08**: Pass/fail definition — verified if recovered signer matches either getAgentWallet OR ownerOf result
- [ ] **VFY-09**: RPC endpoint list — Arbitrum mainnet and Sepolia with chain IDs
- [ ] **VFY-10**: Three named error conditions with recommended agent response (network error, address mismatch, unregistered agent)

### Trust Forwarding

- [ ] **TRUST-01**: Conceptual trust forwarding guidance — one paragraph explaining how agents can relay verification results without prescribing a format

### Build Pipeline

- [ ] **BUILD-01**: Replace ai.html with ERC8004-SKILL.md in public/ directory
- [ ] **BUILD-02**: Update dist script in package.json to copy ERC8004-SKILL.md instead of ai.html
- [ ] **BUILD-03**: Add "For Agents" box to index.html with fetch URL for the skill doc
- [ ] **BUILD-04**: Remove ai.html from the project

### Verification

- [ ] **QA-01**: Compute correct `getAgentWallet(uint256)` function selector from canonical ABI (resolve research inconsistency)
- [ ] **QA-02**: Verify `personal_ecRecover` parameter semantics match existing chain.cljs behavior

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
| SKILL-01 | — | Pending |
| SKILL-02 | — | Pending |
| SKILL-03 | — | Pending |
| FMT-01 | — | Pending |
| FMT-02 | — | Pending |
| FMT-03 | — | Pending |
| SIGN-01 | — | Pending |
| VFY-01 | — | Pending |
| VFY-02 | — | Pending |
| VFY-03 | — | Pending |
| VFY-04 | — | Pending |
| VFY-05 | — | Pending |
| VFY-06 | — | Pending |
| VFY-07 | — | Pending |
| VFY-08 | — | Pending |
| VFY-09 | — | Pending |
| VFY-10 | — | Pending |
| TRUST-01 | — | Pending |
| BUILD-01 | — | Pending |
| BUILD-02 | — | Pending |
| BUILD-03 | — | Pending |
| BUILD-04 | — | Pending |
| QA-01 | — | Pending |
| QA-02 | — | Pending |

**Coverage:**
- v1 requirements: 24 total
- Mapped to phases: 0
- Unmapped: 24

---
*Requirements defined: 2026-02-19*
*Last updated: 2026-02-19 after initial definition*
