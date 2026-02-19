# Codebase Structure

**Analysis Date:** 2026-02-19

## Directory Layout

```
erc8004-arbitrum/
├── src/
│   └── verify/              # Main application namespaces
│       ├── core.cljs        # UI rendering and event handling
│       ├── state.cljs       # Zustand state store
│       ├── chain.cljs       # On-chain verification logic
│       ├── parse.cljs       # Text parsing for signed content
│       └── schema.cljs      # Zod schema validation
├── public/                  # Static HTML and assets
│   ├── index.html           # Main application HTML
│   ├── ai.html              # Alternative HTML variant
│   └── favicon.svg          # Favicon
├── .planning/
│   └── codebase/            # Architecture documentation (generated)
├── package.json             # Dependencies and scripts
├── shadow-cljs.edn          # ClojureScript build configuration
├── bun.lock                 # Bun lockfile
└── README.md                # Project documentation
```

## Directory Purposes

**`src/verify/`:**
- Purpose: ClojureScript application source code
- Contains: Five namespace modules organizing verification logic
- Key files: core.cljs (UI), chain.cljs (on-chain logic), parse.cljs (text parsing)

**`public/`:**
- Purpose: Static web assets and HTML entry points
- Contains: index.html, ai.html, favicon.svg
- Key files: index.html (primary UI template)

**`.planning/codebase/`:**
- Purpose: Architecture and codebase analysis documents
- Contains: Generated ARCHITECTURE.md, STRUCTURE.md, and future STACK.md, CONVENTIONS.md, etc.
- Generated: Yes
- Committed: Yes (markdown only)

## Key File Locations

**Entry Points:**
- `public/index.html`: Main HTML template rendered in browser, loads compiled JavaScript
- `public/ai.html`: Alternative HTML template (identical structure, separate file for flexibility)
- `src/verify/core.cljs`: Application init function (`init`) called by shadow-cljs module initialization

**Configuration:**
- `package.json`: npm/bun dependencies (viem, zustand, zod), build/dev scripts
- `shadow-cljs.edn`: ClojureScript compiler config; specifies `:target :browser`, output dir, modules
- `bun.lock`: Dependency lockfile for Bun package manager

**Core Logic:**
- `src/verify/chain.cljs`: On-chain verification orchestration, signature recovery, contract reading
- `src/verify/parse.cljs`: Parse signed content blocks from text using regex patterns
- `src/verify/schema.cljs`: Zod schema definition and validation function

**UI & State:**
- `src/verify/core.cljs`: All rendering functions, DOM helpers, event binding, initialization
- `src/verify/state.cljs`: Zustand store definition, getters, subscribers

**Testing:**
- Not present. No test files detected.

## Naming Conventions

**Files:**
- Pattern: kebab-case `.cljs` extension (e.g., `verify-all`, `parse-signed-content`)
- Modules map to namespaces (e.g., `src/verify/core.cljs` → `verify.core` namespace)

**Namespaces:**
- Pattern: `verify.<module>` structure
- Examples: `verify.core`, `verify.state`, `verify.chain`, `verify.parse`, `verify.schema`

**Functions:**
- Pattern: kebab-case (standard Clojure convention)
- Examples: `render-verified`, `parse-signed-content`, `recover-signer`, `read-agent-wallet`
- Private functions prefixed with `-` (e.g., `find-signed-by-index` in parse.cljs)
- Exported functions marked with `^:export` (e.g., `^:export init` in core.cljs)

**Variables & State:**
- Pattern: kebab-case for local bindings and state keys
- Examples: `:full-id`, `:chain-id`, `:agent-id`, `:wallet-match`
- State fields match camelCase JS interop (e.g., `.-status`, `.-setVerifying`)

**Constants:**
- Pattern: UPPERCASE with descriptive names
- Examples: `SignedContent` (Zod schema in schema.cljs), `identity-abi` (contract ABI in chain.cljs)
- Regex patterns: suffixed with `-re` (e.g., `agent-line-re`, `full-id-re` in parse.cljs)

## Where to Add New Code

**New Verification Feature (e.g., new chain support):**
- Primary code: `src/verify/chain.cljs`
- Add chain info to `chains` map (line 22-28)
- Modify `make-client` if needed for chain-specific configuration
- Tests: Create `src/verify/chain.test.cljs` following no existing patterns

**New UI Component/Rendering:**
- Implementation: `src/verify/core.cljs` (add `render-*` function following existing pattern)
- State changes: May require additions to `src/verify/state.cljs` store
- Events: Add event listener in `init` function

**New Parsing Rule or Format:**
- Primary code: `src/verify/parse.cljs`
- Add new regex pattern (suffix with `-re`)
- Add/modify parse function
- Update schema in `src/verify/schema.cljs` if new fields required
- Update validation in `verify-parsed!` (core.cljs line 141)

**New Utility/Helper:**
- Location depends on domain:
  - String/text utilities: `src/verify/parse.cljs`
  - Cryptography/signing utilities: `src/verify/chain.cljs`
  - DOM utilities: `src/verify/core.cljs` (near other DOM helpers like `$`, `set-html!`)

**New State Fields:**
- Primary: `src/verify/state.cljs` (add to store initialization, add setter function)
- Update: Core rendering logic in `src/verify/core.cljs` to handle new state
- Validation: Update schema if new state represents user input

## Special Directories

**`node_modules/`:**
- Purpose: Installed dependencies (viem, zustand, zod, shadow-cljs)
- Generated: Yes (bun install)
- Committed: No

**`.git/`:**
- Purpose: Git repository metadata
- Generated: Yes (git init)
- Committed: No

**`public/js/` (created at build time):**
- Purpose: Compiled ClojureScript bundle
- Generated: Yes (shadow-cljs compile)
- Committed: No (built from src/)

**`dist/` (created at build time):**
- Purpose: Production-ready distribution directory
- Generated: Yes (npm run build)
- Committed: No
- Contents: Minified HTML, CSS, compiled JS

## Build Artifact Paths

**Development:**
- Compiled JS: `public/js/main.js` (watch rebuild on src changes)
- Dev server: http://localhost:3100 (from shadow-cljs :devtools)

**Production:**
- Build output: `public/js/main.js` (advanced optimization)
- Distribution: `dist/` directory created with HTML, CSS, JS copies

## Import Patterns

**External Dependencies:**
- Pattern: `(:require ["package" :refer [symbols]])` (ES6 modules)
- Examples:
  - `["zustand/vanilla" :refer [createStore]]` (state store)
  - `["viem" :refer [createPublicClient http recoverMessageAddress]]` (blockchain)
  - `["zod" :refer [z]]` (schema validation)

**Internal Modules:**
- Pattern: `(:require [namespace.module :as alias])`
- Examples:
  - `[verify.parse :as parse]`
  - `[verify.chain :as chain]`
  - `[verify.state :as state]`

**Clojure Standard Library:**
- Pattern: `(:require [clojure.string :as str])`
- Usage: String manipulation (split-lines, trim, replace, join)

---

*Structure analysis: 2026-02-19*
