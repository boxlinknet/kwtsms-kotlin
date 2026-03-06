package com.kwtsms

/**
 * Official Kotlin client for the kwtSMS SMS gateway API.
 *
 * Thread-safe: this class can be shared across threads.
 * All methods are blocking (no coroutine dependency required).
 *
 * @param username API username (not your account mobile number)
 * @param password API password
 * @param senderId Sender ID (default: "KWT-SMS" for testing only)
 * @param testMode When true, messages enter the queue but are not delivered (no credits consumed)
 * @param logFile Path to JSONL log file (empty string to disable logging)
 */
class KwtSMS(
    private val username: String,
    private val password: String,
    private val senderId: String = "KWT-SMS",
    private val testMode: Boolean = false,
    private val logFile: String = "kwtsms.log"
) {
    @Volatile
    var cachedBalance: Double? = null
        private set

    @Volatile
    var cachedPurchased: Double? = null
        private set

    companion object {
        private const val MAX_BATCH_SIZE = 200
        private const val BATCH_DELAY_MS = 500L
        private val ERR013_RETRY_DELAYS = longArrayOf(30_000L, 60_000L, 120_000L)

        /**
         * Create a KwtSMS client from environment variables and/or .env file.
         *
         * Reads: KWTSMS_USERNAME, KWTSMS_PASSWORD, KWTSMS_SENDER_ID, KWTSMS_TEST_MODE, KWTSMS_LOG_FILE.
         * Environment variables take precedence over .env file values.
         */
        @JvmStatic
        fun fromEnv(envFile: String = ".env"): KwtSMS {
            val fileVars = EnvLoader.loadEnvFile(envFile)

            fun resolve(key: String, default: String = ""): String {
                return System.getenv(key)?.takeIf { it.isNotBlank() }
                    ?: fileVars[key]?.takeIf { it.isNotBlank() }
                    ?: default
            }

            val username = resolve("KWTSMS_USERNAME")
            val password = resolve("KWTSMS_PASSWORD")
            val senderId = resolve("KWTSMS_SENDER_ID", "KWT-SMS")
            val testMode = resolve("KWTSMS_TEST_MODE", "0") == "1"
            val logFile = resolve("KWTSMS_LOG_FILE", "kwtsms.log")

            return KwtSMS(
                username = username,
                password = password,
                senderId = senderId,
                testMode = testMode,
                logFile = logFile
            )
        }
    }

    private fun basePayload(): MutableMap<String, Any?> {
        return mutableMapOf(
            "username" to username,
            "password" to password
        )
    }

    // ──────────────────────────────────────────────
    // verify()
    // ──────────────────────────────────────────────

    /**
     * Test credentials and get balance. Never throws.
     */
    fun verify(): VerifyResult {
        return try {
            val response = ApiRequest.post("balance", basePayload(), logFile)
            val result = response["result"]?.toString()

            if (result == "OK") {
                val available = asDouble(response["available"])
                val purchased = asDouble(response["purchased"])
                cachedBalance = available
                cachedPurchased = purchased
                VerifyResult(ok = true, balance = available)
            } else {
                val enriched = enrichError(response)
                val desc = enriched["description"]?.toString() ?: "Unknown error"
                val action = enriched["action"]?.toString()
                val errorMsg = if (action != null) "$desc $action" else desc
                VerifyResult(ok = false, error = errorMsg)
            }
        } catch (e: Exception) {
            VerifyResult(ok = false, error = e.message ?: "Unknown error")
        }
    }

    // ──────────────────────────────────────────────
    // balance()
    // ──────────────────────────────────────────────

    /**
     * Get current SMS credit balance.
     * Returns the live balance on success, or the cached value if the API call fails.
     * Returns null if no cached value exists and the API call fails.
     */
    fun balance(): Double? {
        return try {
            val response = ApiRequest.post("balance", basePayload(), logFile)
            if (response["result"]?.toString() == "OK") {
                val available = asDouble(response["available"])
                val purchased = asDouble(response["purchased"])
                cachedBalance = available
                cachedPurchased = purchased
                available
            } else {
                cachedBalance
            }
        } catch (_: Exception) {
            cachedBalance
        }
    }

    // ──────────────────────────────────────────────
    // send()
    // ──────────────────────────────────────────────

    /**
     * Send an SMS to one or more phone numbers (comma-separated string).
     */
    fun send(mobile: String, message: String, sender: String? = null): SendResult {
        val phones = mobile.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return sendToPhones(phones, message, sender)
    }

    /**
     * Send an SMS to a list of phone numbers.
     */
    fun send(mobiles: List<String>, message: String, sender: String? = null): SendResult {
        return sendToPhones(mobiles, message, sender)
    }

    private fun sendToPhones(phones: List<String>, message: String, sender: String?): SendResult {
        // Validate and normalize all phone numbers
        val validPhones = mutableListOf<String>()
        val invalidEntries = mutableListOf<InvalidEntry>()

        for (phone in phones) {
            val (valid, error, normalized) = PhoneUtils.validatePhoneInput(phone.toString())
            if (valid) {
                validPhones.add(normalized)
            } else {
                invalidEntries.add(InvalidEntry(input = phone, error = error ?: "Invalid phone number"))
            }
        }

        // Deduplicate valid numbers
        val deduplicated = PhoneUtils.deduplicatePhones(validPhones)

        // If no valid numbers remain, return error
        if (deduplicated.isEmpty()) {
            return SendResult(
                result = "ERROR",
                code = "ERR_INVALID_INPUT",
                description = "No valid phone numbers provided",
                action = API_ERRORS["ERR_INVALID_INPUT"],
                invalid = invalidEntries
            )
        }

        // Clean the message
        val cleanedMessage = MessageUtils.cleanMessage(message)
        if (cleanedMessage.isBlank()) {
            return SendResult(
                result = "ERROR",
                code = "ERR009",
                description = "Message is empty after cleaning",
                action = API_ERRORS["ERR009"],
                invalid = invalidEntries
            )
        }

        // If >200 numbers, use bulk send
        if (deduplicated.size > MAX_BATCH_SIZE) {
            val bulkResult = sendBulkInternal(deduplicated, cleanedMessage, sender, invalidEntries)
            // Convert BulkSendResult to SendResult for consistency
            return SendResult(
                result = bulkResult.result,
                msgId = bulkResult.msgIds.firstOrNull(),
                numbers = bulkResult.numbers,
                pointsCharged = bulkResult.pointsCharged,
                balanceAfter = bulkResult.balanceAfter,
                code = bulkResult.code,
                description = bulkResult.description,
                invalid = bulkResult.invalid
            )
        }

        // Single batch send
        return sendSingleBatch(deduplicated, cleanedMessage, sender, invalidEntries)
    }

    private fun sendSingleBatch(
        phones: List<String>,
        cleanedMessage: String,
        sender: String?,
        invalidEntries: List<InvalidEntry>
    ): SendResult {
        val payload = basePayload().apply {
            put("sender", sender ?: senderId)
            put("mobile", phones.joinToString(","))
            put("message", cleanedMessage)
            put("test", if (testMode) "1" else "0")
        }

        return try {
            val response = ApiRequest.post("send", payload, logFile)
            val result = response["result"]?.toString() ?: "ERROR"

            if (result == "OK") {
                val balanceAfter = asDouble(response["balance-after"])
                if (balanceAfter != null) {
                    cachedBalance = balanceAfter
                }
                SendResult(
                    result = "OK",
                    msgId = response["msg-id"]?.toString(),
                    numbers = asInt(response["numbers"]),
                    pointsCharged = asInt(response["points-charged"]),
                    balanceAfter = balanceAfter,
                    unixTimestamp = asLong(response["unix-timestamp"]),
                    invalid = invalidEntries
                )
            } else {
                val enriched = enrichError(response)
                SendResult(
                    result = "ERROR",
                    code = enriched["code"]?.toString(),
                    description = enriched["description"]?.toString(),
                    action = enriched["action"]?.toString(),
                    invalid = invalidEntries
                )
            }
        } catch (e: Exception) {
            SendResult(
                result = "ERROR",
                description = e.message ?: "Unknown error",
                invalid = invalidEntries
            )
        }
    }

    // ──────────────────────────────────────────────
    // sendBulk()
    // ──────────────────────────────────────────────

    /**
     * Send SMS to a large list of phone numbers with automatic batching.
     * Numbers are split into batches of 200, with 0.5s delay between batches.
     * ERR013 (queue full) is retried up to 3 times with exponential backoff.
     */
    fun sendBulk(mobiles: List<String>, message: String, sender: String? = null): BulkSendResult {
        val validPhones = mutableListOf<String>()
        val invalidEntries = mutableListOf<InvalidEntry>()

        for (phone in mobiles) {
            val (valid, error, normalized) = PhoneUtils.validatePhoneInput(phone.toString())
            if (valid) {
                validPhones.add(normalized)
            } else {
                invalidEntries.add(InvalidEntry(input = phone, error = error ?: "Invalid phone number"))
            }
        }

        val deduplicated = PhoneUtils.deduplicatePhones(validPhones)

        if (deduplicated.isEmpty()) {
            return BulkSendResult(
                result = "ERROR",
                code = "ERR_INVALID_INPUT",
                description = "No valid phone numbers provided",
                invalid = invalidEntries
            )
        }

        val cleanedMessage = MessageUtils.cleanMessage(message)
        if (cleanedMessage.isBlank()) {
            return BulkSendResult(
                result = "ERROR",
                code = "ERR009",
                description = "Message is empty after cleaning",
                invalid = invalidEntries
            )
        }

        return sendBulkInternal(deduplicated, cleanedMessage, sender, invalidEntries)
    }

    private fun sendBulkInternal(
        phones: List<String>,
        cleanedMessage: String,
        sender: String?,
        invalidEntries: List<InvalidEntry>
    ): BulkSendResult {
        val batches = phones.chunked(MAX_BATCH_SIZE)
        val msgIds = mutableListOf<String>()
        val errors = mutableListOf<BatchError>()
        var totalNumbers = 0
        var totalPoints = 0
        var lastBalance: Double? = null

        for ((index, batch) in batches.withIndex()) {
            if (index > 0) {
                Thread.sleep(BATCH_DELAY_MS)
            }

            val result = sendBatchWithRetry(batch, cleanedMessage, sender, index + 1)

            if (result.result == "OK") {
                result.msgId?.let { msgIds.add(it) }
                totalNumbers += result.numbers ?: 0
                totalPoints += result.pointsCharged ?: 0
                if (result.balanceAfter != null) {
                    lastBalance = result.balanceAfter
                }
            } else {
                errors.add(
                    BatchError(
                        batch = index + 1,
                        code = result.code ?: "UNKNOWN",
                        description = result.description ?: "Unknown error"
                    )
                )
            }
        }

        val overallResult = when {
            errors.isEmpty() -> "OK"
            errors.size == batches.size -> "ERROR"
            else -> "PARTIAL"
        }

        return BulkSendResult(
            result = overallResult,
            bulk = true,
            batches = batches.size,
            numbers = totalNumbers,
            pointsCharged = totalPoints,
            balanceAfter = lastBalance,
            msgIds = msgIds,
            errors = errors,
            invalid = invalidEntries
        )
    }

    private fun sendBatchWithRetry(
        phones: List<String>,
        cleanedMessage: String,
        sender: String?,
        batchNumber: Int
    ): SendResult {
        var lastResult: SendResult? = null

        for (attempt in 0..ERR013_RETRY_DELAYS.size) {
            val result = sendSingleBatch(phones, cleanedMessage, sender, emptyList())
            lastResult = result

            // Only retry on ERR013 (queue full)
            if (result.code != "ERR013" || attempt >= ERR013_RETRY_DELAYS.size) {
                return result
            }

            Thread.sleep(ERR013_RETRY_DELAYS[attempt])
        }

        return lastResult ?: SendResult(
            result = "ERROR",
            description = "Retry exhausted for batch $batchNumber"
        )
    }

    // ──────────────────────────────────────────────
    // validate()
    // ──────────────────────────────────────────────

    /**
     * Validate phone numbers with the kwtSMS API.
     * Runs local validation first, then sends valid numbers to the API.
     */
    fun validate(phones: List<String>): ValidateResult {
        val validPhones = mutableListOf<String>()
        val rejected = mutableListOf<InvalidEntry>()

        for (phone in phones) {
            val (valid, error, normalized) = PhoneUtils.validatePhoneInput(phone.toString())
            if (valid) {
                validPhones.add(normalized)
            } else {
                rejected.add(InvalidEntry(input = phone, error = error ?: "Invalid phone number"))
            }
        }

        val deduplicated = PhoneUtils.deduplicatePhones(validPhones)

        if (deduplicated.isEmpty()) {
            return ValidateResult(
                rejected = rejected,
                error = "No valid phone numbers to validate"
            )
        }

        return try {
            val payload = basePayload().apply {
                put("mobile", deduplicated.joinToString(","))
            }
            val response = ApiRequest.post("validate", payload, logFile)
            val result = response["result"]?.toString()

            if (result == "OK") {
                @Suppress("UNCHECKED_CAST")
                val mobile = response["mobile"] as? Map<String, Any?> ?: emptyMap()
                ValidateResult(
                    ok = toStringList(mobile["OK"]),
                    er = toStringList(mobile["ER"]),
                    nr = toStringList(mobile["NR"]),
                    rejected = rejected,
                    raw = response
                )
            } else {
                val enriched = enrichError(response)
                ValidateResult(
                    rejected = rejected,
                    error = enriched["description"]?.toString() ?: "Validation failed",
                    raw = response
                )
            }
        } catch (e: Exception) {
            ValidateResult(
                rejected = rejected,
                error = e.message ?: "Unknown error"
            )
        }
    }

    // ──────────────────────────────────────────────
    // senderIds()
    // ──────────────────────────────────────────────

    /**
     * List available sender IDs on this account.
     */
    fun senderIds(): SenderIdResult {
        return try {
            val response = ApiRequest.post("senderid", basePayload(), logFile)
            val result = response["result"]?.toString() ?: "ERROR"

            if (result == "OK") {
                SenderIdResult(
                    result = "OK",
                    senderIds = toStringList(response["senderid"])
                )
            } else {
                val enriched = enrichError(response)
                SenderIdResult(
                    result = "ERROR",
                    code = enriched["code"]?.toString(),
                    description = enriched["description"]?.toString(),
                    action = enriched["action"]?.toString()
                )
            }
        } catch (e: Exception) {
            SenderIdResult(
                result = "ERROR",
                description = e.message ?: "Unknown error"
            )
        }
    }

    // ──────────────────────────────────────────────
    // coverage()
    // ──────────────────────────────────────────────

    /**
     * List active country prefixes on this account.
     */
    fun coverage(): CoverageResult {
        return try {
            val response = ApiRequest.post("coverage", basePayload(), logFile)
            val result = response["result"]?.toString() ?: "ERROR"

            if (result == "OK") {
                CoverageResult(
                    result = "OK",
                    prefixes = toStringList(response["prefixes"])
                )
            } else {
                val enriched = enrichError(response)
                CoverageResult(
                    result = "ERROR",
                    code = enriched["code"]?.toString(),
                    description = enriched["description"]?.toString(),
                    action = enriched["action"]?.toString()
                )
            }
        } catch (e: Exception) {
            CoverageResult(
                result = "ERROR",
                description = e.message ?: "Unknown error"
            )
        }
    }

    // ──────────────────────────────────────────────
    // status()
    // ──────────────────────────────────────────────

    /**
     * Check the queue status of a sent message.
     */
    fun status(msgId: String): StatusResult {
        return try {
            val payload = basePayload().apply {
                put("msgid", msgId)
            }
            val response = ApiRequest.post("status", payload, logFile)
            val result = response["result"]?.toString() ?: "ERROR"

            if (result == "OK") {
                StatusResult(
                    result = "OK",
                    status = response["status"]?.toString(),
                    statusDescription = response["description"]?.toString()
                )
            } else {
                val enriched = enrichError(response)
                StatusResult(
                    result = "ERROR",
                    code = enriched["code"]?.toString(),
                    description = enriched["description"]?.toString(),
                    action = enriched["action"]?.toString()
                )
            }
        } catch (e: Exception) {
            StatusResult(
                result = "ERROR",
                description = e.message ?: "Unknown error"
            )
        }
    }

    // ──────────────────────────────────────────────
    // deliveryReport()
    // ──────────────────────────────────────────────

    /**
     * Get delivery reports for a sent message (international numbers only).
     * Kuwait numbers do not support delivery reports.
     */
    fun deliveryReport(msgId: String): DeliveryReportResult {
        return try {
            val payload = basePayload().apply {
                put("msgid", msgId)
            }
            val response = ApiRequest.post("dlr", payload, logFile)
            val result = response["result"]?.toString() ?: "ERROR"

            if (result == "OK") {
                @Suppress("UNCHECKED_CAST")
                val reportList = response["report"] as? List<Map<String, Any?>> ?: emptyList()
                val entries = reportList.map { entry ->
                    DeliveryReportEntry(
                        number = entry["Number"]?.toString() ?: "",
                        status = entry["Status"]?.toString() ?: ""
                    )
                }
                DeliveryReportResult(
                    result = "OK",
                    report = entries
                )
            } else {
                val enriched = enrichError(response)
                DeliveryReportResult(
                    result = "ERROR",
                    code = enriched["code"]?.toString(),
                    description = enriched["description"]?.toString(),
                    action = enriched["action"]?.toString()
                )
            }
        } catch (e: Exception) {
            DeliveryReportResult(
                result = "ERROR",
                description = e.message ?: "Unknown error"
            )
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private fun asDouble(value: Any?): Double? {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun asInt(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun asLong(value: Any?): Long? {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun toStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            else -> emptyList()
        }
    }
}
