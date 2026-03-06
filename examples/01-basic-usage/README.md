# 01: Basic Usage

## What this example demonstrates

Loading credentials from a `.env` file, verifying API access, listing sender IDs and coverage, and sending a single SMS message.

## Prerequisites

- Java 8+ installed
- kwtSMS account with API credentials

## Environment setup

Create a `.env` file in your project root:

```ini
KWTSMS_USERNAME=kotlin_username
KWTSMS_PASSWORD=kotlin_password
KWTSMS_SENDER_ID=YOUR-SENDERID
KWTSMS_TEST_MODE=1
```

## Walkthrough

1. `KwtSMS.fromEnv()` reads credentials from environment variables first, then falls back to the `.env` file.
2. `verify()` tests the credentials and returns the current balance. Always call this first to confirm your setup works.
3. `senderIds()` lists all sender IDs registered on your account.
4. `coverage()` shows which country prefixes are active. Cache this and check before sending to avoid wasted API calls.
5. `send()` sends the message. The response includes `msgId` (save for status/DLR lookups), `pointsCharged`, and `balanceAfter` (save to avoid extra balance API calls).

## Expected output

```
Credentials OK. Balance: 150.0 credits
Available sender IDs: [KWT-SMS, MY-APP]
Active country prefixes: [965, 966, 971, ...]
SMS sent successfully!
  Message ID: f4c841adee210f31307633ceaebff2ec
  Numbers: 1
  Credits used: 1
  Balance after: 149.0
```

## Common mistakes

1. **Using account mobile number instead of API credentials.** The API username and password are separate from your kwtsms.com login. Find them under Account > API settings.

2. **Forgetting to set `KWTSMS_TEST_MODE=1` during development.** Without test mode, you will consume real credits.

3. **Not saving `msgId` after a send.** You need the message ID to check status or get delivery reports later. If you do not store it at send time, you cannot retrieve it.
