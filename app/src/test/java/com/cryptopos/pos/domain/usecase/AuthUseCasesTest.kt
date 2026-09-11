package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.model.UserSession
import com.cryptopos.pos.domain.repository.AuthRepository
import com.cryptopos.pos.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class LoginUseCaseTest {
    private val auth = FakeAuthRepository()
    private val useCase = LoginUseCase(auth)

    @Test
    fun rejects_short_password() {
        val error = runCatching {
            runBlocking { useCase("user@example.com", "short") }
        }.exceptionOrNull()
        assertTrue(error is PosError.Validation)
    }

    @Test
    fun rejects_invalid_email() {
        val error = runCatching {
            runBlocking { useCase("not-an-email", "Password1!") }
        }.exceptionOrNull()
        assertTrue(error is PosError.Validation)
    }

    @Test
    fun trims_email_and_delegates() = runBlocking {
        val session = useCase("  user@example.com  ", "Password1!")
        assertEquals("user@example.com", session.email)
        assertEquals("user@example.com", auth.lastEmail)
    }
}

class LogoutUseCaseTest {
    @Test
    fun clears_auth_and_payment_cache() = runBlocking {
        val auth = FakeAuthRepository()
        val payments = FakePaymentRepository()
        LogoutUseCase(auth, payments)()
        assertTrue(auth.loggedOut)
        assertTrue(payments.cleared)
    }
}

private class FakeAuthRepository : AuthRepository {
    var lastEmail: String? = null
    var loggedOut = false
    override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun login(email: String, password: String): UserSession {
        lastEmail = email
        return UserSession(
            userId = "u1",
            email = email,
            fullName = "Test User",
            roleCode = "merchant_owner",
        )
    }

    override suspend fun logout() {
        loggedOut = true
    }

    override suspend fun currentUserName(): String? = null
    override suspend fun currentUserEmail(): String? = null
}

private class FakePaymentRepository : PaymentRepository {
    var cleared = false
    override fun observeTransactions() = MutableStateFlow(emptyList<com.cryptopos.pos.domain.model.PaymentTransaction>())
    override suspend fun refreshTransactions(page: Int, pageSize: Int) = Unit
    override suspend fun createPayment(
        amount: BigDecimal,
        currency: String,
        description: String?,
        gatewayProvider: com.cryptopos.pos.domain.model.GatewayProvider,
    ) = error("unused")
    override suspend fun enqueueOfflinePayment(
        amount: BigDecimal,
        currency: String,
        description: String?,
        gatewayProvider: com.cryptopos.pos.domain.model.GatewayProvider,
    ) = error("unused")
    override suspend fun syncPendingPayments(): Int = 0
    override suspend fun getPayment(id: String) = error("unused")
    override suspend fun getReceipt(transactionId: String) = error("unused")
    override suspend fun refundPayment(transactionId: String) = error("unused")
    override suspend fun dashboard(isOffline: Boolean) = error("unused")
    override suspend fun clearLocalCache() {
        cleared = true
    }
}
