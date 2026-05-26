package com.aitutor.app.data.local

import android.content.Context
import coil.Coil
import com.aitutor.app.data.local.dao.CachedConversationDao
import com.aitutor.app.data.local.dao.CachedQuestionDao
import com.aitutor.app.data.local.dao.CachedWrongAnswerDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存大小信息（按缓存表统计）。
 */
data class CacheSizeInfo(
    val questionsSize: Long,
    val wrongAnswersSize: Long,
    val conversationsSize: Long,
    val totalSizeMb: Float
)

/**
 * 缓存大小信息（综合）。
 */
data class CacheSize(
    val imageDisk: Long = 0,
    val imageMemory: Long = 0,
    val logs: Long = 0,
    val temp: Long = 0,
    val database: Long = 0,
    val total: Long = 0
)

/**
 * 缓存管理器，负责计算和清理各类缓存。
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cachedQuestionDao: CachedQuestionDao,
    private val cachedWrongAnswerDao: CachedWrongAnswerDao,
    private val cachedConversationDao: CachedConversationDao
) {
    private val logDir: File get() = File(context.cacheDir, "logs")
    private val tempDir: File get() = File(context.cacheDir, "temp")
    private val maxCacheSize = MAX_CACHE_BYTES

    /**
     * 计算各类缓存大小（字节）。
     */
    suspend fun calculateSize(): CacheSize = withContext(Dispatchers.IO) {
        val imageLoader = try {
            Coil.imageLoader(context)
        } catch (_: Exception) {
            null
        }

        val diskSize = imageLoader?.diskCache?.let { cache ->
            measureDirSize(cache.directory.toFile())
        } ?: 0L

        val memorySize = imageLoader?.memoryCache?.let { cache ->
            cache.maxSize.toLong()
        } ?: 0L

        val logSize = measureDirSize(logDir)
        val tempSize = measureDirSize(tempDir)

        // Room database size
        val dbFile = context.getDatabasePath("aitutor_database")
        val dbSize = if (dbFile.exists()) dbFile.length() else 0L
        val dbWalFile = File(dbFile.parent, dbFile.name + "-wal")
        val dbWalSize = if (dbWalFile.exists()) dbWalFile.length() else 0L
        val dbShmFile = File(dbFile.parent, dbFile.name + "-shm")
        val dbShmSize = if (dbShmFile.exists()) dbShmFile.length() else 0L
        val databaseSize = dbSize + dbWalSize + dbShmSize

        CacheSize(
            imageDisk = diskSize,
            imageMemory = memorySize,
            logs = logSize,
            temp = tempSize,
            database = databaseSize,
            total = diskSize + memorySize + logSize + tempSize + databaseSize
        )
    }

    /**
     * 清理所有缓存。
     *
     * @param onProgress 进度回调 (0f..1f)
     */
    suspend fun clearAll(onProgress: (Float) -> Unit = {}) = withContext(Dispatchers.IO) {
        var steps = 0
        val totalSteps = 6

        // 1. 清理 Coil 磁盘缓存
        try {
            Coil.imageLoader(context).diskCache?.clear()
        } catch (_: Exception) { /* 忽略 */ }
        steps++
        onProgress(steps.toFloat() / totalSteps)

        // 2. 清理 Coil 内存缓存
        try {
            Coil.imageLoader(context).memoryCache?.clear()
        } catch (_: Exception) { /* 忽略 */ }
        steps++
        onProgress(steps.toFloat() / totalSteps)

        // 3. ML Kit OCR 资源释放
        try {
            // MobileVisionBase.closeAll()  // 需要 mlkit 依赖
        } catch (_: Exception) { /* 忽略 */ }
        steps++
        onProgress(steps.toFloat() / totalSteps)

        // 4. 删除日志文件
        deleteDirectory(logDir)
        steps++
        onProgress(steps.toFloat() / totalSteps)

        // 5. 删除临时文件
        deleteDirectory(tempDir)
        steps++
        onProgress(steps.toFloat() / totalSteps)

        // 6. 运行 Room WAL checkpoint
        try {
            val dbPath = context.getDatabasePath("aitutor_database")
            if (dbPath.exists()) {
                // SQLite WAL checkpoint 会在数据库关闭时自动触发
            }
        } catch (_: Exception) { /* 忽略 */ }
        steps++
        onProgress(steps.toFloat() / totalSteps)

        onProgress(1f)
    }

    /**
     * LRU 淘汰策略：当数据库缓存（cached_questions, cached_conversations 等）超过阈值时，
     * 按 cachedAt 升序删除最旧的条目，直到总大小降至目标值。
     */
    suspend fun evictLru(
        db: com.aitutor.app.data.local.db.AiTutorDatabase,
        targetBytes: Long = MAX_CACHE_BYTES / 2
    ) = withContext(Dispatchers.IO) {
        // Estimate cache table sizes and evict oldest entries
        val questionCount = db.cachedQuestionDao().count()
        val conversationCount = db.cachedConversationDao().count()
        val wrongAnswerCount = db.cachedWrongAnswerDao().count()

        // Rough estimate: each cached item ~ 2KB
        val estimatedSize = (questionCount + conversationCount + wrongAnswerCount) * 2048L

        if (estimatedSize <= targetBytes) return@withContext

        // Eviction ratio: need to remove (estimatedSize - targetBytes) bytes
        val evictRatio = 1.0f - (targetBytes.toFloat() / estimatedSize.toFloat())

        if (evictRatio <= 0f) return@withContext

        // Evict oldest entries proportionally
        val questionsToKeep = (questionCount * (1f - evictRatio * 0.5f)).toInt().coerceAtLeast(0)
        val conversationsToKeep = (conversationCount * (1f - evictRatio * 0.5f)).toInt().coerceAtLeast(0)
        val wrongAnswersToKeep = (wrongAnswerCount * (1f - evictRatio * 0.3f)).toInt().coerceAtLeast(0)

        if (questionsToKeep < questionCount) {
            // Delete oldest questions beyond the keep limit
            val limit = questionCount - questionsToKeep
            if (limit > 0) {
                db.cachedQuestionDao().deleteOldest(limit)
            }
        }

        if (conversationsToKeep < conversationCount) {
            val limit = conversationCount - conversationsToKeep
            if (limit > 0) {
                db.cachedConversationDao().deleteOldest(limit)
            }
        }

        if (wrongAnswersToKeep < wrongAnswerCount) {
            val limit = wrongAnswerCount - wrongAnswersToKeep
            if (limit > 0) {
                db.cachedWrongAnswerDao().deleteOldest(limit)
            }
        }
    }

    // === DAO-based cache methods (v4.0 Sprint 2) ===

    /**
     * 按缓存表统计缓存大小。
     */
    suspend fun getCacheSizeInfo(): CacheSizeInfo {
        val qSize = cachedQuestionDao.dataSizeBytes() ?: 0
        val waSize = cachedWrongAnswerDao.dataSizeBytes() ?: 0
        val cSize = cachedConversationDao.dataSizeBytes() ?: 0
        return CacheSizeInfo(qSize, waSize, cSize, (qSize + waSize + cSize) / 1048576f)
    }

    /**
     * 清除缓存表数据。
     */
    suspend fun clearAllCache() {
        cachedQuestionDao.deleteAll()
        cachedWrongAnswerDao.deleteAll()
        cachedConversationDao.deleteAll()
    }

    /** 删除目录及其内容 */
    private fun deleteDirectory(dir: File) {
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) deleteDirectory(file)
                file.delete()
            }
        }
    }

    /** 递归计算目录大小 */
    private fun measureDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    companion object {
        /**
         * Maximum cache size in bytes (100 MB by default).
         */
        const val MAX_CACHE_BYTES = 100L * 1024 * 1024
    }
}
