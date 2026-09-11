package com.cryptopos.pos.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptopos.pos.data.local.db.entity.TerminalHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalHistoryDao {
    @Query("SELECT * FROM terminal_history ORDER BY COALESCE(updatedAt, createdAt) DESC")
    fun observeAll(): Flow<List<TerminalHistoryEntity>>

    @Query("SELECT * FROM terminal_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TerminalHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TerminalHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TerminalHistoryEntity>)

    @Query("DELETE FROM terminal_history")
    suspend fun clear()
}
