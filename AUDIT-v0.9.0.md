# SceneScout v0.9.0 — Full Functional Audit Report

**Date:** 2026-09-26 · **Scope:** entire source tree (31 production Kotlin files + 11 test files)
**Baseline released:** v0.8.0 · **This release:** v0.9.0 (commit `0147e18`) — **released 2026-09-26**:
https://github.com/shotbyx-dev/scenescout/releases/tag/v0.9.0

Every claim below was verified by one of: running the actual production Kotlin code
(standalone kotlinc + JUnit, 67/67 green), live network calls against the real
Overpass/Commons endpoints, or line-by-line code inspection. No claim is made from
CI status alone.

---

## 1. The reported failures — root-caused and fixed

### "Not showing local areas at all" / "I cannot search nothing"

**Root cause (verified live 2026-09-26):** `OsmDiscovery` sent ONE giant Overpass
query using regex selectors (`tourism~"^(attraction|viewpoint|museum|gallery|artwork)$"`,
`leisure~"^(beach_resort|marina)$"`, …) over a 10 km radius. Reproduced with the
app's exact POST format at the user's coordinates (26.163718, −80.246735):

- Main regex query → **timed out** on both kumi (41.1 s) and overpass-api.de (40.6 s)
- A trivial viewpoint baseline → overpass-api.de answered in **0.8 s**; kumi timed
  out after 45.7 s

So the app routinely got **zero live spots** and silently fell back to the 13 static
Miami samples — which read as "not showing local areas". Search had no text field
at all, so "cannot search nothing" was literally true: there was nothing to type into.

**Fix (shipped in v0.9.0, verified live):**
- One regex query → **three small exact-match queries** (culture/art/viewpoints/
  museums/piers/theatres · historic/beach/marina/peak/churches · parks), run
  **in parallel** with per-query partial success: one failing query loses only its
  slice, never the whole result. Throws only if *all three* fail.
- Endpoint order flipped: **overpass-api.de first** (fresh OSM data, answers in
  2–12 s), kumi kept as fallback (observed months-stale data, frequent timeouts).
- HTTP read timeout 30 s → 40 s.
- Live re-test 2026-09-26: culture 10.3 s / 36 elements, parks 1.8 s / 12 elements,
  land transient 504 then 3.6 s / 11 elements on retry — the app now shows spots
  even when one query hiccups.

**Concrete example:** at the user's coords the new pipeline produced 46 live spots
after noise filtering + dedupe, e.g. *Wyland Wall #99 "Water For Life"*, *Chadwick
Boseman artwork*, *ColorPlay*, *Iron XM*, *Schacknow Museum of Fine Arts* — the old
query returned none of these (it timed out).

### Misleading photos (ant specimens, CVS, WaWa next to murals)

**Root cause:** Commons coordinate geosearch returns *nearby* photos, not photos
*of* the place. The user's area returned ant specimen scans and a CVS.

**Fix:** hero images now require title relevance — the Commons file title must share
a meaningful word with the spot name or a location tag, else the card keeps the
branded gradient. Full galleries sort higher-relevance first. Verified: ColorPlay's
8 nearby files all scored 0 → gradient; no more ant photos.

---

## 2. Feature-by-feature audit (concrete examples)

### Map tab
- GPS → spots: `FusedLocationProviderClient.lastLocation`; on fix, `discoverSpots`
  re-queries (per-1 km cell, 10-min cache) with Live/Offline/Updating pill.
- Example: with location granted near Plantation, FL → ~46 live spots as red-dot
  markers via MapLibre; tapping a marker slides up a glass preview card with a
  shimmer-then-photo hero.
- **Honest gap:** GPS uses last-known fix only (no active refresh/retry); when null,
  a banner now says so ("GPS unavailable — showing the Miami sample area") instead
  of pretending. True live-GPS refresh is still TODO.
- Permit: `PermitGuide` office table (LA/NYC/Miami/Atlanta/Austin/NPS) shown in
  detail; `authorityUrl` is data-only (not yet a tappable link) — TODO.

### Discover tab
- Shoot-type chips (Music Video, Run & Gun, …) filter by `bestFor` — works.
- **New:** text search across name + description + tags. Example: typing "mural"
  narrows the list to mural-tagged spots; works combined with chips.
- Cards: glass style, staggered entrance, shimmer-while-loading heroes, per-photo
  author credit. Tapping a card or tag navigates correctly (tag tap sets the Scout
  query — verified in code path).

