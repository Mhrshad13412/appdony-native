package com.appdony.app.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialsManager(context: Context) {

    private val appContext = context.applicationContext

    private var memUsername: String? = null
    private var memPassword: String? = null

    private val sharedPreferences: SharedPreferences? by lazy { createPrefs() }

    private fun createEncrypted(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createPrefs(): SharedPreferences? {
        return try {
            createEncrypted()
        } catch (e: Throwable) {
            try {
                appContext.deleteSharedPreferences(FILE_NAME)
                createEncrypted()
            } catch (e2: Throwable) {
                e2.printStackTrace()
                null
            }
        }
    }

    fun saveCredentials(username: String, password: String) {
        memUsername = username
        memPassword = password
        try {
            sharedPreferences?.edit()
                ?.putString("saved_username", username)
                ?.putString("saved_password", password)
                ?.apply()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun getUsername(): String? {
        return try {
            sharedPreferences?.getString("saved_username", null) ?: memUsername
        } catch (e: Throwable) {
            memUsername
        }
    }

    fun getPassword(): String? {
        return try {
            sharedPreferences?.getString("saved_password", null) ?: memPassword
        } catch (e: Throwable) {
            memPassword
        }
    }

    fun clearCredentials() {
        memUsername = null
        memPassword = null
        try {
            sharedPreferences?.edit()?.clear()?.apply()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private companion object {
        const val FILE_NAME = "appdony_secure_credentials"
    }
}
