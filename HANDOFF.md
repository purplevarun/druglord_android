# Handoff

Context for the next agent or session picking up this repo. Read `AGENTS.md` first: it holds the
rules. This file holds what is not written down anywhere else: why things are the way they are,
the local machine, known gaps, and the plan. Last updated 27 Nov 2026, at commit `05691c3`.

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

A whole run is playable **endlessly**: a market per city with − / + to sell or buy one, train
trips that use up days, the sahukar, the bank, the luggage shop and the godown in every city,
random events on every move, a retire-anytime end, an end screen with a rank and an ending, and
a best-runs list. The clock is the wallet now: living costs bleed every move, and the sahukar's
collector is the deadline. The run and the list survive the app being closed. Details are in
the commit history (`git log`); the main pieces:

- `engine/`: `RunState` (the whole run: cash, debt, `debtDueDay`, bank, transport, godowns,
  holdings, `rngRolls`, `priceShifts`; plus `livingCostPerDay`, `collectorDue`, `isBroke`,
  `rank`), `Game.check` (pure validation) + `Game.apply` (check, move, then roll the dice;
  `Outcome.Done` carries `events`), `chargingLiving` inside `applyMove` (every non-travel move
  = 1 day at the city's rate; shortfalls land on debt), `Market`, `RailNetwork`, `GameCalendar`,
  `GameConstants.RANK_THRESHOLDS`, `LIVING_COST_RUPEES` (per city), `COLLECTOR_AFTER_DAYS` (10),
  `SURVIVAL_LOAN_RUPEES` (₹5,000), `TRANSPORTS`, `GODOWN_*`, the event constants, and
  `Game.applyEvent` (internal test seam). `Action.PayCollector` pays the whole debt cash-first.
- `app/`: `GameScreen` (`Phase`: `NewRun` / `Playing` / `Ended(run, ending)`; `Playing` queues
  `pendingEvents`; `Ending` = RETIRED/BROKE/COLLECTED; forced sheets render over events:
  `CollectorSheet` when `run.collectorDue`, else `BrokeSheet` when `run.isBroke`),
  `HomeScreen` (header shows "Day N · ₹X a day to live here"; sahukar chip shows the
  collector's day), `RailMap`, `MarketScreen`, `Sheet`, `FacilitiesSheets` (Sahukar/Bank/
  Luggage/Godown), `EventSheet` (events + the collector and broke prompt sheets),
  `NewRunScreen` (no picker — every run is endless), `EndScreen` (per-ending line),
  `RunStore` (SharedPreferences, hand-rolled format at **v5**; v1–v4 all load — v4 debts get
  a fresh ten-day clock; `MAX_BEST_RUNS` = 5; `BestRun` has no run length any more).
- Tests: 84 engine tests (including `SurvivalTest` and the 20,000-move simulation, which now
  also plays `PayCollector`) and 2,100+ app tests (120 skipped, as designed).
  `./gradlew test` takes under a minute on this Mac. Engine tests that want a move without
  the dice use `Game.applyQuietly` (internal); UI tests drain event sheets with
  `settleEvents()`.

### Decisions made for the survival pivot (27 Nov 2026)

The user steered hard toward the Drug Lord 2 loop: "it's a casual game… completely removing the
day limit… food and living expenses each day… when u hit 0 u can get a prompt to borrow money,
if already in debt, u cant borrow anymore — this is impt… the sharks will come to collect after
X days and if u dont pay them back they will kill u."

- **No day limit at all** — `runLengthDays`, `daysLeft`, `isEndless`, `isOnLastDay`,
  `NOT_ENOUGH_DAYS`, the length picker, the last-day finish button and the progress bar are all
  gone. The header reads "Day N"; ending is always a retirement or a death.
- **Living costs**: every move bills one day at the city's rate (`LIVING_COST_RUPEES`: Mumbai
  ₹120 … Srinagar ₹60); rail days bill at the destination's rate. Agent's call (user deferred)
  — per-move keeps the pressure constant and the city choice meaningful. What cash can't cover
  lands on the sahukar debt, exactly like unpaid godown rent.
