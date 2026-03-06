package com.kwtsms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonUtilsTest {

    @Test
    fun `toJson - simple string values`() {
        val map = mapOf<String, Any?>("key" to "value")
        val json = JsonUtils.toJson(map)
        assertEquals("{\"key\":\"value\"}", json)
    }

    @Test
    fun `toJson - number values`() {
        val map = mapOf<String, Any?>("num" to 42)
        val json = JsonUtils.toJson(map)
        assertEquals("{\"num\":42}", json)
    }

    @Test
    fun `toJson - boolean values`() {
        val map = mapOf<String, Any?>("flag" to true)
        val json = JsonUtils.toJson(map)
        assertEquals("{\"flag\":true}", json)
    }

    @Test
    fun `toJson - null values`() {
        val map = mapOf<String, Any?>("key" to null)
        val json = JsonUtils.toJson(map)
        assertEquals("{\"key\":null}", json)
    }

    @Test
    fun `toJson - escapes special characters`() {
        val map = mapOf<String, Any?>("msg" to "Hello \"World\"\nNew line")
        val json = JsonUtils.toJson(map)
        assertTrue(json.contains("\\\"World\\\""))
        assertTrue(json.contains("\\n"))
    }

    @Test
    fun `toJson - multiple keys`() {
        val map = mapOf<String, Any?>("a" to "1", "b" to "2")
        val json = JsonUtils.toJson(map)
        assertTrue(json.contains("\"a\":\"1\""))
        assertTrue(json.contains("\"b\":\"2\""))
    }

    @Test
    fun `parseJson - simple object`() {
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson("{\"result\":\"OK\",\"available\":150}") as Map<String, Any?>
        assertEquals("OK", result["result"])
        assertEquals(150, result["available"])
    }

    @Test
    fun `parseJson - nested object`() {
        val json = """{"result":"OK","mobile":{"OK":["96598765432"],"ER":[],"NR":[]}}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals("OK", result["result"])
        @Suppress("UNCHECKED_CAST")
        val mobile = result["mobile"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val okList = mobile["OK"] as List<String>
        assertEquals("96598765432", okList[0])
    }

    @Test
    fun `parseJson - string array`() {
        val json = """{"senderid":["KWT-SMS","MY-APP"]}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val ids = result["senderid"] as List<String>
        assertEquals(2, ids.size)
        assertEquals("KWT-SMS", ids[0])
        assertEquals("MY-APP", ids[1])
    }

    @Test
    fun `parseJson - boolean and null`() {
        val json = """{"active":true,"deleted":false,"data":null}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals(true, result["active"])
        assertEquals(false, result["deleted"])
        assertNull(result["data"])
    }

    @Test
    fun `parseJson - escaped strings`() {
        val json = """{"msg":"Hello \"World\""}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals("Hello \"World\"", result["msg"])
    }

    @Test
    fun `parseJson - double values`() {
        val json = """{"balance":150.5}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals(150.5, result["balance"])
    }

    @Test
    fun `parseJson - negative numbers`() {
        val json = """{"offset":-10}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals(-10, result["offset"])
    }

    @Test
    fun `parseJson - empty object`() {
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson("{}") as Map<String, Any?>
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseJson - empty array`() {
        val json = """{"items":[]}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val items = result["items"] as List<*>
        assertTrue(items.isEmpty())
    }

    @Test
    fun `parseJson - send response format`() {
        val json = """{"result":"OK","msg-id":"abc123","numbers":1,"points-charged":1,"balance-after":180,"unix-timestamp":1684763355}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals("OK", result["result"])
        assertEquals("abc123", result["msg-id"])
        assertEquals(1, result["numbers"])
        assertEquals(1, result["points-charged"])
        assertEquals(180, result["balance-after"])
        assertEquals(1684763355, result["unix-timestamp"])
    }

    @Test
    fun `parseJson - error response format`() {
        val json = """{"result":"ERROR","code":"ERR003","description":"Authentication error, username or password are not correct."}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals("ERROR", result["result"])
        assertEquals("ERR003", result["code"])
        assertTrue(result["description"].toString().contains("Authentication"))
    }

    @Test
    fun `parseJson - delivery report with array of objects`() {
        val json = """{"result":"OK","report":[{"Number":"96598765432","Status":"Received by recipient"}]}"""
        @Suppress("UNCHECKED_CAST")
        val result = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals("OK", result["result"])
        @Suppress("UNCHECKED_CAST")
        val report = result["report"] as List<Map<String, Any?>>
        assertEquals(1, report.size)
        assertEquals("96598765432", report[0]["Number"])
        assertEquals("Received by recipient", report[0]["Status"])
    }

    @Test
    fun `roundtrip - toJson then parseJson`() {
        val original = mapOf<String, Any?>(
            "username" to "test",
            "password" to "pass123",
            "mobile" to "96598765432"
        )
        val json = JsonUtils.toJson(original)
        @Suppress("UNCHECKED_CAST")
        val parsed = JsonUtils.parseJson(json) as Map<String, Any?>
        assertEquals("test", parsed["username"])
        assertEquals("pass123", parsed["password"])
        assertEquals("96598765432", parsed["mobile"])
    }
}
