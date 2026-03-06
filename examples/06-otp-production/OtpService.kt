package otp

import com.kwtsms.KwtSMS
import com.kwtsms.PhoneUtils
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Production-grade OTP service with rate limiting, bot protection, and secure storage.
 *
 * Drop-in implementation — copy into your project and wire up an OtpStore adapter
 * and a CaptchaVerifier (web) or DeviceAttestVerifier (mobile).
 *
 * Flow: sanitize phone -> verify bot protection -> check IP rate limit ->
 *       check phone rate limit -> resend cooldown -> generate code ->
 *       hash (SHA-256 + salt) -> store -> send SMS -> rollback on failure
 */

// ── Configuration ──

data class OtpServiceConfig(
    val codeLength: Int = 6,
    val codeExpiryMs: Long = 5 * 60 * 1000,         // 5 minutes
    val resendCooldownMs: Long = 4 * 60 * 1000,      // 4 minutes (KNET standard)
    val maxAttempts: Int = 5,
    val ipLimitPerHour: Int = 15,
    val phoneLimitPerHour: Int = 5,
    val verifyLimitPerHour: Int = 10,
    val appName: String = "MyApp"
)

// ── Result types ──

data class SendOtpResult(
    val success: Boolean,
    val error: String? = null,
    val retryAfterMs: Long? = null
)

data class VerifyOtpResult(
    val success: Boolean,
    val error: String? = null,
    val attemptsRemaining: Int? = null
)

// ── Store interface ──

data class OtpRecord(
    val phone: String,
    val codeHash: String,
    val codeSalt: String,
    val createdAt: Long,
    val expiresAt: Long,
    val attempts: Int = 0,
    val used: Boolean = false
)

interface OtpStore {
    fun save(record: OtpRecord)
    fun findLatest(phone: String): OtpRecord?
    fun incrementAttempts(phone: String)
    fun markUsed(phone: String)
    fun delete(phone: String)
    fun countByPhone(phone: String, sinceMs: Long): Int
    fun countByIp(ip: String, sinceMs: Long): Int
    fun recordIpUsage(ip: String)
}

// ── Bot prevention interfaces ──

/** For web apps: verify CAPTCHA token (Turnstile, hCaptcha, reCAPTCHA) */
interface CaptchaVerifier {
    fun verify(token: String, ip: String): Boolean
}

/** For mobile apps: verify device attestation token (Play Integrity, App Attest) */
interface DeviceAttestVerifier {
    fun verify(attestationToken: String): Boolean
}

/** Optional: verify JWT/session token for 2FA flows */
interface TokenAuthenticator {
    fun validate(token: String): Boolean
}

// ── OTP Service ──

