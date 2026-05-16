package com.aitutor.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aitutor.app.data.local.dao.ConversationDao
import com.aitutor.app.data.local.dao.MessageDao
import com.aitutor.app.data.local.entity.ConversationEntity
import com.aitutor.app.data.local.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AiTutorDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
