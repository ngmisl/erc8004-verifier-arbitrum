# erc8004-verifier-arbitrum

Browser-based verifier for [ERC-8004](https://eips.ethereum.org/EIPS/eip-8004) agent signatures on Arbitrum, plus a machine-readable skill document that teaches AI agents to verify signatures using only web fetch — no crypto libraries needed.

**Live:** [erc8004.qstorage.quilibrium.com](https://erc8004.qstorage.quilibrium.com)
**Skill Doc:** [ERC8004-SKILL.md](https://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md)
**Contract:** `0x8004A169FB4a3325136EB29fA0ceB6D2e539a432` (Arbitrum One)

## What this does

**For humans:** Paste a signed message or click a verify link → instant VERIFIED/FAILED result. Everything runs client-side against Arbitrum.

**For AI agents:** Fetch `ERC8004-SKILL.md` → follow a 4-step verification procedure using only `eth_call` JSON-RPC requests → determine if a signature is valid. No ethers, no viem, no API keys.

## How it works

1. Parse the signed content block for signature, hash, and agent identity
2. Recover the signer address (inline EVM bytecode via `eth_call` — no deployed contract needed)
3. Read the agent's registered wallet (`getAgentWallet`) and NFT owner (`ownerOf`) from the on-chain registry
4. If the recovered signer matches either address → VERIFIED

## Two verification modes

- **Verify link** — click a link embedded in signed content. Carries signature, hash, and agent ID in the URL fragment. Verifies automatically on load.
- **Full paste** — paste the entire signed message including the signature footer. Lets you see the full content alongside the result.

## Signed content format

```
Your content here...

---
Signed by AgentName (eip155:42161:0x8004...a432:5)
sig: 0xf13bd8...
hash: 0x57caa1...
ts: 2026-02-06T14:32:15.000Z
Verify: https://erc8004.qstorage.quilibrium.com/#eyJ...
```

The agent identity follows `eip155:<chainId>:<registry>:<agentId>`.

## Agent skill document

[`ERC8004-SKILL.md`](https://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md) is a machine-readable markdown document that teaches AI agents to sign and verify ERC-8004 content. It covers:

- Signed block format and field grammar
- Agent identity format (`eip155` full ID)
- Signing instructions (tool interface, output format)
- Step-by-step verification procedure with JSON-RPC templates
- Error conditions and trust forwarding

Key technical detail: public Arbitrum RPCs don't expose `personal_ecRecover`. The skill doc uses `eth_call` with inline Solidity creation bytecode that applies the EIP-191 message prefix and calls the ecrecover precompile as a stateless computation. Any agent that can HTTP POST can verify signatures.

## Development

Requires [bun](https://bun.sh).

```sh
bun install
bun run dev     # shadow-cljs watch → http://localhost:3100
```

## Build

```sh
bun run build   # release compile + copy to dist/
bun run serve   # serve dist/
```

## Stack

ClojureScript · shadow-cljs · viem · zustand · zod
