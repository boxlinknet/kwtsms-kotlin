# 04 - Ktor Endpoint

Demonstrates how to integrate kwtSMS into a Ktor web framework endpoint. A single `POST /send-sms` route validates input locally, cleans the message, sends via kwtSMS, and returns user-facing error messages (never raw API errors).

## Prerequisites

- Java 8+
- kwtSMS account with API credentials
- Ktor dependencies added to your project

## Environment Setup

Create a `.env` file:

```ini
KWTSMS_USERNAME=your_api_user
KWTSMS_PASSWORD=your_api_pass
KWTSMS_SENDER_ID=YOUR-SENDERID
KWTSMS_TEST_MODE=1
```

Add dependencies to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.boxlinknet:kwtsms-kotlin:0.1.2")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
}
```

## Walkthrough

1. **Initialize client** from environment variables using `KwtSMS.fromEnv()`
2. **Parse request** parameters (`phone` and `message`) from the POST body
3. **Validate phone locally** with `PhoneUtils.validatePhoneInput()` before any API call
4. **Clean message** with `MessageUtils.cleanMessage()` to strip emojis, HTML, and invisible characters
5. **Send via API** and map errors to user-friendly messages (never expose ERR codes to end users)

## Expected Output

```
curl -X POST http://localhost:8080/send-sms \
  -d "phone=+96598765432" \
  -d "message=Hello from Ktor!"

{"success":true,"messageId":"f4c841adee210f31307633ceaebff2ec","balance":180.0}
```

## Common Mistakes

1. **Exposing raw API errors to users.** Never return `ERR003` or `Authentication error` to end users. Map to generic messages and log the real error for admins.
2. **Skipping local validation.** Always call `validatePhoneInput()` and `cleanMessage()` before hitting the API. Saves round-trips and credits.
3. **Missing rate limiting.** This example omits rate limiting for simplicity. In production, add per-IP and per-phone rate limits before the SMS send logic.
