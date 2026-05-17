package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aitutor.app.data.local.entity.PendingMessageEntity

/**
 * P1-2: DAO for offline pending message queue.
 */
@Dao
interface PendingMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: PendingMessageEntity): Long

    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_messages SET retryCount = :count WHERE id = :id")
    suspend fun updateRetryCount(id: Long, count: Int)

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun count(): Int
}
