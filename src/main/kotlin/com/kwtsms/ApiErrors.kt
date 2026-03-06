package com.kwtsms

/**
 * Complete mapping of kwtSMS error codes to developer-friendly action messages.
 */
val API_ERRORS: Map<String, String> = mapOf(
    "ERR001" to "API is disabled on this account. Enable it at kwtsms.com → Account → API.",
    "ERR002" to "A required parameter is missing. Check that username, password, sender, mobile, and message are all provided.",
    "ERR003" to "Wrong API username or password. Check KWTSMS_USERNAME and KWTSMS_PASSWORD. These are your API credentials, not your account mobile number.",
    "ERR004" to "This account does not have API access. Contact kwtSMS support to enable it.",
    "ERR005" to "This account is blocked. Contact kwtSMS support.",
    "ERR006" to "No valid phone numbers. Make sure each number includes the country code (e.g., 96598765432 for Kuwait, not 98765432).",
    "ERR007" to "Too many numbers in a single request (maximum 200). Split into smaller batches.",
    "ERR008" to "This sender ID is banned. Use a different sender ID registered on your kwtSMS account.",
    "ERR009" to "Message is empty. Provide a non-empty message text.",
    "ERR010" to "Account balance is zero. Recharge credits at kwtsms.com.",
    "ERR011" to "Insufficient balance for this send. Buy more credits at kwtsms.com.",
    "ERR012" to "Message is too long (over 6 SMS pages). Shorten your message.",
    "ERR013" to "Send queue is full (1000 messages). Wait a moment and try again.",
    "ERR019" to "No delivery reports found for this message.",
    "ERR020" to "Message ID does not exist. Make sure you saved the msg-id from the send response.",
    "ERR021" to "No delivery report available for this message yet.",
    "ERR022" to "Delivery reports are not ready yet. Try again after 24 hours.",
    "ERR023" to "Unknown delivery report error. Contact kwtSMS support.",
    "ERR024" to "Your IP address is not in the API whitelist. Add it at kwtsms.com → Account → API → IP Lockdown, or disable IP lockdown.",
    "ERR025" to "Invalid phone number. Make sure the number includes the country code (e.g., 96598765432 for Kuwait, not 98765432).",
    "ERR026" to "This country is not activated on your account. Contact kwtSMS support to enable the destination country.",
    "ERR027" to "HTML tags are not allowed in the message. Remove any HTML content and try again.",
    "ERR028" to "You must wait at least 15 seconds before sending to the same number again. No credits were consumed.",
    "ERR029" to "Message ID does not exist or is incorrect.",
    "ERR030" to "Message is stuck in the send queue with an error. Delete it at kwtsms.com → Queue to recover credits.",
    "ERR031" to "Message rejected: bad language detected.",
    "ERR032" to "Message rejected: spam detected.",
    "ERR033" to "No active coverage found. Contact kwtSMS support.",
    "ERR_INVALID_INPUT" to "One or more phone numbers are invalid. See details above."
)

/**
 * Enrich an API error response with a developer-friendly action message.
 *
 * If the response contains result=ERROR and a known error code,
 * an "action" field is added with guidance.
 */
fun enrichError(response: Map<String, Any?>): Map<String, Any?> {
    val result = response["result"] as? String ?: return response
    if (result != "ERROR") return response

    val code = response["code"] as? String ?: return response
    val action = API_ERRORS[code] ?: return response

    return response.toMutableMap().apply {
        this["action"] = action
    }
}
