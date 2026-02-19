# Phase 4: Build Pipeline - Research

**Researched:** 2026-02-19
**Domain:** Static build pipeline, HTML editing, file management (bun scripts)
**Confidence:** HIGH

## Summary

Phase 4 is entirely local file operations — no new libraries, no new runtime code, no ClojureScript changes. The work is: update one shell script in `package.json`, edit one HTML file, delete one file, and confirm the correct hosting URL for the "For Agents" box. All four requirements (BUILD-01 through BUILD-04) can be accomplished in a single plan.

The hosting URL conflict documented in STATE.md is resolvable from existing evidence. README.md, index.html, and ERC8004-SKILL.md all reference `https://erc8004.orbiter.website` as the live site. The URL `http://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md` from PROJECT.md appears to be an early draft that was never finalized. The site-consistent URL for the skill doc is `https://erc8004.orbiter.website/ERC8004-SKILL.md`.

No research gaps or open questions block execution. Phase 4 is a mechanical wiring task. The only judgment call is the "For Agents" box design — it must follow the established index.html CSS patterns (dark monospace UI, `--surface` background, `--border` borders, `--blue` accent) without introducing new styles.

**Primary recommendation:** Execute all four BUILD requirements in one plan: fix the `dist` script, update the footer link, add the "For Agents" box, and delete ai.html.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| BUILD-01 | Replace ai.html with ERC8004-SKILL.md in public/ directory | ERC8004-SKILL.md already exists in public/ (confirmed by `ls public/`). ai.html also exists in public/. BUILD-01 means ai.html is removed from public/ — ERC8004-SKILL.md is already in place, so no copy is needed, only deletion. |
| BUILD-02 | Update dist script in package.json to copy ERC8004-SKILL.md instead of ai.html | Current `dist` script: `rm -rf dist && mkdir -p dist/js && cp public/index.html public/ai.html public/favicon.svg dist/ && cp public/js/main.js dist/js/`. Change `public/ai.html` to `public/ERC8004-SKILL.md` in the cp command. |
| BUILD-03 | Add "For Agents" box to index.html with fetch URL for the skill doc | index.html footer currently has `<a href="/ai.html">For Agents</a>`. This must become a visible box (not just a footer link) with the fetch URL `https://erc8004.orbiter.website/ERC8004-SKILL.md`. |
| BUILD-04 | Remove ai.html from the project | Delete `public/ai.html`. After BUILD-02 fixes the dist script, `dist/ai.html` will no longer be produced by future builds. No dist/ cleanup needed in the plan since dist/ is not committed. |
</phase_requirements>

## Standard Stack

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| bun | 1.x | Package manager and script runner | Already the project's package manager |
| shell (bun scripts) | — | File copy/delete in dist script | Existing pattern in package.json |

### Supporting
None. This phase introduces no new dependencies.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Inline shell in package.json scripts | Separate build script file | Overkill for 2-3 commands; keep it inline |

**Installation:** No new packages needed.

## Architecture Patterns

### Current Build Pipeline
```
bun run build
  └── bunx shadow-cljs release app   # ClojureScript → public/js/main.js
  └── bun run dist                    # Copy assets to dist/

bun run dist (current)
  rm -rf dist
  mkdir -p dist/js
  cp public/index.html public/ai.html public/favicon.svg dist/
  cp public/js/main.js dist/js/
```

### Target Build Pipeline
```
bun run dist (after BUILD-02)
  rm -rf dist
  mkdir -p dist/js
  cp public/index.html public/ERC8004-SKILL.md public/favicon.svg dist/
  cp public/js/main.js dist/js/
```

### Pattern: Single cp Command With Multiple Files
**What:** The current dist script uses a single `cp` command listing multiple source files. Keep this pattern — add `ERC8004-SKILL.md`, remove `ai.html`.
**When to use:** Always — matches existing convention.
**Example:**
```bash
# Source: current package.json dist script
cp public/index.html public/ERC8004-SKILL.md public/favicon.svg dist/
```

