# ERC8004-SKILL: Agent Signing and Verification

## Capability

This document teaches AI agents to sign and verify ERC-8004 content on Arbitrum using only web fetch (HTTP requests) — no cryptographic libraries required.

Audience: AI agents. Required tools: web fetch only.

## Scope

This document covers:
- Signed block format (field grammar, required vs optional)
- Agent identity format (eip155 full ID)
- Signing instructions (tool interface, output format, display rules)
- Verification procedure (JSON-RPC calls, address comparison)
- Error conditions and trust forwarding

This document does not cover:
- keccak256 or content hash computation
- ERC-8004 registry deployment
- Wallet creation or management
- Library/SDK usage (ethers, viem, web3.py)
- Trust scoring or reputation systems

## Signed Block Format

### Fields

| Field | Line Pattern | Required | Description |
|-------|-------------|----------|-------------|
| Separator | `---` (three or more dashes) | Yes | Anchors the signature footer; separates content from metadata |
| Agent line | `Signed by <name> (<fullId>)` | Yes | Agent display name and full identity string |
| `sig:` | `sig: 0x<hex>` | Yes | 65-byte ECDSA signature (130 hex characters) |
| `hash:` | `hash: 0x<hex>` | Yes | Pre-computed EIP-191 hash -- passed directly to ecRecover; do not recompute from content |
| `ts:` | `ts: <ISO 8601>` | No | Signing timestamp |
| `Verify:` | `Verify: <URL>` | No | Verification link; display only |

Parser behavior: The parser anchors on the `Signed by` line, then finds the nearest `---` before it (skipping blank lines). Content is everything before the separator. Footer fields may appear in any order, but convention is: agent line, sig, hash, ts, Verify.

### Example: Required Fields Only

```
Approved: invoice #4872 for $2,400. Payment authorized.

---
Signed by Keystone (eip155:42161:0x8004b3A873394d8B0Af8fD5D9C5D5a432:5)
sig: 0xf13bd8a9c7e4d2f0b1a5c8e3f7b2d9a6e4c1f8b0d3a2e5f7c9b4d6e8a1f3c2e5d7b9a0c3f6e8b1d4a2e5c7f9b0d3a6
hash: 0x57caa12b4f8c3d9e2a1b7f4c6e8d0a3b5f7c9e1d3a5b7c9e1f3a5b7c9e1f3a5
```

- Line 1: Content (the signed message)
- Blank line: Separates content from footer
- `---`: Separator
- `Signed by ...`: Agent name and full identity
- `sig: ...`: ECDSA signature
- `hash: ...`: EIP-191 content hash

> These values are synthetic. Do not use for on-chain verification.

### Example: All Fields

```
Approved: invoice #4872 for $2,400. Payment authorized.

---
Signed by Keystone (eip155:42161:0x8004b3A873394d8B0Af8fD5D9C5D5a432:5)
sig: 0xf13bd8a9c7e4d2f0b1a5c8e3f7b2d9a6e4c1f8b0d3a2e5f7c9b4d6e8a1f3c2e5d7b9a0c3f6e8b1d4a2e5c7f9b0d3a6
hash: 0x57caa12b4f8c3d9e2a1b7f4c6e8d0a3b5f7c9e1d3a5b7c9e1f3a5b7c9e1f3a5
ts: 2026-02-19T14:32:15.000Z
Verify: https://erc8004.orbiter.website/#eyJzIjoiMHhmMTNi...
```

Additional optional fields: `ts:` (signing timestamp) and `Verify:` (verification link). Both optional.

> These values are synthetic. Do not use for on-chain verification.

## Agent Identity

### Format

Pattern: `eip155:<chainId>:<registryAddress>:<agentId>`

Complete string: `eip155:42161:0x8004b3A873394d8B0Af8fD5D9C5D5a432:5`

### Field Breakdown

| Component | Value | Type | Description |
|-----------|-------|------|-------------|
| `eip155` | `eip155` | literal prefix | EIP-155 chain namespace identifier |
| `chainId` | `42161` | decimal integer | Arbitrum One chain ID |
| `registryAddress` | `0x8004b3A873394d8B0Af8fD5D9C5D5a432` | 0x-prefixed hex, 20 bytes | ERC-8004 registry contract address |
| `agentId` | `5` | decimal integer | NFT token ID in the registry |

### Supported Chains

| Chain ID | Network | RPC Endpoint |
|----------|---------|-------------|
| 42161 | Arbitrum One | `https://arb1.arbitrum.io/rpc` |
| 421614 | Arbitrum Sepolia | `https://sepolia-rollup.arbitrum.io/rpc` |

## Signing Instructions

### Tool Interface

- Tool name: `sign_content`
- Input: raw content text only. Strip all labels, commentary, formatting, and preamble. Pass only the message text.
- Output: copy-paste ready block containing the original content followed by the signature footer.

### Output Format

The tool returns a string in exactly this structure:

```
<content>

---
Signed by <AgentName> (<fullId>)
sig: 0x<signature>
hash: 0x<hash>
ts: <ISO 8601 timestamp>
Verify: <URL>
```

Output includes all fields (required and optional). The tool produces the complete block; the agent does not construct it manually.

### Display Rules

- Present the entire signed block as a single code block.
- No preamble, commentary, or explanation before or after the block.
- Do not modify, reformat, or truncate any part of the signature footer.
- The Verify link must remain intact and clickable.

## Verification Procedure

<!-- Phase 2 -->

## Result

<!-- Phase 3 -->

## Error Conditions

<!-- Phase 3 -->

## Trust Forwarding

<!-- Phase 3 -->
