package com.kwtsms

/**
 * Phone number validation rule for a country.
 * @param localLengths valid local number lengths (digits after country code)
 * @param mobileStartDigits valid first digit(s) of local number for mobile. Empty means any starting digit accepted.
 */
data class PhoneRule(
    val localLengths: List<Int>,
    val mobileStartDigits: List<String> = emptyList()
)

/**
 * Phone number normalization and validation utilities.
 */
object PhoneUtils {

    /**
     * Country-specific phone number validation rules.
     * Validates local number length and mobile starting digits.
     *
     * Sources: ITU-T E.164, Wikipedia telephone number articles,
     * HowToCallAbroad.com, CountryCode.com.
     *
     * localLengths: valid digit count(s) AFTER country code.
     * mobileStartDigits: valid first character(s) of the local number.
     * Countries not listed pass through with generic E.164 validation (7-15 digits).
     */
    @JvmStatic
    val PHONE_RULES: Map<String, PhoneRule> = mapOf(
        // GCC
        "965" to PhoneRule(listOf(8), listOf("4", "5", "6", "9")),
        "966" to PhoneRule(listOf(9), listOf("5")),
        "971" to PhoneRule(listOf(9), listOf("5")),
        "973" to PhoneRule(listOf(8), listOf("3", "6")),
        "974" to PhoneRule(listOf(8), listOf("3", "5", "6", "7")),
        "968" to PhoneRule(listOf(8), listOf("7", "9")),
        // Levant
        "962" to PhoneRule(listOf(9), listOf("7")),
        "961" to PhoneRule(listOf(7, 8), listOf("3", "7", "8")),
        "970" to PhoneRule(listOf(9), listOf("5")),
        "964" to PhoneRule(listOf(10), listOf("7")),
        "963" to PhoneRule(listOf(9), listOf("9")),
        // Other Arab
        "967" to PhoneRule(listOf(9), listOf("7")),
        "20" to PhoneRule(listOf(10), listOf("1")),
        "218" to PhoneRule(listOf(9), listOf("9")),
        "216" to PhoneRule(listOf(8), listOf("2", "4", "5", "9")),
        "212" to PhoneRule(listOf(9), listOf("6", "7")),
        "213" to PhoneRule(listOf(9), listOf("5", "6", "7")),
        "249" to PhoneRule(listOf(9), listOf("9")),
        // Non-Arab Middle East
        "98" to PhoneRule(listOf(10), listOf("9")),
        "90" to PhoneRule(listOf(10), listOf("5")),
        "972" to PhoneRule(listOf(9), listOf("5")),
        // South Asia
        "91" to PhoneRule(listOf(10), listOf("6", "7", "8", "9")),
        "92" to PhoneRule(listOf(10), listOf("3")),
        "880" to PhoneRule(listOf(10), listOf("1")),
        "94" to PhoneRule(listOf(9), listOf("7")),
        "960" to PhoneRule(listOf(7), listOf("7", "9")),
        // East Asia
        "86" to PhoneRule(listOf(11), listOf("1")),
        "81" to PhoneRule(listOf(10), listOf("7", "8", "9")),
        "82" to PhoneRule(listOf(10), listOf("1")),
        "886" to PhoneRule(listOf(9), listOf("9")),
        // Southeast Asia
        "65" to PhoneRule(listOf(8), listOf("8", "9")),
        "60" to PhoneRule(listOf(9, 10), listOf("1")),
        "62" to PhoneRule(listOf(9, 10, 11, 12), listOf("8")),
        "63" to PhoneRule(listOf(10), listOf("9")),
        "66" to PhoneRule(listOf(9), listOf("6", "8", "9")),
        "84" to PhoneRule(listOf(9), listOf("3", "5", "7", "8", "9")),
        "95" to PhoneRule(listOf(9), listOf("9")),
        "855" to PhoneRule(listOf(8, 9), listOf("1", "6", "7", "8", "9")),
        "976" to PhoneRule(listOf(8), listOf("6", "8", "9")),
        // Europe
        "44" to PhoneRule(listOf(10), listOf("7")),
        "33" to PhoneRule(listOf(9), listOf("6", "7")),
        "49" to PhoneRule(listOf(10, 11), listOf("1")),
        "39" to PhoneRule(listOf(10), listOf("3")),
        "34" to PhoneRule(listOf(9), listOf("6", "7")),
        "31" to PhoneRule(listOf(9), listOf("6")),
        "32" to PhoneRule(listOf(9)),
        "41" to PhoneRule(listOf(9), listOf("7")),
        "43" to PhoneRule(listOf(10), listOf("6")),
        "47" to PhoneRule(listOf(8), listOf("4", "9")),
        "48" to PhoneRule(listOf(9)),
        "30" to PhoneRule(listOf(10), listOf("6")),
        "420" to PhoneRule(listOf(9), listOf("6", "7")),
        "46" to PhoneRule(listOf(9), listOf("7")),
        "45" to PhoneRule(listOf(8)),
        "40" to PhoneRule(listOf(9), listOf("7")),
        "36" to PhoneRule(listOf(9)),
        "380" to PhoneRule(listOf(9)),
        // Americas
        "1" to PhoneRule(listOf(10)),
        "52" to PhoneRule(listOf(10)),
        "55" to PhoneRule(listOf(11)),
        "57" to PhoneRule(listOf(10), listOf("3")),
        "54" to PhoneRule(listOf(10), listOf("9")),
        "56" to PhoneRule(listOf(9), listOf("9")),
        "58" to PhoneRule(listOf(10), listOf("4")),
        "51" to PhoneRule(listOf(9), listOf("9")),
        "593" to PhoneRule(listOf(9), listOf("9")),
        "53" to PhoneRule(listOf(8), listOf("5", "6")),
        // Africa
        "27" to PhoneRule(listOf(9), listOf("6", "7", "8")),
        "234" to PhoneRule(listOf(10), listOf("7", "8", "9")),
        "254" to PhoneRule(listOf(9), listOf("1", "7")),
        "233" to PhoneRule(listOf(9), listOf("2", "5")),
        "251" to PhoneRule(listOf(9), listOf("7", "9")),
        "255" to PhoneRule(listOf(9), listOf("6", "7")),
        "256" to PhoneRule(listOf(9), listOf("7")),
        "237" to PhoneRule(listOf(9), listOf("6")),
        "225" to PhoneRule(listOf(10)),
        "221" to PhoneRule(listOf(9), listOf("7")),
        "252" to PhoneRule(listOf(9), listOf("6", "7")),
        "250" to PhoneRule(listOf(9), listOf("7")),
        // Oceania
        "61" to PhoneRule(listOf(9), listOf("4")),
        "64" to PhoneRule(listOf(8, 9, 10), listOf("2"))
    )

