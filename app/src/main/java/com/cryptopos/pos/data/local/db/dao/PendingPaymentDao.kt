package com.cryptopos.pos.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptopos.pos.data.local.db.entity.PendingPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPaymentDao {
    @Query("SELECT * FROM pending_payments ORDER BY createdAtEpochMs ASC")
    suspend fun getAll(): List<PendingPaymentEntity>

    @Query("SELECT * FROM pending_payments ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<PendingPaymentEntity>>

    @Query("SELECT COUNT(*) FROM pending_payments")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PendingPaymentEntity)

    @Query("DELETE FROM pending_payments WHERE localId = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pending_payments SET attemptCount = attemptCount + 1, lastError = :error WHERE localId = :id")
    suspend fun markAttempt(id: String, error: String?)

    @Query("DELETE FROM pending_payments")
    suspend fun clear()
}
