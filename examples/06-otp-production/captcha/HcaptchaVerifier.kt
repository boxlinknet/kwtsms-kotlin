package otp.captcha

import otp.CaptchaVerifier
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * hCaptcha verifier. GDPR-friendly alternative to reCAPTCHA.
 *
 * Usage:
 *   val verifier = HcaptchaVerifier(secretKey = System.getenv("HCAPTCHA_SECRET"))
 *   val service = OtpService(sms, store, captchaVerifier = verifier)
 *
 * Get your secret key at: https://dashboard.hcaptcha.com/
 *
 * Zero dependencies — uses only java.net.HttpURLConnection.
 * Uses URL-encoded body POST as per hCaptcha API spec.
 */
class HcaptchaVerifier(private val secretKey: String) : CaptchaVerifier {

    override fun verify(token: String, ip: String): Boolean {
        return try {
            val url = URL("https://api.hcaptcha.com/siteverify")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val body = "secret=${URLEncoder.encode(secretKey, "UTF-8")}" +
                "&response=${URLEncoder.encode(token, "UTF-8")}" +
                "&remoteip=${URLEncoder.encode(ip, "UTF-8")}"
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            if (conn.responseCode != 200) return false

            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            response.contains("\"success\":true") || response.contains("\"success\": true")
        } catch (_: Exception) {
            // Fail closed: any error = verification failed
            false
        }
    }
}
