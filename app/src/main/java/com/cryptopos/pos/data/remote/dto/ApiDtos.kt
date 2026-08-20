package com.cryptopos.pos.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Map<String, List<String>>? = null,
    val timestamp: String? = null,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponseDto(
    val tokens: TokenPairDto,
    val user: UserDto,
)

@Serializable
data class TokenPairDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int = 1800,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class UserDto(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val phone: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("role_code") val roleCode: String = "",
)

@Serializable
data class MerchantDto(
    val id: String,
    @SerialName("company_name") val companyName: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val status: String,
    val industry: String? = null,
    @SerialName("tax_number") val taxNumber: String? = null,
)

@Serializable
data class WalletDto(
    val id: String,
    @SerialName("merchant_id") val merchantId: String,
    @SerialName("wallet_address") val walletAddress: String,
    @SerialName("wallet_provider") val walletProvider: String,
    @SerialName("wallet_network") val walletNetwork: String,
    @SerialName("wallet_status") val walletStatus: String,
    @SerialName("is_primary") val isPrimary: Boolean,
)

@Serializable
data class CreatePaymentRequestDto(
    val amount: String,
    val currency: String = "SAR",
    @SerialName("merchant_reference") val merchantReference: String,
    val description: String? = null,
    @SerialName("gateway_provider") val gatewayProvider: String? = "sandbox",
)

@Serializable
data class RefundRequestDto(
    val reason: String? = null,
)

@Serializable
data class SettlementDto(
    val id: String,
    val status: String,
    @SerialName("usdt_amount") val usdtAmount: String,
    @SerialName("exchange_rate") val exchangeRate: String,
    @SerialName("exchange_provider") val exchangeProvider: String,
    @SerialName("wallet_network") val walletNetwork: String,
    @SerialName("wallet_address") val walletAddress: String,
    @SerialName("blockchain_tx_hash") val blockchainTxHash: String? = null,
    @SerialName("confirmation_count") val confirmationCount: Int = 0,
    @SerialName("retry_count") val retryCount: Int = 0,
)

@Serializable
data class ReceiptDto(
    val id: String,
    @SerialName("receipt_number") val receiptNumber: String,
    @SerialName("merchant_name") val merchantName: String,
    val amount: String,
    val currency: String,
    val gateway: String? = null,
    val status: String,
    @SerialName("printable_payload") val printablePayload: Map<String, String?> = emptyMap(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PaymentDto(
    val id: String,
    @SerialName("merchant_id") val merchantId: String,
    val amount: String,
    val currency: String,
    @SerialName("gateway_reference") val gatewayReference: String? = null,
    @SerialName("merchant_reference") val merchantReference: String,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val status: String,
    @SerialName("payment_date") val paymentDate: String? = null,
    val fees: String = "0.00",
    val tax: String = "0.00",
    @SerialName("net_amount") val netAmount: String = "0.00",
    @SerialName("receipt_number") val receiptNumber: String? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val settlement: SettlementDto? = null,
    val receipt: ReceiptDto? = null,
)

@Serializable
data class TransactionListDto(
    val items: List<PaymentDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
)
