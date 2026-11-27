# AGENTS.md

Instructions for any coding agent working in this repo. Claude Code reads this file through `CLAUDE.md`.
For where the work stands, why it is the way it is, and what comes next, read `HANDOFF.md`.

## What this is

An original, offline, ad-free Android trading game: a trader rides trains between six Indian
cities in the year 2000, buying where goods are cheap and selling where they are dear before the
days run out. Kotlin and Jetpack Compose. The release APK is sideloaded, not published to a store.

## Layout

- `engine/`: pure Kotlin, no Android code, all game rules. Every tunable number and table lives in
  `GameConstants.kt`. `Game.apply(state, action)` is the only place a move is checked and applied.
- `app/`: the Android UI. Every colour, size and text style lives in `UiConstants.kt`; every
  on-screen string lives in `res/values/strings.xml`.

## Rules

- The approved look is the paper-and-ink, map-first design (mockup direction "C"). Do not restyle
  it unless the user asks for a visual change; a request about the year 2000 means content
  (prices, units, place names), not looks.
- No numbers, colours, sizes or user-visible text outside the constants and strings files.
- Sizes in dp, text in sp. Every screen must pass `ScreenLayoutTest`: 8 screen sizes by 5 text
  scales, no clipped or split words, controls at least 48 dp, main action always reachable.
- Work test-first and test through the public seams: `Game.apply` for rules, `GameFlowTest` for
  screens. Prove a new check fails on broken code before trusting it.
- No permissions, no network, no ads, no analytics.
- Never commit `release.keystore` or `keystore.properties`.
- Work directly on `main`: no feature or prototype branches. Commit there and push there.

## Commands

Needs JDK 17 and the Android SDK (platform 36).

- `./gradlew test`: engine tests and UI tests.
- `./gradlew :app:assembleRelease`: release APK, signed only when the local keystore files exist.
- `python scripts/sync_agent_skills.py --check`: verify the vendored agent skills.

## Agent skills

Matt Pocock's skills are vendored, pinned to one upstream commit, in `.agents/skills/`, with
identical copies in `.claude/skills/`, `.devin/skills/` and `.windsurf/skills/`. Do not edit them
in place; update them with `scripts/sync_agent_skills.py`.

### Issue tracker

Issues and specs live in GitHub Issues on `purplevarun/druglord_android`, worked with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default roles, each label named after its role: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root, created when first needed. See `docs/agents/domain.md`.
