# Architecture: AI Agent Verification via JSON-RPC

**Research Date:** 2026-02-19
**Milestone:** Subsequent — adding verification capability to ERC8004-SKILL.md
**Dimension:** Architecture — agent-driven JSON-RPC verification flow

---

## Overview

This document describes the machine-readable architecture for AI agents performing ERC-8004 signature verification using only raw HTTP requests. No libraries or crypto primitives are required from the agent's side — ECDSA recovery is delegated to the Arbitrum RPC node via `personal_ecRecover`, and on-chain registry reads use `eth_call` with pre-computed ABI-encoded call data.

**Key insight:** The signed block already contains the final `hash` value. Agents never need to recompute the hash themselves. Verification reduces to two RPC calls and one address comparison.

---

## System Boundary

```
+------------------+        HTTPS POST (JSON-RPC)        +---------------------------+
|                  |  ─────────────────────────────────>  |                           |
|   AI Agent       |  personal_ecRecover(hash, sig)       |  Arbitrum Public RPC      |
|                  |  eth_call → getAgentWallet(agentId)  |  arb1.arbitrum.io/rpc     |
|  (any runtime,   |  <─────────────────────────────────  |                           |
|   web fetch       |  address responses                   |  Chain ID: 42161          |
|   only)          |                                       |  (no auth required)       |
+------------------+                                       +---------------------------+
        |
        | compare addresses
        v
  VERIFIED or FAILED
```

**Component boundaries:**
- The agent owns: parsing the signed block, making HTTP requests, comparing addresses
- The RPC node owns: ECDSA recovery, contract state reads
- The registry contract owns: the authoritative mapping of agentId → wallet/owner

---

## Verification Flow (Ordered Steps)

### Step 1: Parse the Signed Block

Extract these fields from the signed content block:

| Field | Source in block | Format |
|-------|----------------|--------|
| `hash` | `hash: 0x...` line | 32-byte hex string, `0x`-prefixed |
| `signature` | `sig: 0x...` line | 65-byte hex string, `0x`-prefixed (r+s+v) |
| `chainId` | `eip155:<chainId>:...` | Integer |
| `registryAddress` | `eip155:..:<registry>:..` | 20-byte hex, `0x`-prefixed, checksummed or lowercase |
| `agentId` | `eip155:..:..<agentId>` | Non-negative integer |

The full agent identity field format is:

```
eip155:<chainId>:<registryAddress>:<agentId>
```

Example: `eip155:42161:0x80044b2d22e5b8e5C4f5c60FB08b069df1a6A132:5`

**Build order implication:** The skill doc must present the signed block format before the verification steps. The parser must understand the `---` separator structure (content above, footer below with `Signed by`, `sig:`, `hash:`, `ts:` lines).

---

### Step 2: Recover Signer Address via personal_ecRecover

**Method:** `personal_ecRecover`

This is a non-standard but widely supported Ethereum JSON-RPC method. The Arbitrum public RPC at `https://arb1.arbitrum.io/rpc` supports it. It recovers the address that produced an EIP-191 personal_sign signature over the given data.

**Important:** The `hash` field in the signed block is already the final signed hash value. Pass it as `data` in raw hex form. The RPC node applies EIP-191 prefix internally during recovery.

**Request format:**

```json
{
  "jsonrpc": "2.0",
  "method": "personal_ecRecover",
  "params": [
    "0x<hash-hex-32-bytes>",
    "0x<signature-hex-65-bytes>"
  ],
  "id": 1
}
```

**Concrete example:**

```json
{
  "jsonrpc": "2.0",
  "method": "personal_ecRecover",
  "params": [
    "0x57caa12b3e6a43c5f3ef0d7e1f9dbf19a9b2c03e5d6f7a8b9c0d1e2f3a4b5c6d",
    "0xf13bd8a9c24e5b6d7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d01b"
  ],
  "id": 1
}
```

