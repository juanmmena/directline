# Directline

A native, local-first, offline-first Android application built with Kotlin and Jetpack Compose to start WhatsApp and WhatsApp Business conversations without saving phone numbers to Android Contacts.

## Key Features

- **WhatsApp Click to Chat Launcher:** International phone validation & E.164 normalization (`libphonenumber`), country selector, optional prefilled message, and official `https://wa.me/...` launch via `ACTION_VIEW`.
- **Reusable Message Templates:** Categorized templates with dynamic variables (`{{name}}`, `{{phone}}`, `{{product}}`, `{{price}}`, `{{date}}`), live preview, and unresolved placeholder protection.
- **Interaction History:** Local log of WhatsApp launch attempts with phone number snapshots, quick reopen, and deletion.
- **Android Sharesheet Integration:** Share text or numbers from other apps into Directline for review before opening WhatsApp.
- **Google AdMob Integration:** Adaptive Banner and frequency-capped Interstitial ads using official Google test IDs in `debug` builds (configurable via `ADMOB_APP_ID`, `ADMOB_BANNER_AD_UNIT_ID`, and `ADMOB_INTERSTITIAL_AD_UNIT_ID` for `release` builds).
- **Privacy First:** Zero backend, zero accounts, no Android Contacts access, and automatic cloud backup disabled.

## Prerequisites

- **JDK 17+** (Gradle JVM toolchain configured to 17)
- **Android SDK** with:
  - Compile / Target SDK: **API 37**
  - Minimum SDK: **API 24** (Android 7.0+)
- **Android Studio** (recommended) or Android SDK Command-Line Tools (`adb`)

> Gradle (`9.1.0`) is bundled via `./gradlew`.

## Setup

1. **Configure the Android SDK path:**
   - Opening the project in **Android Studio** generates `local.properties` automatically.
   - For CLI builds, set `ANDROID_HOME` or create `local.properties`:
     ```properties
     sdk.dir=/path/to/your/Android/Sdk
     ```

## Building, Installing & Running

### Command Line

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install on connected device / emulator
./gradlew installDebug

# Launch Directline
adb shell am start -n com.juanmanuel.directline/com.directline.MainActivity

# Build release APK
./gradlew assembleRelease
```

> On Windows, replace `./gradlew` with `gradlew.bat`.

## Project Info

| Property | Value |
| --- | --- |
| Application ID | `com.juanmanuel.directline` |
| Namespace | `com.directline` |
| Min / Target / Compile SDK | 24 / 37 / 37 |
| Toolchain | JDK 17 / Kotlin 2.3.20 / KSP 2.3.6 |
| Android Gradle Plugin | 9.0.1 |
| Core Stack | Jetpack Compose, Material 3, Hilt, Room, DataStore, libphonenumber |
