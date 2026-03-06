# 05: Error Handling

## What this example demonstrates

All error paths in the kwtSMS Kotlin client: local phone validation, message cleaning, API error responses with action guidance, and mapping raw errors to user-facing messages.

## Prerequisites

- Java 8+ installed
- kwtSMS account (optional for local validation demos)

## Environment setup

```ini
KWTSMS_USERNAME=kotlin_username
KWTSMS_PASSWORD=kotlin_password
KWTSMS_TEST_MODE=1
```

## Walkthrough

1. **Local validation.** `validatePhoneInput()` catches bad input before any API call: empty strings, emails, too-short/too-long numbers, non-numeric input.
2. **Message cleaning.** `cleanMessage()` strips emojis, HTML, invisible characters, and converts Arabic digits. The `send()` method calls this automatically.
3. **API errors with actions.** Every API error includes a `code`, `description` (from kwtSMS), and `action` (developer guidance from the client library).
4. **User-facing mapping.** Split errors into user-recoverable (show helpful message) and system-level (show generic message, log the real error, alert admin).

## Expected output

```
=== Phone Validation ===
  '' -> ERROR: Phone number is required
  'user@example.com' -> ERROR: 'user@example.com' is an email address, not a phone number
  '12345' -> ERROR: '12345' is too short (5 digits, minimum is 7)
  'abcdef' -> ERROR: 'abcdef' is not a valid phone number, no digits found
  '+96598765432' -> OK (normalized: 96598765432)

=== Message Cleaning ===
  Input:   'Hello [emoji] World'
  Cleaned: 'Hello  World'
  ...
```

## Common mistakes

1. **Showing raw API errors to end users.** "ERR003" and "ERR025" are meaningless to users. Map them to friendly messages.

2. **Not logging system-level errors.** When you show "temporarily unavailable" to the user, make sure the real error (auth failure, zero balance) is logged and alerts the admin.

3. **Not handling network errors.** Connection timeouts and DNS failures return `description` with the error message. Always have a catch-all for unexpected errors.