- **One loan at a time** (user): `Borrow` rejects with `ALREADY_OWES` while debt stands. The
  ₹5,000 starting debt therefore locks borrowing until repaid — the user's rule working as
  stated, and it makes the collector's first deadline (day 10) the game's opening pressure.
- **Broke = nothing left at all** (user): `isBroke` = cash ≤ 0, bank ≤ 0, nothing carried,
  nothing stored → `BrokeSheet`: borrow ₹5,000 (`SURVIVAL_LOAN_RUPEES`) if debt-free, else only
  "It's over". The sheet cannot be dismissed.
- **The collector** (user: 10 days, pay-or-die prompt): `debtDueDay` is set when debt goes
  0 → positive and cleared when it hits 0. `collectorDue` when the day reaches it →
  `CollectorSheet`: `Action.PayCollector` (cash first, then bank) or death. Paying itself
  costs a day's living — pay the man and go broke, and the broke prompt follows.
- **Endings**: `Phase.Ended` carries `Ending.RETIRED` / `BROKE` / `COLLECTED`; the end screen
  says which. Dead runs still land on the best-runs list.
- **Sheet render order** in `GameScreenContent`: overlay sheets → `EventSheet` (queued) →
  `CollectorSheet` → `BrokeSheet` — each higher one can starve the ones below, which is
  intended: the collector's knock matters more than the day's news.
- **Saved format v5**: `run.length` is gone, `run.dueDay` added. A v4 run loads with a fresh
  ten-day clock on any debt it owes. `BestRun` dropped its length field (days survived is kept).
- **Interest and rent still tick only on days that pass** — rail days and bandh days — not per
  move; the simulation would otherwise compound 8% per tap.

### Decisions made for slice 4 (27 Nov 2026)

- Godown in every city: fixed capacity 500 units (`GODOWN_CAPACITY_UNITS`), ₹50/day per city
  holding goods (`GODOWN_DAILY_RENT_RUPEES`), billed inside `apply(Travel)` — unpaid rent lands
  on the debt. Agent-chosen capacity; user picked rent-only pricing, in-person selling.
- Stashed goods count in net worth at their city's price (`priceOfIn`) and are safe from
  pickpockets and checkposts — the strategic reason to stash.
- Store/Retrieve/SellStored actions; `NOT_ENOUGH_STORED`/`GODOWN_FULL` rejections; a fourth
  places chip opens `GodownSheet` (per-good stow/take rows, elsewhere-stashes listed read-only).

### Decisions made for slice 3 (27 Nov 2026)

- All four event families fire **on every move**: 5% per city action, 22% per day on the
  rails (`EVENT_CHANCE_PER_CITY_ACTION`/`EVENT_CHANCE_PER_TRAVEL_DAY`). Deterministic: the
  roll comes from `seed` + `rngRolls`, so the same run replays the same luck.
- Pickpockets take 5–15% of pocket cash (never the bank). Checkposts take a 10% bribe, or
  20% of one good when the pocket is empty. A bandh adds a day — plus a day's interest both
  ways. Mandi news (`PriceSwing`) swings one good ±40–120% in the current city.
- Saturation is **both ways and priced into the trade**: a buy or sell shifts the local
  price ±10 bp per unit (cap ±60%, `PRICE_SHIFT_CAP_BP`) *before* the trade prices, so a
  heavy sell eats its own dump and there is no free round trip. Shifts halve each travel
  day. Small buys can nudge < ₹1 and vanish — that's honest rounding, not a bug.
- UI is a one-tap sheet per the user's call: events land in `pendingEvents` and are told
  one at a time on top of whatever is open. They are not saved: restoring mid-run skips
  the telling but keeps the effects.
- `Game.check` is the pure validation the sheets use to grey out impossible moves; earlier
  they called `Game.apply` and would have burned dice rolls on a discarded outcome.