**Response format:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "0x<recovered-address-20-bytes>"
}
```

**Concrete example response:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
}
```

The `result` is a checksummed Ethereum address. Lowercase both addresses before comparing.

**Error case — invalid signature:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32000,
    "message": "invalid signature"
  }
}
```

**HTTP transport:**

```
POST https://arb1.arbitrum.io/rpc
Content-Type: application/json
```

No authentication headers required.

---

### Step 3: Read Agent Wallet via eth_call → getAgentWallet

**Method:** `eth_call`

Calls `getAgentWallet(uint256 agentId)` on the registry contract. This is a pure view function — no transaction, no gas cost, no state change.

#### Function Selector Computation

The function selector is the first 4 bytes of `keccak256("getAgentWallet(uint256)")`.

```
keccak256("getAgentWallet(uint256)") = 0x3cef5e0f...
```

Pre-computed selector: `0x3cef5e0f`

> Note for skill doc: This selector value must be verified against the actual deployed contract. The computation is: take the canonical signature `getAgentWallet(uint256)` (no spaces, no argument names), compute keccak256, take first 4 bytes as big-endian hex.

#### ABI Encoding for uint256 Argument

ABI encoding pads the agentId to 32 bytes (256 bits), big-endian, zero-padded on the left.

For agentId `5`:
```
0x0000000000000000000000000000000000000000000000000000000000000005
```

For agentId `127`:
```
0x000000000000000000000000000000000000000000000000000000000000007f
```

General rule: Convert the integer to hex, pad to 64 hex characters (32 bytes) with leading zeros.

#### Complete call data

```
call data = selector (4 bytes) + abi-encoded agentId (32 bytes)
          = 0x3cef5e0f + 0000000000000000000000000000000000000000000000000000000000000005
          = 0x3cef5e0f0000000000000000000000000000000000000000000000000000000000000005
