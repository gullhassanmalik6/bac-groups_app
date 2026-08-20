package com.cryptopos.pos.domain.usecase

import com.cryptopos.pos.core.network.ConnectivityObserver
import com.cryptopos.pos.domain.model.DashboardSnapshot
import com.cryptopos.pos.domain.model.MerchantProfile
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.model.Wallet
import com.cryptopos.pos.domain.repository.MerchantRepository
import com.cryptopos.pos.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadDashboardUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val connectivityObserver: ConnectivityObserver,
) {
    suspend operator fun invoke(): DashboardSnapshot =
        paymentRepository.dashboard(isOffline = !connectivityObserver.currentlyOnline())
}

class ObserveTransactionsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) {
    operator fun invoke(): Flow<List<PaymentTransaction>> = paymentRepository.observeTransactions()
}

class RefreshTransactionsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 50) {
        paymentRepository.refreshTransactions(page, pageSize)
    }
}

class GetTransactionUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(id: String): PaymentTransaction = paymentRepository.getPayment(id)
}

class GetMerchantProfileUseCase @Inject constructor(
    private val merchantRepository: MerchantRepository,
) {
    suspend operator fun invoke(): MerchantProfile = merchantRepository.getMerchant()
}

class GetWalletsUseCase @Inject constructor(
    private val merchantRepository: MerchantRepository,
) {
    suspend operator fun invoke(): List<Wallet> = merchantRepository.getWallets()
}

class SyncPendingPaymentsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(): Int = paymentRepository.syncPendingPayments()
}
