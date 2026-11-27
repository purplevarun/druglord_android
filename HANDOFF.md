# Handoff

Context for the next agent or session picking up this repo. Read `AGENTS.md` first: it holds the
rules. This file holds what is not written down anywhere else: why things are the way they are,
the local machine, known gaps, and the plan. Last updated 27 Nov 2026, at commit `a2ddbd4`.

## The goal

Build a small, ad-free, offline Android game to sideload: an **original** trading game in the
"buy low, sell high before the days run out" genre. It was inspired by the PC game Drug Lord 2,
but it is not a clone: none of that game's name, characters, art, look or content is reused,
and the theme is not about drugs.

## Decisions the user has made, and why

| Decision | Detail |
|---|---|
| Theme | A travelling trader riding trains between Indian cities, set in the year 2000. Chosen to be India-friendly and not about drugs. |
| Look | Mockup direction "C": paper and ink, map-first, serif headings. **Locked.** A grey Windows-98 restyle was tried and the user rejected it strongly. "Make it look like 2000" meant content only. |
| Era | Year-2000 prices, trade units, dates and place names (Calcutta, not Kolkata; Mumbai, Chennai, Bangalore as then). |
| Cities | Six: Mumbai, Delhi, Calcutta, Srinagar, Bangalore, Hyderabad. The user removed Jaipur, Kochi, Chennai and Varanasi. |
| Travel | A trip costs one day per rail link (the user did not object). Links are in `GameConstants.RAIL_LINKS`. |
| Font | Gelasio is bundled (user approved), a Georgia-compatible serif. Small text uses the phone's sans-serif. |
| Layout | Must survive every phone size and text-zoom setting, with good spacing. Enforced by `ScreenLayoutTest`. |
| Constants | Every number, colour, size and string in one place (user request). See `AGENTS.md`. |
| Distribution | Sideload only. Signed with a local release key, made the way the user's `coins` repo does it. No store. |
| Size | Small. The release APK is 0.91 MB. |
| Names | All names are placeholders, including the app title "Train Trader". The user said "we'll discuss nomenclature later". |
| Repo | `purplevarun/druglord_android`, named "for now". Public, because GitHub Actions would not run on the private repo (account billing block). |
| Branches | Work only on `main`. All other branches were deleted at the user's request. |
| Process | Use Matt Pocock's skills (vendored in `.agents/skills/`) and Lauren Tan's pstack (not yet added; see below). |

## Where things stand

A whole run is playable: a market per city with − / + to sell or buy one, train trips that use
up days, a way to end the run, an end screen with a rank, and a best-runs list. The run and the
list survive the app being closed. Details are in the commit history (`git log`); the main pieces:

