package com.cryptopos.pos.domain.repository

import com.cryptopos.pos.domain.model.AppLanguage
import com.cryptopos.pos.domain.model.AppSettings
import com.cryptopos.pos.domain.model.DashboardSnapshot
import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.MerchantProfile
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.model.Receipt
import com.cryptopos.pos.domain.model.ThemeMode
import com.cryptopos.pos.domain.model.UserSession
import com.cryptopos.pos.domain.model.Wallet
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun login(email: String, password: String): UserSession
    suspend fun logout()
    suspend fun currentUserName(): String?
    suspend fun currentUserEmail(): String?
}

interface MerchantRepository {
    suspend fun getMerchant(): MerchantProfile
    suspend fun getWallets(): List<Wallet>
}

interface PaymentRepository {
    fun observeTransactions(): Flow<List<PaymentTransaction>>
    suspend fun refreshTransactions(page: Int = 1, pageSize: Int = 50)
    suspend fun createPayment(
        amount: BigDecimal,
        currency: String,
        description: String?,
        gatewayProvider: GatewayProvider,
    ): PaymentTransaction

    suspend fun enqueueOfflinePayment(
        amount: BigDecimal,
        currency: String,
        description: String?,
        gatewayProvider: GatewayProvider,
    ): PaymentTransaction

    suspend fun syncPendingPayments(): Int
    suspend fun getPayment(id: String): PaymentTransaction
    suspend fun getReceipt(transactionId: String): Receipt
    suspend fun refundPayment(transactionId: String): PaymentTransaction
    suspend fun dashboard(isOffline: Boolean): DashboardSnapshot
    suspend fun clearLocalCache()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setGatewayProvider(provider: GatewayProvider)
    suspend fun setCurrency(currency: String)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun current(): AppSettings
}
