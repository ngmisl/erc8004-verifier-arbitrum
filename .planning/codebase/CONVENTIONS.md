# Coding Conventions

**Analysis Date:** 2026-02-19

## Language & Dialect

**Primary Language:** ClojureScript

**Namespace Declaration:**
- All files start with `(ns <namespace>` declaration with namespaced requires
- Uses kebab-case for namespaces: `verify.parse`, `verify.chain`, `verify.state`
- Example: `(ns verify.core (:require [verify.parse :as parse] ...))`

**Runtime:**
- Compiles to JavaScript via shadow-cljs
- Targets browser environment
- Uses ClojureScript interop to JavaScript libraries (viem, zustand, zod)

## Naming Patterns

**Namespaces:**
- Kebab-case: `verify`, `verify.core`, `verify.state`, `verify.chain`
- Organization by feature/domain: parse, chain verification, state management

**Functions:**
- Kebab-case: `parse-signed-content`, `recover-signer`, `verify-all`, `set-html!`
- Side-effect functions use trailing `!`: `set-html!`, `verify-parsed!`, `do-verify!`
- Predicates use `?`: Not observed in current codebase, but convention exists
- Internal/private functions use leading hyphen: `-find-signed-by-index`, `-find-separator-before`

**Variables:**
- Kebab-case: `content-hash`, `agent-wallet`, `signed-idx`, `chain-info`
- Destructured names follow kebab-case: `{:keys [signature hash chain-id registry agent-id]}`
- JavaScript object access converted to kebab-case when extracted: `:full-id` instead of `fullId`

**Constants/Patterns:**
- UPPER_CASE_WITH_UNDERSCORES for regex patterns: `agent-line-re`, `sig-re`, `hash-re`
- Definitions use `def` or `defn`: `(def hex-pattern #"^0x[0-9a-fA-F]+$")`

**Type Names/Records:**
- PascalCase for Zod schema objects: `SignedContent`

## File Organization

**Structure:**
```
src/verify/
├── core.cljs      # Main entry point, DOM handling, rendering, verification flow
├── parse.cljs     # Parsing signed content blocks
├── chain.cljs     # On-chain verification via viem
├── schema.cljs    # Zod schema validation
└── state.cljs     # Zustand state management
```

**File Naming:**
- kebab-case: `core.cljs`, `parse.cljs`, `chain.cljs`, `state.cljs`, `schema.cljs`
- One namespace per file
- Matches namespace structure

## Import Organization

**Order (observed pattern):**
1. Built-in/standard library requires (clojure.string)
2. Third-party library requires (viem, zustand, zod)
3. Local module requires

**Example from `verify.core`:**
```clojure
(ns verify.core
  (:require [verify.parse :as parse]
            [verify.schema :as schema]
            [verify.chain :as chain]
            [verify.state :as state]
            [clojure.string :as str]))
```

**Alias Style:**
- Short namespace aliases: `parse`, `schema`, `chain`, `state`, `str`
- Import specific items with `:refer`: `(:require ["zod" :refer [z]])`
- Prefer `:as` aliases over `:refer` for clarity

**JavaScript Interop:**
- String-based requires for npm packages: `["viem" :refer [...]]`, `["zustand/vanilla" :refer [createStore]]`
- Interop with JavaScript APIs: `js/document`, `js/Date`, `js/Promise`, `js/JSON`, `js/BigInt`

## Code Style

**Indentation:**
- 2-space indentation (standard ClojureScript)
- Consistent parenthesis nesting

**Line Length:**
- Generally follows reasonable limits
- Long HTML/string building uses explicit continuation (multiline strings)

**Comment Style:**
- Semicolon comments for inline: `;; inline comment`
- Section dividers using visual ASCII: `;; ─── Section Name ──────────────────────────`
- Documentation comments using docstrings in quotes: `"Parse a signed content block into its components"`

**Quotes:**
- Double quotes `""` for strings
- Single quotes for character literals (not observed, but standard)

## Function Design

**Parameter Ordering:**
- Most important/primary data first
- Context/configuration last
- Example: `parse-full-id [full-id]`, `read-agent-wallet [client registry agent-id]`

**Return Values:**
- Functions return maps with `:ok` flag for validation: `{:ok true :data ...}` or `{:ok false :error ...}`
- Parser functions return parsed maps or nil: `{:content ... :signature ... :hash ...}` or `nil`
- Async functions return JavaScript Promises: `(js/Promise. (fn [resolve _reject] ...))`

**Function Size:**
- Generally compact, focused functions
- Large functions are organized with internal helpers
- Example: `parse-signed-content` has 2 internal helpers: `find-signed-by-index`, `find-separator-before`