### Pattern: "For Agents" Box as a `.wizard` Section
**What:** index.html uses a `.wizard` div pattern for the "How it works" section — a bordered container with a heading and structured content, separated from the main verification UI by a `border-top`. The "For Agents" box should follow the same visual rhythm.
**When to use:** Adding a discovery section below the main UI.
**Example:**
```html
<!-- Follows the existing .wizard section pattern in index.html -->
<div class="for-agents" style="margin-top: 2.5rem; padding-top: 1.5rem; border-top: 1px solid var(--border);">
  <h2 style="font-size: 0.85rem; font-weight: 500; color: var(--muted); margin-bottom: 0.75rem;">For Agents</h2>
  <p style="font-size: 0.75rem; color: var(--muted); line-height: 1.6;">
    Skill document: fetch <code>https://erc8004.orbiter.website/ERC8004-SKILL.md</code>
  </p>
</div>
```

**Why inline styles are acceptable here:** The existing `.wizard` class already has dedicated CSS. Adding a new class to the `<style>` block is also fine — both approaches match the existing pattern of self-contained styles in index.html.

### Anti-Patterns to Avoid
- **Wrapping the URL in an anchor that navigates to ai.html:** ai.html is being deleted; the "For Agents" link must point to `ERC8004-SKILL.md` (either the fetch URL or a path).
- **Removing the footer "For Agents" link without replacing with the box:** BUILD-03 requires a visible box, not just a footer link update.
- **Copying ai.html to dist/ and also ERC8004-SKILL.md:** The dist script should copy one or the other, not both.
- **Deleting ai.html before updating the dist script:** Order matters — fix the dist script first so `bun run build` won't fail trying to copy a missing file.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Markdown serving | Custom HTTP handler | Static file hosting as-is | ERC8004-SKILL.md served as `text/markdown` or `text/plain` by the host — agents fetch it directly, no transformation needed |
| File existence check | Script validation | Trust the build | bun/shell will error loudly if cp source doesn't exist |

**Key insight:** A `.md` file served from static hosting responds with the file's raw content. Agents fetching `https://erc8004.orbiter.website/ERC8004-SKILL.md` will receive the markdown text directly — no HTML wrapper, no server-side processing needed. Static hosting already satisfies SUCCESS CRITERIA 4.

## Common Pitfalls

### Pitfall 1: Stale dist/ Directory After Build Script Change
**What goes wrong:** Developer runs `bun run build` after fixing the script, but a stale `dist/ai.html` from a previous build still exists and is served locally.
**Why it happens:** The `rm -rf dist` at the start of the dist script clears it, so this should not be a problem — but only if the developer runs the full `bun run build` (not `bun run dist` against an existing dist/).
**How to avoid:** Verify by running `bun run build` from scratch and checking `ls dist/` — should show `index.html`, `ERC8004-SKILL.md`, `favicon.svg`, `js/`.
**Warning signs:** `dist/ai.html` still present after build.

### Pitfall 2: "For Agents" Box URL Points to /ai.html
**What goes wrong:** Developer updates the footer link href to `/ERC8004-SKILL.md` but the visible box text or anchor still says `/ai.html`.
**Why it happens:** The footer currently says `<a href="/ai.html">For Agents</a>`. If only the footer is updated without adding the prominent box, BUILD-03 is not satisfied (requires a visible box, not a footer link).
**How to avoid:** Ensure the box is a distinct section above the footer, not just a footer link. The footer link can be updated or removed.
**Warning signs:** Only a footer link exists; no visible `For Agents` section in the page body.

### Pitfall 3: Hosting URL Mismatch in "For Agents" Box
**What goes wrong:** Box shows `http://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md` (from PROJECT.md) instead of the actual live site URL.
**Why it happens:** PROJECT.md contains an early-draft URL that was never finalized.
**How to avoid:** Use `https://erc8004.orbiter.website/ERC8004-SKILL.md`. Evidence: README.md confirms the live site is `erc8004.orbiter.website`. The verify link in index.html examples and ERC8004-SKILL.md itself use `https://erc8004.orbiter.website`.
**Warning signs:** URL in box differs from the URL shown in `Verify:` links and the README.

### Pitfall 4: Content-Type for .md Files
**What goes wrong:** The static host serves `ERC8004-SKILL.md` with `Content-Type: text/html`, causing browsers/agents to misinterpret the content.
**Why it happens:** Some static hosts guess content type from file extension. `.md` is non-standard — host may serve as `text/plain` or `application/octet-stream`.
**How to avoid:** This is acceptable for the agent use case — agents fetch the raw text regardless of Content-Type. The success criterion is "receives the markdown content, not an HTML wrapper," which is satisfied by any non-HTML content type. No special configuration needed.
**Warning signs:** Host wraps the markdown in an HTML page (not the same as an unfamiliar Content-Type header).

