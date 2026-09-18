package com.cryptopos.pos.domain.fees

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Merchant-fee math only. Does not imply auth, capture, settlement, or payout.
 */
data class FeeResult(
    val grossAmount: BigDecimal,
    val feePercent: BigDecimal,
    val feeAmount: BigDecimal,
    val netAmount: BigDecimal,
    val currency: String,
    val policyVersion: String,
)

object FeeCalculator {
    private val CENT = BigDecimal("0.01")

    fun calculate(
        grossAmount: BigDecimal,
        currency: String,
        feePercent: BigDecimal,
        fixedFee: BigDecimal = BigDecimal.ZERO,
        policyVersion: String = "sandbox-v1",
    ): FeeResult {
        require(grossAmount >= BigDecimal.ZERO) { "grossAmount must be >= 0" }
        require(feePercent >= BigDecimal.ZERO) { "feePercent must be >= 0" }
        val gross = grossAmount.setScale(2, RoundingMode.HALF_UP)
        val percent = feePercent.setScale(4, RoundingMode.HALF_UP)
        val variable = gross.multiply(percent)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        val fee = variable.add(fixedFee.setScale(2, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP)
        val net = gross.subtract(fee).setScale(2, RoundingMode.HALF_UP)
        return FeeResult(
            grossAmount = gross,
            feePercent = percent,
            feeAmount = fee,
            netAmount = net,
            currency = currency.uppercase(),
            policyVersion = policyVersion,
        )
    }

    /** Screenshot example policy — display only, not a live acquirer fee. */
    val SANDBOX_RECEIPT_PERCENT: BigDecimal = BigDecimal("0.5")
    const val SANDBOX_RECEIPT_POLICY: String = "sandbox-receipt-v1"
}