    /**
     * Country names by country code for error messages.
     */
    @JvmStatic
    val COUNTRY_NAMES: Map<String, String> = mapOf(
        // Middle East & North Africa
        "965" to "Kuwait", "966" to "Saudi Arabia", "971" to "UAE",
        "973" to "Bahrain", "974" to "Qatar", "968" to "Oman",
        "962" to "Jordan", "961" to "Lebanon", "970" to "Palestine",
        "964" to "Iraq", "963" to "Syria", "967" to "Yemen",
        "98" to "Iran", "90" to "Turkey", "972" to "Israel",
        "20" to "Egypt", "218" to "Libya", "216" to "Tunisia",
        "212" to "Morocco", "213" to "Algeria", "249" to "Sudan",
        // Africa
        "27" to "South Africa", "234" to "Nigeria", "254" to "Kenya",
        "233" to "Ghana", "251" to "Ethiopia", "255" to "Tanzania",
        "256" to "Uganda", "237" to "Cameroon", "225" to "Ivory Coast",
        "221" to "Senegal", "252" to "Somalia", "250" to "Rwanda",
        // Europe
        "44" to "UK", "33" to "France", "49" to "Germany",
        "39" to "Italy", "34" to "Spain", "31" to "Netherlands",
        "32" to "Belgium", "41" to "Switzerland", "43" to "Austria",
        "46" to "Sweden", "47" to "Norway", "45" to "Denmark",
        "48" to "Poland", "420" to "Czech Republic", "30" to "Greece",
        "40" to "Romania", "36" to "Hungary", "380" to "Ukraine",
        // Americas
        "1" to "USA/Canada", "52" to "Mexico", "55" to "Brazil",
        "54" to "Argentina", "57" to "Colombia", "56" to "Chile",
        "58" to "Venezuela", "51" to "Peru", "593" to "Ecuador", "53" to "Cuba",
        // Asia
        "91" to "India", "92" to "Pakistan", "86" to "China",
        "81" to "Japan", "82" to "South Korea", "886" to "Taiwan",
        "65" to "Singapore", "60" to "Malaysia", "62" to "Indonesia",
        "63" to "Philippines", "66" to "Thailand", "84" to "Vietnam",
        "855" to "Cambodia", "95" to "Myanmar", "880" to "Bangladesh",
        "94" to "Sri Lanka", "960" to "Maldives", "976" to "Mongolia",
        // Oceania
        "61" to "Australia", "64" to "New Zealand"
    )

