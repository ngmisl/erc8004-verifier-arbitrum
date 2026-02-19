# Testing Patterns

**Analysis Date:** 2026-02-19

## Test Framework Status

**Current State:** No automated testing framework currently configured

**Reason:** Browser-based verification application with minimal testable business logic separated from DOM. The codebase is recent (initial commit) and testing infrastructure has not been implemented.

## Recommended Testing Approach

Given the architecture and nature of the application, the following is recommended:

### Unit Testing Layer

**Test Framework:** ClojureScript Test Runner (e.g., shadow-cljs test runner or Jest with ClojureScript)

**Candidates:**
- `shadow-cljs` has built-in test support via `:test` target
- Jest with `@babel/preset-clojurescript`
- Kaocha with ClojureScript support

**What to Test:**
- `verify.parse` - Parsing logic for signed content blocks
- `verify.schema` - Schema validation with Zod
- `verify.chain` - Signature recovery and chain ID support

**What NOT to Test (too integration-heavy):**
- On-chain contract reads (mock instead)
- DOM manipulation (integration test only)
- Zustand store mutations (test store methods directly, not UI)

### Current Testable Units

**`verify.parse/parse-full-id`**
```clojure
;; Testable input/output
(parse-full-id "eip155:42161:0x8004...a432:5")
;; Expected: {:chain-id 42161 :registry "0x8004...a432" :agent-id 5}

(parse-full-id "invalid")
;; Expected: nil
```

**`verify.parse/parse-signed-content`**
```clojure
;; Testable: Takes text, returns parsed map or nil
;; Test cases:
;; - Valid signed content with all fields
;; - Missing --- separator
;; - Missing "Signed by" line
;; - Malformed agent ID
;; - Whitespace variations
;; - Different line endings (CRLF vs LF)
```

**`verify.schema/validate`**
```clojure
;; Testable: Validates against Zod schema
(validate {:content "text"
           :full-id "eip155:42161:0x....:5"
           :signature "0xabc"
           :hash "0xdef"
           :chain-id 42161
           :registry "0x..."
           :agent-id 5})
;; Expected: {:ok true :data ...}
```

**`verify.chain/make-client`**
```clojure
;; Testable: Returns client for known chains
(make-client 42161)      ;; Expected: client object
(make-client 1)          ;; Expected: nil (unsupported chain)
```

### Integration Testing Layer

**What Needs Integration Tests:**
- Full verification flow from pasted text to result
- URL hash decoding and auto-verification
- DOM updates in response to state changes
- Event listener behavior (paste handling, button clicks)

**Approach:**
- Use Playwright or Puppeteer for browser automation
- Test against shadow-cljs dev server
- Verify visual output and form state changes

### Setup Requirements (When Implementing)

**Development Dependencies to Add:**
```clojure
;; In package.json or shadow-cljs.edn
:devDependencies {
  "shadow-cljs": "^2.28.0"  ;; Already have
  ;; Add for testing:
  "@testing-library/dom": "^9.x"
  "jest": "^29.x"
  "@babel/preset-typescript": "^7.x"
  ;; OR use Kaocha for ClojureScript:
  "kaocha": "latest"
}
```

**Test File Organization:**
```
src/verify/
├── core.cljs
├── parse.cljs
├── parse.test.cljs        # Co-located with source
├── schema.cljs
├── schema.test.cljs
├── chain.cljs
├── chain.test.cljs
├── state.cljs
└── state.test.cljs
```

### No Current Testing Infrastructure

**Absent Patterns:**
- No test runners configured in `shadow-cljs.edn`
- No test scripts in `package.json`
- No test files in source tree
- No mocking/assertion libraries imported
- No CI/CD test pipeline

**Why This Is Acceptable:**
- Project is an on-chain verification browser app
- Most business logic is parsing and schema validation (low complexity)
- On-chain reads are straightforward viem calls (well-tested library)
- UI is simple state-driven rendering (hard to unit test, better for integration testing)
- Code is currently small enough to manually test end-to-end

## Manual Testing Patterns

Until automated tests are implemented, testing is done manually:

**Paste Mode:**
1. Copy a full signed content block
2. Paste into textarea
3. Click "Verify" button
4. Check result display (VERIFIED/FAILED/ERROR)

**URL Hash Mode:**
1. Get a verify link
2. Load URL directly
3. Verify form is pre-filled and auto-verifies
4. Check result display

**Error Cases Tested Manually:**
- Invalid hex values in signature/hash
- Malformed agent ID
- Unsupported chain ID
- Network errors (RPC unavailable)
- Signature recovery failure
- On-chain data mismatches

## State Management Testing

**Zustand Store (`verify.state`):**

When tests are implemented, the store should be tested via direct method calls:

```clojure
;; Pseudo-test example
(let [state (get-state)]
  ;; Test action methods
  (.setInput state "new input")
  (assert (= "new input" (.-input (get-state))))

  (.setVerifying state)
  (assert (= "verifying" (.-status (get-state))))

  (.setResult state #js {...})
  (assert (= "verified" (.-status (get-state))))
)
```

**Subscription Testing:**
```clojure
;; Test that subscribers are called on state change
(let [called (atom false)
      listener (fn [] (reset! called true))]
  (subscribe listener)
  (.setInput (get-state) "test")
  (assert @called))
```

## DOM Testing Notes

**Current Implementation:**
- DOM manipulation in `verify.core` is tightly coupled to state
- Rendering functions (`render-idle`, `render-verifying`, `render-verified`) generate HTML strings
- These are inserted via `set-html!` helper

**Testing Challenges:**
- ClojureScript DOM helpers like `$` depend on real DOM
- Promise-based chain verification is hard to mock in unit tests
- viem client creation requires real RPC endpoints

**Mitigation When Testing:**
- Mock `verify.chain` module functions
- Use jsdom or happy-dom to provide DOM
- Stub RPC calls with local responses
- Test rendering functions with mock data

## Code Coverage Targets

When tests are implemented, aim for:

**High Priority (80%+ coverage):**
- `verify.parse` - All parsing functions
- `verify.schema` - Schema validation logic
- String/regex operations

**Medium Priority (60%+ coverage):**
- `verify.chain` - Chain client creation, error paths
- `verify.state` - Store initialization and actions

**Low Priority (optional):**
- `verify.core` rendering functions - Best tested as integration tests
- DOM event handlers - Better tested in browser automation

## Development Workflow (Currently)

1. Run `bun run dev` to start shadow-cljs watch
2. Browser auto-reloads on source changes
3. Test manually via browser UI
4. Use browser console for debugging

**When Testing Framework Added:**
```bash
bun test              # Run all unit tests
bun test --watch      # Watch mode
bun test --coverage   # Coverage report
bun run dev           # Concurrent dev + test watch
```

---

*Testing analysis: 2026-02-19*
