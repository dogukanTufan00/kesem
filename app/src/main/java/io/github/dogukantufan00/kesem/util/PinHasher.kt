package io.github.dogukantufan00.kesem.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {

    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun generateSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hashPin(pin: String, salt: String): String {
        val saltBytes = decodeSaltBytes(salt)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (pin.isBlank() || salt.isBlank() || expectedHash.isBlank()) return false
        val candidateHash = hashPin(pin, salt)
        return MessageDigest.isEqual(
            candidateHash.lowercase().toByteArray(Charsets.US_ASCII),
            expectedHash.lowercase().toByteArray(Charsets.US_ASCII)
        )
    }

    private fun decodeSaltBytes(salt: String): ByteArray {
        val clean = salt.trim()
        if (clean.length % 2 == 0 && clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return ByteArray(clean.length / 2) { idx ->
                clean.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        }
        return clean.toByteArray(Charsets.UTF_8)
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}