    /**
     * Normalize a phone number to kwtSMS-accepted format (digits only, international format).
     *
     * 1. Converts Arabic-Indic (U+0660..U+0669) and Extended Arabic-Indic (U+06F0..U+06F9) digits to Latin
     * 2. Strips all non-digit characters
     * 3. Strips leading zeros
     * 4. Strips domestic trunk prefix (leading 0 after country code, e.g. 9660559... -> 966559...)
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
        if (start >= sb.length) return ""
        var normalized = sb.substring(start)

        // Strip domestic trunk prefix (leading 0 after country code)
        // e.g. 9660559... -> 966559..., 97105x -> 9715x
        val cc = findCountryCode(normalized)
        if (cc != null) {
            val local = normalized.substring(cc.length)
            if (local.startsWith("0")) {
                val stripped = local.trimStart('0')
                if (stripped.isNotEmpty()) {
                    normalized = cc + stripped
                }
            }
        }

        return normalized
    }

    /**
     * Find the country code prefix from a normalized phone number.
     * Tries 3-digit codes first, then 2-digit, then 1-digit (longest match wins).
     */
    @JvmStatic
    fun findCountryCode(normalized: String): String? {
        if (normalized.length >= 3) {
            val cc3 = normalized.substring(0, 3)
            if (PHONE_RULES.containsKey(cc3)) return cc3
        }
        if (normalized.length >= 2) {
            val cc2 = normalized.substring(0, 2)
            if (PHONE_RULES.containsKey(cc2)) return cc2
        }
        if (normalized.isNotEmpty()) {
            val cc1 = normalized.substring(0, 1)
            if (PHONE_RULES.containsKey(cc1)) return cc1
        }
        return null
    }

    /**
     * Validate a normalized phone number against country-specific format rules.
     * Checks local number length and mobile starting digits.
     * Numbers with no matching country rules pass through (generic E.164 only).
     *
     * @return Pair(valid, error) where error is null if valid
     */
    @JvmStatic
    fun validatePhoneFormat(normalized: String): Pair<Boolean, String?> {
        val cc = findCountryCode(normalized) ?: return Pair(true, null)
        val rule = PHONE_RULES[cc] ?: return Pair(true, null)
        val local = normalized.substring(cc.length)
        val country = COUNTRY_NAMES[cc] ?: "+$cc"

        if (local.length !in rule.localLengths) {
            val expected = rule.localLengths.joinToString(" or ")
            return Pair(false, "Invalid $country number: expected $expected digits after +$cc, got ${local.length}")
        }

        if (rule.mobileStartDigits.isNotEmpty()) {
            val hasValidPrefix = rule.mobileStartDigits.any { local.startsWith(it) }
            if (!hasValidPrefix) {
                return Pair(false, "Invalid $country mobile number: after +$cc must start with ${rule.mobileStartDigits.joinToString(", ")}")
            }
        }

        return Pair(true, null)
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
        val trimmed = phone.trim()

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

        // Validate against country-specific format rules
        val (formatValid, formatError) = validatePhoneFormat(normalized)
        if (!formatValid) {
            return Triple(false, formatError, normalized)
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
