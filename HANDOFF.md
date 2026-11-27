# Handoff

Context for the next agent or session picking up this repo. Read `AGENTS.md` first: it holds the
rules. This file holds what is not written down anywhere else: why things are the way they are,
the local machine, known gaps, and the plan. Last updated 27 Nov 2026, at commit `7fb1298`.

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

The trading loop is playable: a market per city with − / + to sell or buy one, and train trips
that use up days. Details are in the commit history (`git log`); the main pieces are:

- `engine/`: `RunState` (the whole run), `Game.apply` (the rules), `Market` (prices derived from
  the run's seed, never stored), `RailNetwork` (routes), `GameCalendar` (day to date).
- `app/`: `GameScreen` (holds state, survives rotation and process death via `rememberSaveable`),
  `HomeScreen`, `RailMap`, `MarketScreen`, `TravelSheet`.
- Tests: 33 engine tests (including a 20,000-move simulation) and 494 app tests (40 more are
  skipped, as designed). `./gradlew test` takes about 4 minutes on this PC.

## Known gaps and open points

- **Prices:** five of the eight base prices are estimates with no year-2000 source: onions,
  basmati, chai, silk sarees, kesar. Gold, cotton and masala (black pepper) are sourced. Each is
  flagged `SOURCED` or `ESTIMATE` in `GameConstants.GOODS`.
- **Tuning I chose, never confirmed by the user:** ₹10,000 starting cash, ₹5,000 debt, 100
  luggage slots with every unit taking one slot, the city price table and each good's volatility.
- **Debt never grows yet.** It waits for the sahukar slice.
- **The run is not saved between sessions.** It survives rotation and the app being stopped in
  the background, but closing the app loses it.
- **No end of run.** On the last day you can still trade, but there is no end screen and no new run.
- **From the mockups, not built:** the exact-amount keypad, the "movers" ticker, travel rumours,
  each city's speciality line, the endless run option.
- **Android 7 (API 24-25):** bold Gelasio renders as regular, because those versions cannot pick
  a weight from a variable font. Never tested on a real old phone.
- **Agent skills:** never checked loading in Copilot, Devin or Windsurf, only that the files are
  in each agent's folder. Some agents may need skills switched on in their settings.
- **pstack is not added.** Its Claude Code version lives in `cursor/plugins` under
  `harness/pstack/`. Check its licence before copying it into this repo the way
  `scripts/sync_agent_skills.py` copies the Pocock skills.
- **The keystore has one copy.** `release.keystore` and `keystore.properties` exist only at the
  repo root on the user's PC (gitignored). The user was told to back them up and has not
  confirmed it. If they are lost, installed copies must be uninstalled before an update.

## The plan

Each item is a vertical slice: engine test-first, then screens, then the size and zoom tests,
then a check on the emulator, then commit and push to `main`.

1. **End of run and saving.** An end screen with final net worth and a rank, the best-runs list,
   a new-run screen with 30 / 60 / 90 / endless days, and saving the run and best runs between
   sessions. Keep the APK small: prefer Android's built-in storage over new libraries.
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

- **JDK 17:** `C:\Users\Varun\tools\jdk-17.0.21.1+2`. `JAVA_HOME` and Path are set for the
  Windows user; shells opened before that change need `JAVA_HOME` exported by hand.
- **Android SDK:** `C:\Users\Varun\AppData\Local\Android\Sdk` (platform 36, build-tools 36.0.0).
- **Emulator:** `Local_AVD`, shows as `emulator-5554`, 1080x2400 at 420 dpi. To screenshot:
  `adb exec-out screencap -p > shot.png`. After changing its font scale or rotation for a check,
  set them back (`font_scale` 1.0, `accelerometer_rotation` 1).
- **Build stack:** Gradle 9.3.1, Android Gradle plugin 8.13.1, Kotlin 2.1.20, Compose BOM
  2025.05.00, Robolectric 4.15.1. These were chosen because they were already cached on this PC.
- **Not installed:** Node and the `claude` command-line tool. Python 3.12 is available.
- **GitHub:** `gh` is signed in as `purplevarun`.

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

- `grill-me` or `grill-with-docs` before each slice: the user has never been grilled, and slice 1
  has open questions (rank names, what counts as the end, whether debt counts against the score).
- `to-spec`, then `to-tickets`, to put the slice into GitHub Issues (the tracker is set up).
- `tdd` while building. It asks you to agree the test seams with the user first: so far they are
  `Game.apply` for rules and `GameFlowTest` / `ScreenLayoutTest` for screens.
- `prototype` for a new screen whose look is not settled. Show it in the approved style.
- `code-review` before each commit, and `diagnosing-bugs` for anything that is not obvious.
- `handoff` at the end of the next session, to refresh this file.
