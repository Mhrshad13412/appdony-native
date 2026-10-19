package com.appdony.app.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "appdony_secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(username: String, password: String) {
        sharedPreferences.edit()
            .putString("saved_username", username)
            .putString("saved_password", password)
            .apply()
    }

    fun getUsername(): String? = sharedPreferences.getString("saved_username", null)
    fun getPassword(): String? = sharedPreferences.getString("saved_password", null)

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}
