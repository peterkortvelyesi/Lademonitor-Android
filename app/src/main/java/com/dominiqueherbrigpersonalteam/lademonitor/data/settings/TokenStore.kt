package com.dominiqueherbrigpersonalteam.lademonitor.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Port of the iOS `KeychainStore`: stores the auth token in encrypted storage rather than plain
 * preferences (the token is a secret). Falls back to plain prefs only if the crypto provider
 * cannot be initialised on a given device, so login never hard-fails.
 */
object TokenStore {

    private const val FILE = "lademonitor_secure"
    private const val KEY_TOKEN = "authToken"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val appContext = context.applicationContext
        prefs = try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    fun readToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun deleteToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
