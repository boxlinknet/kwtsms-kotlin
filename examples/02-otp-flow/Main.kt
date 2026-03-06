import com.kwtsms.KwtSMS
import com.kwtsms.PhoneUtils

/**
 * OTP flow: send a one-time password and verify it.
 *
 * Key rules for OTP:
 * - Always include app/company name in the message (telecom compliance)
 * - Use a Transactional Sender ID (not promotional, not KWT-SMS)
 * - Send to one number per request (never batch OTP sends)
 * - Generate a new code on every resend, invalidate the previous one
 * - Minimum 3-4 minute resend cooldown (KNET standard: 4 minutes)
 */
fun main() {
    val sms = KwtSMS.fromEnv()

    // Validate the phone number locally before calling the API
    val phone = "+965 9876 5432"
    val (valid, error, normalized) = PhoneUtils.validatePhoneInput(phone)
    if (!valid) {
        println("Invalid phone number: $error")
        return
    }

    // Generate a 6-digit OTP using secure random
    val otp = (100000 + java.security.SecureRandom().nextInt(900000)).toString()

    // Include app name for telecom compliance
    val message = "Your OTP for MyApp is: $otp"

    // Send OTP to a single number (never batch OTP sends)
    val result = sms.send(normalized, message)
    if (result.result == "OK") {
        println("OTP sent to $normalized")
        println("Message ID: ${result.msgId}")
        // Save msgId and the hashed OTP in your database
        // In production: hash OTP with bcrypt before storing
    } else {
        println("Failed to send OTP: ${result.description}")
        result.action?.let { println("Action: $it") }
    }
}
