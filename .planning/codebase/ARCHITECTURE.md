# Architecture

**Analysis Date:** 2026-02-19

## Pattern Overview

**Overall:** Client-side verification application with functional, modular design

**Key Characteristics:**
- Runs entirely in the browser (no server-side verification)
- Modular namespace-based organization (ClojureScript)
- Reactive UI driven by centralized state store (Zustand)
- Direct blockchain interaction via viem for on-chain verification
- Immutable data structures with functional programming patterns

## Layers

**UI & Rendering Layer:**
- Purpose: DOM manipulation, event handling, HTML generation
- Location: `src/verify/core.cljs` (functions: `render-*`, `$`, `set-html!`, `set-class!`)
- Contains: Rendering functions, DOM helpers, event listeners
- Depends on: State store, parse module, chain module
- Used by: Browser runtime via `init` export

**State Management Layer:**
- Purpose: Centralized reactive state store for application state
- Location: `src/verify/state.cljs`
- Contains: Zustand store with input, status, result, error state
- Depends on: zustand library
- Used by: core.cljs for state subscriptions and updates

**Business Logic Layer:**
- Purpose: Core verification and signature recovery logic
- Location: `src/verify/chain.cljs`
- Contains: On-chain contract reading, signature recovery, verification orchestration
- Depends on: viem library
- Used by: core.cljs verification flow

**Parsing & Validation Layer:**
- Purpose: Parse signed content blocks and validate data
- Location: `src/verify/parse.cljs` and `src/verify/schema.cljs`
- Contains: Text parsing (parse.cljs), schema validation with Zod (schema.cljs)
- Depends on: clojure.string, zod library
- Used by: core.cljs for input validation before verification

## Data Flow

**Verification Flow (Paste Mode):**

1. User pastes signed content into textarea
2. `init` event handler calls `do-verify!` on paste or button click
3. `parse/parse-signed-content` extracts signature, hash, agent ID, chain from text
4. `schema/validate` validates parsed data against Zod schema
5. `chain/verify-all` runs on-chain verification:
   - Recovers signer address from signature + hash using ECDSA recovery
   - Reads agent wallet address from ERC-8004 registry contract
   - Reads agent owner (NFT owner) from registry
   - Compares signer against both wallet and owner addresses
6. `set-result` updates state with verification result
7. `render-verified` re-renders UI showing verification status

**URL Hash Verification Flow:**

1. Page loads with `#` fragment containing base64-encoded JSON
2. `decode-url-hash` parses the fragment into structured data
3. `verify-parsed!` skips text parsing, uses structured data directly
4. Rest of verification flow proceeds as above

**State Management Flow:**

1. State subscription set in `init` via `state/subscribe`
2. State changes trigger `render` callback automatically
3. Render function dispatches to appropriate renderer (`render-idle`, `render-verifying`, etc.)
4. DOM updated in response to state changes

**Signature Recovery & Validation:**

1. Content text hashed off-chain → produces hash (bytes)
2. Hash signed with agent's private key → produces signature (hex string)
3. On verification: signature + hash → ECDSA recovery → signer address
4. Signer address compared against registered agent wallet/owner addresses

## Key Abstractions

**Signed Content Block:**
- Purpose: Standard format for embedding agent signatures in text
- Examples: Text before `---` separator is content; footer contains `sig:`, `hash:`, `ts:`, agent identity
- Pattern: Regex-based parsing with anchoring on "Signed by" line

**Agent Identity (eip155 format):**
- Purpose: Uniquely identifies agent on-chain across networks
- Format: `eip155:<chainId>:<registryAddress>:<agentId>`
- Examples: `eip155:42161:0x8004...a432:5` (Arbitrum, specific registry and agent)
- Location: `src/verify/parse.cljs` line 8 (full-id-re regex)

**Verification Result Object:**
- Purpose: Complete verification outcome with all relevant metadata
- Contains: `verified` (boolean), `signer`, `agent-wallet`, `agent-owner`, `wallet-match`, `owner-match`, `chain-name`, `registry`, `agent-id`, `timestamp`, `subject`
- Pattern: JavaScript object created in `chain/verify-all` (line 107), converted to Clj in rendering

**URL Proof Fragment:**
- Purpose: Shareable verify link with signature + hash encoded in URL
- Format: Base64-encoded JSON with keys `{s, h, a, t, d, c, n}`
- Usage: `decode-url-hash` parses on page load; `__copyVerifyLink` creates on demand
- Location: `src/verify/core.cljs` lines 183-216, 258-284

## Entry Points

**`verify.core/init`:**
- Location: `src/verify/core.cljs` line 220
- Triggers: Called on page load via shadow-cljs module init
- Responsibilities: Set up state subscriptions, attach all event listeners (textarea input, verify button, paste events), decode and auto-verify URL hash if present

**HTML Entry (index.html / ai.html):**
- Location: `public/index.html`, `public/ai.html`
- Triggers: Browser load
- Responsibilities: Render initial HTML structure, load compiled JavaScript bundle

**State Subscribers:**
- Pattern: `state/subscribe` callback receives state changes, dispatches to render functions
- Initial subscription set in `init` line 222

## Error Handling

**Strategy:** Graceful error display with user-facing messages

**Patterns:**

- **Parse Failures** (line 160): User gets message "Could not parse signed content. Make sure you paste the full block..."
- **Validation Failures** (line 151): User gets "Invalid format. Check the signature and hash values."
- **Chain/RPC Failures** (chain.cljs line 85): Unsupported chain ID returns `{:verified false :error "Unsupported chain: X"}`
- **Signature Recovery Failures** (chain.cljs line 122): Recovers to `{:verified false :error "Signature recovery failed: ..."}`
- **Network/Contract Read Failures** (chain.cljs line 94): `.catch` propagates as error in result object
- **Display Layer** (core.cljs line 121): `render-error` wraps error message in error-styled card

All errors set state to `{status "error" :error <message>}`, triggering `render-error` re-render.

## Cross-Cutting Concerns

**Logging:** No structured logging present. Console would receive promise errors silently in `.catch` blocks.

**Validation:** Two-layer approach:
- Schema validation via Zod (`src/verify/schema.cljs`) for type safety
- Parse validation via regex patterns (`src/verify/parse.cljs`) for format correctness

**Authentication:** Not applicable. Verification is cryptographic, not authentication. User supplies signed proof; app verifies against on-chain state.

**Chain/Network Abstraction:** `chain/make-client` creates viem public clients per chain. Supported chains hardcoded in `chains` map (Arbitrum mainnet 42161, Arbitrum Sepolia 421614).

---

*Architecture analysis: 2026-02-19*
