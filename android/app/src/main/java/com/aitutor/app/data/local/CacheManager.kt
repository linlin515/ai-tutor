package com.aitutor.app.data.local

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存大小信息。
 */
data class CacheSize(
    val imageDisk: Long = 0,
    val imageMemory: Long = 0,
    val logs: Long = 0,
    val temp: Long = 0,
    val total: Long = 0
)

/**
 * 缓存管理器，负责计算和清理各类缓存。
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDir: File get() = File(context.cacheDir, "logs")
    private val tempDir: File get() = File(context.cacheDir, "temp")

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
            // MemoryCache 不直接暴露大小，近似计算
            cache.maxSize.toLong()
        } ?: 0L

        val logSize = measureDirSize(logDir)
        val tempSize = measureDirSize(tempDir)

        CacheSize(
            imageDisk = diskSize,
            imageMemory = memorySize,
            logs = logSize,
            temp = tempSize,
            total = diskSize + memorySize + logSize + tempSize
        )
    }

    /**
     * 清理所有缓存。
     *
     * @param onProgress 进度回调 (0f..1f)
     */
    suspend fun clearAll(onProgress: (Float) -> Unit = {}) = withContext(Dispatchers.IO) {
        var steps = 0
        val totalSteps = 5

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

        // 3. ML Kit OCR 资源释放（如果引用可用）
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

        // 额外：运行 Room WAL checkpoint（如果可访问）
        try {
            val dbPath = context.getDatabasePath("aitutor_database")
            if (dbPath.exists()) {
                // SQLite WAL checkpoint 会自动在关闭时触发
                // Room.databaseBuilder 的 .build() 会触发 checkpoint
            }
        } catch (_: Exception) { /* 忽略 */ }

        onProgress(1f)
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
}
