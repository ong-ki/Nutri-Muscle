# NutriMuscle — Offline Android App (Kotlin + Jetpack Compose)

A 100% offline nutrition, macro, and muscle-building tracker built with Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, and Navigation Compose (`minSdk = 26`).

## Offline Guarantee
- `AndroidManifest.xml` contains **no `INTERNET` permission**, no network libraries, no analytics, and no login.
- All 333 Myanmar & global foods live locally in `app/src/main/assets/foods.json` and seed into Room on first launch.
- Burmese (`NotoSansMyanmar-Regular.ttf`, `NotoSansMyanmar-Bold.ttf`) is bundled in `app/src/main/res/font/`.

## How to Build a Debug APK

1. Open this root directory in **Android Studio** (Ladybug / Meerkat or newer with JDK 17).
2. Let Android Studio sync Gradle (or bootstrap the Gradle wrapper JAR once if running CLI-only via `gradle wrapper --gradle-version 8.9`).
3. Build the debug APK from the terminal:

```bash
./gradlew assembleDebug
```

The generated APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

To run unit tests (BMR/TDEE/macros, Burmese & English food parser, Muscle Score):

```bash
./gradlew testDebugUnitTest
```
