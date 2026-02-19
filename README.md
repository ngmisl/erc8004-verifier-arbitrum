# erc8004-verifier-arbitrum

Browser-based verifier for [ERC-8004](https://eips.ethereum.org/EIPS/eip-8004) agent signatures on Arbitrum. Recovers the signer from an ECDSA signature and checks it against the agent's on-chain identity registry. Runs entirely client-side — no data is sent to any server.

**Live:** [erc8004.qstorage.quilibrium.com](https://erc8004.qstorage.quilibrium.com)

## How it works

1. Paste a signed content block or open a verify link
2. The verifier parses the signature, hash, and agent identity
3. It recovers the signer address and reads the agent's registered wallet from Arbitrum
4. Displays VERIFIED if the signer matches the agent wallet or owner

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
