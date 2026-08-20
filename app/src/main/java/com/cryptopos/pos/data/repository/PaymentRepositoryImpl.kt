package com.cryptopos.pos.data.repository

import com.cryptopos.pos.data.local.datastore.SecureSessionStore
import com.cryptopos.pos.data.local.db.dao.PendingPaymentDao
import com.cryptopos.pos.data.local.db.dao.TransactionDao
import com.cryptopos.pos.data.local.db.entity.PendingPaymentEntity
import com.cryptopos.pos.data.mapper.toDomain
import com.cryptopos.pos.data.mapper.toEntity
import com.cryptopos.pos.data.mapper.toLocalTransaction
import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.data.remote.dto.CreatePaymentRequestDto
import com.cryptopos.pos.data.remote.dto.RefundRequestDto
import com.cryptopos.pos.domain.error.PosError
import com.cryptopos.pos.domain.model.DashboardSnapshot
import com.cryptopos.pos.domain.model.GatewayProvider
import com.cryptopos.pos.domain.model.PaymentTransaction
import com.cryptopos.pos.domain.model.Receipt
import com.cryptopos.pos.domain.model.SalesTotals
import com.cryptopos.pos.domain.model.Wallet
import com.cryptopos.pos.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val api: CryptoPosApi,
    private val dao: TransactionDao,
    private val pendingDao: PendingPaymentDao,
    private val session: SecureSessionStore,
) : PaymentRepository {

    override fun observeTransactions(): Flow<List<PaymentTransaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshTransactions(page: Int, pageSize: Int) {
        val response = api.listPayments(page = page, pageSize = pageSize)
        val items = response.data?.items.orEmpty().map { it.toEntity() }
        if (items.isNotEmpty()) dao.upsertAll(items)
    }

    override suspend fun createPayment(
        amount: BigDecimal,
        currency: String,
        description: String?,
        gatewayProvider: GatewayProvider,
    ): PaymentTransaction {
        val reference = "POS-${UUID.randomUUID().toString().take(12).uppercase()}"
        val response = try {
            api.createPayment(
                CreatePaymentRequestDto(
                    amount = amount.setScale(2).toPlainString(),
                    currency = currency,
                    merchantReference = reference,
                    description = description,
                    gatewayProvider = gatewayProvider.apiValue,
                ),
            )
        } catch (error: Exception) {
            throw PosError.Gateway(error.message ?: "Payment failed", error)
        }
        val data = response.data
            ?: throw PosError.Gateway(response.message.ifBlank { "Payment failed" })
        dao.upsert(data.toEntity())
        return data.toDomain()
    }

    override suspend fun enqueueOfflinePayment(
        amount: BigDecimal,
        currency: String,
        description: String?,
        gatewayProvider: GatewayProvider,
    ): PaymentTransaction {
        val localId = "local-${UUID.randomUUID()}"
        val reference = "POS-OFF-${UUID.randomUUID().toString().take(10).uppercase()}"
        val now = Instant.now()
        val entity = PendingPaymentEntity(
            localId = localId,
            amount = amount.setScale(2).toPlainString(),
            currency = currency,
            description = description,
            merchantReference = reference,
            gatewayProvider = gatewayProvider.apiValue,
            createdAtEpochMs = now.toEpochMilli(),
            createdAtIso = DateTimeFormatter.ISO_INSTANT.format(now),
        )
        pendingDao.upsert(entity)
        dao.upsert(entity.toLocalTransaction())
        return entity.toLocalTransaction().toDomain()
    }

    override suspend fun syncPendingPayments(): Int {
        val pending = pendingDao.getAll()
        var synced = 0
        for (item in pending) {
            try {
                val response = api.createPayment(
                    CreatePaymentRequestDto(
                        amount = item.amount,
                        currency = item.currency,
                        merchantReference = item.merchantReference,
                        description = item.description,
                        gatewayProvider = item.gatewayProvider,
                    ),
                )
                val data = response.data
                    ?: throw PosError.Gateway(response.message.ifBlank { "Sync failed" })
                dao.delete(item.localId)
                dao.upsert(data.toEntity())
                pendingDao.delete(item.localId)
                synced++
            } catch (error: Exception) {
                pendingDao.markAttempt(item.localId, error.message)
            }
        }
        return synced
    }

    override suspend fun getPayment(id: String): PaymentTransaction {
        val cached = dao.getById(id)?.toDomain()
        return try {
            val remote = api.getPayment(id).data?.toEntity()
            if (remote != null) {
                dao.upsert(remote)
                remote.toDomain()
            } else {
                cached ?: throw PosError.Server("Transaction not found")
            }
        } catch (_: Exception) {
            cached ?: throw PosError.Server("Transaction not found")
        }
    }

    override suspend fun getReceipt(transactionId: String): Receipt {
        val data = api.getReceipt(transactionId).data
            ?: throw PosError.Server("Receipt not found")
        return data.toDomain(transactionId = transactionId)
    }

    override suspend fun refundPayment(transactionId: String): PaymentTransaction {
        val data = api.refundPayment(transactionId, RefundRequestDto()).data
            ?: throw PosError.Gateway("Refund failed")
        dao.upsert(data.toEntity())
        return data.toDomain()
    }

    override suspend fun dashboard(isOffline: Boolean): DashboardSnapshot {
        if (!isOffline) {
            runCatching { refreshTransactions() }
            runCatching { syncPendingPayments() }
        }
        val recent = dao.observeAll().first().take(12).map { it.toDomain() }
        val sales = computeSalesTotals()
        val wallet = if (!isOffline) {
            runCatching { api.wallets().data.orEmpty().firstOrNull { it.isPrimary } }
                .getOrNull()
                ?.let {
                    Wallet(
                        id = it.id,
                        address = it.walletAddress,
                        network = it.walletNetwork,
                        status = it.walletStatus,
                        provider = it.walletProvider,
                        isPrimary = it.isPrimary,
                    )
                }
        } else {
            null
        }
        val merchant = if (!isOffline) {
            runCatching { api.merchantMe().data }.getOrNull()
        } else {
            null
        }
        val merchantName = session.merchantName.first()
            ?: merchant?.companyName
            ?: "Merchant"
        if (merchant != null) {
            session.saveMerchantName(merchant.companyName)
        }
        return DashboardSnapshot(
            merchantName = merchantName,
            merchantStatus = merchant?.status ?: if (isOffline) "offline" else "unknown",
            sales = sales,
            recent = recent,
            wallet = wallet,
            isOffline = isOffline,
        )
    }

    override suspend fun clearLocalCache() {
        dao.clear()
        pendingDao.clear()
    }

    private suspend fun computeSalesTotals(): SalesTotals {
        val today = LocalDate.now(ZoneOffset.UTC)
        val weekStart = today.minusDays(6)
        val monthStart = today.withDayOfMonth(1)
        val todayRows = dao.successfulSince(today.toString())
        val weekRows = dao.successfulSince(weekStart.toString())
        val monthRows = dao.successfulSince(monthStart.toString())
        fun sum(rows: List<com.cryptopos.pos.data.local.db.entity.TransactionEntity>): BigDecimal =
            rows.fold(BigDecimal.ZERO) { acc, row ->
                acc + (row.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
        return SalesTotals(
            todayAmount = sum(todayRows),
            todayCount = todayRows.size,
            weekAmount = sum(weekRows),
            weekCount = weekRows.size,
            monthAmount = sum(monthRows),
            monthCount = monthRows.size,
            pendingCount = dao.pendingCount() + pendingDao.getAll().size,
        )
    }
}
