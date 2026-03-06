package com.kwtsms

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvLoaderTest {

    private fun withTempEnv(content: String, block: (String) -> Unit) {
        val file = File.createTempFile("test_env", ".env")
        try {
            file.writeText(content)
            block(file.absolutePath)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `loadEnvFile - parses key-value pairs`() {
        withTempEnv("FOO=bar\nBAZ=qux") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("bar", vars["FOO"])
            assertEquals("qux", vars["BAZ"])
        }
    }

    @Test
    fun `loadEnvFile - ignores blank lines and comments`() {
        withTempEnv("# comment\n\nFOO=bar\n\n# another comment\nBAZ=qux") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals(2, vars.size)
            assertEquals("bar", vars["FOO"])
            assertEquals("qux", vars["BAZ"])
        }
    }

    @Test
    fun `loadEnvFile - strips inline comments from unquoted values`() {
        withTempEnv("FOO=bar # this is a comment") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("bar", vars["FOO"])
        }
    }

    @Test
    fun `loadEnvFile - preserves hash in quoted values`() {
        withTempEnv("FOO=\"bar#baz\"") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("bar#baz", vars["FOO"])
        }
    }

    @Test
    fun `loadEnvFile - supports single-quoted values`() {
        withTempEnv("FOO='hello world'") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("hello world", vars["FOO"])
        }
    }

    @Test
    fun `loadEnvFile - supports double-quoted values`() {
        withTempEnv("FOO=\"hello world\"") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("hello world", vars["FOO"])
        }
    }

    @Test
    fun `loadEnvFile - missing file returns empty map`() {
        val vars = EnvLoader.loadEnvFile("/nonexistent/path/.env")
        assertTrue(vars.isEmpty())
    }

    @Test
    fun `loadEnvFile - trims whitespace around keys and values`() {
        withTempEnv("  FOO  =  bar  ") { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("bar", vars["FOO"])
        }
    }

    @Test
    fun `loadEnvFile - handles kwtsms variables`() {
        withTempEnv(
            """
            KWTSMS_USERNAME=kotlin_testuser
            KWTSMS_PASSWORD=kotlin_testpass
            KWTSMS_SENDER_ID=MY-APP  # my sender
            KWTSMS_TEST_MODE=1
            KWTSMS_LOG_FILE=kwtsms.log
            """.trimIndent()
        ) { path ->
            val vars = EnvLoader.loadEnvFile(path)
            assertEquals("kotlin_testuser", vars["KWTSMS_USERNAME"])
            assertEquals("kotlin_testpass", vars["KWTSMS_PASSWORD"])
            assertEquals("MY-APP", vars["KWTSMS_SENDER_ID"])
            assertEquals("1", vars["KWTSMS_TEST_MODE"])
            assertEquals("kwtsms.log", vars["KWTSMS_LOG_FILE"])
        }
    }
}
