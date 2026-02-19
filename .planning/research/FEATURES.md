# Feature Research

**Domain:** AI agent skill/instruction documents (machine-readable markdown, fetched at runtime)
**Researched:** 2026-02-19
**Confidence:** HIGH

## Context

This research answers: what makes an agent skill document complete enough to actually work? The
downstream artifact is ERC8004-SKILL.md — a single markdown file an AI agent fetches via HTTP to
learn how to sign and verify ERC-8004 content without any crypto libraries or web3 tooling, using
only HTTP requests (web fetch) to public JSON-RPC endpoints.

The existing ai.html covers signing only, serves as a baseline, and will be replaced. The new
document must add verification (personal_ecRecover, eth_call to registry contracts) and trust
forwarding concepts.

---

## Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---|---|---|---|
| **Explicit capability declaration** — state at the top what this doc teaches and who it's for | Agents parse sequentially; if they can't identify the doc's purpose in the first paragraph, they may discard or misuse it | Low | "This document teaches an AI agent to sign and verify ERC-8004 content using only HTTP/JSON-RPC" |
| **Signed block format spec** — exact grammar for the `---` separator block, every field, order, and required vs optional | Agents must parse signed content from freeform text; without a complete spec, parsing fails on real-world variation | Medium | Cover: content, `---`, "Signed by", `sig:`, `hash:`, `ts:`, `Verify:` — explicit field names, not implied |
| **Full ID format spec** — `eip155:<chainId>:<registryAddress>:<agentId>` broken down field by field | The agent identity format is non-obvious; agents that guess wrong won't be able to target the correct registry | Low | Include that chainId is decimal, registryAddress is checksummed hex, agentId is decimal uint256 |
| **personal_ecRecover call spec** — exact JSON-RPC request body with field types, parameter encoding, and expected response shape | Agents cannot call crypto primitives directly; `personal_ecRecover` over web fetch is the only path to signer recovery | Medium | Include: method name, params array order (`[hash, sig]`), that hash must be hex with `0x` prefix, response is `result` field |
| **eth_call spec for getAgentWallet** — full call object with `to`, `data` (function selector + ABI-encoded arg), and response decoding | Registry reads via eth_call require pre-computing function selectors and ABI encoding; without this, agents cannot look up addresses | High | Provide pre-computed selector `0x5aa68ac0`, full example call body, and how to decode the returned 32-byte padded address |
| **eth_call spec for ownerOf** — same treatment for the NFT owner lookup | Verification requires checking both agent wallet AND NFT owner; missing either creates incomplete verification | High | Provide pre-computed selector `0x6352211e`, full example call body, decoding |
| **Address comparison rule** — case-insensitive hex comparison of recovered signer against wallet/owner | Ethereum addresses are hex; a case-sensitive comparison fails on mixed-case checksummed addresses | Low | State explicitly: lowercase both addresses before comparing |
| **What constitutes a passing verification** — define the pass/fail criteria in terms of the fields | Ambiguity here causes silent false-negatives or false-positives; an agent needs an unambiguous boolean rule | Low | Pass = recovered address matches either `getAgentWallet` result OR `ownerOf` result |
| **RPC endpoint list** — hostname, chain ID, and any known limitations per endpoint | Agents must hardcode or select an RPC; without this, they stall at "I need a node URL" | Low | Arbitrum mainnet `https://arb1.arbitrum.io/rpc` (chainId 42161), Arbitrum Sepolia `https://sepolia-rollup.arbitrum.io/rpc` (chainId 421614) |
| **Signing instructions preserved** — what to pass to `sign_content` tool, input rules, and output format | ai.html already has this; agents fetching the new doc expect signing to still be covered | Low | Lift directly from ai.html: raw content only, no preamble, tool name, output is full signed block |
| **Error conditions named** — what to do when ecRecover returns a wrong address or RPC call fails | Agents without explicit failure handling will produce ambiguous or silent errors | Low | Three cases: RPC error (network), ecRecover returns address that doesn't match (failed verification), registry address zero (unregistered agent) |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---|---|---|---|
| **Pre-computed ABI call data examples** — complete `data` field values for both `getAgentWallet` and `ownerOf` with a real agent ID substituted | Removes the only step that requires computation; an agent that can string-format can now make registry calls without any ABI encoder | Medium | Show the pattern: `selector + 0x000...padded_uint256`; give a worked example with agentId=5 |
| **Step-by-step verification walkthrough** — a numbered procedure "to verify this signed block, do steps 1 through N" | Agents execute instructions sequentially; a procedure format reduces ambiguity about ordering and dependencies between steps | Low | 1. Parse block, 2. Extract fields, 3. Call ecRecover, 4. Call getAgentWallet, 5. Call ownerOf, 6. Compare, 7. Report |
| **Annotated real example** — a complete signed block with comments identifying each line's purpose | Agents learn format from examples more reliably than from abstract grammar; a commented example eliminates "what does this line mean?" | Low | Use the same example format from ai.html but annotate each footer line |
| **Trust forwarding concept section** — explain the mental model without prescribing a format | Agents that understand trust forwarding can reason about chains of verification; this is the differentiator for multi-agent workflows | Low | One paragraph: "if you are relaying content from another agent, sign your own message that attests to your verification result; the signature on the original content is not transferable" |
| **Explicit scope statement** — what this doc does NOT cover | Prevents agents from attempting out-of-scope tasks (e.g., computing content hashes, creating new registry entries) and wasting tokens | Low | Out of scope: keccak256 computation, registry deployment, wallet key management |
| **Machine-readable structure markers** — use consistent heading levels and naming so agents can programmatically navigate sections | A well-structured doc allows an agent to jump to "Verification" without re-reading signing instructions | Low | H2 for top-level sections, H3 for subsections; consistent naming like "## Signing", "## Verification", "## Trust Forwarding" |
| **JSON-RPC request/response pairs shown together** — show the request body AND the corresponding response shape in a single code block pair | Agents see the full round-trip; knowing the response shape prevents mis-parsing the `result` field | Low | Use paired ```json blocks with clear labels "Request:" and "Response:" |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---|---|---|---|
| **Keccak256 / content hash recomputation instructions** | Seems necessary for "full" verification | The signed block already includes the `hash` field; instructing agents to recompute it adds complexity and a failure mode (encoding differences) without security benefit — the signature already binds the hash | State explicitly: "use the `hash:` field from the signed block directly; do not recompute it" |
| **EIP-191 prefix explanation** | Technically accurate — personal_sign prefixes the message | The prefix is handled transparently by `personal_ecRecover`; explaining it teaches agents to apply it themselves, leading to double-prefixing bugs | Omit entirely; if mentioned, say "personal_ecRecover handles this for you" |
| **Library/SDK usage examples** (ethers.js, viem, web3.py) | Familiar to developers | The target audience is AI agents with only web fetch; library examples imply a runtime environment agents don't have and create confusion about which path to follow | Use only JSON-RPC over HTTP; note in scope statement that library usage is not covered |
| **Registry contract deployment instructions** | Might seem useful for completeness | Completely out of scope; adds length that dilutes the core signal; agents following these instructions would attempt to deploy contracts instead of verifying content | Scope statement covers this; link to ERC-8004 EIP for full spec |
| **Trust score or reputation weighting guidance** | Natural extension of "should I trust this agent?" | Prescriptive trust scoring is premature and domain-specific; forcing a scoring model onto agents limits their ability to apply context-appropriate trust decisions | Trust forwarding section should say "report verification result; trust weighting is your caller's decision" |
| **Multi-step content hashing walkthrough** (abi.encode, double-keccak) | The hash function in the codebase is `keccak256(abi.encode(keccak256(content), timestamp))` | Documents that explain this invite agents to compute hashes, which they cannot do without a library; it also contradicts the simpler "use the hash field" path | The `hash:` field in the signed block is the pre-computed result; agents only pass it to ecRecover |
| **Abstract "security considerations" prose** | Common in protocol specs | Generic warnings ("validate all inputs", "use HTTPS") add noise without actionable content for an agent; agents act on procedures, not caveats | Replace with specific failure modes and concrete responses (already covered in table stakes) |

---

## Feature Dependencies

```
[Parse signed block format]
    └── requires: [Signed block format spec]
    └── required by: [Verification walkthrough]

