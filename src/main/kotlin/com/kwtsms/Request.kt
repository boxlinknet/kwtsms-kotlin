package com.kwtsms

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Minimal JSON utilities for kwtSMS API communication.
 * No external JSON library dependency.
 */
internal object JsonUtils {

    /**
     * Serialize a map to a JSON string. Handles String, Number, Boolean, List, and Map values.
     */
    fun toJson(map: Map<String, Any?>): String {
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((key, value) in map) {
            if (!first) sb.append(',')
            first = false
            sb.append('"').append(escapeJson(key)).append("\":")
            appendValue(sb, value)
        }
        sb.append('}')
        return sb.toString()
    }

    private fun appendValue(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is String -> sb.append('"').append(escapeJson(value)).append('"')
            is Number -> sb.append(value)
            is Boolean -> sb.append(value)
            is List<*> -> {
                sb.append('[')
                value.forEachIndexed { i, v ->
                    if (i > 0) sb.append(',')
                    appendValue(sb, v)
                }
                sb.append(']')
            }
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) sb.append(',')
                    first = false
                    sb.append('"').append(escapeJson(k.toString())).append("\":")
                    appendValue(sb, v)
                }
                sb.append('}')
            }
            else -> sb.append('"').append(escapeJson(value.toString())).append('"')
        }
    }

    private fun escapeJson(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append("\\u${ch.code.toString(16).padStart(4, '0')}")
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Parse a JSON string into a Map or List. Handles nested structures.
     */
    fun parseJson(json: String): Any? {
        val parser = JsonParser(json.trim())
        return parser.parseValue()
    }

    @Suppress("TooManyFunctions")
    private class JsonParser(private val json: String) {
        private var pos = 0
        private var depth = 0
        private val maxDepth = 50

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= json.length) return null
            return when (json[pos]) {
                '{' -> {
                    if (++depth > maxDepth) throw ApiException("JSON nesting too deep (max $maxDepth)")
                    parseObject().also { depth-- }
                }
                '[' -> {
                    if (++depth > maxDepth) throw ApiException("JSON nesting too deep (max $maxDepth)")
                    parseArray().also { depth-- }
                }
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            pos++ // skip {
            skipWhitespace()
            if (pos < json.length && json[pos] == '}') {
                pos++
                return map
            }
            while (pos < json.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < json.length && json[pos] == ',') {
                    pos++
                } else {
                    break
                }
            }
            skipWhitespace()
            if (pos < json.length && json[pos] == '}') pos++
            return map
        }

        private fun parseArray(): List<Any?> {
            val list = mutableListOf<Any?>()
            pos++ // skip [
            skipWhitespace()
            if (pos < json.length && json[pos] == ']') {
                pos++
                return list
            }
            while (pos < json.length) {
                list.add(parseValue())
                skipWhitespace()
                if (pos < json.length && json[pos] == ',') {
                    pos++
                } else {
                    break
                }
            }
            skipWhitespace()
            if (pos < json.length && json[pos] == ']') pos++
            return list
        }

        private fun parseString(): String {
            skipWhitespace()
            expect('"')
            val sb = StringBuilder()
            while (pos < json.length && json[pos] != '"') {
                if (json[pos] == '\\') {
                    pos++
                    if (pos < json.length) {
                        when (json[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (pos + 4 < json.length) {
                                    val hex = json.substring(pos + 1, pos + 5)
                                    sb.append(hex.toInt(16).toChar())
                                    pos += 4
                                }
                            }
                            else -> sb.append(json[pos])
                        }
                    }
                } else {
                    sb.append(json[pos])
                }
                pos++
            }
            if (pos < json.length) pos++ // skip closing "
            return sb.toString()
        }

        private fun parseNumber(): Number {
            val start = pos
            if (pos < json.length && json[pos] == '-') pos++
            while (pos < json.length && json[pos].isDigit()) pos++
            var isDouble = false
            if (pos < json.length && json[pos] == '.') {
                isDouble = true
                pos++
                while (pos < json.length && json[pos].isDigit()) pos++
            }
            if (pos < json.length && (json[pos] == 'e' || json[pos] == 'E')) {
                isDouble = true
                pos++
                if (pos < json.length && (json[pos] == '+' || json[pos] == '-')) pos++
                while (pos < json.length && json[pos].isDigit()) pos++
            }
            val numStr = json.substring(start, pos)
            return if (isDouble) {
                numStr.toDouble()
            } else {
                val longVal = numStr.toLong()
                if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) longVal.toInt() else longVal
            }
        }

        private fun parseBoolean(): Boolean {
            return if (json.startsWith("true", pos)) {
                pos += 4
                true
            } else {
                pos += 5
                false
            }
        }

        private fun parseNull(): Any? {
            pos += 4
            return null
        }

        private fun skipWhitespace() {
            while (pos < json.length && json[pos].isWhitespace()) pos++
        }

        private fun expect(ch: Char) {
            if (pos < json.length && json[pos] == ch) {
                pos++
            }
        }
    }
}

/**
 * HTTP request layer for kwtSMS API. Uses java.net.HttpURLConnection (no dependencies).
 */
internal object ApiRequest {

    private const val BASE_URL = "https://www.kwtsms.com/API"
    private const val TIMEOUT_MS = 15_000
    private const val MAX_RESPONSE_SIZE = 1_048_576 // 1 MB

    /**
     * Make a POST request to a kwtSMS API endpoint.
     *
     * @param endpoint API endpoint name (e.g., "send", "balance")
     * @param payload Request body as a map
     * @param logFile Path to JSONL log file (empty string to disable logging)
     * @return Parsed JSON response as a map
     * @throws ApiException on network or parsing errors
     */
    fun post(
        endpoint: String,
        payload: Map<String, Any?>,
        logFile: String = ""
    ): Map<String, Any?> {
        val url = "$BASE_URL/$endpoint/"
        val jsonBody = JsonUtils.toJson(payload)
        var responseBody = ""

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.doOutput = true

            // Write request body
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            // Read response (including error responses: kwtSMS returns JSON in 4xx bodies)
            val stream = try {
                connection.inputStream
            } catch (_: Exception) {
                connection.errorStream
            }

            responseBody = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val sb = StringBuilder()
                    val buf = CharArray(8192)
                    var total = 0
                    var n: Int
                    while (reader.read(buf).also { n = it } != -1 && total < MAX_RESPONSE_SIZE) {
                        sb.append(buf, 0, n)
                        total += n
                    }
                    sb.toString()
                }
            } else {
                ""
            }

            connection.disconnect()

            if (responseBody.isBlank()) {
                Logger.writeLog(logFile, endpoint, payload, "", false, "Empty response")
                throw ApiException("Empty response from API")
            }

            @Suppress("UNCHECKED_CAST")
            val parsed = JsonUtils.parseJson(responseBody) as? Map<String, Any?>
                ?: throw ApiException("Invalid JSON response")

            val isOk = parsed["result"]?.toString() == "OK"
            Logger.writeLog(logFile, endpoint, payload, responseBody, isOk)

            return parsed
        } catch (e: SocketTimeoutException) {
            Logger.writeLog(logFile, endpoint, payload, responseBody, false, "Request timed out after ${TIMEOUT_MS}ms")
            throw ApiException("Request timed out after ${TIMEOUT_MS}ms")
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown network error"
            Logger.writeLog(logFile, endpoint, payload, responseBody, false, msg)
            throw ApiException("Network error: $msg")
        }
    }
}

/**
 * Exception for API communication errors.
 */
class ApiException(message: String) : Exception(message)
