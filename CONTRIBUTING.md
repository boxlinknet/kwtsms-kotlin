# Contributing

Contributions are welcome: bug reports, fixes, new examples, and documentation improvements.

## Before You Start

- Search [existing issues](https://github.com/boxlinknet/kwtsms-kotlin/issues) before opening a new one
- Open an issue before starting large changes to discuss the approach
- All contributions must pass the test suite

## Development Setup

### Prerequisites

- Java 8+ (download from [Adoptium](https://adoptium.net/))
- Gradle 8+ (or use the included `./gradlew` wrapper)

### Clone and build

```bash
git clone https://github.com/boxlinknet/kwtsms-kotlin.git
cd kwtsms-kotlin
./gradlew build
```

### Verify the setup

```bash
./gradlew test
```

## Running Tests

### Tier 1: Unit tests (no network, no credentials)

```bash
./gradlew test --tests "com.kwtsms.PhoneUtilsTest"
./gradlew test --tests "com.kwtsms.MessageUtilsTest"
./gradlew test --tests "com.kwtsms.ApiErrorsTest"
./gradlew test --tests "com.kwtsms.EnvLoaderTest"
./gradlew test --tests "com.kwtsms.JsonUtilsTest"
```

### Tier 2: All unit tests

```bash
./gradlew test
```

### Tier 3: Integration tests (requires credentials)

```bash
KOTLIN_USERNAME=your_api_user KOTLIN_PASSWORD=your_api_pass ./gradlew test --tests "com.kwtsms.IntegrationTest"
```

Integration tests use `testMode=true` (no credits consumed). They are skipped automatically if credentials are not set.

## Build

```bash
./gradlew build
```

Output: `build/libs/kwtsms-0.1.0.jar`

## Project Structure

```
kwtsms-kotlin/
├── build.gradle.kts              # Gradle build config (Kotlin DSL)
├── settings.gradle.kts           # Project name
├── jitpack.yml                   # JitPack JDK configuration
├── src/main/kotlin/com/kwtsms/
│   ├── KwtSMS.kt                 # Main client class (all API methods)
│   ├── Models.kt                 # Data classes (SendResult, BulkSendResult, etc.)
│   ├── PhoneUtils.kt             # normalizePhone(), validatePhoneInput(), deduplicatePhones()
│   ├── MessageUtils.kt           # cleanMessage()
│   ├── ApiErrors.kt              # Error code map, enrichError()
│   ├── Request.kt                # HTTP POST, JSON parser/serializer
│   ├── EnvLoader.kt              # .env file parser
│   └── Logger.kt                 # JSONL logger
├── src/test/kotlin/com/kwtsms/
│   ├── PhoneUtilsTest.kt         # Phone normalization and validation tests
│   ├── MessageUtilsTest.kt       # Message cleaning tests
│   ├── ApiErrorsTest.kt          # Error mapping and mocked API tests
│   ├── EnvLoaderTest.kt          # .env parsing tests
│   ├── JsonUtilsTest.kt          # JSON serializer/parser tests
│   └── IntegrationTest.kt        # Live API tests (test mode)
└── examples/                     # Runnable example programs
```

## Making Changes

### Branch naming

```
fix/short-description        # bug fix
feat/short-description       # new feature
docs/short-description       # documentation only
test/short-description       # tests only
chore/short-description      # build, tooling, dependency updates
```

### Commit style (Conventional Commits)

```
<type>: <short description>

[optional body]
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

Examples:
```
feat: add status() method for message queue lookup
fix: handle ERR028 (15s same-number cooldown) in bulk send
test: cover Arabic digit normalization edge cases
```

## Adding a New Method

1. Write a failing test in `src/test/kotlin/com/kwtsms/`
2. Run `./gradlew test` and verify it fails
3. Implement the method in the appropriate source file
4. Run `./gradlew test` and verify it passes
5. Add a result data class to `Models.kt` if needed
6. Update the README with signature, parameters, and example
7. Update CHANGELOG.md under `[Unreleased]`

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes following the guidelines above
3. Run `./gradlew test` and ensure all tests pass
4. Push and open a PR against `main`

### PR checklist

```
- [ ] Tests added/updated for all changed behavior
- [ ] All existing tests pass (`./gradlew test`)
- [ ] Build succeeds without warnings (`./gradlew build`)
- [ ] CHANGELOG.md updated under [Unreleased]
- [ ] No new runtime dependencies added (zero-dep policy)
- [ ] Public types exported if new public types added
```

## Reporting Bugs

Include in your bug report:
- Kotlin version (`kotlin -version`)
- Java version (`java -version`)
- kwtsms-kotlin version
- Minimal code to reproduce the issue
- Expected vs actual behavior

## Security Issues

Do not open public issues for security vulnerabilities. Use [GitHub Security Advisories](https://github.com/boxlinknet/kwtsms-kotlin/security/advisories) or contact support directly.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
