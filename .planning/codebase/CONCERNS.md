# Codebase Concerns

**Analysis Date:** 2026-02-19

## Security Issues

**XSS Vulnerability - Direct innerHTML with Controlled Content:**
- Issue: `core.cljs` uses `.innerHTML` to render verification results, which could be exploited if error messages or metadata contain unescaped HTML
- Files: `src/verify/core.cljs` (lines 14, 26-30, 48-119, 122-128)
- Impact: If agent metadata name field contains HTML/JS, it would be executed in the browser. Error messages from chain calls are concatenated directly into HTML.
- Fix approach: Replace `.innerHTML` with `.textContent` for dynamic content, or use a templating library that auto-escapes. For structured output, build DOM elements programmatically instead of string concatenation.

**Metadata Parsing Without Strict Validation:**
- Issue: Base64-encoded metadata from tokenURI is decoded and JSON.parsed without validation. The `agent-name` field is rendered directly in HTML.
- Files: `src/verify/chain.cljs` (lines 69-76), `src/verify/core.cljs` (line 114)
- Impact: Malicious or malformed metadata could execute code or break rendering. `decode-data-uri` silently returns nil on parse failure.
- Fix approach: Validate metadata object schema using zod before rendering. Sanitize the `name` field explicitly.

**No Input Length Limits:**
- Issue: User can paste arbitrary-length text into the textarea without validation
- Files: `src/verify/core.cljs` (line 226-229)
- Impact: Very large paste could cause performance degradation, memory spike, or DoS conditions. Parser processes entire input without bounds checks.
- Fix approach: Add max length validation in parse step. Reject or truncate content over ~500KB before processing.

## Performance Bottlenecks

**Synchronous Base64 Operations on Large Content:**
- Issue: `utf8->b64` and `b64->utf8` functions process strings character-by-character, creating intermediate arrays
- Files: `src/verify/core.cljs` (lines 166-179)
- Impact: For large content (>100KB), encoding/decoding URL hashes could be slow. `utf8->b64` uses `String.fromCharCode.apply()` which has argument limits (~65K args).
- Improvement path: For large content, use `btoa`/`atob` directly without intermediate Uint8Array if possible. Consider streaming or chunked encoding.

**Promise Chain in verify-all:**
- Issue: Multiple sequential promise chains (recover → read wallet/owner/uri → resolve) with no timeout mechanism
- Files: `src/verify/chain.cljs` (lines 78-125)
- Impact: If an RPC endpoint is slow or unresponsive, verification hangs indefinitely. User sees "Verifying..." spinner with no timeout.
- Improvement path: Wrap RPC calls with a timeout promise (e.g., 10s). Return error if RPC exceeds threshold.

**Parser Uses Multiple Regex Passes:**
- Issue: `parse-signed-content` scans lines multiple times with `find-signed-by-index`, `find-separator-before`, then extracts with 4 separate `re-find` calls
- Files: `src/verify/parse.cljs` (lines 40-75)
- Impact: Inefficient for large documents. Each pass is O(n) in line count.
- Improvement path: Single pass through lines, extracting all components at once with a more complex regex or state machine.

## Fragile Areas

**Parser Depends on Specific Format Order:**
- Files: `src/verify/parse.cljs` (lines 40-75)
- Why fragile: Parser expects exact order: content, ---, Signed by, sig, hash, ts. If lines are reordered, parse fails silently (returns nil). No helpful error message about what went wrong.
- Safe modification: Add intermediate logging/debugging output showing parse state. Consider returning error details instead of just nil.
- Test coverage: No test fixtures for malformed inputs (missing sig, reordered lines, extra whitespace variations).

**URL Hash Decoding with Silent Failures:**
- Files: `src/verify/core.cljs` (lines 183-216)
- Why fragile: If base64 decode fails, JSON parse fails, or fields are missing, the entire catch block returns nil. User sees no error, goes to blank state.
- Safe modification: Add specific error handling for decode vs. parse vs. missing field cases. Return error details.
- Test coverage: No tests for malformed URL hashes, truncated base64, invalid JSON in URL.

**Hardcoded RPC Endpoints Without Fallback:**
- Files: `src/verify/chain.cljs` (lines 22-28)
- Why fragile: Only one RPC URL per chain. If the Arbitrum Foundation RPC is down, verification fails. No fallback or retry logic.
- Safe modification: Add array of RPC URLs per chain, implement fallback logic in `make-client`. Cache successful endpoints.
- Test coverage: No tests for RPC failure scenarios.

**Minimal ABI Could Silently Fail:**
- Files: `src/verify/chain.cljs` (lines 5-20)
- Why fragile: If the registry contract has a different ABI or missing functions, `readContract` calls fail silently. `tokenURI` is caught and nil is acceptable, but other reads are not.
- Safe modification: Add explicit error handling for each contract read with descriptive errors. Validate returned types.
- Test coverage: No tests against different contract implementations.

## Missing Error Context

