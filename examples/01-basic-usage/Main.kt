import com.kwtsms.KwtSMS

/**
 * Basic usage: verify credentials, check balance, send a single SMS.
 *
 * Before running, create a .env file:
 *   KWTSMS_USERNAME=kotlin_username
 *   KWTSMS_PASSWORD=kotlin_password
 *   KWTSMS_SENDER_ID=YOUR-SENDERID
 *   KWTSMS_TEST_MODE=1
 */
fun main() {
    // Load client from environment variables / .env file
    val sms = KwtSMS.fromEnv()

    // Step 1: Verify credentials
    val verify = sms.verify()
    if (!verify.ok) {
        println("Verification failed: ${verify.error}")
        return
    }
    println("Credentials OK. Balance: ${verify.balance} credits")

    // Step 2: List sender IDs
    val senderResult = sms.senderIds()
    if (senderResult.result == "OK") {
        println("Available sender IDs: ${senderResult.senderIds}")
    }

    // Step 3: Check coverage
    val coverageResult = sms.coverage()
    if (coverageResult.result == "OK") {
        println("Active country prefixes: ${coverageResult.prefixes}")
    }

    // Step 4: Send an SMS
    val result = sms.send("96598765432", "Hello from kwtSMS Kotlin client!")
    if (result.result == "OK") {
        println("SMS sent successfully!")
        println("  Message ID: ${result.msgId}")
        println("  Numbers: ${result.numbers}")
        println("  Credits used: ${result.pointsCharged}")
        println("  Balance after: ${result.balanceAfter}")
    } else {
        println("Send failed: ${result.description}")
        result.action?.let { println("  Action: $it") }
    }
}
