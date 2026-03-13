package otp.captcha

import otp.CaptchaVerifier
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cloudflare Turnstile CAPTCHA verifier. Free, privacy-friendly.
 *
 * Usage:
 *   val verifier = TurnstileVerifier(secretKey = System.getenv("TURNSTILE_SECRET"))
 *   val service = OtpService(sms, store, captchaVerifier = verifier)
 *
 * Get your secret key at: https://dash.cloudflare.com/?to=/:account/turnstile
 *
 * Zero dependencies — uses only java.net.HttpURLConnection.
 */
class TurnstileVerifier(private val secretKey: String) : CaptchaVerifier {

    override fun verify(token: String, ip: String): Boolean {
        return try {
            val url = URL("https://challenges.cloudflare.com/turnstile/v0/siteverify")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val body = buildJsonBody(
                "secret" to secretKey,
                "response" to token,
                "remoteip" to ip
            )
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            if (conn.responseCode != 200) return false

            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            // Simple check — avoid JSON library dependency
            response.contains("\"success\":true") || response.contains("\"success\": true")
        } catch (_: Exception) {
            // Fail closed: any error = verification failed
            false
        }
    }

    private fun escapeJsonValue(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) sb.append("\\u${ch.code.toString(16).padStart(4, '0')}") else sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun buildJsonBody(vararg pairs: Pair<String, String>): String {
        val entries = pairs.joinToString(",") { (k, v) -> "\"${escapeJsonValue(k)}\":\"${escapeJsonValue(v)}\"" }
        return "{$entries}"
    }
}
