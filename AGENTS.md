# AGENTS.md — Directline Engineering Guide

## Project Overview

**Directline** is a native, local-first, offline-first Android application built with Kotlin and Jetpack Compose. Its primary purpose is to simplify starting WhatsApp and WhatsApp Business conversations via official Click to Chat links (`https://wa.me/...`) without saving phone numbers to Android Contacts.

## Documentation Index

- [README.md](README.md) — Setup, build, install, and test commands.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — MVVM layers, WhatsApp pipeline, Room ER schema, and navigation.
- [docs/WHATSAPP_INTEGRATION.md](docs/WHATSAPP_INTEGRATION.md) — Click to Chat format, URL encoding, WhatsApp Business behavior, and limitations.
- [docs/PRIVACY_AND_PERMISSIONS.md](docs/PRIVACY_AND_PERMISSIONS.md) — Local-first guarantees, permissions, AdMob configuration, and data deletion rules.

## Technology Stack

- **Language:** Kotlin 2.3.20 (JVM toolchain 17)
- **Build System:** Gradle 9.1.0, Android Gradle Plugin 9.0.1 (built-in Kotlin), KSP 2.3.6
- **UI:** Jetpack Compose (BOM 2026.03.01), Material 3, Navigation Compose 2.9.0
- **DI:** Hilt 2.59.2 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Local Persistence:** Room 2.8.5 (SQLite), DataStore Preferences 1.3.0
- **Phone Validation:** Google `libphonenumber` 9.0.44
- **Async:** Kotlin Coroutines & Flow
- **Testing:** JUnit 4, `kotlinx-coroutines-test`, AndroidX Test, Compose UI Test

## Architecture & Package Organization

Pragmatic MVVM with clean separation of concerns in a single `:app` module (`com.directline`):

- `com.directline.core.whatsapp` — Phone parsing/validation/normalization, Click to Chat destination builder, message URL encoding, and `ACTION_VIEW` Android Intent launcher.
- `com.directline.core.ads` — Google AdMob adaptive banner (`AdMobBanner`) and frequency-capped interstitial manager (`InterstitialAdManager`) using official test IDs in `debug`.
- `com.directline.core.template` — Template variable extraction, validation, and substitution (`{{name}}`, `{{phone}}`, `{{product}}`, `{{price}}`, `{{date}}`).
- `com.directline.core.share` — Safe extraction of candidate phone numbers from incoming `ACTION_SEND` text.
- `com.directline.core.database` — Room database, entities, DAOs.
- `com.directline.core.datastore` — DataStore preferences (theme, dynamic color, default country, default message).
- `com.directline.core.designsystem` — Material 3 theme, colors, typography, reusable UI components.
- `com.directline.core.navigation` — Adaptive navigation (`NavigationBar` / `NavigationRail` / `PermanentNavigationDrawer`) and `NavHost`.
- `com.directline.domain` — Domain models, repository interfaces, and use cases (`LaunchWhatsAppUseCase`).
- `com.directline.data` — Entity-to-domain mappers and Room/DataStore repository implementations.
- `com.directline.di` — Hilt modules (`DatabaseModule`, `RepositoryModule`, `WhatsAppModule`).
- `com.directline.feature.*` — Feature screens and ViewModels (`home`, `templates`, `history`, `settings`).

## Naming & State Management Conventions

- Screens: `<Feature>Screen.kt` (stateless/state-hoisted Composables where possible).
- ViewModels: `<Feature>ViewModel.kt` annotated with `@HiltViewModel`, exposing a single immutable `StateFlow<<Feature>UiState>`.
- Collect flows in Compose with `collectAsStateWithLifecycle()`.
- Use `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (not the deprecated `androidx.hilt.navigation.compose.hiltViewModel`).

## Engineering & Product Rules

1. **WhatsApp Integration Rules:**
   - Only use official `https://wa.me/{digits}` and `https://wa.me/{digits}?text={encoded}` Click to Chat URIs launched via `Intent.ACTION_VIEW`.
   - Never construct WhatsApp URLs inside Composables or ViewModels — always go through `WhatsAppLauncher` / `LaunchWhatsAppUseCase`.
   - Never hardcode undocumented WhatsApp/WhatsApp Business internal activities.
   - Never auto-send messages or auto-launch WhatsApp on text change or incoming share; the user must explicitly confirm.
   - Never claim a phone number has an active WhatsApp account or that a message was delivered/read.
2. **Phone Validation Rules:**
   - Validate all phone numbers with `libphonenumber` and normalize to E.164 (`+...`).
   - Do not silently assume a country when an ambiguous local number has no selected country.
3. **Database & Privacy Rules:**
   - Zero unnecessary permissions (`READ_CONTACTS`, `QUERY_ALL_PACKAGES`, `INTERNET`, etc. are forbidden).
   - Never log raw or normalized phone numbers.
   - Store phone number snapshots in `InteractionHistoryEntity` so history remains meaningful if contacts change or are deleted.
   - Deleting a contact must not delete unrelated templates or history snapshots.

## Testing, CI & Git Conventions

- **Unit tests:** Every change to validation, URL building, templates, CSV parsing, share extraction, or use cases must include JUnit tests with fakes (no Android/WhatsApp dependencies in unit tests).
- **CI Workflows:**
  - `.github/workflows/ci.yml` runs `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
  - `.github/workflows/commitlint.yml` enforces Conventional Commits (`.commitlintrc.json`): `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `ci:`, `build:`, `chore:`.
  - `.github/workflows/pr-size.yml` automatically labels PR size (`size/XS` through `size/XL`) and warns on PRs exceeding 1,000 changed lines.

## Build & Test Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install on connected device / emulator
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```
