package com.cryptopos.pos.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptopos.pos.data.local.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE pendingSync = 1")
    suspend fun pendingSync(): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE status IN ('completed', 'captured', 'settlement_complete', 'success', 'paid')
        AND createdAt IS NOT NULL AND createdAt >= :isoPrefix
        """,
    )
    suspend fun successfulSince(isoPrefix: String): List<TransactionEntity>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE status IN ('pending', 'processing', 'authorized', 'pending_sync')
        """,
    )
    suspend fun pendingCount(): Int

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)
}
