package com.cryptopos.pos.data.local.datastore

/**
 * Optional on-device login memory (encrypted). Cleared only when the user unchecks
 * “Save for this device” or explicitly wipes remembered credentials.
 */
data class RememberedLogin(
    val email: String,
    val password: String,
)

interface RememberedLoginStore {
    suspend fun getRememberedLogin(): RememberedLogin?
    suspend fun saveRememberedLogin(email: String, password: String)
    suspend fun clearRememberedLogin()
}
