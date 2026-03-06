package com.kwtsms

/**
 * Phone number normalization and validation utilities.
 */
object PhoneUtils {

    /**
     * Normalize a phone number to kwtSMS-accepted format (digits only, international format).
     *
     * 1. Converts Arabic-Indic (U+0660..U+0669) and Extended Arabic-Indic (U+06F0..U+06F9) digits to Latin
     * 2. Strips all non-digit characters
     * 3. Strips leading zeros
     */
    @JvmStatic
    fun normalizePhone(phone: String): String {
        val sb = StringBuilder(phone.length)
        for (ch in phone) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append((ch - '\u0660' + '0'.code).toChar())
                in '\u06F0'..'\u06F9' -> sb.append((ch - '\u06F0' + '0'.code).toChar())
                in '0'..'9' -> sb.append(ch)
                // skip all non-digit characters
            }
        }
        // Strip leading zeros
        var start = 0
        while (start < sb.length && sb[start] == '0') {
            start++
        }
        return if (start >= sb.length) "" else sb.substring(start)
    }

    /**
     * Validate a phone number input before sending to the API.
     *
     * Returns a Triple of (valid, error, normalized).
     * - If valid is true, normalized contains the cleaned number.
     * - If valid is false, error contains the reason.
     */
    @JvmStatic
    fun validatePhoneInput(phone: String): Triple<Boolean, String?, String> {
        val raw = phone.toString() // ensure string
        val trimmed = raw.trim()

        if (trimmed.isEmpty()) {
            return Triple(false, "Phone number is required", "")
        }

        if (trimmed.contains('@')) {
            return Triple(false, "'$trimmed' is an email address, not a phone number", "")
        }

        val normalized = normalizePhone(trimmed)

        if (normalized.isEmpty()) {
            return Triple(false, "'$trimmed' is not a valid phone number, no digits found", "")
        }

        if (normalized.length < 7) {
            return Triple(false, "'$trimmed' is too short (${normalized.length} digits, minimum is 7)", normalized)
        }

        if (normalized.length > 15) {
            return Triple(false, "'$trimmed' is too long (${normalized.length} digits, maximum is 15)", normalized)
        }

        return Triple(true, null, normalized)
    }

    /**
     * Deduplicate a list of phone numbers while preserving order.
     */
    @JvmStatic
    fun deduplicatePhones(phones: List<String>): List<String> {
        val seen = LinkedHashSet<String>(phones.size)
        for (phone in phones) {
            seen.add(phone)
        }
        return seen.toList()
    }
}
