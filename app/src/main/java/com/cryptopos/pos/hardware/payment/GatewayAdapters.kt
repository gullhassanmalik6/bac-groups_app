package com.cryptopos.pos.hardware.payment

import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.payment.ChargeRequest
import com.cryptopos.pos.domain.payment.PaymentGateway
import com.cryptopos.pos.domain.payment.PaymentGatewayResolver
import com.cryptopos.pos.domain.repository.PaymentRepository
import javax.inject.Inject
import javax.inject.Singleton

abstract class BackendPaymentGateway(
    private val paymentRepository: PaymentRepository,
    override val provider: GatewayProvider,
) : PaymentGateway {
    override suspend fun charge(request: ChargeRequest): PaymentTransaction {
        return paymentRepository.createPayment(
            amount = request.amount,
            currency = request.currency,
            description = buildDescription(request),
            gatewayProvider = provider,
        )
    }

    private fun buildDescription(request: ChargeRequest): String? {
        val base = request.description
        val tap = request.cardTap ?: return base
        val suffix = listOfNotNull(tap.brandHint, tap.maskedPan).joinToString(" ")
        return listOfNotNull(base, suffix.takeIf { it.isNotBlank() }).joinToString(" | ")
    }
}

class SandboxGatewayAdapter @Inject constructor(
    paymentRepository: PaymentRepository,
) : BackendPaymentGateway(paymentRepository, GatewayProvider.SANDBOX)

class StripeGatewayAdapter @Inject constructor(
    paymentRepository: PaymentRepository,
) : BackendPaymentGateway(paymentRepository, GatewayProvider.STRIPE)

class HyperPayGatewayAdapter @Inject constructor(
    paymentRepository: PaymentRepository,
) : BackendPaymentGateway(paymentRepository, GatewayProvider.HYPERPAY)

class CheckoutGatewayAdapter @Inject constructor(
    paymentRepository: PaymentRepository,
) : BackendPaymentGateway(paymentRepository, GatewayProvider.CHECKOUT)

class MoyasarGatewayAdapter @Inject constructor(
    paymentRepository: PaymentRepository,
) : BackendPaymentGateway(paymentRepository, GatewayProvider.MOYASAR)

class NowPaymentsGatewayAdapter @Inject constructor(
    paymentRepository: PaymentRepository,
) : BackendPaymentGateway(paymentRepository, GatewayProvider.NOWPAYMENTS)

@Singleton
class DefaultPaymentGatewayResolver @Inject constructor(
    private val sandbox: SandboxGatewayAdapter,
    private val nowPayments: NowPaymentsGatewayAdapter,
    private val stripe: StripeGatewayAdapter,
    private val hyperPay: HyperPayGatewayAdapter,
    private val checkout: CheckoutGatewayAdapter,
    private val moyasar: MoyasarGatewayAdapter,
) : PaymentGatewayResolver {
    override fun resolve(provider: GatewayProvider): PaymentGateway = when (provider) {
        GatewayProvider.SANDBOX -> sandbox
        GatewayProvider.NOWPAYMENTS -> nowPayments
        GatewayProvider.STRIPE -> stripe
        GatewayProvider.HYPERPAY -> hyperPay
        GatewayProvider.CHECKOUT -> checkout
        GatewayProvider.MOYASAR -> moyasar
    }
}