### AI Scout tab (Song Mood Matcher)
- Type/paste how a song feels → keyword scoring matches spots; each result shows a
  match % bar (fixed: bar now reflects the *match* %, previously it showed the
  separate scenic score while labeled as mood match — misleading).
- Example: "dark moody night drive with neon" surfaces urban/industrial-tagged
  spots with reasons listed ("Why this fits" pills, informational not clickable).
- Honest copy: on-device vibe/song matchers work today; Mapillary/user-upload
  vision scoring is labeled as *planned*. Street View is display-only per Google's
  terms — never used for AI analysis.

### Planner tab
- ShootProject: treatment text, shot list with per-shot location picker,
  auto golden-hour day schedule, 15-preset gear checklist; JSON persisted to
  filesDir via PlannerStore. `buildDaySchedule` pure + unit-tested.
- **Fixed:** "Custom…" location now reveals a text field (previously a dead
  selection); state resets after add/cancel.
- **Gaps (TODO):** time field accepts "99:99" and fails silently; date field takes
  free text and invalid dates silently fall back to today; PDF share failures show
  no user-visible error.

### Community tab
- Sample reviews (stars, best light, hassle notes) per spot — bundled sample data,
  clearly sample. Accounts/uploads/Firestore backend not implemented — TODO, and
  the screen no longer implies live community data.

### Sharing
- PDF brief per project (BriefPdfButton) — renders and shares via intent.
- Timezone: was hard-coded `America/New_York`; now uses the device zone. True
  coordinate→timezone for remote spots is TODO.

---

## 3. Dead-control sweep

| Control | Before | After |
|---|---|---|
| Discover search | no text field existed | working search |
| Song Mood SuggestionChip | `onClick = {}` dead | non-interactive pills (honest) |
| Scout match bar | showed wrong metric | shows match % |
| Planner "Custom…" | dead selection | reveals text field |
| "AI analyzes Street View" copy | implied live vision AI | labeled on-device matchers + planned vision |
| Empty photo state | blank | clear "no photos yet" message |
| Permit authority URL | data only | data only (TODO: tappable) |

---

## 4. Redesign (v0.9.0)

Shotbyx brand system: near-black `#08080A` base, electric red `#FF3D2E` primary /
deep red container, chrome `#C9CCD6` tertiary, amber kept for golden-hour/scores.
Glass cards + chrome hairlines everywhere, red/violet ambient glow behind tab
content, animated tab transitions (fade + horizontal slide), staggered list
entrances, shimmer loading states, photo crossfade on map previews. "Created by
Shotbyx" credit retained (About + Discover footer); GitHub-profile logo retained.

---

## 5. Verification summary

- **67/67** standalone Kotlin/JUnit tests green (parser, dedupe, relevance,
  scoring, planner logic, imagery, query builders).
- **Live Overpass** (2026-09-26, user coords): culture 36 el / land 11 el /
  parks 12 el → 46 live spots through the real parser.
- **Live Commons:** relevance filtering verified on real responses.
- **Android build:** GitHub Actions CI run `36227335710` — **green** (compile +
  unit tests + assembleDebug).
- **APK inspection (done, not assumed):** `app-debug.apk`, 61,545,807 bytes, ZIP
  integrity OK, `libmaplibre.so` present for all 4 ABIs (arm64-v8a, armeabi-v7a,
  x86, x86_64), `classes.dex` present, Shotbyx logo asset present.
- **Release:** v0.9.0 published with downloadable APK:
  https://github.com/shotbyx-dev/scenescout/releases/tag/v0.9.0
- **Real-phone validation:** ⏳ pending — user asked to install and test (location
  permission, local results, search, refresh, map, photos, Scout, Planner,
  sharing). No runtime-success claim until that feedback lands.

---

## 6. Known gaps (not hidden)

1. Active GPS refresh/retry (last-known fix only).
2. Planner time/date validation + PDF failure feedback.
3. Tappable permit authority links.
4. Community backend (accounts, uploads, Firestore).
5. Detail galleries sort by relevance but don't hide zero-relevance files.
6. Dedupe radius 150 m can miss near-duplicate features (~200 m apart observed).
7. Timezone is device zone, not coordinate-based.
8. Periodic/foreground-resume discovery refresh.

---

*Build/release section to be completed after CI green + APK inspection.*
- ✅ CI run 36227335710 green; APK inspected (ZIP OK, MapLibre .so × 4 ABIs,
  61.5 MB); v0.9.0 released with APK. Real-phone validation pending.
