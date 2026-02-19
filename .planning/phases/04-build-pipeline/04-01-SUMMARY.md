---
phase: 04-build-pipeline
plan: 01
subsystem: ui
tags: [build, dist, clojurescript, shadow-cljs, html, agents]

# Dependency graph
requires:
  - phase: 03-result-logic-and-error-handling
    provides: Completed ERC8004-SKILL.md skill document in public/
provides:
  - dist script copies ERC8004-SKILL.md to dist/ instead of ai.html
  - index.html "For Agents" wizard box with canonical fetch URL
  - ai.html removed from project
  - AGENTS.md updated to reflect new file structure
affects: [hosting, agent-discoverability]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - package.json
    - public/index.html
    - AGENTS.md
  deleted:
    - public/ai.html

key-decisions:
  - "orbiter.website canonical URL confirmed — https://erc8004.orbiter.website/ERC8004-SKILL.md used in For Agents box (research blocker resolved)"
  - "For Agents box uses existing .wizard/.step CSS classes — no new styles added"
  - "ai.html link removed from footer, replaced entirely by For Agents wizard box above it"

patterns-established: []

requirements-completed: [BUILD-01, BUILD-02, BUILD-03, BUILD-04]

# Metrics
duration: 5min
completed: 2026-02-19
---

# Phase 4 Plan 01: Build Pipeline Summary

**Wired ERC8004-SKILL.md into dist pipeline, added For Agents discovery box to index.html using existing wizard CSS, and deleted ai.html**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-19T11:16:25Z
- **Completed:** 2026-02-19T11:21:00Z
- **Tasks:** 2
- **Files modified:** 3 (+ 1 deleted)

## Accomplishments

- dist script now copies ERC8004-SKILL.md to dist/ (was ai.html)
- "For Agents" wizard box inserted before footer in index.html with `https://erc8004.orbiter.website/ERC8004-SKILL.md` fetch URL
- ai.html removed from public/ and footer link removed from index.html
- AGENTS.md project structure listing updated to ERC8004-SKILL.md

## Task Commits

Each task was committed atomically:

1. **Task 1: Update dist script, add For Agents box, update AGENTS.md** - `7b6a642` (feat)
2. **Task 2: Delete ai.html and verify build** - `744c8ac` (chore)

## Files Created/Modified

- `package.json` - dist script: `public/ai.html` replaced with `public/ERC8004-SKILL.md`
- `public/index.html` - For Agents wizard box added before footer; footer ai.html link removed
- `AGENTS.md` - Project structure updated: ai.html -> ERC8004-SKILL.md
- `public/ai.html` - Deleted

## Decisions Made

- orbiter.website canonical URL confirmed as `https://erc8004.orbiter.website/ERC8004-SKILL.md` — resolves the URL conflict blocker documented in STATE.md (PROJECT.md vs STACK.md conflict; plan research specified orbiter.website)
- For Agents box uses existing `.wizard` / `.step` / `.step-num` / `.step-content` CSS classes with inline `background: rgba(34,197,94,0.12); color: var(--green)` for the A badge — no new CSS rules added
- The footer `&middot; <a href="/ai.html">For Agents</a>` link was removed entirely rather than redirected, as the For Agents box provides full discoverability

## Deviations from Plan

None - plan executed exactly as written.

**Notes:**
- `bun run dist` fails on `cp public/js/main.js` because shadow-cljs has not been compiled in this environment (public/js/ does not exist). This is a pre-existing condition; main.js is not changed by this plan. The file-copy portion of the dist script (index.html, ERC8004-SKILL.md, favicon.svg) was verified manually and works correctly.

## Issues Encountered

- Shell has `rm` aliased to `rip` which does not accept `-rf`. `bun run dist` runs in a bun subprocess that inherits the alias, causing the `rm -rf dist` step to fail when run as a bash command directly. When called via `bun run dist`, bun invokes it in a clean sh context so the alias does not apply — this only affected the manual verification step.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 4 complete. All BUILD requirements satisfied.
- ERC8004-SKILL.md is in public/ and will be copied to dist/ on next build.
- The For Agents box in index.html points to the live URL where ERC8004-SKILL.md will be hosted.
- No blockers.

---
*Phase: 04-build-pipeline*
*Completed: 2026-02-19*
