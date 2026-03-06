package com.kwtsms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiErrorsTest {

    // ──────────────────────────────────────────────
    // API_ERRORS map
    // ──────────────────────────────────────────────

    @Test
    fun `API_ERRORS has exactly 29 entries`() {
        assertEquals(29, API_ERRORS.size)
    }

    @Test
    fun `API_ERRORS ERR003 mentions credentials`() {
        val action = API_ERRORS["ERR003"]!!
        assertTrue(action.contains("KWTSMS_USERNAME"))
        assertTrue(action.contains("KWTSMS_PASSWORD"))
    }

    @Test
    fun `API_ERRORS ERR006 mentions country code`() {
        val action = API_ERRORS["ERR006"]!!
        assertTrue(action.contains("country code"))
    }

    @Test
    fun `API_ERRORS ERR010 mentions recharge`() {
        val action = API_ERRORS["ERR010"]!!
        assertTrue(action.contains("kwtsms.com"))
    }

    @Test
    fun `API_ERRORS ERR013 mentions wait`() {
        val action = API_ERRORS["ERR013"]!!
        assertTrue(action.contains("Wait"))
    }

    @Test
    fun `API_ERRORS ERR024 mentions IP`() {
        val action = API_ERRORS["ERR024"]!!
        assertTrue(action.contains("IP"))
    }

    @Test
    fun `API_ERRORS ERR028 mentions 15 seconds`() {
        val action = API_ERRORS["ERR028"]!!
        assertTrue(action.contains("15 seconds"))
    }

    @Test
    fun `API_ERRORS covers all known error codes`() {
        val expectedCodes = listOf(
            "ERR001", "ERR002", "ERR003", "ERR004", "ERR005",
            "ERR006", "ERR007", "ERR008", "ERR009", "ERR010",
            "ERR011", "ERR012", "ERR013",
            "ERR019", "ERR020", "ERR021", "ERR022", "ERR023",
            "ERR024", "ERR025", "ERR026", "ERR027", "ERR028",
            "ERR029", "ERR030", "ERR031", "ERR032", "ERR033",
            "ERR_INVALID_INPUT"
        )
        for (code in expectedCodes) {
            assertTrue(API_ERRORS.containsKey(code), "Missing error code: $code")
        }
    }

    // ──────────────────────────────────────────────
    // enrichError()
    // ──────────────────────────────────────────────

    @Test
    fun `enrichError - adds action for known error code`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR003",
            "description" to "Authentication error"
        )
        val enriched = enrichError(response)
        assertEquals(API_ERRORS["ERR003"], enriched["action"])
    }

    @Test
    fun `enrichError - all known codes get an action`() {
        for ((code, expectedAction) in API_ERRORS) {
            val response = mapOf<String, Any?>(
                "result" to "ERROR",
                "code" to code,
                "description" to "Some description"
            )
            val enriched = enrichError(response)
            assertEquals(expectedAction, enriched["action"], "Missing action for $code")
        }
    }

    @Test
    fun `enrichError - unknown code returns no action`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR999",
            "description" to "Unknown error"
        )
        val enriched = enrichError(response)
        assertFalse(enriched.containsKey("action"))
    }

    @Test
    fun `enrichError - OK response not enriched`() {
        val response = mapOf<String, Any?>(
            "result" to "OK",
            "available" to 150
        )
        val enriched = enrichError(response)
        assertFalse(enriched.containsKey("action"))
    }

    @Test
    fun `enrichError - response without code not enriched`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "description" to "Some error"
        )
        val enriched = enrichError(response)
        assertFalse(enriched.containsKey("action"))
    }

    @Test
    fun `enrichError - preserves original fields`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR003",
            "description" to "Auth error",
            "extra" to "field"
        )
        val enriched = enrichError(response)
        assertEquals("ERROR", enriched["result"])
        assertEquals("ERR003", enriched["code"])
        assertEquals("Auth error", enriched["description"])
        assertEquals("field", enriched["extra"])
    }

    // ──────────────────────────────────────────────
    // Mocked API response tests
    // ──────────────────────────────────────────────

    @Test
    fun `mock ERR003 - wrong credentials`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR003",
            "description" to "Authentication error, username or password are not correct."
        )
        val enriched = enrichError(response)
        assertEquals("ERROR", enriched["result"])
        assertTrue(enriched["action"].toString().contains("KWTSMS_USERNAME"))
    }

    @Test
    fun `mock ERR026 - country not allowed`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR026",
            "description" to "Country not activated"
        )
        val enriched = enrichError(response)
        assertTrue(enriched["action"].toString().contains("country"))
    }

    @Test
    fun `mock ERR025 - invalid number`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR025",
            "description" to "Invalid number"
        )
        val enriched = enrichError(response)
        assertTrue(enriched["action"].toString().contains("country code"))
    }

    @Test
    fun `mock ERR010 - zero balance`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR010",
            "description" to "Zero balance"
        )
        val enriched = enrichError(response)
        assertTrue(enriched["action"].toString().contains("kwtsms.com"))
    }

    @Test
    fun `mock ERR024 - IP not whitelisted`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR024",
            "description" to "IP not in whitelist"
        )
        val enriched = enrichError(response)
        assertTrue(enriched["action"].toString().contains("IP"))
    }

    @Test
    fun `mock ERR028 - rate limit`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR028",
            "description" to "Rate limited"
        )
        val enriched = enrichError(response)
        assertTrue(enriched["action"].toString().contains("15 seconds"))
    }

    @Test
    fun `mock ERR008 - banned sender ID`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR008",
            "description" to "Sender ID is banned"
        )
        val enriched = enrichError(response)
        assertTrue(enriched["action"].toString().contains("sender ID"))
    }

    @Test
    fun `mock ERR999 - unknown error does not crash`() {
        val response = mapOf<String, Any?>(
            "result" to "ERROR",
            "code" to "ERR999",
            "description" to "Some unknown error"
        )
        val enriched = enrichError(response)
        assertEquals("Some unknown error", enriched["description"])
        assertNull(enriched["action"])
    }
}
