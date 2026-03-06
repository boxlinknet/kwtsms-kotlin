package otp

import com.kwtsms.KwtSMS
import otp.adapters.MemoryStore
import otp.captcha.TurnstileVerifier

/**
 * Production OTP example: wires together OtpService with a store and CAPTCHA verifier.
 *
 * This demonstrates the setup. In a real app, you would call sendOtp() and verifyOtp()
 * from your HTTP endpoint handler (Ktor, Spring, etc.).
 */
fun main() {
    // 1. Initialize kwtSMS client
    val sms = KwtSMS.fromEnv()

    // 2. Choose a store (MemoryStore for dev, SqliteStore for production)
    val store = MemoryStore()

    // 3. Choose bot protection
    //    Web apps: TurnstileVerifier or HcaptchaVerifier
    //    Android apps: PlayIntegrityVerifier
    val captcha = TurnstileVerifier(
        secretKey = System.getenv("TURNSTILE_SECRET") ?: "test-secret"
    )

    // 4. Create OTP service with configuration
    val otpService = OtpService(
        sms = sms,
        store = store,
        captchaVerifier = captcha,
        config = OtpServiceConfig(
            codeLength = 6,
            codeExpiryMs = 5 * 60 * 1000,      // 5 minutes
            resendCooldownMs = 4 * 60 * 1000,   // 4 minutes (KNET standard)
            maxAttempts = 5,
            ipLimitPerHour = 15,
            phoneLimitPerHour = 5,
            appName = "MyApp"
        )
    )

    // 5. Send OTP (from your HTTP endpoint)
    val sendResult = otpService.sendOtp(
        phone = "+965 9876 5432",
        botToken = "captcha-token-from-frontend",
        ip = "192.168.1.1"
    )

    if (sendResult.success) {
        println("OTP sent successfully!")
    } else {
        println("Failed: ${sendResult.error}")
        sendResult.retryAfterMs?.let {
            println("Retry after: ${it / 1000} seconds")
        }
    }

    // 6. Verify OTP (from your verification endpoint)
    val verifyResult = otpService.verifyOtp(
        phone = "+965 9876 5432",
        code = "123456",
        ip = "192.168.1.1"
    )

    if (verifyResult.success) {
        println("OTP verified! User authenticated.")
    } else {
        println("Verification failed: ${verifyResult.error}")
        verifyResult.attemptsRemaining?.let {
            println("Attempts remaining: $it")
        }
    }
}
