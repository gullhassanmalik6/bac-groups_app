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
    @SerialName("protocol_code") val protocolCode: String? = null,
    @SerialName("payment_mode") val paymentMode: String? = null,
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

@Serializable
data class CreateTerminalSessionRequestDto(
    @SerialName("amount_minor") val amountMinor: Long,
    val currency: String,
    @SerialName("transaction_type") val transactionType: String = "SALE",
    @SerialName("protocol_code") val protocolCode: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
)

@Serializable
data class AuthorizeTerminalSessionRequestDto(
    @SerialName("payment_method_token") val paymentMethodToken: String = "pm_test_visa_success",
    val scenario: String? = null,
)

@Serializable
data class TerminalSessionEventDto(
    val from: String? = null,
    val to: String? = null,
    val note: String? = null,
)

@Serializable
data class TerminalSessionDto(
    val id: String,
    val state: String,
    @SerialName("amount_minor") val amountMinor: Long,
    val currency: String,
    @SerialName("transaction_type") val transactionType: String,
    @SerialName("protocol_id") val protocolId: String? = null,
    @SerialName("protocol_code") val protocolCode: String? = null,
    @SerialName("protocol_label") val protocolLabel: String? = null,
    @SerialName("sandbox_outcome") val sandboxOutcome: String? = null,
    val environment: String = "SANDBOX",
    @SerialName("authorization_code") val authorizationCode: String? = null,
    @SerialName("processor_reference") val processorReference: String? = null,
    @SerialName("processor_status") val processorStatus: String? = null,
    @SerialName("processor_message") val processorMessage: String? = null,
    @SerialName("signature_required") val signatureRequired: Boolean = false,
    @SerialName("card_brand") val cardBrand: String? = null,
    @SerialName("card_last4") val cardLast4: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    val events: List<TerminalSessionEventDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class TerminalSessionListDto(
    val items: List<TerminalSessionDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class RegisterDeviceRequestDto(
    @SerialName("serial_number") val serialNumber: String,
    val model: String = "Android POS",
    @SerialName("android_version") val androidVersion: String? = null,
    val manufacturer: String = "Generic",
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("connectivity_status") val connectivityStatus: String? = null,
    @SerialName("printer_status") val printerStatus: String? = null,
    @SerialName("card_reader_status") val cardReaderStatus: String? = null,
    @SerialName("overall_status") val overallStatus: String? = null,
    val firmware: String? = null,
)

@Serializable
data class DeviceHeartbeatRequestDto(
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    @SerialName("connectivity_status") val connectivityStatus: String = "ONLINE",
    @SerialName("printer_status") val printerStatus: String? = null,
    @SerialName("card_reader_status") val cardReaderStatus: String? = null,
    @SerialName("overall_status") val overallStatus: String? = null,
)

@Serializable
data class DeviceDto(
    val id: String,
    @SerialName("serial_number") val serialNumber: String,
    val model: String,
    @SerialName("android_version") val androidVersion: String? = null,
    val status: String,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("merchant_id") val merchantId: String,
    @SerialName("connectivity_status") val connectivityStatus: String = "ONLINE",
    val manufacturer: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("printer_status") val printerStatus: String? = null,
    @SerialName("card_reader_status") val cardReaderStatus: String? = null,
    @SerialName("overall_status") val overallStatus: String? = null,
)

@Serializable
data class DeviceListDto(
    val items: List<DeviceDto> = emptyList(),
    val total: Int = 0,
)
