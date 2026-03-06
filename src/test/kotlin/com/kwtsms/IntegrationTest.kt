package com.kwtsms

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests against the live kwtSMS API.
 *
 * Requires KOTLIN_USERNAME and KOTLIN_PASSWORD environment variables.
 * Always uses testMode=true so no credits are consumed and no messages are delivered.
 * Skipped gracefully if credentials are not set.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class IntegrationTest {

    private lateinit var sms: KwtSMS
    private var credentialsAvailable = false

    @BeforeAll
    fun setup() {
        val username = System.getenv("KOTLIN_USERNAME") ?: ""
        val password = System.getenv("KOTLIN_PASSWORD") ?: ""
        credentialsAvailable = username.isNotBlank() && password.isNotBlank()
        if (credentialsAvailable) {
            sms = KwtSMS(
                username = username,
                password = password,
                testMode = true,
                logFile = "" // no log file during tests
            )
        }
    }

    private fun requireCredentials() {
        assumeTrue(credentialsAvailable, "KOTLIN_USERNAME / KOTLIN_PASSWORD not set, skipping integration tests")
    }

    // ──────────────────────────────────────────────
    // verify()
    // ──────────────────────────────────────────────

    @Test
    @Order(1)
    fun `verify - valid credentials`() {
        requireCredentials()
        val result = sms.verify()
        assertTrue(result.ok, "verify() should succeed: ${result.error}")
        assertNotNull(result.balance)
        assertTrue(result.balance!! >= 0, "Balance should be non-negative")
    }

    @Test
    @Order(2)
    fun `verify - wrong credentials`() {
        requireCredentials()
        val bad = KwtSMS(
            username = "wrong_user_12345",
            password = "wrong_pass_12345",
            testMode = true,
            logFile = ""
        )
        val result = bad.verify()
        assertTrue(!result.ok, "verify() with wrong credentials should fail")
        assertNotNull(result.error)
    }

    // ──────────────────────────────────────────────
    // balance()
    // ──────────────────────────────────────────────

    @Test
    @Order(3)
    fun `balance - returns non-negative number`() {
        requireCredentials()
        val balance = sms.balance()
        assertNotNull(balance)
        assertTrue(balance >= 0)
    }

    // ──────────────────────────────────────────────
    // send()
    // ──────────────────────────────────────────────

    @Test
    @Order(10)
    fun `send - single valid Kuwait number`() {
        requireCredentials()
        val result = sms.send("96598765432", "Test message from Kotlin client")
        // In test mode, the API should accept the message
        assertEquals("OK", result.result, "Send failed: ${result.description} ${result.action}")
        assertNotNull(result.msgId)
    }

    @Test
    @Order(11)
    fun `send - plus prefix normalization`() {
        requireCredentials()
        val result = sms.send("+96598765432", "Test with + prefix")
        assertEquals("OK", result.result, "Send with + prefix failed: ${result.description}")
    }

    @Test
    @Order(12)
    fun `send - 00 prefix normalization`() {
        requireCredentials()
        val result = sms.send("0096598765432", "Test with 00 prefix")
        assertEquals("OK", result.result, "Send with 00 prefix failed: ${result.description}")
    }

    @Test
    @Order(13)
    fun `send - Arabic digit normalization`() {
        requireCredentials()
        // ٩٦٥٩٨٧٦٥٤٣٢
        val result = sms.send(
            "\u0669\u0666\u0665\u0669\u0668\u0667\u0666\u0665\u0664\u0663\u0662",
            "Test with Arabic digits"
        )
        assertEquals("OK", result.result, "Send with Arabic digits failed: ${result.description}")
    }

    @Test
    @Order(14)
    fun `send - email address rejected locally`() {
        requireCredentials()
        val result = sms.send("user@example.com", "Test message")
        assertEquals("ERROR", result.result)
        assertTrue(result.invalid.isNotEmpty())
        assertTrue(result.invalid[0].error.contains("email"))
    }

    @Test
    @Order(15)
    fun `send - too short number rejected locally`() {
        requireCredentials()
        val result = sms.send("12345", "Test message")
        assertEquals("ERROR", result.result)
        assertTrue(result.invalid.isNotEmpty())
        assertTrue(result.invalid[0].error.contains("too short"))
    }

    @Test
    @Order(16)
    fun `send - letters only rejected locally`() {
        requireCredentials()
        val result = sms.send("abcdef", "Test message")
        assertEquals("ERROR", result.result)
        assertTrue(result.invalid.isNotEmpty())
        assertTrue(result.invalid[0].error.contains("no digits"))
    }

    @Test
    @Order(17)
    fun `send - mixed valid and invalid numbers`() {
        requireCredentials()
        val result = sms.send(
            listOf("96598765432", "user@example.com", "12345"),
            "Mixed number test"
        )
        // The valid number should be sent
        assertEquals("OK", result.result, "Mixed send failed: ${result.description}")
        // Invalid entries should be reported
        assertEquals(2, result.invalid.size)
    }

    @Test
    @Order(18)
    fun `send - empty message`() {
        requireCredentials()
        val result = sms.send("96598765432", "")
        assertEquals("ERROR", result.result)
    }

    @Test
    @Order(19)
    fun `send - emoji-only message`() {
        requireCredentials()
        val result = sms.send("96598765432", "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02")
        assertEquals("ERROR", result.result)
        // Message should be empty after emoji stripping
    }

    @Test
    @Order(20)
    fun `send - deduplication of normalized numbers`() {
        requireCredentials()
        val result = sms.send(
            listOf("+96598765432", "0096598765432", "96598765432"),
            "Dedup test"
        )
        assertEquals("OK", result.result, "Dedup send failed: ${result.description}")
        // All three normalize to the same number; should send once
        assertEquals(1, result.numbers)
    }

    @Test
    @Order(21)
    fun `send - single leading zero stripped`() {
        requireCredentials()
        // 098765432 -> 98765432 (7+ digits, valid)
        val result = sms.send("098765432", "Leading zero test")
        // May succeed or fail depending on the country route, but should not crash
        assertNotNull(result.result)
    }

    // ──────────────────────────────────────────────
    // senderIds()
    // ──────────────────────────────────────────────

    @Test
    @Order(30)
    fun `senderIds - returns non-empty list`() {
        requireCredentials()
        val result = sms.senderIds()
        assertEquals("OK", result.result, "senderIds() failed: ${result.description}")
        assertTrue(result.senderIds.isNotEmpty())
    }

    // ──────────────────────────────────────────────
    // coverage()
    // ──────────────────────────────────────────────

    @Test
    @Order(31)
    fun `coverage - returns non-empty prefix list`() {
        requireCredentials()
        val result = sms.coverage()
        assertEquals("OK", result.result, "coverage() failed: ${result.description}")
        assertTrue(result.prefixes.isNotEmpty())
    }

    // ──────────────────────────────────────────────
    // validate()
    // ──────────────────────────────────────────────

    @Test
    @Order(40)
    fun `validate - valid Kuwait number`() {
        requireCredentials()
        val result = sms.validate(listOf("96598765432"))
        assertTrue(result.error == null, "validate() returned error: ${result.error}")
        assertTrue(result.ok.isNotEmpty() || result.er.isNotEmpty() || result.nr.isNotEmpty(),
            "validate() returned no categorized numbers")
    }

    @Test
    @Order(41)
    fun `validate - invalid input rejected locally`() {
        requireCredentials()
        val result = sms.validate(listOf("user@example.com", "12345"))
        assertTrue(result.rejected.isNotEmpty())
        assertEquals(2, result.rejected.size)
    }

    // ──────────────────────────────────────────────
    // status()
    // ──────────────────────────────────────────────

    @Test
    @Order(50)
    fun `status - fake message ID returns error`() {
        requireCredentials()
        val result = sms.status("fake_msg_id_12345")
        assertEquals("ERROR", result.result)
    }

    // ──────────────────────────────────────────────
    // deliveryReport()
    // ──────────────────────────────────────────────

    @Test
    @Order(51)
    fun `deliveryReport - fake message ID returns error`() {
        requireCredentials()
        val result = sms.deliveryReport("fake_msg_id_12345")
        assertEquals("ERROR", result.result)
    }

    // ──────────────────────────────────────────────
    // Wrong sender ID
    // ──────────────────────────────────────────────

    @Test
    @Order(60)
    fun `send - wrong sender ID handled gracefully`() {
        requireCredentials()
        val result = sms.send("96598765432", "Test wrong sender", "NONEXISTENT-SENDER-ID-12345")
        // API may accept in test mode or reject with ERR008
        // Either way, the client should not crash
        assertNotNull(result.result)
        assertTrue(result.result == "OK" || result.result == "ERROR")
    }

    // ──────────────────────────────────────────────
    // Cached balance
    // ──────────────────────────────────────────────

    @Test
    @Order(70)
    fun `cached balance - populated after verify`() {
        requireCredentials()
        val freshClient = KwtSMS(
            username = System.getenv("KOTLIN_USERNAME")!!,
            password = System.getenv("KOTLIN_PASSWORD")!!,
            testMode = true,
            logFile = ""
        )
        // Before verify, cache is null
        assertTrue(freshClient.cachedBalance == null)

        freshClient.verify()
        assertNotNull(freshClient.cachedBalance)
        assertTrue(freshClient.cachedBalance!! >= 0)
    }
}
