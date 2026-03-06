package com.kwtsms

/**
 * Entry representing a phone number that failed local validation.
 */
data class InvalidEntry(
    val input: String,
    val error: String
)

/**
 * Result of verify() call.
 */
data class VerifyResult(
    val ok: Boolean,
    val balance: Double? = null,
    val error: String? = null
)

/**
 * Result of a single send() call.
 */
data class SendResult(
    val result: String,
    val msgId: String? = null,
    val numbers: Int? = null,
    val pointsCharged: Int? = null,
    val balanceAfter: Double? = null,
    val unixTimestamp: Long? = null,
    val code: String? = null,
    val description: String? = null,
    val action: String? = null,
    val invalid: List<InvalidEntry> = emptyList()
)

/**
 * Per-batch error in bulk send.
 */
data class BatchError(
    val batch: Int,
    val code: String,
    val description: String
)

/**
 * Result of a bulk send (>200 numbers, auto-batched).
 */
data class BulkSendResult(
    val result: String,
    val bulk: Boolean = true,
    val batches: Int = 0,
    val numbers: Int = 0,
    val pointsCharged: Int = 0,
    val balanceAfter: Double? = null,
    val msgIds: List<String> = emptyList(),
    val errors: List<BatchError> = emptyList(),
    val invalid: List<InvalidEntry> = emptyList(),
    val code: String? = null,
    val description: String? = null
)

/**
 * Result of validate() call.
 */
data class ValidateResult(
    val ok: List<String> = emptyList(),
    val er: List<String> = emptyList(),
    val nr: List<String> = emptyList(),
    val rejected: List<InvalidEntry> = emptyList(),
    val error: String? = null,
    val raw: Map<String, Any?> = emptyMap()
)

/**
 * Result of senderIds() call.
 */
data class SenderIdResult(
    val result: String,
    val senderIds: List<String> = emptyList(),
    val code: String? = null,
    val description: String? = null,
    val action: String? = null
)

/**
 * Result of coverage() call.
 */
data class CoverageResult(
    val result: String,
    val prefixes: List<String> = emptyList(),
    val code: String? = null,
    val description: String? = null,
    val action: String? = null
)

/**
 * Result of status() call.
 */
data class StatusResult(
    val result: String,
    val status: String? = null,
    val statusDescription: String? = null,
    val code: String? = null,
    val description: String? = null,
    val action: String? = null
)

/**
 * Single entry in a delivery report.
 */
data class DeliveryReportEntry(
    val number: String,
    val status: String
)

/**
 * Result of deliveryReport() call.
 */
data class DeliveryReportResult(
    val result: String,
    val report: List<DeliveryReportEntry> = emptyList(),
    val code: String? = null,
    val description: String? = null,
    val action: String? = null
)
