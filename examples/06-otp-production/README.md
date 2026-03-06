# 06 - Production OTP

A complete, drop-in OTP implementation with secure code storage, rate limiting, bot protection, and database adapters. Copy into your project and wire up to your HTTP framework.

## Prerequisites

- Java 8+
- kwtSMS account with a **Transactional** Sender ID (not promotional, not KWT-SMS)
- CAPTCHA provider (Cloudflare Turnstile or hCaptcha) for web apps
- Google Play Integrity API setup for Android apps

## Environment Setup

Create a `.env` file:

```ini
KWTSMS_USERNAME=kotlin_username
KWTSMS_PASSWORD=kotlin_password
KWTSMS_SENDER_ID=YOUR-TRANSACTIONAL-SENDERID
KWTSMS_TEST_MODE=1
TURNSTILE_SECRET=your_turnstile_secret
```

## Project Structure

```
06-otp-production/
    OtpService.kt           -- Core service: sendOtp(), verifyOtp()
    Main.kt                  -- Wiring example
    adapters/
        MemoryStore.kt       -- In-memory store (dev/testing only)
        SqliteStore.kt       -- SQLite store (production, zero-config)
    captcha/
        TurnstileVerifier.kt -- Cloudflare Turnstile (free, recommended)
        HcaptchaVerifier.kt  -- hCaptcha (GDPR-safe alternative)
    attestation/
        PlayIntegrityVerifier.kt -- Android device attestation
```

## Walkthrough

1. **Initialize** `KwtSMS` client from environment variables
2. **Choose a store**: `MemoryStore` for development, `SqliteStore` for production (auto-creates tables, WAL mode)
3. **Choose bot protection**: `TurnstileVerifier` or `HcaptchaVerifier` for web; `PlayIntegrityVerifier` for Android
4. **Create OtpService** with configuration (code length, expiry, cooldown, rate limits)
5. **Call sendOtp()** from your HTTP endpoint — handles the full flow: phone validation, bot verification, rate limiting, code generation, secure hashing, SMS send, rollback on failure
6. **Call verifyOtp()** from your verification endpoint — timing-safe comparison, attempt tracking, expiry check

## Security Features

- **SHA-256 + random salt** for OTP storage (never stores plain codes)
- **Timing-safe comparison** via `MessageDigest.isEqual` (prevents timing attacks)
- **SecureRandom** for code generation (not `Math.random()`)
- **Rate limiting** per IP (15/hour), per phone (5/hour), per verify (10/hour)
- **Resend cooldown** of 4 minutes (KNET standard)
- **Max 5 wrong attempts** before code invalidation
- **Rate limit counters not rolled back on failure** (prevents bypass)
- **Fail-closed CAPTCHA** (any error = verification failed)
- **One number per OTP send** (never batched)

## Expected Output

```
OTP sent successfully!
Verification failed: Invalid code.
Attempts remaining: 4
```

## Common Mistakes

1. **Using `Math.random()` for OTP codes.** It is not cryptographically secure. Always use `SecureRandom`.
2. **Storing plain OTP codes in the database.** Always hash with SHA-256 + salt before storing. If your database is compromised, plain codes leak immediately.
3. **Using a Promotional Sender ID for OTP.** Messages to DND numbers are silently blocked and credits are still deducted. Use a Transactional Sender ID.
4. **Rolling back rate limit counters on send failure.** An attacker can intentionally cause failures to bypass rate limits. Always increment counters before the send.
5. **Skipping CAPTCHA/attestation.** Without bot protection, scripts can drain your entire SMS balance in minutes.
