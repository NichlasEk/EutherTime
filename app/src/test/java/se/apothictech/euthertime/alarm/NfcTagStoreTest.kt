package se.apothictech.euthertime.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NfcTagStoreTest {
    @Test
    fun fingerprintIsStableForTheSameSaltAndTag() {
        val salt = byteArrayOf(1, 2, 3, 4)
        val tag = byteArrayOf(9, 8, 7, 6)

        assertEquals(NfcTagStore.saltedHash(salt, tag), NfcTagStore.saltedHash(salt, tag))
    }

    @Test
    fun fingerprintChangesForAnotherTagOrSalt() {
        val tag = byteArrayOf(9, 8, 7, 6)

        assertNotEquals(NfcTagStore.saltedHash(byteArrayOf(1), tag), NfcTagStore.saltedHash(byteArrayOf(2), tag))
        assertNotEquals(NfcTagStore.saltedHash(byteArrayOf(1), tag), NfcTagStore.saltedHash(byteArrayOf(1), byteArrayOf(9, 8, 7, 5)))
    }
}
