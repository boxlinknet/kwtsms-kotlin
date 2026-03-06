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

            val body = """{"secret":"$secretKey","response":"$token","remoteip":"$ip"}"""
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
}
