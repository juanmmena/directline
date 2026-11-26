# Privacy, Permissions & Data Management

Directline is designed from the ground up to be **local-first**, **offline-first**, and **privacy-preserving**.

## Core Guarantees

- **No account or registration** required.
- **No backend servers** or cloud synchronization.
- **No Android Contacts access** — Directline maintains its own independent local SQLite database via Room.
- **No WhatsApp conversation access** — Directline only constructs and launches official `https://wa.me` links.
- **No plaintext phone number logging** in application logs or crash diagnostics.
- **Cloud backup disabled** (`android:allowBackup="false"` in `AndroidManifest.xml`) so local phone numbers and notes are not silently uploaded to cloud backups.

## Android Permissions

Directline declares only the network permissions required by the **Google Mobile Ads SDK (AdMob)**:
- `INTERNET`
- `ACCESS_NETWORK_STATE`

Explicitly forbidden permissions:
- `READ_CONTACTS` / `WRITE_CONTACTS`
- `READ_PHONE_STATE`
- `SEND_SMS` / `CALL_PHONE`
- `QUERY_ALL_PACKAGES`
- Broad external storage permissions (`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`)

### AdMob Configuration
- **Debug builds** always use official Google AdMob test IDs (`ca-app-pub-3940256099942544~3347511713` app ID, `9214589741` adaptive banner, `1033173712` interstitial).
- **Release builds** read `ADMOB_APP_ID`, `ADMOB_BANNER_AD_UNIT_ID`, and `ADMOB_INTERSTITIAL_AD_UNIT_ID` from Gradle properties or environment variables (never hardcoded in source control).
- Interstitial ads never block WhatsApp launches when offline or unloaded.

## Data Deletion

- **Settings → Delete All Local Data** permanently clears templates, categories, interaction history, and preferences stored on the device.
