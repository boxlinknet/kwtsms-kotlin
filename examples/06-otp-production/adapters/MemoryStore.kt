package otp.adapters

import otp.OtpRecord
import otp.OtpStore

/**
 * In-memory OTP store. Zero dependencies. For development and testing only.
 *
 * NOT suitable for production:
 * - Data lost on restart
 * - Not shared across server instances
 * - No automatic cleanup of expired records
 *
 * For production, use SqliteStore or implement OtpStore with your database.
 */
class MemoryStore : OtpStore {
    private val records = mutableMapOf<String, OtpRecord>()
    private val ipUsage = mutableListOf<Pair<String, Long>>() // (ip, timestamp)

    override fun save(record: OtpRecord) {
        records[record.phone] = record
    }

    override fun findLatest(phone: String): OtpRecord? {
        return records[phone]
    }

    override fun incrementAttempts(phone: String) {
        records[phone]?.let {
            records[phone] = it.copy(attempts = it.attempts + 1)
        }
    }

    override fun markUsed(phone: String) {
        records[phone]?.let {
            records[phone] = it.copy(used = true)
        }
    }

    override fun delete(phone: String) {
        records.remove(phone)
    }

    override fun countByPhone(phone: String, sinceMs: Long): Int {
        val record = records[phone] ?: return 0
        return if (record.createdAt >= sinceMs) 1 else 0
    }

    override fun countByIp(ip: String, sinceMs: Long): Int {
        return ipUsage.count { it.first == ip && it.second >= sinceMs }
    }

    override fun recordIpUsage(ip: String) {
        ipUsage.add(ip to System.currentTimeMillis())
        // Cleanup entries older than 2 hours
        val cutoff = System.currentTimeMillis() - 7200_000
        ipUsage.removeAll { it.second < cutoff }
    }
}
