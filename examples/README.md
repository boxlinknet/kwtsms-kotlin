# Examples

| # | Example | Description | Credentials needed? |
|---|---------|-------------|-------------------|
| 01 | [Basic Usage](01-basic-usage/) | Verify, send, check balance, list sender IDs and coverage | Yes |
| 02 | [OTP Flow](02-otp-flow/) | Send a one-time password with phone validation and secure generation | Yes |
| 03 | [Bulk SMS](03-bulk-sms/) | Send to >200 numbers with automatic batching and retry | Yes |
| 04 | [Ktor Endpoint](04-ktor-endpoint/) | Web framework integration with input validation and user-facing errors | Yes |
| 05 | [Error Handling](05-error-handling/) | All error paths, validation, cleaning, and user-facing message mapping | Partial |
| 06 | [Production OTP](06-otp-production/) | Drop-in OTP with DB storage, rate limiting, CAPTCHA, device attestation | Yes |

## Running examples

1. Set up your `.env` file with kwtSMS credentials (see each example's README)
2. Set `KWTSMS_TEST_MODE=1` during development (no credits consumed, no messages delivered)
3. Run with: `kotlinc -cp kwtsms.jar ExampleFile.kt -include-runtime -d example.jar && java -jar example.jar`

Or add the examples to a Gradle project that depends on `kwtsms-kotlin`.
