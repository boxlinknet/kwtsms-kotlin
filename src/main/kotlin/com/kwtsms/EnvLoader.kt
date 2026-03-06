package com.kwtsms

import java.io.File

/**
 * Minimal .env file parser. Never throws, never modifies system environment.
 */
object EnvLoader {

    /**
     * Parse a .env file and return a map of key-value pairs.
     *
     * Rules:
     * - Ignores blank lines and lines starting with #
     * - Strips inline # comments from unquoted values
     * - Supports single-quoted and double-quoted values (preserves # inside quotes)
     * - Returns empty map for missing files
     * - Never modifies the system environment
     */
    @JvmStatic
    fun loadEnvFile(filePath: String = ".env"): Map<String, String> {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return emptyMap()
        }

        val result = mutableMapOf<String, String>()

        try {
            file.readLines().forEach { rawLine ->
                val line = rawLine.trim()

                // Skip blank lines and comments
                if (line.isEmpty() || line.startsWith('#')) return@forEach

                // Find the first = sign
                val eqIndex = line.indexOf('=')
                if (eqIndex < 0) return@forEach

                val key = line.substring(0, eqIndex).trim()
                if (key.isEmpty()) return@forEach

                var value = line.substring(eqIndex + 1).trim()

                // Handle quoted values
                if (value.length >= 2) {
                    val first = value[0]
                    val last = value[value.length - 1]
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length - 1)
                        result[key] = value
                        return@forEach
                    }
                }

                // Strip inline comments for unquoted values
                val commentIndex = value.indexOf('#')
                if (commentIndex > 0) {
                    value = value.substring(0, commentIndex).trim()
                }

                result[key] = value
            }
        } catch (_: Exception) {
            // Never crash on file read errors
        }

        return result
    }
}
