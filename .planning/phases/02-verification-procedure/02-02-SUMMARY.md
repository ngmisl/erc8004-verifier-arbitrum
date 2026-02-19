---
phase: 02-verification-procedure
plan: 02
subsystem: documentation
tags: [skill-doc, json-rpc, eth_call, abi-encoding, arbitrum, verification, erc8004]

# Dependency graph
requires:
  - phase: 02-verification-procedure
    plan: 01
    provides: Verification Procedure intro, Step 1 (extract fields), Step 2 (personal_ecRecover)
provides:
  - "### Step 3: Read Agent Wallet (eth_call)" with selector 0x00339509, template, worked example
  - "### Step 4: Read NFT Owner (eth_call)" with selector 0x6352211e, template, worked example
  - "### RPC Endpoints" table (chainId 42161 and 421614 with RPC URLs)
  - ABI encoding rule (selector + agentId zero-padded to 64 hex chars)
  - Complete ## Verification Procedure section (Steps 1-4 + RPC Endpoints)
affects: [phase-03]

# Tech tracking
tech-stack:
  added: []
  patterns: [full-independent-template-per-function (no DRY for agent-facing specs), ABI-encoding-note-before-eth_call-steps]

key-files:
  created: []
  modified: [public/ERC8004-SKILL.md]

key-decisions:
  - "Each eth_call function (getAgentWallet, ownerOf) gets its own full independent template and worked example — not DRYed — per locked decision that agent-facing specs must be copy-pasteable without context"
  - "ABI encoding rule stated once before Step 3 (not repeated in Step 4) — applies to both eth_call steps that follow"
  - "Intentional-duplication note added to RPC Endpoints table explaining why it mirrors Supported Chains from Agent Identity"
  - "getAgentWallet selector 0x00339509 is QA-01 resolved value (computed via viem getFunctionSelector in Phase 2 research)"

patterns-established:
  - "Full-independent-template pattern: each eth_call function has its own template + worked example even when the pattern is identical — prevents agents from needing to reference other steps"
  - "Intentional-duplication note: when content is deliberately repeated for agent convenience, add a blockquote explaining the intent"

requirements-completed: [VFY-03, VFY-04, VFY-05, VFY-06, VFY-09]

# Metrics
duration: 1min
completed: 2026-02-19
---

# Phase 2 Plan 02: Steps 3-4 (eth_call) and RPC Endpoints Summary

**eth_call specs for getAgentWallet (selector 0x00339509) and ownerOf (selector 0x6352211e) with full independent templates, worked examples, and RPC endpoint table — completing the Verification Procedure section**

## Performance

- **Duration:** 1 min
- **Started:** 2026-02-19T10:39:39Z
- **Completed:** 2026-02-19T10:40:26Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Added ABI encoding rule explaining selector + zero-padded agentId call data construction
- Added Step 3 (getAgentWallet eth_call) with selector 0x00339509, full independent template, worked example (agentId=5), synthetic response, and disclaimer blockquote
- Added Step 4 (ownerOf eth_call) with selector 0x6352211e, full independent template, worked example (agentId=5), synthetic response, and disclaimer blockquote
- Added RPC Endpoints table (42161 Arbitrum One, 421614 Arbitrum Sepolia) with intentional-duplication note
- Removed `<!-- Phase 2 continued -->` marker; `## Result` and Phase 3 sections left intact
- The complete Verification Procedure (Steps 1-4 + RPC Endpoints) is now in ERC8004-SKILL.md

## Task Commits

Each task was committed atomically:

1. **Task 1: Write Step 3 (getAgentWallet eth_call), Step 4 (ownerOf eth_call), and RPC Endpoints** - `9ef862e` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `/home/teresa/dev/erc8004-arbitrum/public/ERC8004-SKILL.md` - Added Steps 3-4 (eth_call with templates and worked examples) and RPC Endpoints subsection to Verification Procedure

## Decisions Made
- Each eth_call function has its own full independent template and worked example — per the locked decision that agent-facing specs must be copy-pasteable without needing to reference sibling steps. This is intentionally not DRYed.
- ABI encoding rule stated once before Step 3 (not duplicated in Step 4) since it applies to both steps that immediately follow.
- Intentional-duplication note added to RPC Endpoints blockquote to explain why the table mirrors the Supported Chains table from Agent Identity.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- The complete Verification Procedure section (Steps 1-4 + RPC Endpoints) is written and in ERC8004-SKILL.md
- Phase 2 is complete
- Phase 3 can now fill in the `## Result`, `## Error Conditions`, and `## Trust Forwarding` sections (currently `<!-- Phase 3 -->` placeholders)

## Self-Check: PASSED

- public/ERC8004-SKILL.md: FOUND
- 02-02-SUMMARY.md: FOUND
- commit 9ef862e: FOUND
- Step 3 heading: FOUND
- Step 4 heading: FOUND
- 0x00339509 selector: FOUND
- 0x6352211e selector: FOUND
- RPC Endpoints table with 421614: FOUND
- Phase 2 continued marker: GONE
- Result heading: FOUND

---
*Phase: 02-verification-procedure*
*Completed: 2026-02-19*
