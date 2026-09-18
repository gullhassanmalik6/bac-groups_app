package com.cryptopos.pos.data.repository

import com.cryptopos.pos.data.local.datastore.SecureSessionStore
import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.data.remote.dto.LoginRequestDto
import com.cryptopos.pos.data.remote.dto.RefreshRequestDto
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.model.UserSession
import com.cryptopos.pos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: CryptoPosApi,
    private val session: SecureSessionStore,
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = session.isLoggedIn

    override suspend fun login(email: String, password: String): UserSession {
        val response = try {
            api.login(LoginRequestDto(email.trim(), password))
        } catch (error: Exception) {
            throw PosError.Server(error.message ?: "Login failed", error)
        }
        val data = response.data ?: throw PosError.Server(response.message.ifBlank { "Login failed" })
        val fullName = "${data.user.firstName} ${data.user.lastName}".trim()
        session.saveSession(
            accessToken = data.tokens.accessToken,
            refreshToken = data.tokens.refreshToken,
            email = data.user.email,
            name = fullName,
        )
        runCatching {
            val merchant = api.merchantMe().data
            if (merchant != null) session.saveMerchantName(merchant.companyName)
        }
        return UserSession(
            userId = data.user.id,
            email = data.user.email,
            fullName = fullName,
            roleCode = data.user.roleCode,
        )
    }

    override suspend fun logout() {
        val refresh = session.getRefreshToken()
        if (!refresh.isNullOrBlank()) {
            runCatching { api.logout(RefreshRequestDto(refresh)) }
        }
        // Keep optional "save for this device" credentials + device id.
        session.clearSession()
    }

    override suspend fun currentUserName(): String? = session.userName.first()

    override suspend fun currentUserEmail(): String? = session.userEmail.first()
}
