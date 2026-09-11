package com.cryptopos.pos.data.repository

import com.cryptopos.pos.data.local.db.dao.TerminalHistoryDao
import com.cryptopos.pos.data.local.db.dao.TransactionDao
import com.cryptopos.pos.data.mapper.toHistoryEntity
import com.cryptopos.pos.data.mapper.toHistoryTransaction
import com.cryptopos.pos.data.remote.api.CryptoPosApi
import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.repository.HistoryRepository
import com.cryptopos.pos.domain.repository.PaymentRepository
import com.cryptopos.pos.domain.terminal.TerminalSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val api: CryptoPosApi,
    private val terminalHistoryDao: TerminalHistoryDao,
    private val transactionDao: TransactionDao,
    private val paymentRepository: PaymentRepository,
) : HistoryRepository {

    override fun observeHistory(): Flow<List<HistoryTransaction>> =
        combine(
            terminalHistoryDao.observeAll(),
            transactionDao.observeAll(),
        ) { terminals, payments ->
            val merged = terminals.map { it.toHistoryTransaction() } +
                payments.map { it.toHistoryTransaction() }
            merged.sortedByDescending { it.date ?: "" }
        }

    override suspend fun refresh() {
        runCatching { paymentRepository.refreshTransactions() }
        runCatching {
            val response = api.listTerminalSessions()
            val items = response.data?.items.orEmpty().map { it.toHistoryEntity(usedRemote = true) }
            if (items.isNotEmpty()) {
                terminalHistoryDao.upsertAll(items)
            }
        }
    }

    override suspend fun get(source: HistorySource, id: String): HistoryTransaction? =
        when (source) {
            HistorySource.TERMINAL -> terminalHistoryDao.getById(id)?.toHistoryTransaction()
            HistorySource.LEGACY -> {
                transactionDao.getById(id)?.toHistoryTransaction()
                    ?: runCatching {
                        paymentRepository.getPayment(id).toHistoryTransaction()
                    }.getOrNull()
            }
        }

    override suspend fun recordTerminalSession(session: TerminalSession) {
        val entity = session.toHistoryEntity() ?: return
        terminalHistoryDao.upsert(entity)
    }
}
