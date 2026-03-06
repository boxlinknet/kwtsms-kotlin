import com.kwtsms.KwtSMS
import com.kwtsms.PhoneUtils
import com.kwtsms.MessageUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Ktor web framework integration: a single POST endpoint that sends an SMS.
 *
 * Demonstrates how to wire kwtSMS into a Ktor HTTP endpoint with:
 * - Input validation before API call
 * - User-facing error messages (never expose raw API errors)
 * - Proper HTTP status codes
 *
 * Dependencies (add to build.gradle.kts):
 *   implementation("io.ktor:ktor-server-netty:2.3.7")
 *   implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
 *   implementation("com.github.boxlinknet:kwtsms-kotlin:0.1.2")
 */
fun main() {
    val sms = KwtSMS.fromEnv()

    embeddedServer(Netty, port = 8080) {
        routing {
            post("/send-sms") {
                // Parse request body
                val params = call.receiveParameters()
                val phone = params["phone"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Phone number is required.")
                )
                val message = params["message"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Message is required.")
                )

                // Step 1: Validate phone locally (no API call)
                val (valid, error, normalized) = PhoneUtils.validatePhoneInput(phone)
                if (!valid) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Please enter a valid phone number (e.g., +965 9876 5432).")
                    )
                }

                // Step 2: Clean message
                val cleaned = MessageUtils.cleanMessage(message)
                if (cleaned.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Message is empty after cleaning.")
                    )
                }

                // Step 3: Send via kwtSMS
                val result = sms.send(normalized!!, cleaned)
                if (result.result == "OK") {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "messageId" to result.msgId,
                        "balance" to result.balanceAfter
                    ))
                } else {
                    // Map API errors to user-facing messages
                    val userMessage = when (result.code) {
                        "ERR003", "ERR010", "ERR011" ->
                            "SMS service is temporarily unavailable. Please try again later."
                        "ERR006", "ERR025" ->
                            "Please enter a valid phone number in international format."
                        "ERR026" ->
                            "SMS delivery to this country is not available."
                        "ERR028" ->
                            "Please wait a moment before sending again."
                        "ERR031", "ERR032" ->
                            "Your message could not be sent. Please try again with different content."
                        else ->
                            "Something went wrong. Please try again later."
                    }

                    // Log the real error for admin debugging
                    println("SMS error: ${result.code} - ${result.description}")

                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to userMessage
                    ))
                }
            }
        }
    }.start(wait = true)
}
