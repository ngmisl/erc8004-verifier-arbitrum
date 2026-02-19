# ERC-8004 Signature Verifier (Arbitrum)

A browser-based tool that verifies ERC-8004 agent signatures against Arbitrum on-chain identity registries. Written in ClojureScript, compiled with shadow-cljs, runs entirely client-side.

## Tech stack

- **Language:** ClojureScript
- **Build:** shadow-cljs (`:target :browser`, `:optimizations :advanced`)
- **Runtime deps:** viem (Ethereum client), zustand (state), zod (validation)
- **Package manager:** bun
- **Dev server:** shadow-cljs devtools on port 3100

## Commands

- `bun run dev` — start shadow-cljs watch (dev server at http://localhost:3100)
- `bun run build` — release build + copy to `dist/`
- `bun run serve` — serve the `dist/` directory

## Project structure

```
src/verify/
  core.cljs    — entry point, DOM rendering, URL hash decoding, init
  parse.cljs   — text parser for signed content blocks
  schema.cljs  — zod validation of parsed data
  chain.cljs   — viem client, on-chain reads (ownerOf, getAgentWallet, tokenURI), signature recovery
  state.cljs   — zustand vanilla store (input, status, result, error)

public/
  index.html   — main verifier UI (single-page, all CSS inlined)
  ERC8004-SKILL.md  — machine-readable agent skill document (markdown)
  favicon.svg
```

## Architecture

### Verification flow

1. User pastes signed content or arrives via verify link (URL hash)
2. `parse/parse-signed-content` extracts: content, full-id, signature, hash, timestamp
3. `schema/validate` checks parsed data against zod schema
4. `chain/verify-all` runs on-chain verification:
   - Recovers signer address from ECDSA signature via `recoverMessageAddress`
   - Reads agent wallet and owner from the ERC-8004 registry contract
   - Matches recovered signer against agent wallet or owner
5. Result rendered as VERIFIED/FAILED card

### Agent identity format

`eip155:<chainId>:<registryAddress>:<agentId>` — parsed by `parse/parse-full-id`

### Supported chains

| Chain ID | Name             | RPC                                      |
|----------|------------------|------------------------------------------|
| 42161    | Arbitrum         | https://arb1.arbitrum.io/rpc             |
| 421614   | Arbitrum Sepolia  | https://sepolia-rollup.arbitrum.io/rpc   |

### State machine

`idle` → `verifying` → `verified` | `error`

Managed by zustand vanilla store in `state.cljs`. UI subscribes via `state/subscribe`.

### Verify link format

URL hash contains base64-encoded JSON: `{s, h, a, n?, t?, d?}` (signature, hash, agent full ID, name, timestamp, subject). Decoded in `core/decode-url-hash`. Links bypass the text parser and call `verify-parsed!` directly.

### Contract ABI

Minimal read-only ABI in `chain.cljs` for three functions:
- `ownerOf(uint256 tokenId) → address`
- `getAgentWallet(uint256 agentId) → address`
- `tokenURI(uint256 tokenId) → string`

## Conventions

- All JS interop uses `#js` literals for JS objects/arrays
- ClojureScript maps for internal data, `clj->js`/`js->clj` at boundaries
- DOM manipulation via direct `.querySelector` / `.innerHTML` (no React/reagent)
- Promises handled with `.then`/`.catch` chains
- No server-side code — everything runs in the browser
