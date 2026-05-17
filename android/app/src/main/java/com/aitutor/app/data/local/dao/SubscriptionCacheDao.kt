package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aitutor.app.data.local.entity.SubscriptionCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionCacheDao {

    @Query("SELECT * FROM subscription_cache WHERE id = 1")
    fun observe(): Flow<SubscriptionCacheEntity?>

    @Query("SELECT * FROM subscription_cache WHERE id = 1")
    suspend fun get(): SubscriptionCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: SubscriptionCacheEntity)

    @Query("DELETE FROM subscription_cache")
    suspend fun clear()
}
