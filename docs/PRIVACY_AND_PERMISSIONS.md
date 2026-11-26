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

Directline declares **zero** runtime or sensitive permissions in `AndroidManifest.xml`.

Explicitly forbidden permissions:
- `READ_CONTACTS` / `WRITE_CONTACTS`
- `READ_PHONE_STATE`
- `SEND_SMS` / `CALL_PHONE`
- `QUERY_ALL_PACKAGES`
- `INTERNET`
- Broad external storage permissions (`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`)

CSV import and export use Android's **Storage Access Framework** (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`), granting one-time scoped access only to the user-selected file.

## Data Deletion

- Deleting an individual contact removes only that contact, its phone numbers, and its tag links. Templates and interaction history snapshots remain intact.
- **Settings → Delete All Local Data** permanently clears contacts, tags, templates, categories, interaction history, and preferences stored on the device.
