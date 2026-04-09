package eu.kanade.tachiyomi.zeus

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ZeusPreferences(context: Context) {

    private val plain = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val encrypted = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENC_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrNull()

    var installedVersion: String
        get() = plain.getString(KEY_VERSION, null) ?: ""
        set(value) = plain.edit { putString(KEY_VERSION, value) }

    var binaryArch: String
        get() = plain.getString(KEY_ARCH, null) ?: ""
        set(value) = plain.edit { putString(KEY_ARCH, value) }

    var lastCheckTimestamp: Long
        get() = plain.getLong(KEY_LAST_CHECK, 0L)
        set(value) = plain.edit { putLong(KEY_LAST_CHECK, value) }

    var githubRepo: String
        get() = plain.getString(KEY_REPO, DEFAULT_REPO) ?: DEFAULT_REPO
        set(value) = plain.edit { putString(KEY_REPO, value) }

    var githubPat: String
        get() = encrypted?.getString(KEY_PAT, null) ?: plain.getString(KEY_PAT, null) ?: ""
        set(value) {
            if (encrypted != null) {
                encrypted.edit { putString(KEY_PAT, value) }
            } else {
                plain.edit { putString(KEY_PAT, value) }
            }
        }

    fun clearPat() {
        encrypted?.edit { remove(KEY_PAT) }
        plain.edit { remove(KEY_PAT) }
    }

    companion object {
        private const val PREFS_NAME = "zeus_prefs"
        private const val ENC_PREFS_NAME = "zeus_enc_prefs"
        private const val KEY_VERSION = "version"
        private const val KEY_ARCH = "arch"
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_REPO = "github_repo"
        private const val KEY_PAT = "github_pat"
        const val DEFAULT_REPO = "aniyomiorg/zeusdl"
        const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