```

#### Request format

```json
{
  "jsonrpc": "2.0",
  "method": "eth_call",
  "params": [
    {
      "to": "0x<registry-address>",
      "data": "0x3cef5e0f0000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ],
  "id": 2
}
```

**Concrete example** (agentId 5, registry `0x80044b2d22e5b8e5C4f5c60FB08b069df1a6A132`):

```json
{
  "jsonrpc": "2.0",
  "method": "eth_call",
  "params": [
    {
      "to": "0x80044b2d22e5b8e5C4f5c60FB08b069df1a6A132",
      "data": "0x3cef5e0f0000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ],
  "id": 2
}
```

#### Response format

The result is a 32-byte ABI-encoded `address`. Addresses in ABI encoding are padded to 32 bytes with 12 leading zero bytes.

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": "0x000000000000000000000000d8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
}
```

#### Decoding the response

Strip the leading 24 hex characters (12 zero bytes) to get the 20-byte address:

```
raw:     000000000000000000000000d8dA6BF26964aF9D7eEd9e03E53415D37aA96045
decoded: 0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045
```

Practical shortcut: take the last 40 hex characters of the result string (after the `0x`), prepend `0x`.

---

### Step 4: (Optional) Read Agent Owner via eth_call → ownerOf

`ownerOf(uint256 tokenId)` is the standard ERC-721 function. It returns the NFT owner, which is an alternative verification path. Some agents may sign with the owner key rather than the configured wallet.

#### Function Selector

Pre-computed selector for `ownerOf(uint256)`: `0x6352211e`

> Computation: keccak256("ownerOf(uint256)") → take first 4 bytes

#### Request format

```json
{
  "jsonrpc": "2.0",
  "method": "eth_call",
  "params": [
    {
      "to": "0x<registry-address>",
      "data": "0x6352211e0000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ],
  "id": 3
}
```

Response decoding is identical to `getAgentWallet`: take the last 40 hex characters.

---

### Step 5: Compare Addresses

Lowercase all addresses before comparing. Both recovered signer and on-chain addresses may be in any case.

```
signer_lower   = recovered address from Step 2, lowercased
wallet_lower   = decoded address from Step 3, lowercased
owner_lower    = decoded address from Step 4, lowercased (if retrieved)

verified = (signer_lower == wallet_lower) OR (signer_lower == owner_lower)
```

**Outcome:** VERIFIED if either comparison is true. FAILED otherwise.

---

## Data Flow Direction

```
Signed Block Text
        |
        v (parse: regex extraction)
   hash, signature, chainId, registry, agentId
        |
        +──────────────────────────────────────────+
        |                                          |
        v (HTTP POST → personal_ecRecover)         v (HTTP POST → eth_call)
  recovered signer address               agent wallet address
                                    (+ optionally: owner address)
        |                                          |
        +──────────────────────────────────────────+
                          |
                          v (case-insensitive string compare)
                    VERIFIED / FAILED
```

Data flows strictly forward. There is no feedback loop. Each step depends only on the parsed fields from Step 1 and the RPC responses. Steps 2 and 3 can be executed in parallel (same inputs, independent outputs).

---

## RPC Endpoint Behavior

### Arbitrum Mainnet

- Endpoint: `https://arb1.arbitrum.io/rpc`
- Chain ID: `42161` (hex: `0xa4b1`)
- Operator: Offchain Labs
- Auth: None required
- Rate limits: Unofficial; aggressive polling may be throttled. One-off verification calls are well within limits.
- `personal_ecRecover`: Supported
- `eth_call`: Supported, `"latest"` block tag is valid

### Arbitrum Sepolia (Testnet)

- Endpoint: `https://sepolia-rollup.arbitrum.io/rpc`
- Chain ID: `421614` (hex: `0x66eee`)
- Auth: None required
- Same method support as mainnet

### Error Cases from the RPC Layer

| Condition | Error signal | Agent action |
|-----------|-------------|--------------|
| Invalid hex in params | JSON-RPC error `-32602` (invalid params) | Abort; signed block is malformed |
| Signature not 65 bytes | JSON-RPC error `-32000` (invalid signature) | Report: signature format invalid |
| Contract not deployed at address | `eth_call` returns `0x` empty result | Report: registry not found |
| Agent ID does not exist (ownerOf reverts) | `eth_call` returns error or empty | Treat as not verified |
| Network unreachable | HTTP error (timeout, 5xx) | Retry once; report RPC unavailable |

---

## ABI Encoding Reference

### Encoding uint256

For any unsigned integer value `n` (agentId):

1. Convert `n` to hexadecimal: e.g., `5` → `5`, `42` → `2a`, `1000` → `3e8`
2. Pad to 64 hex characters with leading zeros
3. The result is the 32-byte ABI word

| agentId | ABI encoded |
|---------|-------------|
| 1 | `0000000000000000000000000000000000000000000000000000000000000001` |
| 5 | `0000000000000000000000000000000000000000000000000000000000000005` |
| 100 | `0000000000000000000000000000000000000000000000000000000000000064` |
| 1337 | `0000000000000000000000000000000000000000000000000000000000000539` |

### Decoding address Return Value

ABI-encoded address response is always 32 bytes (64 hex chars after `0x`):

```
0x000000000000000000000000<20-byte-address>
```

To decode:
- Take the full `result` string
- The last 40 hex characters (positions 26–65 after `0x`, i.e., after 24 zero chars) are the address
- Prepend `0x`

### Function Selectors

| Function | Canonical signature | Selector |
|----------|-------------------|----------|
| `getAgentWallet` | `getAgentWallet(uint256)` | `0x3cef5e0f` |
| `ownerOf` | `ownerOf(uint256)` | `0x6352211e` |
| `tokenURI` | `tokenURI(uint256)` | `0xc87b56dd` |

> The skill doc should include these pre-computed selectors with a note that they are derived from `keccak256(canonicalSignature)[0:4]`. Agents that cannot compute keccak256 use the pre-computed values directly.

---

## Complete Request/Response Examples (Mainnet, agentId 5)

### personal_ecRecover

**Request:**
```json
POST https://arb1.arbitrum.io/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "personal_ecRecover",
  "params": [
    "0x57caa12b3e6a43c5f3ef0d7e1f9dbf19a9b2c03e5d6f7a8b9c0d1e2f3a4b5c6d",
    "0xf13bd8a9c24e5b6d7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d01b"
  ],
  "id": 1
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

### eth_call → getAgentWallet(5) on registry 0x80044b...

**Request:**
```json
POST https://arb1.arbitrum.io/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "eth_call",
  "params": [
    {
      "to": "0x80044b2d22e5b8e5C4f5c60FB08b069df1a6A132",
      "data": "0x3cef5e0f0000000000000000000000000000000000000000000000000000000000000005"
    },
    "latest"
  ],
  "id": 2
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

**Decoded wallet address:** `0xd8da6bf26964af9d7eed9e03e53415d37aa96045`

### Comparison

```
signer (lowercased): 0xd8da6bf26964af9d7eed9e03e53415d37aa96045
wallet (lowercased): 0xd8da6bf26964af9d7eed9e03e53415d37aa96045
match: true → VERIFIED
```

---

## Skill Document Structure Implications

The verification section of ERC8004-SKILL.md should be structured in build order — each piece of information needed before the next step is presented before it is used:

1. **Signed block format** — establishes what fields are available to parse
2. **Field extraction** — which lines contain hash, sig, agentId, registry
3. **personal_ecRecover call** — exact JSON with field mapping to parsed values
4. **Response reading** — how to extract the address from the result
5. **eth_call construction** — selector + ABI-encoded agentId, field mapping
6. **Response decoding** — last-40-chars rule for address extraction
7. **Address comparison** — lowercase-both then compare

Pre-computed values the skill doc must provide:
- Function selectors for `getAgentWallet(uint256)` and `ownerOf(uint256)`
- ABI encoding examples for several representative agentId values
- RPC endpoint URLs for mainnet and Sepolia

Fallback guidance:
- If `personal_ecRecover` returns an error: report signature invalid, do not proceed to chain read
- If `eth_call` returns `0x` (empty): registry address is wrong or agent ID does not exist
- If `eth_call` reverts: agent ID may not exist in the registry; treat as unverified
- If addresses do not match: report FAILED (not an error — a valid cryptographic outcome)
- If the RPC is unreachable: report unable to verify, not failed — the signature itself may be valid

---

## Build Order Implications for the Milestone

The verification capability depends on:

1. **Signed block parsing spec** (already in ai.html, must carry over) — defines what inputs verification receives
2. **RPC call specs** (this document) — defines the two HTTP calls and their exact formats
3. **The skill doc itself** (ERC8004-SKILL.md) — combines parsing spec + RPC specs into agent-consumable instructions

There are no circular dependencies. The RPC call architecture does not require any changes to the existing verifier application — it is a parallel description of what the browser app already does, expressed for agents using raw HTTP instead of viem.

The `personal_ecRecover` path and the `eth_call` path are independent and can be executed concurrently by an agent that supports parallel requests. The comparison step is the only synchronization point.

---

## Quality Gate Checklist

- [x] Components clearly defined with boundaries (agent, RPC node, registry contract)
- [x] Data flow direction explicit (parse → two parallel RPC calls → compare)
- [x] Build order implications noted (signed block format must precede verification steps in skill doc)
- [x] Exact JSON-RPC request formats for personal_ecRecover and eth_call
- [x] ABI encoding for getAgentWallet(uint256) and ownerOf(uint256)
- [x] Function selector computation explained with pre-computed values
- [x] Response decoding (address extraction from 32-byte ABI-encoded result)
- [x] Error cases and fallbacks for each RPC call and comparison step

---

*Architecture research: 2026-02-19*
