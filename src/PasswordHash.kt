import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Filename: PasswordHash.kt
 * Author: Andrias Zelele
 * Date: 2026-02-22
 *
 * Description:
 * Provides password hashing and verification using PBKDF2 (HMAC-SHA256).
 * This utility generates a cryptographically secure random salt for each
 * password, derives a fixed-length key, and stores the result in a compact
 * string format suitable for file persistence.
 *
 * Storage Format:
 * iterations:saltB64:hashB64
 *
 * Security Notes:
 * - Password input is accepted as CharArray to reduce the lifetime of sensitive data in memory.
 * - This class clears the provided password CharArray as a best-effort after hashing/verifying.
 * - Comparisons are performed using a constant-time equality check to reduce timing attacks.
 */
object PasswordHash {

    /** Default PBKDF2 iteration count (work factor). */
    private const val ITERATIONS = 120_000

    /** Derived key length in bits. */
    private const val KEY_LENGTH_BITS = 256

    /** Secure random generator used for producing salts. */
    private val random = SecureRandom()

    /**
     * Hashes a password using PBKDF2 + per-password random salt.
     *
     * Behavior:
     * - Generates a 16-byte random salt.
     * - Derives a key using PBKDF2WithHmacSHA256.
     * - Base64-encodes the salt and derived key for storage.
     * - Clears the provided password CharArray as a best-effort security measure.
     *
     * @param password the raw password characters
     * @return a storage string in the format: iterations:saltB64:hashB64
     */
    fun hash(password: CharArray): String {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS)

        val b64 = Base64.getEncoder()
        val saltB64 = b64.encodeToString(salt)
        val hashB64 = b64.encodeToString(hash)

        // Best effort: clear sensitive data.
        password.fill('\u0000')

        return "$ITERATIONS:$saltB64:$hashB64"
    }

    /**
     * Verifies a password against a stored hash string produced by hash().
     *
     * Behavior:
     * - Parses the stored format (iterations:saltB64:hashB64).
     * - Recomputes the PBKDF2 key using the extracted salt and iteration count.
     * - Clears the provided password CharArray as a best-effort security measure.
     * - Compares expected vs. computed keys using a constant-time comparison.
     *
     * @param password the raw password characters
     * @param stored the stored password string (iterations:saltB64:hashB64)
     * @return true if the password matches; false otherwise
     * @throws IllegalArgumentException if the stored format is invalid
     */
    fun verify(password: CharArray, stored: String): Boolean {
        val parts = stored.split(":")
        require(parts.size == 3) { "Invalid stored password format" }

        val iterations = parts[0].toInt()
        val salt = Base64.getDecoder().decode(parts[1])
        val expectedHash = Base64.getDecoder().decode(parts[2])

        // Derive a key with the same bit-length as the stored hash.
        val testHash = pbkdf2(password, salt, iterations, expectedHash.size * 8)

        // Best effort: clear sensitive data.
        password.fill('\u0000')

        return constantTimeEquals(expectedHash, testHash)
    }

    /**
     * Derives a key from the given password using PBKDF2WithHmacSHA256.
     *
     * @param password raw password characters
     * @param salt random salt bytes
     * @param iterations PBKDF2 iteration count
     * @param keyLenBits desired key length in bits
     * @return derived key bytes
     */
    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLenBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLenBits)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    /**
     * Compares two byte arrays in constant time to reduce timing side-channels.
     *
     * @param a first array
     * @param b second array
     * @return true if identical; false otherwise
     */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }
}