## Code Examples

### BUILD-02: Updated dist Script
```bash
# In package.json "scripts"
"dist": "rm -rf dist && mkdir -p dist/js && cp public/index.html public/ERC8004-SKILL.md public/favicon.svg dist/ && cp public/js/main.js dist/js/"
```

### BUILD-03: "For Agents" Box HTML
Insert above the `<div class="footer">` in index.html:
```html
  <div class="wizard" style="margin-top: 1.5rem;">
    <h2>For Agents</h2>
    <div class="steps">
      <div class="step">
        <div class="step-num" style="background: rgba(34,197,94,0.12); color: var(--green);">A</div>
        <div class="step-content">
          <div class="step-title">Skill document</div>
          <div class="step-desc">
            Fetch <code>https://erc8004.orbiter.website/ERC8004-SKILL.md</code> to learn
            how to sign and verify ERC-8004 content using only JSON-RPC web fetch calls.
          </div>
        </div>
      </div>
    </div>
  </div>
```

Note: Uses existing `.wizard`, `.steps`, `.step`, `.step-num`, `.step-content`, `.step-title`, `.step-desc`, `code` CSS classes — no new styles needed.

### BUILD-04: Footer Link Update
Current footer contains `<a href="/ai.html">For Agents</a>`. After BUILD-03 adds the visible box, this footer link should be removed or updated to `/ERC8004-SKILL.md`:
```html
<!-- Remove this line from the footer: -->
&middot; <a href="/ai.html">For Agents</a>
```
Since the visible box now serves as the "For Agents" discovery entry point, the footer link is redundant. Removing it is cleaner than updating it.

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| ai.html (HTML agent instructions) | ERC8004-SKILL.md (markdown) | Agents fetch markdown natively; no HTML parsing needed |
| Footer-only "For Agents" link | Visible "For Agents" box + footer | Prominent discovery; survives layout changes |

**Deprecated/outdated:**
- `public/ai.html`: Replaced by `public/ERC8004-SKILL.md` (already exists)
- `dist` script copying `ai.html`: Replace with `ERC8004-SKILL.md`

## Open Questions

1. **Hosting URL Conflict**
   - What we know: PROJECT.md says `http://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md`; STACK.md / README.md / index.html / ERC8004-SKILL.md all reference `https://erc8004.orbiter.website`
   - What's unclear: Whether `qstorage.quilibrium.com` was an intended alternate host that is still live
   - Recommendation: Use `https://erc8004.orbiter.website/ERC8004-SKILL.md`. All in-project evidence points to orbiter.website as the canonical live host. The qstorage URL is an early draft artifact. If the user has a specific reason to use the qstorage URL, they should override this before the plan executes.

## Sources

### Primary (HIGH confidence)
- Direct file inspection: `package.json` — current dist script confirmed
- Direct file inspection: `public/index.html` — current HTML structure, CSS classes, footer confirmed
- Direct file inspection: `public/ERC8004-SKILL.md` — file exists in public/, content confirmed
- Direct file inspection: `public/ai.html` — file exists in public/, confirmed for deletion
- Direct file inspection: `README.md` — `https://erc8004.orbiter.website` confirmed as live site

### Secondary (MEDIUM confidence)
- `.planning/STATE.md` — URL conflict documented; both candidate URLs noted
- `.planning/PROJECT.md` — qstorage URL; contradicted by all other project files
- `.planning/ROADMAP.md` — Phase 4 plan structure: single plan (04-01) for all four BUILD requirements

### Tertiary (LOW confidence)
- No web searches performed — this phase requires no external library research

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; existing bun scripts pattern
- Architecture: HIGH — directly read from package.json and index.html
- Pitfalls: HIGH — derived from current file state and URL conflict evidence
- URL recommendation: MEDIUM — inferred from convergent project evidence, not confirmed by live HTTP request

**Research date:** 2026-02-19
**Valid until:** 2026-03-21 (stable; file paths won't change)