**Error Handling:**
- Returns nil for parsing failures: `when-let` patterns catch nil returns
- Returns error objects in maps: `{:ok false :error "message"}`
- Promise rejection caught in `.catch` handlers
- Try-catch for exceptional cases with `:default` handler: `(catch :default _ default-return)`

## Data Structures

**Maps:**
- Use keyword keys: `:content`, `:signature`, `:hash`, `:chain-id`
- Use `-` in keyword names for multi-word keys: `:full-id`, `:chain-id`, `:agent-id`, `:agent-wallet`

**Vectors:**
- Use for sequences, especially split lines: `(str/split-lines text)`
- Use `vec` to convert to vectors when needed: `(vec (...))`

**Pattern Matching:**
- Destructuring in function parameters: `[{:keys [signature hash chain-id]}]`
- Case statements for status values: `(case status "idle" ... "verifying" ...)`
- When-let for optional values: `(when-let [el ($ "#input")] ...)`

## JavaScript Interop

**DOM Access:**
- Helper function for querySelector: `(defn $ [sel] (.querySelector js/document sel))`
- Direct property access: `(.-innerHTML el)`, `(.-value textarea)`, `(.-message e)`
- Property setting: `(set! (.-innerHTML el) html)`

**Promise Handling:**
- Use `.then` and `.catch` for promise chains
- Arrow functions in promise handlers: `(fn [r] ...)`
- Wrap synchronous code in Promise constructor: `(js/Promise. (fn [resolve _reject] ...))`

**Type Conversion:**
- `clj->js` converts ClojureScript to JavaScript
- `js->clj` converts JavaScript to ClojureScript, with `:keywordize-keys true` option
- `#js` literal for JavaScript object/array: `#js {:key "value"}` or `#js [item1 item2]`

**JavaScript Standard Library:**
- `js/Date`, `js/BigInt`, `js/Promise`, `js/JSON`, `js/TextEncoder`, `js/TextDecoder`
- Use via dot notation: `(.encode (js/TextEncoder.) s)`

## Regex and String Operations

**Regex Patterns:**
- Defined as constants with `_re` suffix: `agent-line-re`, `sig-re`, `hash-re`
- Literal syntax: `#"^0x[0-9a-fA-F]+$"`
- Used with `re-find`, `re-matches`, `re-search`

**String Operations:**
- Use `clojure.string` namespace alias `str`
- Common functions: `str/trim`, `str/split-lines`, `str/replace`, `str/split`, `str/join`
- String concatenation with `str` function: `(str "prefix" value "suffix")`

## Validation

**Zod Schema Usage:**
- Define schemas as ClojureScript constants converted to JavaScript: `(.object z #js {...})`
- Use `.safeParse` for safe validation returning `{:success ... :error ...}`
- Convert between kebab-case (Clojure) and camelCase (JavaScript) in schema fields

**Example Pattern:**
```clojure
(let [result (.safeParse SignedContent (clj->js {...}))]
  (if (.-success result)
    {:ok true :data parsed}
    {:ok false :error (-> result .-error .format)}))
```

## State Management

**Zustand Store:**
- Single store created with `createStore` from zustand/vanilla
- Store contains both state and action functions
- Actions follow naming pattern with `set` prefix: `setInput`, `setVerifying`, `setResult`, `setError`
- Subscribe to store: `(.subscribe store listener)`
- Get current state: `(.getState store)`

**Pattern:**
```clojure
(def store
  (createStore
    (fn [set _get]
      #js {:input ""
           :status "idle"
           :setInput (fn [v] (set #js {:input v}))
           ...})))
```

## Side Effects

**DOM Mutation:**
- Grouped in helper functions with `!` suffix
- Use when-let to safely get DOM elements: `(when-let [el ($ sel)] ...)`
- Set innerHTML/className instead of manipulating structure

**Event Listeners:**
- Attached via `.addEventListener` with anonymous functions
- Use `(.-target e)` to access event target
- Use `.preventDefault` to prevent defaults

**Async Effects:**
- `.then` chains for promise-based operations
- `.catch` for error handling
- `js/setTimeout` for delayed operations

## Comments and Documentation

**Docstrings:**
- Appear after function name in quotes (ClojureScript convention)
- Describe what the function does and what it returns
- Example: `"Parse a signed content block into its components. Returns {:content :full-id :signature :hash :timestamp} or nil."`

**Section Comments:**
- Use ASCII dividers for major sections
- Format: `;; ─── Section Name ──────────────────────────────────`
- Helps visually organize related functions

**Inline Comments:**
- Sparingly used for non-obvious logic
- Explain the "why" not the "what"

---

*Convention analysis: 2026-02-19*
