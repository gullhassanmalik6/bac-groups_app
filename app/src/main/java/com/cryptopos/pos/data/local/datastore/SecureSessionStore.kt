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
 */
@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
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

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
        snapshot.value = SessionSnapshot()
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
    }
}
