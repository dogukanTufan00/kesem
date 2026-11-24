package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.util.PinHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `hashPin is deterministic with same salt and verifies matching PIN`() {
        val salt = PinHasher.generateSalt()
        assertEquals(32, salt.length)

        val hash1 = PinHasher.hashPin("4829", salt)
        val hash2 = PinHasher.hashPin("4829", salt)
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)

        assertTrue(PinHasher.verifyPin("4829", salt, hash1))
        assertFalse(PinHasher.verifyPin("0000", salt, hash1))
        assertFalse(PinHasher.verifyPin("", salt, hash1))
    }

    @Test
    fun `different salts produce different hashes for same PIN`() {
        val saltA = PinHasher.generateSalt()
        val saltB = PinHasher.generateSalt()
        assertNotEquals(saltA, saltB)

        val hashA = PinHasher.hashPin("1234", saltA)
        val hashB = PinHasher.hashPin("1234", saltB)
        assertNotEquals(hashA, hashB)
        assertFalse(PinHasher.verifyPin("1234", saltB, hashA))
    }
}
