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
        val (valid, error, _) = PhoneUtils.validatePhoneInput("1234567")
        assertTrue(valid)
        assertNull(error)
    }

    @Test
    fun `validatePhoneInput - maximum valid length (15 digits)`() {
        val (valid, error, _) = PhoneUtils.validatePhoneInput("123456789012345")
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