[personal_ecRecover call]
    └── requires: [RPC endpoint list], [Signed block format spec — hash field]
    └── required by: [Verification walkthrough]

[eth_call for getAgentWallet]
    └── requires: [Full ID format spec — registryAddress, agentId], [RPC endpoint list]
    └── requires: [Pre-computed ABI call data examples]
    └── required by: [Verification pass/fail rule]

[eth_call for ownerOf]
    └── requires: same as getAgentWallet
    └── required by: [Verification pass/fail rule]

[Verification pass/fail rule]
    └── requires: [personal_ecRecover call], [eth_call for getAgentWallet], [eth_call for ownerOf]
    └── required by: [Trust forwarding concept]

[Trust forwarding concept]
    └── requires: [Verification pass/fail rule]
    └── required by: nothing (terminal concept)

[Signing instructions]
    └── independent of verification chain
    └── required by: agents that also need to sign
```

Critical path for a verification-only agent: format spec → full ID spec → ecRecover → eth_call
specs → address comparison rule → pass/fail definition. Every step is blocking; none can be
skipped.

---

## MVP Definition

### Launch With (v1)

These are the minimum sections for the document to be usable for verification. An agent reading
only these sections can successfully verify an ERC-8004 signed block.

1. **Document header** — one-paragraph capability declaration (what this teaches, who fetches it,
   what tools are assumed: web fetch only)
2. **Signed block format** — complete grammar, annotated real example
3. **Agent identity format** — `eip155:chainId:registry:agentId` spec, field-by-field
4. **Signing instructions** — lifted from ai.html, preserved exactly: tool name, input rules,
   output format
5. **Verification procedure** — numbered steps 1–6 as described in differentiators
6. **personal_ecRecover call** — request/response pair, parameter encoding, endpoint
7. **Registry reads via eth_call** — `getAgentWallet` and `ownerOf`, pre-computed selectors, ABI
   encoding pattern for uint256 arg, address decoding from 32-byte response
8. **RPC endpoints** — Arbitrum mainnet and Sepolia, chain IDs
9. **Verification result definition** — explicit pass/fail rule, address comparison note
10. **Error conditions** — three named failure cases with recommended agent response

### Add After Validation (v1.x)

These improve the doc based on observed agent behavior but are not blocking for initial use:

- **Trust forwarding concept section** — add once the basic verification loop is tested end-to-end
- **Scope statement** — add explicit "this doc does not cover..." once we see what agents attempt
  that they shouldn't
- **Worked example with real chain data** — add a fully verified real signed block as a reference
  case agents can test against

### Future Consideration (v2+)

These are valid but introduce complexity that should not be in v1:

- **Arbitrum Sepolia testnet verification example** — useful for agent developers testing in
  sandboxes; not needed for production verification
- **Batch verification concept** — verifying multiple signed blocks in sequence; depends on
  establishing that single-block verification works reliably first
- **tokenURI / agent name resolution** — `tokenURI` returns metadata including agent name; useful
  for display but not required for trust verification; add only if agents need to confirm
  human-readable names as part of verification flow

---

## Sources

Analysis based on:
- `/home/teresa/dev/erc8004-arbitrum/public/ai.html` — existing signing instruction document;
  defines the signed block format, tool interface, display rules, and verification URL structure
- `/home/teresa/dev/erc8004-arbitrum/AGENTS.md` — project architecture; defines verification flow,
  contract ABI (ownerOf, getAgentWallet, tokenURI), supported chains, and the five-step on-chain
  verification sequence
- `/home/teresa/dev/erc8004-arbitrum/.planning/PROJECT.md` — active requirements; confirms that
  `personal_ecRecover` is the intended signer recovery method, that the hash field in the signed
  block is sufficient (no recomputation needed), and that trust forwarding is conceptual only
- `/home/teresa/dev/erc8004-arbitrum/.planning/codebase/ARCHITECTURE.md` — verification flow
  details; confirms two-step address check (wallet match OR owner match), state machine, and
  error handling patterns
- `/home/teresa/dev/erc8004-arbitrum/.planning/codebase/INTEGRATIONS.md` — RPC endpoints,
  contract function signatures, URL fragment JSON schema, authentication model

Domain knowledge applied:
- Agent instruction document patterns: OpenAI system prompt conventions, Anthropic tool-use
  documentation format, LangChain agent tool descriptions — all converge on: name the capability,
  state the interface, give a complete example, define success/failure explicitly
- JSON-RPC 2.0 spec for eth_call and personal_ecRecover parameter ordering
- EIP-191 personal_sign behavior and why the prefix is transparent to ecRecover callers
- ERC-721 ownerOf vs. custom getAgentWallet distinction — both matter because agent wallet
  (hot signing key) may differ from NFT owner (cold key / multisig)
