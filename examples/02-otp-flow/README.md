# 02: OTP Flow

## What this example demonstrates

Sending a one-time password (OTP) via SMS with proper phone validation, secure code generation, and compliance with telecom requirements.

## Prerequisites

- Java 8+ installed
- kwtSMS account with a **Transactional** Sender ID (not promotional, not KWT-SMS)

## Environment setup

```ini
KWTSMS_USERNAME=kotlin_username
KWTSMS_PASSWORD=kotlin_password
KWTSMS_SENDER_ID=YOUR-TRANSACTIONAL-SENDERID
KWTSMS_TEST_MODE=1
```

## Walkthrough

1. **Phone validation first.** `validatePhoneInput()` catches invalid numbers before any API call. No wasted requests.
2. **Secure random for OTP.** Uses `java.security.SecureRandom`, not `Math.random()` or `kotlin.random.Random`.
3. **App name in message.** Telecom compliance requires the company/app name in every OTP message.
4. **Single number per request.** OTP sends must go to one number at a time to avoid ERR028 (15s rate limit) rejecting the batch.
5. **Save the message ID.** Store `msgId` for status lookups and support requests.

## Expected output

```
OTP sent to 96598765432
Message ID: f4c841adee210f31307633ceaebff2ec
```

## Common mistakes

1. **Using `KWT-SMS` sender for OTP.** KWT-SMS is a shared promotional sender. OTP messages sent with it are filtered by DND on Zain and Ooredoo, silently failing to deliver while still consuming credits. Use a Transactional Sender ID.

2. **Batching OTP sends.** Sending OTP to multiple numbers in one API call means a 15-second cooldown applies to all numbers. If any number triggers ERR028, the entire request is rejected.

3. **Using Math.random() for OTP codes.** `Math.random()` is not cryptographically secure. Use `SecureRandom` for OTP generation.
