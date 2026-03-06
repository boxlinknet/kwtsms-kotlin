import com.kwtsms.KwtSMS
import com.kwtsms.PhoneUtils
import com.kwtsms.MessageUtils
import com.kwtsms.API_ERRORS

/**
 * Error handling: demonstrates all error paths and user-facing message patterns.
 */
fun main() {
    val sms = KwtSMS.fromEnv()

    // ── Local validation errors (no API call needed) ──

    val inputs = listOf(
        "",                    // empty
        "user@example.com",    // email
        "12345",               // too short
        "abcdef",              // no digits
        "+96598765432"         // valid
    )

    println("=== Phone Validation ===")
    for (input in inputs) {
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput(input)
        if (valid) {
            println("  '$input' -> OK (normalized: $normalized)")
        } else {
            println("  '$input' -> ERROR: $error")
        }
    }

    // ── Message cleaning ──

    println("\n=== Message Cleaning ===")
    val dirtyMessages = listOf(
        "Hello \uD83D\uDE00 World",           // emoji
        "<b>Bold</b> text",                     // HTML
        "\uFEFFBOM prefix",                     // BOM
        "OTP: \u0661\u0662\u0663\u0664",       // Arabic digits
        "Clean message"                         // nothing to clean
    )

    for (msg in dirtyMessages) {
        val cleaned = MessageUtils.cleanMessage(msg)
        println("  Input:   '$msg'")
        println("  Cleaned: '$cleaned'")
        println()
    }

    // ── API error handling ──

    println("=== API Errors ===")
    val result = sms.send("96598765432", "Test message")
    when (result.result) {
        "OK" -> {
            println("Sent successfully! Balance: ${result.balanceAfter}")
        }
        "ERROR" -> {
            println("Error: ${result.description}")
            result.action?.let { println("Action: $it") }
            result.code?.let { code ->
                // Map to user-facing messages
                val userMessage = when (code) {
                    "ERR006", "ERR025" -> "Please enter a valid phone number in international format (e.g., +965 9876 5432)."
                    "ERR003" -> "SMS service is temporarily unavailable. Please try again later."
                    "ERR010", "ERR011" -> "SMS service is temporarily unavailable. Please try again later."
                    "ERR026" -> "SMS delivery to this country is not available. Please contact support."
                    "ERR028" -> "Please wait a moment before requesting another code."
                    "ERR031", "ERR032" -> "Your message could not be sent. Please try again with different content."
                    "ERR013" -> "SMS service is busy. Please try again in a few minutes."
                    else -> "Something went wrong. Please try again later."
                }
                println("User-facing: $userMessage")
            }
        }
    }

    // ── Browse all error codes ──

    println("\n=== All Error Codes (${API_ERRORS.size}) ===")
    for ((code, action) in API_ERRORS) {
        println("  $code: $action")
    }
}
