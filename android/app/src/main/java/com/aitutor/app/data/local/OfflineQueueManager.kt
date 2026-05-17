package com.aitutor.app.data.local

import com.aitutor.app.data.local.dao.PendingMessageDao
import com.aitutor.app.data.local.entity.PendingMessageEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P1-2: Offline message queue manager.
 *
 * Provides atomic database operations for the pending message queue.
 * Enqueue/Dequeue/Flush logic is driven by ChatViewModel which observes
 * network state and calls the appropriate methods.
 */
@Singleton
class OfflineQueueManager @Inject constructor(
    private val pendingMessageDao: PendingMessageDao
) {
    companion object {
        const val MAX_RETRY_COUNT = 3
    }

    /** Callback invoked by ChatViewModel when flushing each pending message. */
    fun interface SendCallback {
        suspend fun onSend(conversationId: Long, content: String): Boolean
    }

    private var sendCallback: SendCallback? = null

    fun registerSendCallback(callback: SendCallback) {
        sendCallback = callback
    }

    /** Enqueue a message for later sending. Returns the new total count. */
    suspend fun enqueue(conversationId: Long, content: String): Int {
        val entity = PendingMessageEntity(
            conversationId = conversationId,
            content = content
        )
        pendingMessageDao.insert(entity)
        return pendingMessageDao.count()
    }

    /** Get current pending message count from database. */
    suspend fun getPendingCount(): Int = pendingMessageDao.count()

    /**
     * Flush all pending messages.
     * Sends each in FIFO order via the registered callback.
     * Success -> delete from queue.
     * Failure -> increment retryCount; discard after MAX_RETRY_COUNT.
     *
     * @return the number of messages successfully flushed.
     */
    suspend fun flush(): Int {
        val pending = pendingMessageDao.getAll()
        if (pending.isEmpty()) return 0

        val callback = sendCallback ?: return 0
        var flushed = 0

        for (msg in pending) {
            val success = try {
                callback.onSend(msg.conversationId, msg.content)
            } catch (_: Exception) {
                false
            }

            if (success) {
                pendingMessageDao.deleteById(msg.id)
                flushed++
            } else {
                val newCount = msg.retryCount + 1
                if (newCount >= MAX_RETRY_COUNT) {
                    pendingMessageDao.deleteById(msg.id)
                } else {
                    pendingMessageDao.updateRetryCount(msg.id, newCount)
                }
            }
        }

        return getPendingCount()
    }

    /** Remove all pending messages. */
    suspend fun clear() {
        val all = pendingMessageDao.getAll()
        all.forEach { pendingMessageDao.deleteById(it.id) }
    }
}
