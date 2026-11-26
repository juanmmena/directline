# WhatsApp Integration Guide

Directline integrates with **WhatsApp** and **WhatsApp Business** exclusively through official **WhatsApp Click to Chat** HTTPS links launched via Android's `Intent.ACTION_VIEW`.

## Destination Format

- Without a message:
  ```text
  https://wa.me/{international_digits_without_plus}
  ```
  Example: `https://wa.me/573001234567`

- With a prefilled message:
  ```text
  https://wa.me/{international_digits_without_plus}?text={url_encoded_message}
  ```
  Example: `https://wa.me/573001234567?text=Hello%21%20How%20are%20you%3F`

## Phone Validation & Normalization

1. All user input is validated with Google's `libphonenumber`.
2. Formatting characters (spaces, hyphens, parentheses) and `00` international prefixes are handled cleanly.
3. Valid numbers are normalized to **E.164** (`+573001234567`), and the leading `+` is stripped only when constructing the `wa.me` URI.
4. **Ambiguous local numbers** without a `+` prefix require an explicit country selection (`PhoneNumberValidationResult.CountryRequired`). Directline never silently guesses a country.

## Message Encoding

- Messages are UTF-8 URL-encoded (`MessageEncoder`), converting spaces to `%20` and preserving accented characters, Unicode, emoji, special characters (`&`, `?`, `=`, `#`), and line breaks (`%0A`).
- Whitespace-only messages are treated as empty (no `?text=` parameter appended).
- Messages exceeding 4,096 characters are rejected with a clear validation message.

## WhatsApp vs. WhatsApp Business Resolution

Directline relies on Android's standard `ACTION_VIEW` URI resolution and never hardcodes undocumented internal WhatsApp activities:

| Device Configuration | Expected Android Behavior |
| --- | --- |
| Only WhatsApp installed | Opens WhatsApp directly. |
| Only WhatsApp Business installed | Opens WhatsApp Business directly. |
| Both installed | Android opens the system app chooser or the user's configured default handler. |
| Neither installed | Opens in a browser (if available) or returns `WhatsAppLaunchResult.ApplicationUnavailable`. |

## Incoming Share Integration (`ACTION_SEND`)

Directline registers an `ACTION_SEND` (`text/plain`) intent filter in `AndroidManifest.xml`:
- Incoming text is treated as untrusted input.
- `SharedTextPhoneExtractor` extracts candidate numbers from `wa.me` URLs, `tel:` URIs, or plain text.
- The number is loaded into the Home launcher with a review banner — **Directline never auto-launches WhatsApp without explicit user confirmation**.

## Known Limitations

Click to Chat does **not** allow Directline to:
- Verify whether a phone number has an active WhatsApp account.
- Send messages automatically (the user always sends manually inside WhatsApp).
- Confirm whether a conversation opened successfully or whether a message was delivered/read.
- Read WhatsApp chats or message history.

## References

- [WhatsApp Click to Chat FAQ](https://faq.whatsapp.com/5913398998672934)
- [Android Intents and Intent Filters](https://developer.android.com/guide/components/intents-filters)
