package com.cryptopos.pos.data.repository

import com.cryptopos.pos.data.local.db.dao.TerminalHistoryDao
import com.cryptopos.pos.data.local.db.dao.TransactionDao
import com.cryptopos.pos.data.local.db.entity.TerminalHistoryEntity
import com.cryptopos.pos.data.local.db.entity.TransactionEntity
import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.data.remote.dto.ApiResponse
import com.cryptopos.pos.data.remote.dto.TerminalSessionListDto
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.TerminalTransactionType
import com.cryptopos.pos.domain.repository.PaymentRepository
import com.cryptopos.pos.domain.terminal.TerminalSession
import com.cryptopos.pos.domain.terminal.TerminalTransactionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class HistoryRepositoryImplTest {

    @Test
    fun observeHistory_merges_terminal_and_legacy_sorted() = runBlocking {
        val terminalDao = FakeTerminalHistoryDao(
            listOf(
                terminalEntity("t-old", "2026-09-10T10:00:00Z"),
                terminalEntity("t-new", "2026-09-12T10:00:00Z"),
            ),
        )
        val txDao = FakeTransactionDao(
            listOf(legacyEntity("p1", "2026-09-11T10:00:00Z")),
        )
        val repo = HistoryRepositoryImpl(
            api = ThrowingApi(),
            terminalHistoryDao = terminalDao,
            transactionDao = txDao,
            paymentRepository = NoOpPayments(),
        )
        val rows = repo.observeHistory().first()
        assertEquals(listOf("t-new", "p1", "t-old"), rows.map { it.id })
        assertEquals(HistorySource.TERMINAL, rows.first().source)
        assertEquals(HistorySource.LEGACY, rows[1].source)
    }

    @Test
    fun refresh_failure_keeps_local_cache() = runBlocking {
        val terminalDao = FakeTerminalHistoryDao(listOf(terminalEntity("local-1", "2026-09-11T10:00:00Z")))
        val repo = HistoryRepositoryImpl(
            api = object : ThrowingApi() {
                override suspend fun listTerminalSessions(): ApiResponse<TerminalSessionListDto> {
                    error("network down")
                }
            },
            terminalHistoryDao = terminalDao,
            transactionDao = FakeTransactionDao(),
            paymentRepository = object : NoOpPayments() {
                override suspend fun refreshTransactions(page: Int, pageSize: Int) {
                    error("legacy refresh failed")
                }
            },
        )
        repo.refresh()
        assertEquals(1, terminalDao.observeAll().first().size)
        assertEquals("local-1", repo.get(HistorySource.TERMINAL, "local-1")!!.id)
    }

    @Test
    fun recordTerminalSession_upserts_completed_only() = runBlocking {
        val terminalDao = FakeTerminalHistoryDao()
        val repo = HistoryRepositoryImpl(
            api = ThrowingApi(),
            terminalHistoryDao = terminalDao,
            transactionDao = FakeTransactionDao(),
            paymentRepository = NoOpPayments(),
        )
        repo.recordTerminalSession(
            TerminalSession(
                id = "skip",
                state = TerminalTransactionState.PROTOCOL_SELECTED,
                amountRaw = "1.00",
                currency = "USD",
            ),
        )
        assertTrue(terminalDao.observeAll().first().isEmpty())

        repo.recordTerminalSession(
            TerminalSession(
                id = "done",
                state = TerminalTransactionState.COMPLETED,
                amountRaw = "10.00",
                amountMinor = 1000,
                currency = "USD",
                transactionType = TerminalTransactionType.SALE,
                protocolCode = "101.2",
                authorizationCode = "TEST-1",
                processorReference = "sbx_1",
                cardBrand = "VISA",
                cardLast4 = "1111",
            ),
        )
        assertEquals("done", terminalDao.getById("done")!!.id)
        assertNull(repo.get(HistorySource.LEGACY, "missing"))
    }

    private fun terminalEntity(id: String, updatedAt: String) = TerminalHistoryEntity(
        id = id,
        state = "COMPLETED",
        amountRaw = "10.00",
        amountMinor = 1000,
        currency = "USD",
        transactionType = "SALE",
        protocolId = null,
        protocolCode = "101.2",
        protocolLabel = "101.2 - Cloud Sale",
        environment = "SANDBOX",
        authorizationCode = "TEST",
        processorReference = "sbx_$id",
        processorStatus = "APPROVED",
        processorMessage = null,
        cardBrand = "VISA",
        cardLast4 = "1111",
        paymentMethod = "VISA ****1111",
        usedRemoteProcessor = false,
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    private fun legacyEntity(id: String, createdAt: String) = TransactionEntity(
        id = id,
        merchantId = "m1",
        amount = "5.00",
        currency = "USD",
        status = "success",
        merchantReference = "ref-$id",
        gatewayReference = "gw-$id",
        paymentMethod = "card",
        receiptNumber = null,
        failureReason = null,
        paymentDate = createdAt,
        netAmount = "5.00",
        fees = "0",
        createdAt = createdAt,
        usdtAmount = null,
        settlementStatus = null,
    )
}

private class FakeTerminalHistoryDao(
    initial: List<TerminalHistoryEntity> = emptyList(),
) : TerminalHistoryDao {
    private val state = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<TerminalHistoryEntity>> = state
    override suspend fun getById(id: String): TerminalHistoryEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun upsert(item: TerminalHistoryEntity) {
        state.value = state.value.filterNot { it.id == item.id } + item
    }
    override suspend fun upsertAll(items: List<TerminalHistoryEntity>) {
        val ids = items.map { it.id }.toSet()
        state.value = state.value.filterNot { it.id in ids } + items
    }
    override suspend fun clear() {
        state.value = emptyList()
    }
}

private class FakeTransactionDao(
    initial: List<TransactionEntity> = emptyList(),
) : TransactionDao {
    private val state = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<TransactionEntity>> = state
    override suspend fun getById(id: String): TransactionEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun upsert(item: TransactionEntity) = Unit
    override suspend fun upsertAll(items: List<TransactionEntity>) = Unit
    override suspend fun pendingSync(): List<TransactionEntity> = emptyList()
    override suspend fun successfulSince(isoPrefix: String): List<TransactionEntity> = emptyList()
    override suspend fun pendingCount(): Int = 0
    override suspend fun clear() = Unit
    override suspend fun delete(id: String) = Unit
}

private open class NoOpPayments : PaymentRepository {
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
    override suspend fun clearLocalCache() = Unit
}

/** Minimal Retrofit stub — only override what a test needs. */
private open class ThrowingApi : CryptoPosApi {
    private fun unused(): Nothing = error("unused api call")
    override suspend fun login(body: com.cryptopos.pos.data.remote.dto.LoginRequestDto) = unused()
    override suspend fun refresh(body: com.cryptopos.pos.data.remote.dto.RefreshRequestDto) = unused()
    override suspend fun logout(body: com.cryptopos.pos.data.remote.dto.RefreshRequestDto) = unused()
    override suspend fun me() = unused()
    override suspend fun merchantMe() = unused()
    override suspend fun wallets() = unused()
    override suspend fun createPayment(body: com.cryptopos.pos.data.remote.dto.CreatePaymentRequestDto) = unused()
    override suspend fun listPayments(status: String?, page: Int, pageSize: Int) = unused()
    override suspend fun getPayment(id: String) = unused()
    override suspend fun getReceipt(id: String) = unused()
    override suspend fun refundPayment(
        id: String,
        body: com.cryptopos.pos.data.remote.dto.RefundRequestDto,
    ) = unused()
    override suspend fun createTerminalSession(
        body: com.cryptopos.pos.data.remote.dto.CreateTerminalSessionRequestDto,
    ) = unused()
    override suspend fun listTerminalSessions(): ApiResponse<TerminalSessionListDto> = unused()
    override suspend fun getTerminalSession(id: String) = unused()
    override suspend fun authorizeTerminalSession(
        id: String,
        body: com.cryptopos.pos.data.remote.dto.AuthorizeTerminalSessionRequestDto,
    ) = unused()
    override suspend fun captureTerminalSession(id: String) = unused()
    override suspend fun voidTerminalSession(id: String) = unused()
    override suspend fun cancelTerminalSession(id: String) = unused()
    override suspend fun registerDevice(body: com.cryptopos.pos.data.remote.dto.RegisterDeviceRequestDto) = unused()
    override suspend fun deviceHeartbeat(body: com.cryptopos.pos.data.remote.dto.DeviceHeartbeatRequestDto) = unused()
    override suspend fun listDevices() = unused()
}
