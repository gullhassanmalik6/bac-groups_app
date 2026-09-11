package com.cryptopos.pos.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "terminal_history")
data class TerminalHistoryEntity(
    @PrimaryKey val id: String,
    val state: String,
    val amountRaw: String,
    val amountMinor: Long,
    val currency: String,
    val transactionType: String?,
    val protocolId: String?,
    val protocolCode: String?,
    val protocolLabel: String?,
    val environment: String,
    val authorizationCode: String?,
    val processorReference: String?,
    val processorStatus: String?,
    val processorMessage: String?,
    val cardBrand: String?,
    val cardLast4: String?,
    val paymentMethod: String?,
    val usedRemoteProcessor: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