**Generic Error Messages:**
- Issue: Most error handling converts exceptions to generic messages ("Verification failed", "Signature recovery failed")
- Files: `src/verify/core.cljs` (lines 151, 154), `src/verify/chain.cljs` (lines 121-125)
- Impact: User cannot diagnose issues. Is it network? Contract? Invalid input? Unclear.
- Fix approach: Preserve error details in state, show more specific messages (e.g., "RPC endpoint unreachable", "Agent not found in registry", "Invalid signature format").

**Silent Failures in Chain Reads:**
- Issue: `Promise.all` in `verify-all` catches only the tokenURI failure explicitly. Other failures would crash the promise chain.
- Files: `src/verify/chain.cljs` (lines 90-94)
- Impact: If `read-agent-wallet` fails, the whole verification crashes without a user-facing error message.
- Fix approach: Add explicit `.catch` handlers for each promise, returning error objects instead of throwing.

## Data Validation Gaps

**Signature Format Not Strictly Validated Before Calling viem:**
- Issue: Zod schema checks for hex pattern, but doesn't validate signature length (65 bytes = 130 hex chars)
- Files: `src/verify/schema.cljs` (lines 4-14)
- Impact: Invalid signatures are sent to `recoverMessageAddress`, which fails with vague error.
- Fix approach: Add explicit length validation for sig/hash fields in schema.

**Chain ID Validation Only Checks Support:**
- Issue: Parser checks `parse-full-id` returns non-nil (found in regex), but doesn't validate the chain ID is in the supported list
- Files: `src/verify/parse.cljs` (lines 12-18), `src/verify/chain.cljs` (lines 83-86)
- Impact: User can paste a valid eip155 string with unsupported chain, gets error only at verification time.
- Fix approach: Validate chain ID against supported chains in schema or parse stage.

## Browser Compatibility

**Relies on Modern Browser APIs:**
- Issue: Code uses `Clipboard API` (clipboard.writeText), `TextEncoder`/`TextDecoder`, `Promise`, `Uint8Array`
- Files: `src/verify/core.cljs` (lines 277, 169, 175)
- Impact: IE11 and older browsers will fail silently. No graceful degradation.
- Fix approach: Add feature detection and polyfills, or document minimum browser requirements.

## Rate Limiting & CORS

**Public RPC Endpoints Lack Rate Limit Handling:**
- Issue: Using public Arbitrum Foundation RPC endpoints without API key. These have rate limits.
- Files: `src/verify/chain.cljs` (lines 23, 26)
- Impact: High-traffic site could hit rate limits, verification fails for all users until limit resets.
- Fix approach: Implement backoff/retry logic, add fallback endpoints, document rate limit expectations.

**No CORS Handling:**
- Issue: RPC calls are from browser directly to public endpoints. No proxy or CORS headers mentioned.
- Files: `src/verify/chain.cljs` (viem HTTP transport)
- Impact: If CORS headers aren't present, browser blocks requests. Silent failure possible.
- Fix approach: Test CORS with target RPC endpoints. Consider proxy if needed.

## Testing & Quality

**No Test Coverage:**
- Issue: Zero unit or integration tests in codebase
- Impact: No regression detection, no validation of critical paths (signature recovery, contract reads, parsing)
- Fix approach: Add test suite for parse/schema/chain modules, mock viem and contract responses.

**No Integration Tests Against Testnet:**
- Issue: No automated tests against Arbitrum Sepolia or Arbitrum mainnet
- Impact: Breaking changes in contract interface or RPC behavior discovered only by users
- Fix approach: Add CI/CD pipeline running against Sepolia, validate contract interactions.

## Code Quality

**HTML String Building via Concatenation:**
- Files: `src/verify/core.cljs` (lines 45-119)
- Issue: Large HTML template built by concatenating strings, hard to maintain and easy to break
- Fix approach: Use a template DSL or HTML builder library. Alternatively, factor HTML chunks into helper functions.

**State Machine Not Formalized:**
- Files: `src/verify/state.cljs` (lines 4-15)
- Issue: State transitions ("idle" → "verifying" → "verified" | "error") are implicit in function calls, no validation of valid transitions
- Fix approach: Add state validation, prevent invalid transitions (e.g., can't transition to "idle" from "error" directly).

**No Logging:**
- Issue: Errors and state changes are not logged. User has no debug trail.
- Fix approach: Add structured logging for verification steps, errors, and state changes. Store logs for user diagnostics.

## Dependencies at Risk

**viem Version Constraint is Loose:**
- Issue: `package.json` pins `"viem": "^2.45.1"`, but no lock on major version
- Impact: Major version bump could introduce breaking changes to `recoverMessageAddress` or `createPublicClient` API
- Fix approach: Pin to exact minor version or use a narrower range (`~2.45.1`). Test against specific versions.

**zod Validation Only at Runtime:**
- Issue: Zod schema is evaluated at runtime, no compile-time guarantees
- Impact: If schema drifts from actual data shape, errors only caught at runtime
- Fix approach: Use Zod inference to type-check downstream code, ensure schema stays in sync with usage.

---

*Concerns audit: 2026-02-19*
