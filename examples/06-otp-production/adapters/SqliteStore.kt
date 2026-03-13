package otp.adapters

import otp.OtpRecord
import otp.OtpStore
import java.sql.Connection
import java.sql.DriverManager

/**
 * SQLite-backed OTP store. Embedded, zero-config, auto-creates tables.
 *
 * Uses WAL mode for concurrent reads. All methods use try-with-resources
 * for prepared statements to prevent resource leaks.
 *
 * Thread safety: all methods are synchronized on the connection.
 * For higher concurrency, use a connection pool (HikariCP, etc.).
 *
 * Dependencies (add to build.gradle.kts):
 *   implementation("org.xerial:sqlite-jdbc:3.44.1.0")
 *
 * Usage:
 *   val store = SqliteStore("otp.db")  // creates file if not exists
 *   val service = OtpService(sms, store, captchaVerifier)
 */
class SqliteStore(dbPath: String) : OtpStore {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath").also { c ->
        c.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
        c.createStatement().use { it.execute("""
            CREATE TABLE IF NOT EXISTS otp_records (
                phone TEXT PRIMARY KEY,
                code_hash TEXT NOT NULL,
                code_salt TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0,
                used INTEGER NOT NULL DEFAULT 0
            )
        """) }
        c.createStatement().use { it.execute("""
            CREATE TABLE IF NOT EXISTS ip_usage (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ip TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """) }
        c.createStatement().use { it.execute("""
            CREATE INDEX IF NOT EXISTS idx_ip_usage_ip_time ON ip_usage(ip, created_at)
        """) }
    }

    override fun save(record: OtpRecord) {
        synchronized(conn) {
            conn.prepareStatement("""
                INSERT OR REPLACE INTO otp_records (phone, code_hash, code_salt, created_at, expires_at, attempts, used)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, record.phone)
                stmt.setString(2, record.codeHash)
                stmt.setString(3, record.codeSalt)
                stmt.setLong(4, record.createdAt)
                stmt.setLong(5, record.expiresAt)
                stmt.setInt(6, record.attempts)
                stmt.setInt(7, if (record.used) 1 else 0)
                stmt.executeUpdate()
            }
        }
    }

    override fun findLatest(phone: String): OtpRecord? {
        synchronized(conn) {
            conn.prepareStatement("SELECT * FROM otp_records WHERE phone = ?").use { stmt ->
                stmt.setString(1, phone)
                val rs = stmt.executeQuery()
                if (!rs.next()) return null
                return OtpRecord(
                    phone = rs.getString("phone"),
                    codeHash = rs.getString("code_hash"),
                    codeSalt = rs.getString("code_salt"),
                    createdAt = rs.getLong("created_at"),
                    expiresAt = rs.getLong("expires_at"),
                    attempts = rs.getInt("attempts"),
                    used = rs.getInt("used") == 1
                )
            }
        }
    }

    override fun incrementAttempts(phone: String) {
        synchronized(conn) {
            conn.prepareStatement("UPDATE otp_records SET attempts = attempts + 1 WHERE phone = ?").use { stmt ->
                stmt.setString(1, phone)
                stmt.executeUpdate()
            }
        }
    }

    override fun markUsed(phone: String) {
        synchronized(conn) {
            conn.prepareStatement("UPDATE otp_records SET used = 1 WHERE phone = ?").use { stmt ->
                stmt.setString(1, phone)
                stmt.executeUpdate()
            }
        }
    }

    override fun delete(phone: String) {
        synchronized(conn) {
            conn.prepareStatement("DELETE FROM otp_records WHERE phone = ?").use { stmt ->
                stmt.setString(1, phone)
                stmt.executeUpdate()
            }
        }
    }

    override fun countByPhone(phone: String, sinceMs: Long): Int {
        synchronized(conn) {
            conn.prepareStatement("SELECT COUNT(*) FROM otp_records WHERE phone = ? AND created_at >= ?").use { stmt ->
                stmt.setString(1, phone)
                stmt.setLong(2, sinceMs)
                val rs = stmt.executeQuery()
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    override fun countByIp(ip: String, sinceMs: Long): Int {
        synchronized(conn) {
            conn.prepareStatement("SELECT COUNT(*) FROM ip_usage WHERE ip = ? AND created_at >= ?").use { stmt ->
                stmt.setString(1, ip)
                stmt.setLong(2, sinceMs)
                val rs = stmt.executeQuery()
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    override fun recordIpUsage(ip: String) {
        synchronized(conn) {
            conn.prepareStatement("INSERT INTO ip_usage (ip, created_at) VALUES (?, ?)").use { stmt ->
                stmt.setString(1, ip)
                stmt.setLong(2, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM ip_usage WHERE created_at < ?").use { stmt ->
                stmt.setLong(1, System.currentTimeMillis() - 7200_000)
                stmt.executeUpdate()
            }
        }
    }
}
