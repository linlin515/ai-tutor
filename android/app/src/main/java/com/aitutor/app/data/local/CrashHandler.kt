package com.aitutor.app.data.local

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器。
 *
 * 捕获未处理的异常，将崩溃信息写入 filesDir/crashes/ 目录，
 * 并确保保留最多 20 个日志文件。
 */
class CrashHandler private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_DIR = "crashes"
        private const val MAX_LOG_COUNT = 20
        private const val FILENAME_PREFIX = "crash_"
        private const val FILENAME_SUFFIX = ".txt"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private const val USER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

        @Volatile
        private var instance: CrashHandler? = null

        /**
         * 获取 CrashHandler 单例实例。
         */
        fun getInstance(): CrashHandler? = instance

        /**
         * 初始化并注册全局未捕获异常处理器。
         * 线程安全，仅执行一次。
         */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(CrashHandler::class.java) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(instance)
                        Log.i(TAG, "CrashHandler initialized and registered")
                    }
                }
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashReport(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
        }

        // 继续交给默认 handler（系统默认或之前的 handler）
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * 将崩溃信息写入文件。
     */
    private fun saveCrashReport(thread: Thread, throwable: Throwable) {
        val crashDir = getCrashDir()
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }

        val now = System.currentTimeMillis()
        val fileName = FILENAME_PREFIX + DATE_FORMAT.format(Date(now)) + FILENAME_SUFFIX
        val file = File(crashDir, fileName)

        val report = buildCrashReport(thread, throwable, now)

        try {
            FileWriter(file).use { writer ->
                writer.write(report)
                writer.flush()
            }
            Log.i(TAG, "Crash report saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash file", e)
        }

        // 清理旧日志，确保不超过上限
        trimOldLogs(crashDir)
    }

    /**
     * 构建崩溃报告字符串。
     */
    private fun buildCrashReport(thread: Thread, throwable: Throwable, timestamp: Long): String {
        val sb = StringBuilder()
        val userDateFormat = SimpleDateFormat(USER_DATE_FORMAT, Locale.getDefault())

        sb.appendLine("============================================")
        sb.appendLine("          CRASH REPORT")
        sb.appendLine("============================================")
        sb.appendLine()

        // 时间
        sb.appendLine("[Time]")
        sb.appendLine("  Timestamp: $timestamp")
        sb.appendLine("  DateTime: ${userDateFormat.format(Date(timestamp))}")
        sb.appendLine()

        // 设备信息
        sb.appendLine("[Device]")
        sb.appendLine("  Brand: ${Build.BRAND}")
        sb.appendLine("  Model: ${Build.MODEL}")
        sb.appendLine("  Manufacturer: ${Build.MANUFACTURER}")
        sb.appendLine("  Android Version: ${Build.VERSION.RELEASE}")
        sb.appendLine("  API Level: ${Build.VERSION.SDK_INT}")
        sb.appendLine("  Build Fingerprint: ${Build.FINGERPRINT}")
        sb.appendLine()

        // App 版本
        sb.appendLine("[App]")
        try {
            val pkgInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_ACTIVITIES
            )
            sb.appendLine("  Package: ${context.packageName}")
            sb.appendLine("  Version Name: ${pkgInfo.versionName ?: "N/A"}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sb.appendLine("  Version Code: ${pkgInfo.longVersionCode}")
            } else {
                @Suppress("DEPRECATION")
                sb.appendLine("  Version Code: ${pkgInfo.versionCode}")
            }
        } catch (e: Exception) {
            sb.appendLine("  Version Info: Failed to retrieve (${e.message})")
        }
        sb.appendLine()

        // 线程信息
        sb.appendLine("[Thread]")
        sb.appendLine("  Name: ${thread.name}")
        sb.appendLine("  ID: ${thread.id}")
        sb.appendLine("  Priority: ${thread.priority}")
        sb.appendLine("  State: ${thread.state}")
        sb.appendLine("  Group: ${thread.threadGroup?.name ?: "N/A"}")
        sb.appendLine()

        // 异常信息
        sb.appendLine("[Exception]")
        sb.appendLine("  Type: ${throwable.javaClass.name}")
        sb.appendLine("  Message: ${throwable.message ?: "No message"}")
        sb.appendLine()

        // 完整堆栈
        sb.appendLine("[Stack Trace]")
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        sb.appendLine(sw.toString())

        // 如果存在 Cause，追加 Cause 堆栈
        var cause = throwable.cause
        var causeIndex = 0
        while (cause != null && causeIndex < 10) {
            sb.appendLine()
            sb.appendLine("--- Caused by ($causeIndex): ${cause.javaClass.name} ---")
            sb.appendLine("  Message: ${cause.message ?: "No message"}")
            val causeSw = StringWriter()
            val causePw = PrintWriter(causeSw)
            cause.printStackTrace(causePw)
            causePw.flush()
            sb.appendLine(causeSw.toString())
            cause = cause.cause
            causeIndex++
        }
        sb.appendLine()

        // 内核日志尾部（仅 API 26+，通过 Runtime.getRuntime().exec 获取可能不可靠，跳过）

        sb.appendLine("============================================")
        sb.appendLine("          END OF CRASH REPORT")
        sb.appendLine("============================================")

        return sb.toString()
    }

    /**
     * 清理旧日志，确保文件数不超过 MAX_LOG_COUNT。
     */
    private fun trimOldLogs(crashDir: File) {
        val files = crashDir.listFiles { file ->
            file.isFile && file.name.startsWith(FILENAME_PREFIX) && file.name.endsWith(FILENAME_SUFFIX)
        }?.sortedBy { it.lastModified() } ?: return

        if (files.size <= MAX_LOG_COUNT) return

        // 删除最旧的文件，直到数量达标
        val toDelete = files.size - MAX_LOG_COUNT
        files.take(toDelete).forEach { file ->
            if (file.delete()) {
                Log.d(TAG, "Deleted old crash log: ${file.name}")
            } else {
                Log.w(TAG, "Failed to delete old crash log: ${file.name}")
            }
        }
    }

    /**
     * 获取崩溃日志目录。
     */
    private fun getCrashDir(): File {
        return File(context.filesDir, CRASH_DIR)
    }

    // ========================================
    // 公开 API — 供 CrashLogViewModel 等使用
    // ========================================

    /**
     * 获取所有崩溃日志文件，按最后修改时间降序排列（最新的在前）。
     */
    fun getCrashFiles(): List<File> {
        val dir = getCrashDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file ->
            file.isFile && file.name.startsWith(FILENAME_PREFIX) && file.name.endsWith(FILENAME_SUFFIX)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 读取崩溃日志文件内容。
     */
    fun readCrashFile(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read crash file: ${file.name}", e)
            null
        }
    }

    /**
     * 删除单个崩溃日志文件。
     */
    fun deleteCrashFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete crash file: ${file.name}", e)
            false
        }
    }

    /**
     * 清空所有崩溃日志。
     */
    fun clearAllCrashes(): Int {
        val files = getCrashFiles()
        var deleted = 0
        files.forEach { file ->
            if (deleteCrashFile(file)) deleted++
        }
        return deleted
    }

    /**
     * 获取崩溃日志数量。
     */
    fun getCrashCount(): Int {
        return getCrashFiles().size
    }
}