- `engine/`: `RunState` (the whole run, plus `isEndless`, `isOnLastDay`, `rank`), `Game.apply`
  (the rules), `Market` (prices derived from the run's seed, never stored), `RailNetwork`
  (routes), `GameCalendar` (day to date), `GameConstants.RANK_THRESHOLDS`.
- `app/`: `GameScreen` (holds `Phase`: `NewRun` / `Playing` / `Ended`), `HomeScreen`, `RailMap`,
  `MarketScreen`, `TravelSheet`, `EndRunSheet` (the end/retire confirmation), `NewRunScreen`
  (30/60/90/endless + best runs), `EndScreen`, `RunStore` (SharedPreferences, hand-rolled
  versioned format; `MAX_BEST_RUNS` = 5).
- Tests: 39 engine tests (including a 20,000-move simulation) and 1,307 app tests (120 more are
  skipped, as designed). `./gradlew test` takes under a minute on this Mac.

### Decisions made for slice 1 (27 Nov 2026)

- Retire-anytime "End the run" lives in the header; a prominent "Finish the run" button appears
  pinned above the market drawer on the last day of finite runs. Both confirm through
  `EndRunSheet`. Endless runs only get the retire button.
- Score = final net worth (goods liquidated at the last day's prices, minus debt). User's call.
- Endless is a real option: `ENDLESS_RUN_LENGTH_DAYS = Int.MAX_VALUE`; header shows
  "Day N · endless" and no progress bar.
- Ranks are a placeholder Hindi ladder: Divala < 0, Thelawala ≥ 0, Dukandaar ≥ ₹25k,
  Saudagar ≥ ₹1L, Seth ≥ ₹10L, Crorepati ≥ ₹1Cr. Thresholds are guesses; tune in the balance pass.
- App opens on the new-run screen when no run is saved, resumes mid-run when one is.

## Known gaps and open points

- **Prices:** five of the eight base prices are estimates with no year-2000 source: onions,
  basmati, chai, silk sarees, kesar. Gold, cotton and masala (black pepper) are sourced. Each is
  flagged `SOURCED` or `ESTIMATE` in `GameConstants.GOODS`.
- **Tuning I chose, never confirmed by the user:** ₹10,000 starting cash, ₹5,000 debt, 100
  luggage slots with every unit taking one slot, the city price table and each good's volatility.
- **Debt never grows yet.** It waits for the sahukar slice.
- **From the mockups, not built:** the exact-amount keypad, the "movers" ticker, travel rumours,
  each city's speciality line.
- **Saved-state format is v1.** `RunStore` checks `FORMAT_VERSION`; a format change needs a bump
  and (ideally) a migration, or saved runs silently reset.
- **Android 7 (API 24-25):** bold Gelasio renders as regular, because those versions cannot pick
  a weight from a variable font. Never tested on a real old phone.
- **Agent skills:** never checked loading in Copilot, Devin or Windsurf, only that the files are
  in each agent's folder. Some agents may need skills switched on in their settings.
- **pstack is not added.** Its Claude Code version lives in `cursor/plugins` under
  `harness/pstack/`. Check its licence before copying it into this repo the way
  `scripts/sync_agent_skills.py` copies the Pocock skills.
- **The keystore has one copy.** `release.keystore` and `keystore.properties` exist only at the
  repo root on this Mac (gitignored). The user was told to back them up and has not
  confirmed it. If they are lost, installed copies must be uninstalled before an update.

## The plan

Each item is a vertical slice: engine test-first, then screens, then the size and zoom tests,
then a check on the emulator, then commit and push to `main`.

1. ~~End of run and saving.~~ **Done** in `a2ddbd4` (issue #1).
2. **Sahukar and bank.** A moneylender with daily interest (about 8% a day in the mockup), a bank
   paying about 1% a day that is safe from pickpockets, and transport upgrades
   (jhola, trunk, tempo, truck) that raise luggage capacity.
3. **Random events.** A checkpost (pay, talk your way through, or take the back road for a day),
   pickpockets on the train, late trains, bandhs, and price spikes or crashes (monsoon, bumper
   harvest, Diwali, wedding season). Also market saturation: dumping a lot in one city pushes
   its price down for a few days.
4. **Godown:** a store in each city.
5. **Exact amounts:** a keypad on long press, and a max button.
6. **The rest of the mockup:** the movers ticker, rumours and city specialities.
7. **Naming pass with the user:** the game title, goods, ranks and the repo name.
8. **Balance pass:** use the simulation to tune prices and risk, and verify the estimated prices.
9. **Releases:** version numbering and a script that builds the signed APK and attaches it to a
   GitHub release.

## This machine

This is now a **Mac** (`/Users/ritikasaxena/Desktop/Varun/druglord_android`), not the Windows PC
the earlier notes describe.

- **JDK 17:** `~/Library/Java/JavaVirtualMachines/ms-17.0.20.1` — Gradle's `jvmToolchain(17)`
  finds it on its own; `JAVA_HOME` does not need exporting. (JDK 21 is the shell default.)
- **Android SDK:** `~/Library/Android/sdk` (platform 36, build-tools 35 + 36). `local.properties`
  with `sdk.dir` was missing and has been created — it is gitignored, so recreate it on any
  fresh clone. `adb`/`emulator` come from Homebrew, not the SDK dir.
- **Emulator:** `Local_AVD`, shows as `emulator-5554`, 1080x2400. Start it with
  `emulator -avd Local_AVD`. To screenshot: `adb exec-out screencap -p > shot.png`.
  To tap through the UI: `adb shell uiautomator dump /sdcard/ui.xml` for bounds, then
  `adb shell input tap x y`. After changing font scale or rotation for a check, set them back.
- **Build stack:** Gradle 9.3.1, Android Gradle plugin 8.13.1, Kotlin 2.1.20, Compose BOM
  2025.05.00, Robolectric 4.15.1.
- **GitHub:** `gh` is signed in as `purplevarun`. Issue tracker is live: #1 was this slice.

## Gotchas already hit

- **`hasVisualOverflow`:** in Compose it also flags text that is merely shorter than its line,
  so the layout test checks height overflow and line widths instead.
- **Split words:** a row squeezed narrower than its longest word splits the word ("Basmat|i")
  rather than clipping it. The layout test checks for this; it was proven to catch it.
- **Bundled font weights:** with a variable font file, declaring a weight is not enough; each
  weight must be selected with `FontVariation`. `DisplayFontTest` guards this.
- **Back button in tests:** wait for idle before pressing back, or the back handler is not
  registered yet.
- **Release builds:** unit tests are switched off for the release build, because the UI tests
  need the debug-only test activity.

## Suggested skills

- `grilling` before each slice: slice 1 was decided by asking the open questions directly
  (worked well). Slice 2 open questions: sahukar interest rate and compounding, the bank's
  interest rate and whether deposits travel with you, upgrade capacities and prices, and where
  the moneylender/bank live in the UI (a third sheet? a city facility?).
- `to-spec`, then `to-tickets`, to put the slice into GitHub Issues (the tracker is set up;
  neither skill is vendored here yet, issues were written by hand so far).
- `tdd` while building. The agreed seams: `Game.apply`/`RunState` for rules,
  `GameFlowTest` / `ScreenLayoutTest` for screens, `RunStoreTest` for persistence.
- `prototype` for a new screen whose look is not settled. Show it in the approved style.
- `code-review` before each commit, and `diagnosing-bugs` for anything that is not obvious.
- `handoff` at the end of the next session, to refresh this file.
