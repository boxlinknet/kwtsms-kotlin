package com.kwtsms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneUtilsTest {

    // ──────────────────────────────────────────────
    // normalizePhone()
    // ──────────────────────────────────────────────

    @Test
    fun `normalizePhone - plus prefix stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("+96598765432"))
    }

    @Test
    fun `normalizePhone - double zero prefix stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("0096598765432"))
    }

    @Test
    fun `normalizePhone - spaces stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("965 9876 5432"))
    }

    @Test
    fun `normalizePhone - dashes stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("965-9876-5432"))
    }

    @Test
    fun `normalizePhone - dots stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("965.9876.5432"))
    }

    @Test
    fun `normalizePhone - parentheses stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("(965) 98765432"))
    }

    @Test
    fun `normalizePhone - slashes stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("965/9876/5432"))
    }

    @Test
    fun `normalizePhone - mixed separators`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("+965 (987) 65-432"))
    }

    @Test
    fun `normalizePhone - Arabic-Indic digits converted`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("\u0669\u0666\u0665\u0669\u0668\u0667\u0666\u0665\u0664\u0663\u0662"))
    }

    @Test
    fun `normalizePhone - Extended Arabic-Indic digits converted`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("\u06F9\u06F6\u06F5\u06F9\u06F8\u06F7\u06F6\u06F5\u06F4\u06F3\u06F2"))
    }

    @Test
    fun `normalizePhone - leading zeros stripped`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("0096598765432"))
    }

    @Test
    fun `normalizePhone - single leading zero stripped`() {
        assertEquals("598765432", PhoneUtils.normalizePhone("0598765432"))
    }

    @Test
    fun `normalizePhone - empty string`() {
        assertEquals("", PhoneUtils.normalizePhone(""))
    }

    @Test
    fun `normalizePhone - only spaces`() {
        assertEquals("", PhoneUtils.normalizePhone("   "))
    }

    @Test
    fun `normalizePhone - only letters`() {
        assertEquals("", PhoneUtils.normalizePhone("abcdef"))
    }

    @Test
    fun `normalizePhone - only zeros`() {
        assertEquals("", PhoneUtils.normalizePhone("0000"))
    }

    @Test
    fun `normalizePhone - Saudi trunk prefix stripped (9660559)`() {
        assertEquals("966559876543", PhoneUtils.normalizePhone("9660559876543"))
    }

    @Test
    fun `normalizePhone - Saudi with plus and trunk prefix`() {
        assertEquals("966559876543", PhoneUtils.normalizePhone("+9660559876543"))
    }

    @Test
    fun `normalizePhone - Saudi with 00 and trunk prefix`() {
        assertEquals("966559876543", PhoneUtils.normalizePhone("009660559876543"))
    }

    @Test
    fun `normalizePhone - UAE trunk prefix stripped (97105x)`() {
        assertEquals("971501234567", PhoneUtils.normalizePhone("9710501234567"))
    }

    @Test
    fun `normalizePhone - Egypt trunk prefix stripped (2001x)`() {
        // +20 0 1012345678 -> strip +, strip trunk 0 after cc 20
        assertEquals("201012345678", PhoneUtils.normalizePhone("+2001012345678"))
    }

    @Test
    fun `normalizePhone - Kuwait no trunk prefix (no change)`() {
        assertEquals("96598765432", PhoneUtils.normalizePhone("96598765432"))
    }

    // ──────────────────────────────────────────────
    // findCountryCode()
    // ──────────────────────────────────────────────

    @Test
    fun `findCountryCode - 3-digit code (Kuwait 965)`() {
        assertEquals("965", PhoneUtils.findCountryCode("96598765432"))
    }

    @Test
    fun `findCountryCode - 2-digit code (Egypt 20)`() {
        assertEquals("20", PhoneUtils.findCountryCode("201012345678"))
    }

    @Test
    fun `findCountryCode - 1-digit code (USA 1)`() {
        assertEquals("1", PhoneUtils.findCountryCode("12125551234"))
    }

    @Test
    fun `findCountryCode - unknown code returns null`() {
        assertNull(PhoneUtils.findCountryCode("99999999999"))
    }

    // ──────────────────────────────────────────────
    // validatePhoneFormat()
    // ──────────────────────────────────────────────

    @Test
    fun `validatePhoneFormat - valid Kuwait number`() {
        val (valid, error) = PhoneUtils.validatePhoneFormat("96598765432")
        assertTrue(valid)
        assertNull(error)
    }

    @Test
    fun `validatePhoneFormat - Kuwait wrong length`() {
        val (valid, error) = PhoneUtils.validatePhoneFormat("9659876543") // 7 local digits, needs 8
        assertFalse(valid)
        assertTrue(error!!.contains("Kuwait"))
        assertTrue(error.contains("8 digits"))
    }

    @Test
    fun `validatePhoneFormat - Kuwait wrong mobile prefix`() {
        val (valid, error) = PhoneUtils.validatePhoneFormat("96518765432") // starts with 1, invalid
        assertFalse(valid)
        assertTrue(error!!.contains("Kuwait"))
        assertTrue(error.contains("must start with"))
    }

    @Test
    fun `validatePhoneFormat - Saudi valid`() {
        val (valid, _) = PhoneUtils.validatePhoneFormat("966559876543")
        assertTrue(valid)
    }

    @Test
    fun `validatePhoneFormat - Saudi wrong mobile prefix`() {
        val (valid, error) = PhoneUtils.validatePhoneFormat("966159876543") // starts with 1
        assertFalse(valid)
        assertTrue(error!!.contains("Saudi Arabia"))
    }

    @Test
    fun `validatePhoneFormat - unknown country passes through`() {
        val (valid, _) = PhoneUtils.validatePhoneFormat("99912345678")
        assertTrue(valid)
    }

    @Test
    fun `validatePhoneFormat - Belgium length only (no mobile prefix check)`() {
        val (valid, _) = PhoneUtils.validatePhoneFormat("32471234567") // 9 local digits
        assertTrue(valid)
    }

    // ──────────────────────────────────────────────
    // validatePhoneInput()
    // ──────────────────────────────────────────────

    @Test
    fun `validatePhoneInput - valid Kuwait number`() {
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput("96598765432")
        assertTrue(valid)
        assertNull(error)
        assertEquals("96598765432", normalized)
    }

    @Test
    fun `validatePhoneInput - valid with plus prefix`() {
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput("+96598765432")
        assertTrue(valid)
        assertNull(error)
        assertEquals("96598765432", normalized)
    }

    @Test
    fun `validatePhoneInput - valid with 00 prefix`() {
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput("0096598765432")
        assertTrue(valid)
        assertNull(error)
        assertEquals("96598765432", normalized)
    }

    @Test
    fun `validatePhoneInput - minimum valid length (7 digits)`() {
        // Use a number that doesn't match any country code (e.g. 999...)
        val (valid, error, _) = PhoneUtils.validatePhoneInput("9991234")
        assertTrue(valid)
        assertNull(error)
    }

    @Test
    fun `validatePhoneInput - maximum valid length (15 digits)`() {
        // Use a number that doesn't match any country code
        val (valid, error, _) = PhoneUtils.validatePhoneInput("999123456789012")
        assertTrue(valid)
        assertNull(error)
    }

    @Test
    fun `validatePhoneInput - empty string`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("")
        assertFalse(valid)
        assertEquals("Phone number is required", error)
    }

    @Test
    fun `validatePhoneInput - blank string`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("   ")
        assertFalse(valid)
        assertEquals("Phone number is required", error)
    }

    @Test
    fun `validatePhoneInput - email address`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("user@example.com")
        assertFalse(valid)
        assertTrue(error!!.contains("email address"))
    }

    @Test
    fun `validatePhoneInput - no digits`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("abcdef")
        assertFalse(valid)
        assertTrue(error!!.contains("no digits found"))
    }

    @Test
    fun `validatePhoneInput - too short (6 digits)`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("123456")
        assertFalse(valid)
        assertTrue(error!!.contains("too short"))
        assertTrue(error.contains("6 digits"))
    }

    @Test
    fun `validatePhoneInput - too long (16 digits)`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("1234567890123456")
        assertFalse(valid)
        assertTrue(error!!.contains("too long"))
        assertTrue(error.contains("16 digits"))
    }

    @Test
    fun `validatePhoneInput - Arabic digits converted and validated`() {
        // ٩٦٥٩٨٧٦٥٤٣٢ = 96598765432 (11 digits, valid)
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput("\u0669\u0666\u0665\u0669\u0668\u0667\u0666\u0665\u0664\u0663\u0662")
        assertTrue(valid)
        assertNull(error)
        assertEquals("96598765432", normalized)
    }

    @Test
    fun `validatePhoneInput - whitespace trimmed before validation`() {
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput("  96598765432  ")
        assertTrue(valid)
        assertNull(error)
        assertEquals("96598765432", normalized)
    }

    @Test
    fun `validatePhoneInput - Saudi trunk prefix stripped and valid`() {
        val (valid, error, normalized) = PhoneUtils.validatePhoneInput("+9660559876543")
        assertTrue(valid)
        assertNull(error)
        assertEquals("966559876543", normalized)
    }

    @Test
    fun `validatePhoneInput - Kuwait invalid mobile prefix rejected`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("96518765432")
        assertFalse(valid)
        assertTrue(error!!.contains("Kuwait"))
    }

    @Test
    fun `validatePhoneInput - Saudi wrong length rejected`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("96655987") // too few local digits
        assertFalse(valid)
        assertTrue(error!!.contains("Saudi Arabia"))
    }

    // ──────────────────────────────────────────────
    // deduplicatePhones()
    // ──────────────────────────────────────────────

    @Test
    fun `deduplicatePhones - removes duplicates preserving order`() {
        val input = listOf("96598765432", "96512345678", "96598765432", "96599999999", "96512345678")
        val result = PhoneUtils.deduplicatePhones(input)
        assertEquals(listOf("96598765432", "96512345678", "96599999999"), result)
    }

    @Test
    fun `deduplicatePhones - no duplicates returns same list`() {
        val input = listOf("96598765432", "96512345678")
        val result = PhoneUtils.deduplicatePhones(input)
        assertEquals(input, result)
    }

    @Test
    fun `deduplicatePhones - empty list`() {
        val result = PhoneUtils.deduplicatePhones(emptyList())
        assertEquals(emptyList(), result)
    }
}
