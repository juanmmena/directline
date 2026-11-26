# CSV Import & Export Format

Directline supports backing up and restoring local contacts using RFC-4180 compliant CSV files via Android's Storage Access Framework (**Settings → Contacts Backup (CSV)**).

## Schema

```csv
name,phone_number,tags,notes
Maria,+573001234567,Customer;VIP,"Interested in premium, fast shipping"
John,+14155552671,Personal,Friend
```

### Columns

| Column | Required | Description |
| --- | --- | --- |
| `name` | Yes | Contact display name or alias. |
| `phone_number` | Yes | Phone number in international format (`+...`) or local format if a Default Country is configured in Settings. |
| `tags` | No | Optional tags separated by `;` or `|`. |
| `notes` | No | Optional free-text notes (wrap in double quotes if containing commas or quotes). |

## Import Behavior

1. Every row is parsed and validated using `libphonenumber` before any database write occurs.
2. A **Preview Dialog** displays:
   - Valid contacts ready to import.
   - Duplicate phone numbers (already present locally or repeated in the CSV) that will be skipped.
   - Row-level validation errors (missing name, invalid phone number, missing country code).
3. Users explicitly confirm before importing; invalid rows are skipped without corrupting valid records.
4. Exported CSV files can be re-imported cleanly (round-trip compatible).
