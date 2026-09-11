package com.cryptopos.pos.domain.repository

import com.cryptopos.pos.domain.model.HistorySource
import com.cryptopos.pos.domain.model.HistoryTransaction
import com.cryptopos.pos.domain.terminal.TerminalSession
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeHistory(): Flow<List<HistoryTransaction>>
    suspend fun refresh()
    suspend fun get(source: HistorySource, id: String): HistoryTransaction?
    suspend fun recordTerminalSession(session: TerminalSession)
}
