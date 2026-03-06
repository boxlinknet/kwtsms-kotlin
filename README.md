# kwtsms-kotlin

[![Test](https://github.com/boxlinknet/kwtsms-kotlin/actions/workflows/test.yml/badge.svg)](https://github.com/boxlinknet/kwtsms-kotlin/actions/workflows/test.yml)
[![CodeQL](https://github.com/boxlinknet/kwtsms-kotlin/actions/workflows/codeql.yml/badge.svg)](https://github.com/boxlinknet/kwtsms-kotlin/actions/workflows/codeql.yml)
[![JitPack](https://jitpack.io/v/boxlinknet/kwtsms-kotlin.svg)](https://jitpack.io/#boxlinknet/kwtsms-kotlin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://adoptium.net/)

Official Kotlin/JVM client for the [kwtSMS](https://www.kwtsms.com) SMS gateway API. Zero dependencies, thread-safe, works on Android and server-side JVM.

## About kwtSMS

kwtSMS is a Kuwaiti SMS gateway trusted by top businesses to deliver messages anywhere in the world, with private Sender ID, free API testing, non-expiring credits, and competitive flat-rate pricing. Secure, simple to integrate, built to last. Open a free account in under 1 minute, no paperwork or payment required. [Click here to get started](https://www.kwtsms.com/signup/) 👍

## Prerequisites

You need **Java** (8 or newer) and **Gradle** installed. If you are using Android Studio, both are already included.

### Step 1: Check if Java is installed

```bash
java -version
```

If not installed:
- **All platforms:** Download from https://adoptium.net/ (Temurin JDK, free)
- **macOS:** `brew install openjdk`
- **Ubuntu/Debian:** `sudo apt update && sudo apt install default-jdk`

### Step 2: Add JitPack repository

In your `settings.gradle.kts` (or `build.gradle.kts` under `repositories`):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
```

### Step 3: Add kwtsms dependency

In your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.boxlinknet:kwtsms-kotlin:0.1.1")
}
```

Then sync your project (Android Studio: click "Sync Now", or run `./gradlew build`).

## Quick Start

```kotlin
import com.kwtsms.KwtSMS

fun main() {
    // Load credentials from .env file or environment variables
    val sms = KwtSMS.fromEnv()

    // Verify credentials
    val verify = sms.verify()
    println("Balance: ${verify.balance} credits")

    // Send an SMS
    val result = sms.send("96598765432", "Hello from kwtSMS!")
    println("Result: ${result.result}, Message ID: ${result.msgId}")
}
```

## Setup / Configuration

Create a `.env` file in your project root:

```ini
KWTSMS_USERNAME=your_api_user
KWTSMS_PASSWORD=your_api_pass
KWTSMS_SENDER_ID=YOUR-SENDERID
KWTSMS_TEST_MODE=1
KWTSMS_LOG_FILE=kwtsms.log
```

Environment variables take precedence over `.env` file values. Set `KWTSMS_TEST_MODE=0` when ready for production.

## Credential Management

### Environment variables / .env file (default)

```kotlin
val sms = KwtSMS.fromEnv()  // reads KWTSMS_USERNAME, KWTSMS_PASSWORD, etc.
```

### Constructor injection

```kotlin
val sms = KwtSMS(
    username = "your_api_user",
    password = "your_api_pass",
    senderId = "YOUR-SENDERID",
    testMode = true
)
```

### For Android apps

**Backend proxy (strongly recommended):** Your mobile app calls YOUR backend server, which holds the kwtSMS credentials and makes the API call. The app never touches the SMS API directly. This is the only pattern that fully protects credentials.

**If direct API access is required (not recommended):**
- Store credentials in `EncryptedSharedPreferences`
- Provide a settings Activity for entering/updating credentials
- Include a "Test Connection" button that calls `verify()`
- NEVER store credentials in `strings.xml`, `BuildConfig` fields, or hardcoded in source

**Loading from Firebase Remote Config:**

```kotlin
val config = Firebase.remoteConfig
config.fetchAndActivate().addOnCompleteListener {
    val sms = KwtSMS(
        username = config.getString("kwtsms_username"),
        password = config.getString("kwtsms_password")
    )
}
```

**Bot protection for Android (Google Play Integrity API):**

Android apps do NOT need CAPTCHA. Use Google Play Integrity API instead — it verifies app genuineness and device integrity, blocking scripts, bots, and modified binaries. Verify the integrity token on your backend server before allowing OTP sends. See [Play Integrity documentation](https://developer.android.com/google/play/integrity).

## Methods

### `verify()`

Test credentials and get balance. Never throws.

```kotlin
val result: VerifyResult = sms.verify()
// result.ok      -> true if credentials are valid
// result.balance -> current balance (Double?)
// result.error   -> error message if ok is false
```

### `balance()`

Get current SMS credit balance. Returns cached value if API call fails.

```kotlin
val balance: Double? = sms.balance()
```

### `send(mobile, message, sender?)`

Send SMS to one or more phone numbers.

```kotlin
// Single number
val result: SendResult = sms.send("96598765432", "Hello!")

// Multiple numbers (comma-separated)
val result = sms.send("96598765432,96512345678", "Hello!")

// Multiple numbers (list)
val result = sms.send(listOf("96598765432", "96512345678"), "Hello!")

// Custom sender ID
val result = sms.send("96598765432", "Hello!", "MY-SENDER")
```

**SendResult fields:**

| Field | Type | Description |
|-------|------|-------------|
| `result` | `String` | `"OK"` or `"ERROR"` |
| `msgId` | `String?` | Message ID (save for status/DLR lookups) |
| `numbers` | `Int?` | Count of numbers accepted |
| `pointsCharged` | `Int?` | Credits consumed |
| `balanceAfter` | `Double?` | Balance after send (save to avoid extra API calls) |
| `unixTimestamp` | `Long?` | Server timestamp (GMT+3, not UTC) |
| `code` | `String?` | Error code (e.g., `"ERR003"`) |
| `description` | `String?` | Error description from API |
| `action` | `String?` | Developer-friendly guidance |
| `invalid` | `List<InvalidEntry>` | Numbers that failed local validation |

### `sendBulk(mobiles, message, sender?)`

Send to >200 numbers with automatic batching.

```kotlin
val result: BulkSendResult = sms.sendBulk(phoneList, "Bulk message")
// result.result       -> "OK", "PARTIAL", or "ERROR"
// result.batches      -> number of batches sent
// result.msgIds       -> list of message IDs (one per batch)
// result.errors       -> per-batch errors
// result.invalid      -> numbers that failed local validation
```

### `validate(phones)`

Validate phone numbers with the kwtSMS API.

```kotlin
val result: ValidateResult = sms.validate(listOf("96598765432", "+96512345678"))
// result.ok       -> valid and routable numbers
// result.er       -> format errors
// result.nr       -> no route (country not activated)
// result.rejected -> locally rejected (email, too short, etc.)
```

### `senderIds()`

List available sender IDs.

```kotlin
val result: SenderIdResult = sms.senderIds()
// result.senderIds -> ["KWT-SMS", "MY-APP"]
```

### `coverage()`

List active country prefixes.

```kotlin
val result: CoverageResult = sms.coverage()
// result.prefixes -> ["965", "966", "971", ...]
```

### `status(msgId)`

Check message queue status.

```kotlin
val result: StatusResult = sms.status("f4c841adee210f31307633ceaebff2ec")
// result.status            -> "sent", "pending", etc.
// result.statusDescription -> human-readable status
```

### `deliveryReport(msgId)`

Get delivery reports (international numbers only, not available for Kuwait).

```kotlin
val result: DeliveryReportResult = sms.deliveryReport("f4c841adee210f31307633ceaebff2ec")
// result.report -> list of DeliveryReportEntry(number, status)
```

## Utility Functions

### `PhoneUtils.normalizePhone(phone)`

```kotlin
import com.kwtsms.PhoneUtils

PhoneUtils.normalizePhone("+96598765432")      // "96598765432"
PhoneUtils.normalizePhone("0096598765432")     // "96598765432"
PhoneUtils.normalizePhone("965 9876 5432")     // "96598765432"
PhoneUtils.normalizePhone("٩٦٥٩٨٧٦٥٤٣٢")    // "96598765432"
```

### `PhoneUtils.validatePhoneInput(phone)`

```kotlin
val (valid, error, normalized) = PhoneUtils.validatePhoneInput("+96598765432")
// valid=true, error=null, normalized="96598765432"

val (valid, error, _) = PhoneUtils.validatePhoneInput("user@example.com")
// valid=false, error="'user@example.com' is an email address, not a phone number"
```

### `MessageUtils.cleanMessage(text)`

```kotlin
import com.kwtsms.MessageUtils

MessageUtils.cleanMessage("Hello 😀 World")     // "Hello  World"
MessageUtils.cleanMessage("<b>Bold</b>")         // "Bold"
MessageUtils.cleanMessage("\uFEFFBOM text")      // "BOM text"
MessageUtils.cleanMessage("OTP: ١٢٣٤")          // "OTP: 1234"
```

## Input Sanitization

`send()` automatically calls `cleanMessage()` on every message before sending. Three categories of content cause silent delivery failure (API returns OK, but the message gets stuck in the queue and is never delivered):

| Content | Effect | What cleanMessage() does |
|---------|--------|--------------------------|
| Emojis | Stuck in queue, credits wasted, no error | Stripped |
| Hidden control characters (BOM, zero-width space, soft hyphen) | Spam filter rejection or queue stuck | Stripped |
| Arabic/Hindi numerals in body | OTP codes render inconsistently | Converted to Latin digits |
| HTML tags | ERR027 rejection | Stripped |

Arabic text is fully supported and preserved. Only digits, invisible chars, emojis, control chars, and HTML are affected.

## Error Handling

```kotlin
val result = sms.send("96598765432", "Hello!")
when (result.result) {
    "OK" -> {
        println("Sent! Balance: ${result.balanceAfter}")
    }
    "ERROR" -> {
        println("Error: ${result.description}")
        println("Action: ${result.action}")
    }
}
```

### Common error codes

| Code | Meaning | Action |
|------|---------|--------|
| ERR003 | Wrong credentials | Check KWTSMS_USERNAME and KWTSMS_PASSWORD |
| ERR006 | No valid numbers | Include country code (e.g., 96598765432) |
| ERR009 | Empty message | Provide non-empty message text |
| ERR010 | Zero balance | Recharge at kwtsms.com |
| ERR013 | Queue full | Auto-retried with backoff |
| ERR025 | Invalid number format | Use digits-only international format |
| ERR028 | Same number too fast | Wait 15 seconds between sends to same number |

All 33 error codes are mapped. Access the full map via `API_ERRORS`:

```kotlin
import com.kwtsms.API_ERRORS

for ((code, action) in API_ERRORS) {
    println("$code: $action")
}
```

## Phone Number Formats

| Input | Normalized | Valid? |
|-------|-----------|--------|
| `96598765432` | `96598765432` | Yes |
| `+96598765432` | `96598765432` | Yes |
| `0096598765432` | `96598765432` | Yes |
| `965 9876 5432` | `96598765432` | Yes |
| `965-9876-5432` | `96598765432` | Yes |
| `٩٦٥٩٨٧٦٥٤٣٢` | `96598765432` | Yes |
| `user@example.com` | | No (email) |
| `12345` | | No (too short) |
| `abcdef` | | No (no digits) |

## Test Mode

Set `KWTSMS_TEST_MODE=1` or pass `testMode = true` to the constructor. In test mode:
- Messages enter the kwtSMS queue but are NOT delivered to handsets
- No SMS credits are consumed
- Test messages appear in the Sending Queue at kwtsms.com
- Delete them from the queue to recover any tentatively held credits

Set `KWTSMS_TEST_MODE=0` before going live.

## Sender ID

`KWT-SMS` is a shared test sender. It causes delivery delays, is blocked on Virgin Kuwait numbers, and must never be used in production.

Register a private Sender ID through your kwtSMS account:
- **Promotional** (10 KD): for marketing, offers, announcements
- **Transactional** (15 KD): for OTP, alerts, notifications (bypasses DND)

For OTP/authentication, you MUST use a **Transactional** Sender ID. Promotional sender IDs are filtered by DND (Do Not Disturb) on Zain and Ooredoo, meaning OTP messages silently fail to deliver and credits are still deducted.

Sender ID is **case sensitive**: `Kuwait` is not the same as `KUWAIT`.

## What's Handled Automatically

- Phone number normalization (Arabic digits, +/00 prefix, spaces, dashes)
- Phone number deduplication before sending
- Local phone validation before API calls (no wasted requests)
- Message cleaning (emojis, HTML, invisible chars, Arabic digits)
- Empty-after-cleaning detection
- Auto-batching for >200 numbers
- ERR013 (queue full) retry with exponential backoff
- Balance caching from send responses
- Error enrichment with developer-friendly action messages
- Password masking in logs
- JSONL logging of all API calls (never crashes main flow)

## Security Checklist

```
BEFORE GOING LIVE:
[ ] Bot protection enabled (CAPTCHA for web, Play Integrity for Android)
[ ] Rate limit per phone number (max 3-5/hour)
[ ] Rate limit per IP address (max 10-20/hour)
[ ] Rate limit per user/session if authenticated
[ ] Monitoring/alerting on abuse patterns
[ ] Admin notification on low balance
[ ] Test mode OFF (KWTSMS_TEST_MODE=0)
[ ] Private Sender ID registered (not KWT-SMS)
[ ] Transactional Sender ID for OTP (not promotional)
```

## Best Practices

### 1. Validate before calling the API

The #1 cause of wasted API calls is sending invalid input and letting the API reject it. Validate locally first:

```kotlin
// BAD: sends everything to the API, wastes round-trips
val result = sms.send(userPhone, userMessage)

// GOOD: validate locally, only send clean input
val (valid, error, normalized) = PhoneUtils.validatePhoneInput(userPhone)
if (!valid) return mapOf("error" to error)

val message = MessageUtils.cleanMessage(userMessage)
if (message.isBlank()) return mapOf("error" to "Message is empty after cleaning.")

val result = sms.send(normalized!!, message)
```

| Check | When | Why |
|-------|------|-----|
| Phone number format | Before `send()` | Reject emails, too-short numbers, non-numeric input locally |
| Message not empty | Before `send()` | Don't waste an API call to get ERR009 |
| Country prefix active | Before `send()` | Call `coverage()` once at startup, cache prefixes. Reject unsupported countries locally |
| Balance sufficient | Before `send()` | Use `cachedBalance` from previous sends. If 0, show error immediately |
| Sender ID valid | Before `send()` | Cache `senderIds()` result. Reject unknown senders locally |

### 2. User-facing error messages

Never expose raw API errors like "ERR006" to end users. Map them to friendly messages:

| Situation | Raw error | Show to user |
|-----------|----------|--------------|
| Invalid phone | ERR006, ERR025 | "Please enter a valid phone number (e.g., +965 9876 5432)." |
| Wrong credentials | ERR003 | "SMS service is temporarily unavailable. Please try again later." |
| No balance | ERR010, ERR011 | "SMS service is temporarily unavailable. Please try again later." |
| Country not supported | ERR026 | "SMS delivery to this country is not available." |
| Rate limited | ERR028 | "Please wait a moment before requesting another code." |
| Message rejected | ERR031, ERR032 | "Your message could not be sent. Please try again with different content." |
| Network error | timeout | "Could not connect to SMS service. Check your internet connection." |
| Queue full | ERR013 | "SMS service is busy. Please try again in a few minutes." |

**Key principle:** User-recoverable errors (bad phone, rate limited) get helpful messages. System-level errors (auth, balance, network) get a generic message + admin alert.

### 3. OTP requirements

- Always include app name: `"Your OTP for APPNAME is: 123456"`
- Resend timer: minimum 3-4 minutes (KNET standard is 4 minutes)
- OTP expiry: 3-5 minutes
- Always generate a new code on resend, invalidate previous codes
- Use **Transactional** Sender ID (promotional is filtered by DND)
- Send to one number per request (never batch OTP sends)

### 4. Country coverage pre-check

Call `coverage()` once at startup and cache the active prefixes. Before every send, check the number's country prefix against the cache. If the country is not active, return an error immediately without hitting the API.

### 5. Balance monitoring

- Save `balanceAfter` from every send response to your database/cache
- Set up low-balance alerts (e.g., below 50 credits)
- Before bulk sends, estimate cost (recipients x pages) and warn if insufficient

## Examples

See the [`examples/`](examples/) directory for complete runnable examples:

| Example | Description |
|---------|-------------|
| [01-basic-usage](examples/01-basic-usage/) | Verify credentials, send a message, check status |
| [02-otp-flow](examples/02-otp-flow/) | OTP generation, sending, and verification |
| [03-bulk-sms](examples/03-bulk-sms/) | Sending to large number lists with auto-batching |
| [04-error-handling](examples/04-error-handling/) | Handling all error codes and edge cases |

## FAQ

**1. My message was sent successfully (result: OK) but the recipient didn't receive it. What happened?**

Check the **Sending Queue** at [kwtsms.com](https://www.kwtsms.com/login/). If your message is stuck there, it was accepted by the API but not dispatched. Common causes are emoji in the message, hidden characters from copy-pasting, or spam filter triggers. Delete it from the queue to recover your credits. Also verify that `test` mode is off (`KWTSMS_TEST_MODE=0`). Test messages are queued but never delivered.

**2. What is the difference between Test mode and Live mode?**

**Test mode** (`KWTSMS_TEST_MODE=1`) sends your message to the kwtSMS queue but does NOT deliver it to the handset. No SMS credits are consumed. Use this during development. **Live mode** (`KWTSMS_TEST_MODE=0`) delivers the message for real and deducts credits. Always develop in test mode and switch to live only when ready for production.

**3. What is a Sender ID and why should I not use "KWT-SMS" in production?**

A **Sender ID** is the name that appears as the sender on the recipient's phone (e.g., "MY-APP" instead of a random number). `KWT-SMS` is a shared test sender. It causes delivery delays, is blocked on Virgin Kuwait, and should never be used in production. Register your own private Sender ID through your kwtSMS account. For OTP/authentication messages, you need a **Transactional** Sender ID to bypass DND (Do Not Disturb) filtering.

**4. I'm getting ERR003 "Authentication error". What's wrong?**

You are using the wrong credentials. The API requires your **API username and API password**, NOT your account mobile number. Log in to [kwtsms.com](https://www.kwtsms.com/login/), go to Account > API settings, and check your API credentials. Also make sure you are using POST (not GET) and `Content-Type: application/json`.

**5. Can I send to international numbers (outside Kuwait)?**

International sending is **disabled by default** on kwtSMS accounts. Contact kwtSMS support to request activation for specific country prefixes. Use `coverage()` to check which countries are currently active on your account. Be aware that activating international coverage increases exposure to automated abuse. Implement rate limiting and CAPTCHA before enabling.

## Help & Support

- **[kwtSMS FAQ](https://www.kwtsms.com/faq/)**: Answers to common questions about credits, sender IDs, OTP, and delivery
- **[kwtSMS Support](https://www.kwtsms.com/support.html)**: Open a support ticket or browse help articles
- **[Contact kwtSMS](https://www.kwtsms.com/#contact)**: Reach the kwtSMS team directly for Sender ID registration and account issues
- **[API Documentation (PDF)](https://www.kwtsms.com/doc/KwtSMS.com_API_Documentation_v41.pdf)**: kwtSMS REST API v4.1 full reference
- **[kwtSMS Dashboard](https://www.kwtsms.com/login/)**: Recharge credits, buy Sender IDs, view message logs, manage coverage
- **[Other Integrations](https://www.kwtsms.com/integrations.html)**: Plugins and integrations for other platforms and languages

## License

MIT
