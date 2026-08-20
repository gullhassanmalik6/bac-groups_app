package com.cryptopos.pos.domain.payment

import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.PaymentTransaction
import java.math.BigDecimal

data class CardTapEvent(
    val brandHint: String?,
    val maskedPan: String?,
)

data class ChargeRequest(
    val amount: BigDecimal,
    val currency: String,
    val description: String?,
    val cardTap: CardTapEvent?,
)

/**
 * Payment provider seam. Concrete adapters map to backend gateway_provider values.
 */
interface PaymentGateway {
    val provider: GatewayProvider
    suspend fun charge(request: ChargeRequest): PaymentTransaction
}

interface PaymentGatewayResolver {
    fun resolve(provider: GatewayProvider): PaymentGateway
}

interface CardReader {
    suspend fun waitForCardTap(timeoutMs: Long = 45_000): Result<CardTapEvent>
    fun isNfcAvailable(): Boolean
}
