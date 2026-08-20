package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.model.UserSession
import com.cryptopos.pos.domain.repository.AuthRepository
import com.cryptopos.pos.domain.repository.PaymentRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): UserSession {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@") || password.length < 8) {
            throw PosError.Validation("Enter a valid email and password (min 8 characters).")
        }
        return authRepository.login(trimmed, password)
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke() {
        authRepository.logout()
        paymentRepository.clearLocalCache()
    }
}

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.isLoggedIn
}