class OtpService(
    private val sms: KwtSMS,
    private val store: OtpStore,
    private val captchaVerifier: CaptchaVerifier? = null,
    private val attestVerifier: DeviceAttestVerifier? = null,
    private val tokenAuth: TokenAuthenticator? = null,
    private val config: OtpServiceConfig = OtpServiceConfig()
) {
    private val random = SecureRandom()

    /**
     * Send OTP to a phone number.
     *
     * @param phone raw phone input (any format)
     * @param botToken CAPTCHA token (web) or attestation token (mobile)
     * @param ip client IP address for rate limiting
     * @param authToken optional JWT/session token for 2FA flows
     */
    fun sendOtp(
        phone: String,
        botToken: String,
        ip: String,
        authToken: String? = null
    ): SendOtpResult {
        // 1. Sanitize phone
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput(phone)
        if (!valid || normalized == null) {
            return SendOtpResult(false, "Please enter a valid phone number.")
        }

        // 2. Verify bot protection
        val botVerified = when {
            captchaVerifier != null -> captchaVerifier.verify(botToken, ip)
            attestVerifier != null -> attestVerifier.verify(botToken)
            else -> false
        }
        if (!botVerified) {
            return SendOtpResult(false, "Verification failed. Please try again.")
        }

        // 3. Verify auth token (optional, for 2FA flows)
        if (tokenAuth != null && authToken != null) {
            if (!tokenAuth.validate(authToken)) {
                return SendOtpResult(false, "Session expired. Please log in again.")
            }
        }

        // 4. Check IP rate limit
        val ipCount = store.countByIp(ip, System.currentTimeMillis() - 3600_000)
        if (ipCount >= config.ipLimitPerHour) {
            return SendOtpResult(false, "Too many requests. Please try again later.")
        }

        // 5. Check phone rate limit
        val phoneCount = store.countByPhone(normalized, System.currentTimeMillis() - 3600_000)
        if (phoneCount >= config.phoneLimitPerHour) {
            return SendOtpResult(
                false,
                "Too many requests to this number. Please try again later.",
                retryAfterMs = config.resendCooldownMs
            )
        }

        // 6. Check resend cooldown
        val existing = store.findLatest(normalized)
        if (existing != null) {
            val elapsed = System.currentTimeMillis() - existing.createdAt
            if (elapsed < config.resendCooldownMs) {
                val remaining = config.resendCooldownMs - elapsed
                return SendOtpResult(
                    false,
                    "Please wait before requesting another code.",
                    retryAfterMs = remaining
                )
            }
        }

        // 7. Generate code using SecureRandom (not Math.random)
        val upperBound = Math.pow(10.0, config.codeLength.toDouble()).toInt()
        val lowerBound = Math.pow(10.0, (config.codeLength - 1).toDouble()).toInt()
        val code = (lowerBound + random.nextInt(upperBound - lowerBound)).toString()

        // 8. Hash with SHA-256 + random salt (never store plain codes)
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hash = hashCode(code, salt)

        // 9. Store the record
        val now = System.currentTimeMillis()
        val record = OtpRecord(
            phone = normalized,
            codeHash = hash,
            codeSalt = saltBase64,
            createdAt = now,
            expiresAt = now + config.codeExpiryMs
        )

        // Record rate limit usage BEFORE send (prevent bypass by failing sends)
        store.recordIpUsage(ip)
        store.save(record)

        // 10. Send SMS (one number per OTP request, never batch)
        val message = "Your OTP for ${config.appName} is: $code"
        val result = sms.send(normalized, message)

        if (result.result != "OK") {
            // Rollback the OTP record on send failure
            // Do NOT rollback rate limit counters (prevents bypass)
            store.delete(normalized)
            return SendOtpResult(false, "Could not send SMS. Please try again later.")
        }

        return SendOtpResult(true)
    }

    /**
     * Verify an OTP code.
     *
     * @param phone raw phone input
     * @param code the code entered by the user
     * @param ip client IP for rate limiting
     */
    fun verifyOtp(phone: String, code: String, ip: String): VerifyOtpResult {
        // 1. Sanitize inputs
        val (valid, _, normalized) = PhoneUtils.validatePhoneInput(phone)
        if (!valid || normalized == null) {
            return VerifyOtpResult(false, "Invalid phone number.")
        }

        val cleanCode = code.trim()
        if (cleanCode.length != config.codeLength || !cleanCode.all { it.isDigit() }) {
            return VerifyOtpResult(false, "Invalid code format.")
        }

        // 2. Check verify rate limit
        val ipCount = store.countByIp(ip, System.currentTimeMillis() - 3600_000)
        if (ipCount >= config.verifyLimitPerHour) {
            return VerifyOtpResult(false, "Too many attempts. Please try again later.")
        }

        // 3. Load record
        val record = store.findLatest(normalized)
            ?: return VerifyOtpResult(false, "No OTP found for this number. Please request a new code.")

        // 4. Check if already used
        if (record.used) {
            return VerifyOtpResult(false, "This code has already been used. Please request a new code.")
        }

        // 5. Check expiry
        if (System.currentTimeMillis() > record.expiresAt) {
            store.delete(normalized)
            return VerifyOtpResult(false, "Code expired. Please request a new code.")
        }

        // 6. Check max attempts
        if (record.attempts >= config.maxAttempts) {
            store.delete(normalized)
            return VerifyOtpResult(false, "Too many wrong attempts. Please request a new code.")
        }

        // 7. Compare hash (timing-safe via MessageDigest.isEqual)
        val salt = Base64.getDecoder().decode(record.codeSalt)
        val inputHash = hashCode(cleanCode, salt)
        val match = MessageDigest.isEqual(
            inputHash.toByteArray(Charsets.UTF_8),
            record.codeHash.toByteArray(Charsets.UTF_8)
        )

        store.recordIpUsage(ip)

        if (!match) {
            store.incrementAttempts(normalized)
            val remaining = config.maxAttempts - record.attempts - 1
            return VerifyOtpResult(false, "Invalid code.", attemptsRemaining = remaining)
        }

        // 8. Mark as used
        store.markUsed(normalized)
        return VerifyOtpResult(true)
    }

    /** SHA-256 hash with salt */
    private fun hashCode(code: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(code.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }
}
