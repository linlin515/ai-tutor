package com.aitutor.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aitutor.app.data.local.entity.UserScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserScoreDao {

    @Query("SELECT * FROM user_score WHERE id = 'user_score'")
    fun getScore(): Flow<UserScoreEntity?>

    @Query("SELECT * FROM user_score WHERE id = 'user_score'")
    suspend fun getScoreOnce(): UserScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScore(score: UserScoreEntity)
}
