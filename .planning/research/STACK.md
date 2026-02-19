# Stack Research

**Domain:** AI agent skill documents — machine-readable instructions for Ethereum JSON-RPC interaction
**Researched:** 2026-02-19
**Confidence:** HIGH

## Context

This research is scoped to: creating a skill document (`ERC8004-SKILL.md`) that teaches AI agents to
verify ERC-8004 signatures using only web fetch (HTTP POST to public Arbitrum RPC). The existing
ClojureScript browser app is not changing. The new `.md` file replaces `public/ai.html` as the
primary machine-readable instruction surface for agents.

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|---|---|---|---|
| Markdown (.md) | CommonMark | Skill document format | LLMs ingest markdown natively; no HTML parser required; renders in GitHub/Claude/GPT; survives copy-paste intact; fetched as plain text via any HTTP client |
| JSON-RPC 2.0 | spec | Wire protocol for Ethereum calls | All public Arbitrum nodes speak it; no SDK, no wallet, no auth required; one POST per call; works from `curl`, Python `requests`, JS `fetch`, or any HTTP tool an agent has access to |
| EIP-191 personal sign | — | Signature scheme for content hash recovery | The existing app signs with personal_sign; `personal_ecRecover` is the exact inverse; both are standard across all Ethereum nodes |
| ABI encoding (manual hex) | — | Encoding `eth_call` calldata | Agents cannot run ethers/viem; they must hand-encode selectors and parameters; keccak4-byte selectors + 32-byte padded uint256 is the entire encoding surface needed here |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---|---|---|---|
| None — raw HTTP only | — | RPC transport | An agent calls JSON-RPC with `fetch`/`requests`/`curl`; no library is needed or appropriate; libraries are for human-authored code, not agent runtime |

---

## Skill Document Structure — Recommended Format

Structure the `.md` file so that an agent reading it top-to-bottom acquires everything needed to
execute, in execution order. Do not use headers for navigation — use them to delimit executable
steps.

### Prescribed section order for `ERC8004-SKILL.md`

```
# ERC-8004 Verification Skill

## What this skill does          ← one-paragraph contract: inputs → outputs
## Prerequisites                 ← what the agent must have before starting
## Step 1: Parse the signed block ← regex patterns, field names
## Step 2: Recover the signer    ← personal_ecRecover JSON-RPC call + full example
## Step 3: Read the agent wallet ← eth_call with manual ABI encoding + full example
## Step 4: Read the NFT owner    ← eth_call for ownerOf + full example
## Step 5: Compare and conclude  ← exact boolean logic
## RPC endpoints                 ← chain ID → URL table, no auth
## Error handling                ← what each failure means, what to return
## Full worked example           ← end-to-end inputs and outputs for one real case
```

Key rules for agent-consumable markdown:
- Every code block must be runnable as-is (no `...` placeholders that aren't labeled as such).
- Put the concrete example first, the explanation second. Agents execute before they reason.
- Use fenced code blocks with explicit language tags (`json`, `shell`) — this signals executability.
- Keep prose to one sentence per concept. Agents do not benefit from motivation paragraphs.
- Never use tables where a code block works better. Tables require parsing; code blocks are copied.

---

## JSON-RPC Patterns

### personal_ecRecover — recover signer from EIP-191 signature

The existing app signs with EIP-191 personal_sign over the raw content hash bytes (not the content
text). The hash is `keccak256(abi.encode(keccak256(content_utf8), timestamp_utf8))`. Agents do not
need to recompute the hash — it is in the signed block as the `hash:` field. They pass it directly
to `personal_ecRecover`.

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "personal_ecRecover",
  "params": [
    "0x<content_hash_32_bytes_hex>",
    "0x<signature_65_bytes_hex>"
  ]
}
```

POST to: `https://arb1.arbitrum.io/rpc` (chain 42161) or `https://sepolia-rollup.arbitrum.io/rpc` (chain 421614).

Response:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "0x<recovered_address_20_bytes_checksummed>"
}
```

Note: The hash field in the signed block is already the prefixed personal_sign hash. Pass it as a
hex string — the node treats it as raw bytes for recovery. Do not double-prefix.

### eth_call — read contract state without a wallet

`eth_call` requires manually encoded calldata. For this skill, only two functions are needed:

#### getAgentWallet(uint256 agentId) → address

Function selector: first 4 bytes of `keccak256("getAgentWallet(uint256)")` = `0x45a3a8d5`

Calldata encoding:
```
0x45a3a8d5                                                          ← 4-byte selector
000000000000000000000000000000000000000000000000000000000000000N   ← agentId, 32-byte big-endian uint256
```

Where `N` is the agentId in hex. For agentId=5: `0000000000000000000000000000000000000000000000000000000000000005`

Full request:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "eth_call",
  "params": [
    {
      "to": "0x<registry_address>",
      "data": "0x45a3a8d5000000000000000000000000000000000000000000000000000000000000000N"
    },
    "latest"
  ]
}
```

Response: `{"result": "0x000000000000000000000000<20_byte_address_padded_to_32>"}` — take the last 40 hex chars as the address.

#### ownerOf(uint256 tokenId) → address

Function selector: `keccak256("ownerOf(uint256)")` first 4 bytes = `0x6352211e`

