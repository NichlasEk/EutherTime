package se.apothictech.euthertime.alarm

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

object NfcTagStore {
    private const val PREFS_NAME = "euthertime_nfc_challenge"
    private const val KEY_SALT = "tag_salt"
    private const val KEY_HASH = "tag_hash"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnrolled(context: Context): Boolean = prefs(context).contains(KEY_HASH)

    fun fingerprint(context: Context): String? =
        prefs(context).getString(KEY_HASH, null)?.takeLast(8)?.uppercase()

    fun enroll(context: Context, tagId: ByteArray): String {
        require(tagId.isNotEmpty()) { "This NFC tag does not expose a stable identifier" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = saltedHash(salt, tagId)
        prefs(context).edit {
            putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(KEY_HASH, hash)
        }
        return hash.takeLast(8).uppercase()
    }

    fun matches(context: Context, tagId: ByteArray): Boolean {
        if (tagId.isEmpty()) return false
        val preferences = prefs(context)
        val encodedSalt = preferences.getString(KEY_SALT, null) ?: return false
        val expected = preferences.getString(KEY_HASH, null) ?: return false
        val salt = runCatching { Base64.decode(encodedSalt, Base64.NO_WRAP) }.getOrNull() ?: return false
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.US_ASCII),
            saltedHash(salt, tagId).toByteArray(Charsets.US_ASCII),
        )
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }

    internal fun saltedHash(salt: ByteArray, tagId: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(salt + tagId)
            .joinToString("") { byte -> "%02x".format(byte) }
}
