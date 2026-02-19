# ERC-8004 Verifier — Agent Skill Expansion

## What This Is

A browser-based ERC-8004 signature verifier for Arbitrum, expanding to include a machine-readable skill document (`ERC8004-SKILL.md`) that teaches AI agents both how to sign and verify content using on-chain identity. The verifier runs entirely client-side with no server.

## Core Value

AI agents can verify ERC-8004 signed messages from other agents using only web fetch — no libraries, no special tools — enabling decentralized trust chains between agents.

## Requirements

### Validated

- ✓ Browser-based signature verification (paste mode) — existing
- ✓ Verify link mode (URL hash with base64 proof) — existing
- ✓ On-chain ECDSA recovery against Arbitrum registry — existing
- ✓ Human-friendly UI with VERIFIED/FAILED result cards — existing
- ✓ Agent signing instructions page (ai.html) — existing
- ✓ Support for Arbitrum mainnet and Sepolia — existing

### Active

- [ ] Replace ai.html with ERC8004-SKILL.md — complete agent skill document covering signing and verification
- [ ] Verification instructions using raw JSON-RPC calls (web fetch only, no libraries needed)
- [ ] Pre-computed function selectors and ABI encoding examples for registry reads
- [ ] Trust forwarding guidance — conceptual, not prescriptive
- [ ] "For Agents" box on index.html pointing to fetch URL for the skill doc
- [ ] Update build pipeline to include ERC8004-SKILL.md instead of ai.html

### Out of Scope

- HTTP API / server-side verification endpoint — agents verify directly via RPC
- MCP tool for verification — agents follow the skill doc instructions instead
- Prescriptive trust-forwarding format — agents use their own signing capability
- Content hash recomputation in the skill doc — the hash field in signed blocks is sufficient

## Context

- The signed content format includes a `hash` field that is the final hash the signature covers. Agents don't need keccak256 to verify — they only need `personal_ecRecover(hash, signature)` via JSON-RPC.
- Public Arbitrum RPC at `https://arb1.arbitrum.io/rpc` supports `personal_ecRecover`.
- Registry contract reads use `getAgentWallet(uint256)` and `ownerOf(uint256)` via `eth_call`.
- The skill doc will be hosted at `http://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md`.
- Existing ai.html content for signing instructions should be preserved and incorporated.

## Constraints

- **Static hosting**: No server-side code — skill doc is a static .md file served alongside the verifier
- **Agent capability**: Assume agents can only web fetch (HTTP requests) — no crypto libraries, no web3 tooling
- **Backward compatibility**: Verify links and paste verification must continue working unchanged

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| .md instead of .html for agent doc | Agents parse markdown natively; cleaner for machine consumption | — Pending |
| ERC8004-SKILL.md filename | Communicates it's a skill document, not just info | — Pending |
| personal_ecRecover for signer recovery | Available on public RPC, avoids agents needing crypto primitives | — Pending |
| Conceptual trust forwarding (not prescriptive) | Agents have different signing capabilities; guidance > format | — Pending |

---
*Last updated: 2026-02-19 after initialization*
