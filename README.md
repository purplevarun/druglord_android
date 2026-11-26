# druglord_android

An original, offline, ad-free trading game for Android: a travelling trader rides the rails
between Indian cities in the year 2000, buying where goods are cheap and selling where they
are scarce, before the days run out.

Status: walking skeleton. The home screen (net worth, rail map, market drawer) runs on a
device; trading, travel and events are not built yet. UI mockups live on the
`prototype/ui-mockups` branch.

- No ads, no analytics, no permissions at all.
- Kotlin and Jetpack Compose, with no image assets.

## Layout

- `engine/` is pure Kotlin with no Android code: all game rules.
  Every tunable number and table is in `GameConstants.kt`.
- `app/` is the Android UI. Every colour, size and text style is in `UiConstants.kt`;
  every on-screen string is in `res/values/strings.xml`.

## Build and test

Needs JDK 17 and the Android SDK (platform 36).

    ./gradlew test                  # engine tests + UI layout tests
    ./gradlew :app:assembleRelease  # release APK in app/build/outputs/apk/release/

The UI layout tests lay the real screens out at eight screen sizes and five text-zoom
settings and fail if any text is cut off, runs off the screen, or a control is too small
to tap.

The release build is currently signed with the debug key so it can be installed and
measured. It needs a real key before it is distributed.