Same encoding as above, substituting agentId with tokenId (they are the same value in ERC-8004).

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "eth_call",
  "params": [
    {
      "to": "0x<registry_address>",
      "data": "0x6352211e000000000000000000000000000000000000000000000000000000000000000N"
    },
    "latest"
  ]
}
```

Response decoded identically — last 40 hex chars are the owner address.

### Decoding the response address

```
result = "0x000000000000000000000000d8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
address = "0x" + result[26:]   ← skip "0x" + 24 leading zero chars = index 26 in the string
```

---

## ABI Encoding — What to Include in the Skill Document

Agents cannot run keccak256 at runtime unless they have a library. The skill document must embed
the pre-computed selectors as constants. Include:

| Function signature | Selector | Note |
|---|---|---|
| `getAgentWallet(uint256)` | `0x45a3a8d5` | Returns the delegated signing wallet |
| `ownerOf(uint256)` | `0x6352211e` | Returns the NFT owner (fallback match) |

Do not include `tokenURI` — agents do not need metadata to verify a signature. Omit it to reduce
surface area.

For the parameter encoding rule, state it once, concretely: pad the agentId to 64 hex characters
(32 bytes) with leading zeros. Do not describe the full ABI spec — one rule, one example.

---

## Verification Logic — Exact Specification

Include this as executable pseudocode in the skill document, not prose:

```
signer   = personal_ecRecover(hash, sig)           # lowercase both for comparison
wallet   = eth_call getAgentWallet(agentId)
owner    = eth_call ownerOf(agentId)

verified = (signer.toLowerCase() == wallet.toLowerCase())
        || (signer.toLowerCase() == owner.toLowerCase())
```

The `||` is intentional and must be documented: an agent may sign with either the delegated wallet
or the NFT owner address. Both are valid.

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|---|---|---|
| Markdown skill document (.md) | HTML instruction page (ai.html) | Never for agent consumption — HTML requires parsing, CSS obscures content, agents fetching HTML get noise |
| Markdown skill document (.md) | OpenAPI / JSON Schema | Only if the skill exposes an HTTP API for agents to call; this skill describes what the agent itself must call, not an API it provides |
| Markdown skill document (.md) | System prompt injection | When the agent runtime allows it and the instructions are stable — but a fetchable document is self-updating and doesn't consume context window on every turn |
| personal_ecRecover via JSON-RPC | Off-chain ECDSA library | Never for agents that only have web fetch — personal_ecRecover is guaranteed available on all EVM nodes |
| Manual hex ABI encoding | ethers.js / viem | Only in code running in a JS runtime the agent controls; not available in pure-HTTP agent contexts |
| Hard-coded selectors in skill doc | Ask agent to compute keccak256 | Never — agents cannot reliably compute keccak256 without a tool; pre-compute and embed the constants |
| `eth_call` with `"latest"` | Block number pinning | Only for reproducible historical queries; for current agent wallet reads, `"latest"` is correct |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|---|---|---|
| HTML for skill documents | LLMs parse it as text — tags become noise, CSS is irrelevant, structure is lost | Plain markdown |
| SDK-dependent examples (ethers.js, viem, web3.py) | Agent has no npm/pip at runtime; examples become aspirational instead of executable | Raw `fetch`/`curl` with JSON body |
| `eth_getLogs` for verification | Requires block range, filter topics, and event ABI decoding — far more complexity than a direct `eth_call` read | `eth_call` against the registry |
| WebSocket JSON-RPC (`wss://`) | Agents using HTTP fetch cannot open WebSocket connections | HTTP endpoint only |
| `personal_sign` in the skill document | Agents are verifying, not signing; including signing instructions conflates the two roles | Omit signing entirely from the verification skill |
| Description of the keccak hash construction | Verification agents receive the pre-computed hash in the signed block; they don't need to recompute it | State "use the `hash:` field verbatim" |
| Chain explorers (Arbiscan) as a verification step | Explorers have rate limits, scraping terms, and HTML responses; they're for humans | JSON-RPC only |
| Mutable RPC endpoints that require API keys | API keys change, rotate, expire; skill docs should reference only public endpoints | `arb1.arbitrum.io/rpc` (keyless, stable) |
| `eth_call` with `"pending"` block tag | Reads uncommitted state; agent wallets are written in confirmed txs | `"latest"` |

---

## Public RPC Endpoints (no auth required)

| Chain ID | Name | Endpoint |
|---|---|---|
| 42161 | Arbitrum One | `https://arb1.arbitrum.io/rpc` |
| 421614 | Arbitrum Sepolia | `https://sepolia-rollup.arbitrum.io/rpc` |

Both are official Offchain Labs endpoints. No API key, no rate-limit header, no CORS issue for
agents making server-to-server requests. Include both in the skill doc so agents can handle testnet
agent IDs without failing.

---

## Skill Document Delivery

Host the skill document at a stable URL. Convention: `https://erc8004.orbiter.website/ERC8004-SKILL.md`
or alongside the verifier as a static file. Agents discover it via:
- A `Link:` header on the main page pointing to the skill doc
- A reference in existing signed content blocks
- Direct URL inclusion in system prompts

The `.md` extension signals to agents and tooling that this is a markdown text document. Do not
serve it as `text/html` — serve as `text/markdown` or `text/plain`.

---

## Sources

- Ethereum JSON-RPC specification: https://ethereum.org/en/developers/docs/apis/json-rpc/
- EIP-191 personal sign: https://eips.ethereum.org/EIPS/eip-191
- ERC-8004 draft: https://eips.ethereum.org/EIPS/eip-8004
- Arbitrum public RPC docs: https://docs.arbitrum.io/build-decentralized-apps/public-chains
- ABI encoding spec (function selector + parameter encoding): https://docs.soliditylang.org/en/latest/abi-spec.html
- Existing app chain.cljs — confirms `personal_ecRecover` is the inverse of `recoverMessageAddress`, and that `getAgentWallet` / `ownerOf` are the only two reads needed for verification
- Existing app parse.cljs — confirms the `hash:` field is the raw bytes to pass to `personal_ecRecover`, not content text
