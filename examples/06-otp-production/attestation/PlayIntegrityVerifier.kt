package otp.attestation

import otp.DeviceAttestVerifier
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Play Integrity API server-side verifier.
 *
 * Replaces CAPTCHA for Android apps. Verifies that the request comes from a genuine
 * app installation on a real, unmodified device.
 *
 * Setup:
 * 1. Enable Play Integrity API in Google Cloud Console
 * 2. Link your app in Play Console > App Integrity
 * 3. Pass the integrity token from the Android client to your backend
 * 4. This verifier calls Google's server-side API to decode the token
 *
 * Usage:
 *   val verifier = PlayIntegrityVerifier(
 *       projectNumber = System.getenv("GOOGLE_PROJECT_NUMBER"),
 *       accessToken = getGoogleAccessToken()  // OAuth2 service account token
 *   )
 *   val service = OtpService(sms, store, attestVerifier = verifier)
 *
 * Zero dependencies — uses only java.net.HttpURLConnection.
 *
 * See: https://developer.android.com/google/play/integrity
 */
class PlayIntegrityVerifier(
    private val projectNumber: String,
    private val accessToken: String
) : DeviceAttestVerifier {

    override fun verify(attestationToken: String): Boolean {
        return try {
            val url = URL(
                "https://playintegrity.googleapis.com/v1/$projectNumber:decodeIntegrityToken"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val body = buildJsonBody("integrity_token" to attestationToken)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            if (conn.responseCode != 200) return false

            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()

            // Check for valid device verdict
            // In production, parse the full JSON and check specific verdict fields:
            // - deviceRecognitionVerdict should contain "MEETS_DEVICE_INTEGRITY"
            // - appRecognitionVerdict should be "PLAY_RECOGNIZED"
            // - accountDetails.appLicensingVerdict should be "LICENSED"
            response.contains("MEETS_DEVICE_INTEGRITY") &&
                response.contains("PLAY_RECOGNIZED")
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
