package com.kwtsms

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * JSONL logger for API calls. Never throws, never blocks the main flow.
 */
internal object Logger {

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    /**
     * Mask credentials in a request payload for logging.
     * Replaces the "password" value with "***".
     */
    fun maskCredentials(payload: Map<String, Any?>): Map<String, String> {
        val masked = mutableMapOf<String, String>()
        for ((key, value) in payload) {
            masked[key] = when (key) {
                "password" -> "***"
                "username" -> {
                    val v = value?.toString() ?: ""
                    if (v.length > 2) v.substring(0, 2) + "***" else "***"
                }
                else -> value?.toString() ?: ""
            }
        }
        return masked
    }

    /**
     * Write a JSONL log entry. Never throws.
     */
    fun writeLog(
        logFile: String,
        endpoint: String,
        request: Map<String, Any?>,
        response: String,
        ok: Boolean,
        error: String? = null
    ) {
        if (logFile.isEmpty()) return

        try {
            val ts = Instant.now().atOffset(ZoneOffset.UTC).format(isoFormatter)
            val masked = maskCredentials(request)

            val entry = buildJsonObject {
                put("ts", ts)
                put("endpoint", endpoint)
                put("request", masked)
                put("response", response)
                put("ok", ok)
                if (error != null) {
                    put("error", error)
                }
            }

            val file = File(logFile)
            file.appendText(entry + "\n")
        } catch (_: Exception) {
            // Logging must never crash the main flow
        }
    }

    /**
     * Build a simple JSON object string from key-value pairs.
     */
    private fun buildJsonObject(block: JsonBuilder.() -> Unit): String {
        val builder = JsonBuilder()
        builder.block()
        return builder.build()
    }

    internal class JsonBuilder {
        private val entries = mutableListOf<String>()

        fun put(key: String, value: String) {
            entries.add("\"${escapeJson(key)}\":\"${escapeJson(value)}\"")
        }

        fun put(key: String, value: Boolean) {
            entries.add("\"${escapeJson(key)}\":$value")
        }

        fun put(key: String, value: Map<String, String>) {
            val inner = value.entries.joinToString(",") { (k, v) ->
                "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
            }
            entries.add("\"${escapeJson(key)}\":{$inner}")
        }

        fun build(): String = "{${entries.joinToString(",")}}"

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
    }
}
