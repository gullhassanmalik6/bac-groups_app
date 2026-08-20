package com.cryptopos.pos.domain.model

import java.math.BigDecimal

data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val roleCode: String,
)

data class MerchantProfile(
    val id: String,
    val companyName: String,
    val email: String,
    val phone: String,
    val status: String,
    val city: String,
    val country: String,
    val industry: String?,
    val vatNumber: String?,
)

data class Wallet(
    val id: String,
    val address: String,
    val network: String,
    val status: String,
    val provider: String,
    val isPrimary: Boolean,
)

data class PaymentTransaction(
    val id: String,
    val amount: String,
    val currency: String,
    val status: String,
    val merchantReference: String,
    val gatewayReference: String?,
    val paymentMethod: String?,
    val receiptNumber: String?,
    val failureReason: String?,
    val paymentDate: String?,
    val netAmount: String,
    val fees: String,
    val createdAt: String?,
    val usdtAmount: String?,
    val settlementStatus: String?,
    val isPendingSync: Boolean = false,
)

data class Receipt(
    val id: String,
    val receiptNumber: String,
    val merchantName: String,
    val amount: String,
    val currency: String,
    val status: String,
    val gateway: String?,
    val createdAt: String?,
    val transactionId: String?,
    val lines: Map<String, String?>,
    val qrPayload: String,
)

data class SalesTotals(
    val todayAmount: BigDecimal,
    val todayCount: Int,
    val weekAmount: BigDecimal,
    val weekCount: Int,
    val monthAmount: BigDecimal,
    val monthCount: Int,
    val pendingCount: Int,
)

data class DashboardSnapshot(
    val merchantName: String,
    val merchantStatus: String,
    val sales: SalesTotals,
    val recent: List<PaymentTransaction>,
    val wallet: Wallet?,
    val isOffline: Boolean,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class GatewayProvider(val apiValue: String, val displayName: String) {
    SANDBOX("sandbox", "Sandbox"),
    NOWPAYMENTS("nowpayments", "NOWPayments"),
    MOYASAR("moyasar", "Moyasar"),
    STRIPE("stripe", "Stripe"),
    HYPERPAY("hyperpay", "HyperPay"),
    CHECKOUT("checkout", "Checkout"),
    ;

    companion object {
        fun fromApi(value: String?): GatewayProvider =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: SANDBOX
    }
}

enum class AppLanguage(val tag: String, val displayName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية"),
    ;

    companion object {
        fun fromTag(value: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(value, ignoreCase = true) } ?: ENGLISH
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val biometricEnabled: Boolean = false,
    val gatewayProvider: GatewayProvider = GatewayProvider.SANDBOX,
    val currency: String = "SAR",
    val language: AppLanguage = AppLanguage.ENGLISH,
)

data class PendingPayment(
    val localId: String,
    val amount: String,
    val currency: String,
    val description: String?,
    val merchantReference: String,
    val gatewayProvider: String,
    val createdAtEpochMs: Long,
)
