package com.cryptopos.pos.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
    @PrimaryKey val localId: String,
    val amount: String,
    val currency: String,
    val description: String?,
    val merchantReference: String,
    val gatewayProvider: String,
    val createdAtEpochMs: Long,
    val createdAtIso: String,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)