### Decisions made for slice 2 (27 Nov 2026)

- Sahukar: borrow any amount and repay, in every city, at 8% a day compounding on each travel
  day. Bank: one account in every city, pickpocket-safe, 1% a day on travel days. Both accrue
  inside `Game.apply(Travel)`, not on a timer.
- Luggage ladder: jhola 100 (start) → trunk 250 ₹4,000 → tempo 600 ₹15,000 → truck 1,500
  ₹50,000. `RunState.luggageCapacity` is now derived from the transport, not stored.
- Facilities live in the drawer's "places" chips (sahukar/bank/luggage), each opening a sheet.
  Chips are verb + amount (`Borrow ₹1,000`, `All`) — long labels overflowed the sheet at
  text-zoom x2.0. The places row itself scrolls inside a capped height for the same reason.

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
  luggage slots with every unit taking one slot, the city price table and each good's
  volatility, the living-cost table (₹60–₹120/city), the ten-day collector clock, the ₹5,000
  survival loan, 500-unit godowns at ₹50/day.
- **From the mockups, not built:** the exact-amount keypad, the "movers" ticker, travel rumours,
  each city's speciality line.
- **Saved-state format is v5.** `RunStore` reads v1–v4 (v4 debts get a fresh ten-day clock,
  earlier versions pick up the newer fields empty). A format change still needs a bump or
  saved runs silently reset.
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
2. ~~Sahukar and bank.~~ **Done** in `6cd0668` (issue #2): 8%/day debt, 1%/day pickpocket-safe
   savings, jhola→truck upgrades, places chips in the drawer opening sheets.
3. ~~Random events.~~ **Done** in `3d27009` (issue #3): pickpockets, checkposts, bandhs and
   mandi news on every move; trades move the local price, which fades each travel day.
4. ~~Godown.~~ **Done** in `ae79477` (issue #4): a 500-unit store per city at ₹50/day, billed
   on the rails, safe from the road's hazards.
5. ~~Survival loop.~~ **Done** in `05691c3` (issue #5): endless runs, per-city living costs on
   every move, one loan at a time, the collector's ten-day pay-or-die, broke-or-borrow endings.
6. **Exact amounts:** a keypad on long press, and a max button.
7. **The rest of the mockup:** the movers ticker, rumours and city specialities.
8. **Naming pass with the user:** the game title, goods, ranks and the repo name.
9. **Balance pass:** use the simulation to tune prices and risk, and verify the estimated
   prices. Now bigger: living costs vs profit margins, the 10-day clock, the survival loan.
10. **Releases:** version numbering and a script that builds the signed APK and attaches it to
    a GitHub release.

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
- **Emulator tap coordinates:** screenshot pixels are scaled; taps must use the bounds from
  `adb shell uiautomator dump`, not the screenshot. Tapping scaled coordinates hits the scrim
  and silently dismisses sheets (hit twice).
- **Deterministic events:** a UI test that must see an event finds it by scanning `rngRolls`
  and calling `Game.apply` until `Done.events` is non-empty, then starts the run with that
  count — no flakiness, no mocking.
- **Release builds:** unit tests are switched off for the release build, because the UI tests
  need the debug-only test activity.

## Suggested skills

- `grilling` before each slice: all five shipped slices were decided by asking the open
  questions directly (worked well). For slice 6, the questions are keypad placement,
  what "max" means per row (cash-limited? luggage-limited?), and long-press vs a button.
- `to-spec`, then `to-tickets`, to put the slice into GitHub Issues (the tracker is set up;
  neither skill is vendored here yet, issues were written by hand so far).
- `tdd` while building. The agreed seams: `Game.apply`/`RunState` for rules,
  `GameFlowTest` / `ScreenLayoutTest` for screens, `RunStoreTest` for persistence.
- `prototype` for a new screen whose look is not settled. Show it in the approved style.
- `code-review` before each commit, and `diagnosing-bugs` for anything that is not obvious.
- `handoff` at the end of the next session, to refresh this file.
