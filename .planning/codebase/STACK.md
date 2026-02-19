# Technology Stack

**Analysis Date:** 2026-02-19

## Languages

**Primary:**
- ClojureScript 1.x (via shadow-cljs) - All application source code at `src/verify/`
- JavaScript/ECMAScript (via ClojureScript compilation) - Runtime target for browser environment

**Secondary:**
- EDN (Extensible Data Notation) - Build configuration in `shadow-cljs.edn`

## Runtime

**Environment:**
- Browser (modern ES6+)
- Target: Chrome, Firefox, Safari, Edge with WebSocket support

**Package Manager:**
- Bun 1.x - Primary package manager and task runner
- Lockfile: `bun.lock` (present)

## Frameworks

**Core:**
- shadow-cljs 2.28.0 - ClojureScript compiler and build tool for browser development

**State Management:**
- zustand 5.0.0 - Lightweight state store, used at `src/verify/state.cljs`

**Validation:**
- zod 3.24.0 - TypeScript-first schema validation library, integrated at `src/verify/schema.cljs`

**Web3 / Blockchain:**
- viem 2.45.1 - TypeScript HTTP RPC client for blockchain interactions at `src/verify/chain.cljs`

## Key Dependencies

**Critical:**
- viem 2.45.1 - Handles ECDSA signature recovery and on-chain contract reads (RPC calls to Arbitrum)
- zustand 5.0.0 - Manages UI state (input, verification status, results, errors)
- zod 3.24.0 - Validates parsed signature data (format validation before on-chain verification)
- shadow-cljs 2.28.0 - Compiles ClojureScript to optimized browser JavaScript

**Cryptography (via viem dependencies):**
- @noble/hashes 1.8.0 - Cryptographic hash functions
- @noble/curves 1.9.1 - ECDSA curve implementations
- @noble/ciphers 1.3.0 - Cipher operations
- @scure/bip32 1.7.0 - BIP32 hierarchical deterministic key derivation
- @scure/bip39 1.6.0 - BIP39 mnemonic seed phrases

## Configuration

**Environment:**
- No .env file used - Hardcoded RPC endpoints for public chains at `src/verify/chain.cljs`
- Chains configured directly in code: Arbitrum (42161) and Arbitrum Sepolia (421614)
- RPC endpoints: `https://arb1.arbitrum.io/rpc` and `https://sepolia-rollup.arbitrum.io/rpc`

**Build:**
- `shadow-cljs.edn` - Shadow CLJS build configuration
  - Source path: `src/`
  - Output directory: `public/js/`
  - Target: `:browser` with `:advanced` optimization
  - Dev server: HTTP on port 3100 at `public/` root
  - Entry point module: `:main` with init function `verify.core/init`

**Package Configuration:**
- `package.json` - Node/Bun package manifest with 3 runtime dependencies and 1 dev dependency

## Scripts

**Development:**
- `bun run dev` - Starts shadow-cljs watch mode (rebuilds on changes)
- Development server: `http://localhost:3100/`

**Build:**
- `bun run build` - Shadow-cljs release build (advanced optimizations) followed by dist directory setup
- `bun run dist` - Copies compiled JS and HTML assets to `dist/` directory
- `bun run serve` - Serves `dist/` directory locally (for testing production build)

## Platform Requirements

**Development:**
- Bun 1.x runtime
- Modern text editor or IDE
- Web browser for testing

**Production:**
- Static web hosting (GitHub Pages, Netlify, Vercel, etc.)
- No backend server required
- No database required
- Browser with WebSocket support for RPC connections

## Browser Compatibility

- Requires modern ES6+ JavaScript support
- WebSocket support for RPC communication with blockchain nodes
- DOM APIs: `querySelector`, `addEventListener`, `navigator.clipboard`
- TextEncoder/TextDecoder for UTF-8 base64 encoding/decoding
- Uint8Array and related TypedArrays for binary data handling
- `js/Promise` for asynchronous operations

---

*Stack analysis: 2026-02-19*
