# Saudagar

An original, offline, ad-free trading game for Android: a travelling trader rides the rails
between six Indian cities in the year 2000, buying where goods are cheap and selling where
they are dear.

Status: a whole run is playable, endlessly. Food and lodging bleed every move, the sahukar's
collector is the deadline, and broke means broke — unless one last loan can turn it around.
Trains, events, rumours, godowns, the bank, the luggage shop, the exact-amount keypad,
retire-or-die endings, best runs, and saving between sessions are all in.

- No ads, no analytics, no permissions at all.
- Kotlin and Jetpack Compose, with no image assets.
- Headings and amounts use the bundled Gelasio font (SIL Open Font License, see
  `licenses/Gelasio-OFL.txt`); small text uses the phone's own sans-serif.

## Layout

- `engine/` is pure Kotlin with no Android code: all game rules.
  Every tunable number and table is in `GameConstants.kt`.
- `app/` is the Android UI. Every colour, size and text style is in `UiConstants.kt`;
  every on-screen string is in `res/values/strings.xml`.

## Build and test

Needs JDK 17 and the Android SDK (platform 36).

    ./gradlew test                  # engine tests + UI layout tests
    ./gradlew :app:assembleRelease  # release APK in app/build/outputs/apk/release/

The UI layout tests lay every screen out at eight screen sizes and five text-zoom settings
and fail if any text is cut off, a word is split across lines, anything runs off the screen,
a control is too small to tap, or a screen's main action is out of reach. The game flow tests
play through the screens the way a player would. The engine has a simulation test of 20,000
random moves.

## Release signing

The APK is sideloaded, not published to a store. Release builds are signed with
`release.keystore` at the repo root, using the passwords in `keystore.properties`
(`storePassword`, `keyPassword`, `keyAlias`). Both files are gitignored and exist only on
the build machine. Without them (CI, a fresh clone) the release APK is built unsigned.

Back the keystore and its properties file up somewhere outside the repo. If they are lost,
a new key has to be made, and anyone with the game installed must uninstall it before they
can install an update.

## Releasing

    python scripts/release.py 1.0.0   # bumps the version, tests, builds, tags, ships

The script rewrites `versionCode`/`versionName` in `app/build.gradle.kts`, runs the full
suite, builds and signature-checks the APK, tags `v1.0.0`, pushes, and attaches
`saudagar-1.0.0.apk` to a GitHub release. Version codes are `major*10000 + minor*100 +
patch`, so every semver bump also rises numerically.
