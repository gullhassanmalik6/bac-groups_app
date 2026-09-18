package com.cryptopos.pos.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted token + identity storage backed by AndroidX Security Crypto.
 * Remembered login credentials survive logout; session tokens do not.
 */
@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) : RememberedLoginStore {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val snapshot = MutableStateFlow(readSnapshot())

    val accessToken: Flow<String?> = snapshot.map { it.accessToken }
    val refreshToken: Flow<String?> = snapshot.map { it.refreshToken }
    val isLoggedIn: Flow<Boolean> = accessToken.map { !it.isNullOrBlank() }
    val userEmail: Flow<String?> = snapshot.map { it.email }
    val userName: Flow<String?> = snapshot.map { it.name }
    val merchantName: Flow<String?> = snapshot.map { it.merchantName }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String,
        name: String,
    ) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .apply()
        snapshot.value = readSnapshot()
    }

    suspend fun saveMerchantName(name: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_MERCHANT, name).apply()
        snapshot.value = readSnapshot()
    }

    suspend fun saveDeviceId(deviceId: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        snapshot.value = readSnapshot()
    }

    suspend fun getDeviceId(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_DEVICE_ID, null)
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
        snapshot.value = readSnapshot()
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ACCESS, null)
    }

    suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_REFRESH, null)
    }

    /**
     * Clears auth session only. Keeps device id and optional remembered login.
     */
    suspend fun clearSession() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_MERCHANT)
            .apply()
        snapshot.value = readSnapshot()
    }

    /** Full wipe including remembered credentials (rare / factory-style reset). */
    suspend fun clear() = withContext(Dispatchers.IO) {
        val deviceId = prefs.getString(KEY_DEVICE_ID, null)
        prefs.edit().clear().apply()
        if (!deviceId.isNullOrBlank()) {
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        snapshot.value = readSnapshot()
    }

    override suspend fun getRememberedLogin(): RememberedLogin? = withContext(Dispatchers.IO) {
        if (!prefs.getBoolean(KEY_REMEMBER, false)) return@withContext null
        val email = prefs.getString(KEY_REMEMBER_EMAIL, null)?.trim().orEmpty()
        val password = prefs.getString(KEY_REMEMBER_PASSWORD, null).orEmpty()
        if (email.isBlank() || password.isBlank()) null
        else RememberedLogin(email = email, password = password)
    }

    override suspend fun saveRememberedLogin(email: String, password: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, true)
            .putString(KEY_REMEMBER_EMAIL, email.trim())
            .putString(KEY_REMEMBER_PASSWORD, password)
            .apply()
    }

    override suspend fun clearRememberedLogin() = withContext(Dispatchers.IO) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, false)
            .remove(KEY_REMEMBER_EMAIL)
            .remove(KEY_REMEMBER_PASSWORD)
            .apply()
    }

    private fun readSnapshot(): SessionSnapshot = SessionSnapshot(
        accessToken = prefs.getString(KEY_ACCESS, null),
        refreshToken = prefs.getString(KEY_REFRESH, null),
        email = prefs.getString(KEY_EMAIL, null),
        name = prefs.getString(KEY_NAME, null),
        merchantName = prefs.getString(KEY_MERCHANT, null),
        deviceId = prefs.getString(KEY_DEVICE_ID, null),
    )

    private data class SessionSnapshot(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val email: String? = null,
        val name: String? = null,
        val merchantName: String? = null,
        val deviceId: String? = null,
    )

    private companion object {
        const val FILE_NAME = "cryptopos_secure_session"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EMAIL = "user_email"
        const val KEY_NAME = "user_name"
        const val KEY_MERCHANT = "merchant_name"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_REMEMBER = "remember_login"
        const val KEY_REMEMBER_EMAIL = "remember_email"
        const val KEY_REMEMBER_PASSWORD = "remember_password"
    }
}
