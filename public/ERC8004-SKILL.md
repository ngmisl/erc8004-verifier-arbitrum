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

The following steps produce three addresses: a recovered signer address from the signature, an agent wallet address from the registry, and an NFT owner address from the registry. All calls are JSON-RPC over HTTP POST to an Arbitrum RPC endpoint with `Content-Type: application/json`. Steps 2, 3, and 4 are independent and may run concurrently.

### Step 1: Extract Fields

From the signed block (see Signed Block Format above), extract the `hash:` field value and the `sig:` field value. From the agent identity string in the `Signed by` line (see Agent Identity above), extract the `chainId`, `registryAddress`, and `agentId` components.

You now have five values: `hash`, `sig`, `chainId`, `registryAddress`, `agentId`.

### Step 2: Recover Signer (personal_ecRecover)

Call `personal_ecRecover` to recover the address that produced the signature. Pass the `hash:` field value directly as `params[0]`. Pass the `sig:` field value directly as `params[1]`. The RPC node applies the EIP-191 prefix internally.

**Template:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "personal_ecRecover",
  "params": [
    "<hash: field value>",
    "<sig: field value>"
  ]
}
```

**Response template:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "<recovered signer address>"
}
```

**Worked example** (using the synthetic values from the Example: Required Fields Only above):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "personal_ecRecover",
  "params": [
    "0x57caa12b4f8c3d9e2a1b7f4c6e8d0a3b5f7c9e1d3a5b7c9e1f3a5b7c9e1f3a5",
    "0xf13bd8a9c7e4d2f0b1a5c8e3f7b2d9a6e4c1f8b0d3a2e5f7c9b4d6e8a1f3c2e5d7b9a0c3f6e8b1d4a2e5c7f9b0d3a6"
  ]
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
}
```

> These values are synthetic. Do not use for on-chain verification.

The `result` field is the recovered signer address. Save this for comparison in the Result step.

**ABI encoding:** Call data = `0x` + function selector (8 hex chars) + agentId zero-padded to 64 hex chars. Convert the `agentId` from Step 1 (decimal integer) to hex, then left-pad with zeros to 64 characters.

### Step 3: Read Agent Wallet (eth_call)

Call `eth_call` to read the agent's wallet address from the registry contract. The `getAgentWallet(uint256)` selector is `0x00339509`.

**Template:**

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "eth_call",
  "params": [
    {
      "to": "<registryAddress from Step 1>",
      "data": "0x00339509<agentId zero-padded to 64 hex chars>"
    },
    "latest"
  ]
}
```

**Worked example** (agentId=5, registry from Step 1):

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "eth_call",
  "params": [
    {
      "to": "0x8004b3A873394d8B0Af8fD5D9C5D5a432",
      "data": "0x003395090000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ]
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": "0x000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"
}
```

> These values are synthetic. Do not use for on-chain verification.

The `result` field contains the agent wallet address. Save this for comparison in the Result step.

### Step 4: Read NFT Owner (eth_call)

Call `eth_call` to read the NFT owner address from the registry contract. The `ownerOf(uint256)` selector is `0x6352211e`.

**Template:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "eth_call",
  "params": [
    {
      "to": "<registryAddress from Step 1>",
      "data": "0x6352211e<agentId zero-padded to 64 hex chars>"
    },
    "latest"
  ]
}
```

**Worked example** (agentId=5, registry from Step 1):

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "eth_call",
  "params": [
    {
      "to": "0x8004b3A873394d8B0Af8fD5D9C5D5a432",
      "data": "0x6352211e0000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ]
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": "0x000000000000000000000000e41d2489571d322189246dafa5ebde1f4699f498"
}
```

> These values are synthetic. Do not use for on-chain verification.

The `result` field contains the NFT owner address. Save this for comparison in the Result step.

### RPC Endpoints

Select the RPC endpoint based on the `chainId` from Step 1.

| Chain ID | Network | RPC Endpoint |
|----------|---------|-------------|
| 42161 | Arbitrum One | `https://arb1.arbitrum.io/rpc` |
| 421614 | Arbitrum Sepolia | `https://sepolia-rollup.arbitrum.io/rpc` |

> This table duplicates the Supported Chains table in Agent Identity — that is intentional. Agents reading this procedure should not need to scroll back.

## Result

Lowercase all three addresses, then compare.

```
signer_lower  = <recovered address from Step 2>.toLowerCase()
wallet_lower  = <agent wallet from Step 3>.toLowerCase()
owner_lower   = <NFT owner from Step 4>.toLowerCase()

verified = (signer_lower == wallet_lower) OR (signer_lower == owner_lower)
```

- `signer_lower`: recovered signer from `personal_ecRecover`, lowercased
- `wallet_lower`: agent wallet from `getAgentWallet`, lowercased
- `owner_lower`: NFT owner from `ownerOf`, lowercased
- `verified`: true if either comparison matches — both addresses are authorized signers for the agent

`personal_ecRecover` returns EIP-55 checksummed (mixed-case) addresses; `eth_call` results are lowercase hex. Lowercasing both sides ensures consistent comparison.

## Error Conditions

Three conditions require distinct handling.

| Condition | What Happened | Recommended Response |
|-----------|---------------|---------------------|
| Network error | An RPC call failed before returning an address | Report: unable to complete verification due to network error. Do not report as unverified. |
| Address mismatch | All calls succeeded; recovered signer matches neither registry address | Report: NOT VERIFIED. The signature is not from an authorized key for this agent. |
| Unregistered agent | `eth_call` returned zero address or empty for `getAgentWallet` or `ownerOf` | Report: agent identity not found in registry. Verification cannot be completed. |

## Trust Forwarding

An agent that has verified a signed block may communicate the result to downstream agents by stating the outcome (verified, not verified, or error), the verified identity (full ID), and the content hash. The verifying agent's own identity should accompany the attestation — downstream agents evaluate trust based on who is relaying the result, not the format of the relay.
