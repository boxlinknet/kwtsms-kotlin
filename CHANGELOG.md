# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.3] - 2026-03-14

### Added
- Country-specific phone validation: 80+ countries with local length and mobile prefix rules
- `PHONE_RULES` map with validation rules per country code
- `COUNTRY_NAMES` map for human-readable error messages
- `findCountryCode()` to match country code from normalized number (longest match)
- `validatePhoneFormat()` to validate against country-specific format rules
- Domestic trunk prefix stripping in `normalizePhone()` (e.g. 9660559... to 966559...)
- GitGuardian secret scanning workflow
- Dependabot commit message prefix (`deps`)

## [0.1.2] - 2026-03-06

### Added
- Example 04: Ktor web framework endpoint integration
- Example 06: Production OTP with OtpService, rate limiting, secure hashing
  - Database adapters: MemoryStore (dev), SqliteStore (production)
  - CAPTCHA verifiers: TurnstileVerifier, HcaptchaVerifier
  - Device attestation: PlayIntegrityVerifier (Android)

### Changed
- Renumbered examples: error handling moved from 04 to 05
- README title changed to "kwtSMS Kotlin Client"

## [0.1.1] - 2026-03-06

### Added
- README badges (CI, CodeQL, JitPack, License, Java version)
- Best Practices section in README (local validation, user-facing errors, OTP, coverage, balance)
- Examples reference table in README
- Android Google Play Integrity API guidance

### Changed
- Bumped CI actions: checkout v4→v6, gradle/actions v4→v5, upload-artifact v4→v7, codeql-action v3→v4
- Merged Dependabot PR for actions/setup-java v4→v5

## [0.1.0] - 2026-03-06

Initial release of the `kwtsms` Kotlin client library.

### Added

**Core client (`KwtSMS`)**
- `KwtSMS(username, password, senderId, testMode, logFile)` constructor
- `KwtSMS.fromEnv(envFile)` factory method: loads credentials from env vars with .env fallback
- `verify()`: test credentials, returns `VerifyResult` with ok/balance/error
- `balance()`: get current credits, caches result, falls back to cached value on failure
- `send(mobile, message, sender)`: send SMS to one or more numbers (comma-separated or list)
- `sendBulk(mobiles, message, sender)`: auto-batches >200 numbers with 0.5s delay, ERR013 retry (30s/60s/120s)
- `validate(phones)`: validate phone numbers with API, local pre-validation
- `senderIds()`: list available sender IDs
- `coverage()`: list active country prefixes
- `status(msgId)`: check message queue status
- `deliveryReport(msgId)`: get delivery reports (international numbers only)
- Cached balance tracking: `cachedBalance`, `cachedPurchased` (volatile, thread-safe)
- Phone number deduplication before sending

**Phone normalization (`PhoneUtils`)**
- `normalizePhone()`: Arabic-Indic and Extended Arabic-Indic digit conversion, strip non-digits, strip leading zeros
- `validatePhoneInput()`: catches empty, email, no-digits, too short (<7), too long (>15)
- `deduplicatePhones()`: order-preserving deduplication

**Message cleaning (`MessageUtils`)**
- `cleanMessage()`: strips emojis (17 Unicode ranges), hidden invisible chars (7 types), directional formatting (10 code points), C0/C1 controls (preserves \n and \t), HTML tags; converts Arabic digits to Latin

**Error handling**
- 29 error codes mapped to developer-friendly action messages (`API_ERRORS`)
- `enrichError()`: adds action field to API error responses
- All methods return structured result types, never throw

**HTTP layer**
- Zero dependencies: uses `java.net.HttpURLConnection`
- Custom JSON parser and serializer (no org.json/Gson/Moshi dependency)
- 15-second timeout, reads 4xx/5xx response bodies
- `Content-Type: application/json` and `Accept: application/json` on every request

**Logging**
- JSONL logging with ISO8601 timestamps (UTC)
- Password masked as `***` in all logs
- Logging failures never crash the main flow

**Tests**
- `PhoneUtilsTest.kt`: 19 tests (normalize, validate, deduplicate)
- `MessageUtilsTest.kt`: 36 tests (preservation, conversion, stripping, edge cases)
- `ApiErrorsTest.kt`: 22 tests (error map, enrichment, mocked API responses)
- `EnvLoaderTest.kt`: 9 tests (.env parsing)
- `JsonUtilsTest.kt`: 14 tests (serialize, parse, roundtrip)
- `IntegrationTest.kt`: 18 tests (live API with test mode, skipped without credentials)

**Documentation**
- README with install, quick start, all methods, utility functions, error handling, FAQ
- 4 example programs with companion documentation
- CHANGELOG, CONTRIBUTING, LICENSE

[Unreleased]: https://github.com/boxlinknet/kwtsms-kotlin/compare/v0.1.3...HEAD
[0.1.3]: https://github.com/boxlinknet/kwtsms-kotlin/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/boxlinknet/kwtsms-kotlin/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/boxlinknet/kwtsms-kotlin/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/boxlinknet/kwtsms-kotlin/releases/tag/v0.1.0
