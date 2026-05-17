package com.aitutor.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.app.data.local.CrashHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志条目 UI 模型。
 */
data class CrashLogItem(
    val file: File,
    val fileName: String,
    val dateTime: String,
    val exceptionType: String,
    val exceptionSummary: String
)

/**
 * 崩溃日志详情状态。
 */
data class CrashLogDetailState(
    val fileName: String = "",
    val content: String = "",
    val isLoading: Boolean = false
)

/**
 * 崩溃日志列表状态。
 */
data class CrashLogListState(
    val items: List<CrashLogItem> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true
)

/**
 * 崩溃日志页面 ViewModel。
 */
class CrashLogViewModel(application: Application) : AndroidViewModel(application) {

    private val crashHandler: CrashHandler? = CrashHandler.getInstance()

    private val _listState = MutableStateFlow(CrashLogListState())
    val listState: StateFlow<CrashLogListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(CrashLogDetailState())
    val detailState: StateFlow<CrashLogDetailState> = _detailState.asStateFlow()

    private val _deleteResult = MutableStateFlow<String?>(null)
    val deleteResult: StateFlow<String?> = _deleteResult.asStateFlow()

    companion object {
        private val FILE_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private val DISPLAY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    init {
        refreshList()
    }

    /**
     * 刷新崩溃日志列表。
     */
    fun refreshList() {
        viewModelScope.launch(Dispatchers.IO) {
            _listState.value = _listState.value.copy(isLoading = true)
            val handler = crashHandler
            val items = if (handler != null) {
                handler.getCrashFiles().mapNotNull { file ->
                    parseCrashFile(file)
                }
            } else {
                // CrashHandler 尚未初始化时尝试直接从目录读取
                val crashDir = File(getApplication<Application>().filesDir, "crashes")
                if (crashDir.exists()) {
                    crashDir.listFiles { f ->
                        f.isFile && f.name.startsWith("crash_") && f.name.endsWith(".txt")
                    }?.sortedByDescending { it.lastModified() }?.mapNotNull { file ->
                        parseCrashFile(file)
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            }
            _listState.value = CrashLogListState(
                items = items,
                isLoading = false,
                isEmpty = items.isEmpty()
            )
        }
    }

    /**
     * 加载崩溃日志详情。
     */
    fun loadDetail(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _detailState.value = CrashLogDetailState(
                fileName = file.name,
                isLoading = true
            )
            val content = crashHandler?.readCrashFile(file)
                ?: try { file.readText() } catch (e: Exception) { "无法读取崩溃日志" }
            _detailState.value = CrashLogDetailState(
                fileName = file.name,
                content = content,
                isLoading = false
            )
        }
    }

    /**
     * 删除单个崩溃日志。
     */
    fun deleteCrash(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = crashHandler?.deleteCrashFile(file) ?: file.delete()
            if (success) {
                _deleteResult.value = "已删除 ${file.name}"
                refreshList()
            } else {
                _deleteResult.value = "删除失败"
            }
        }
    }

    /**
     * 清空所有崩溃日志。
     */
    fun clearAllCrashes() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = crashHandler?.clearAllCrashes()
                ?: run {
                    val dir = File(getApplication<Application>().filesDir, "crashes")
                    var deleted = 0
                    dir.listFiles()?.forEach { if (it.delete()) deleted++ }
                    deleted
                }
            _deleteResult.value = "已清空 $count 个崩溃日志"
            refreshList()
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    /**
     * 从崩溃日志文件中解析摘要信息。
     */
    private fun parseCrashFile(file: File): CrashLogItem? {
        try {
            // 从文件名解析时间
            val fileName = file.name
            val dateTime = try {
                val dateStr = fileName
                    .removePrefix("crash_")
                    .removeSuffix(".txt")
                val date = FILE_DATE_FORMAT.parse(dateStr)
                if (date != null) DISPLAY_DATE_FORMAT.format(date) else "未知时间"
            } catch (e: Exception) {
                "未知时间"
            }

            // 读取前几行获取异常类型和消息
            val lines = try {
                file.useLines { it.take(50).toList() }
            } catch (e: Exception) {
                emptyList()
            }

            var exceptionType = "未知"
            var exceptionMsg = ""
            var inExceptionSection = false

            for (line in lines) {
                if (line.startsWith("[Exception]")) {
                    inExceptionSection = true
                    continue
                }
                if (inExceptionSection) {
                    if (line.startsWith("  Type:")) {
                        exceptionType = line.substringAfter("  Type:").trim()
                    } else if (line.startsWith("  Message:")) {
                        exceptionMsg = line.substringAfter("  Message:").trim()
                    } else if (line.startsWith("[")) {
                        break
                    }
                }
            }

            val summary = if (exceptionMsg.isNotEmpty() && exceptionMsg != "No message") {
                exceptionMsg
            } else {
                exceptionType
            }

            return CrashLogItem(
                file = file,
                fileName = fileName,
                dateTime = dateTime,
                exceptionType = exceptionType,
                exceptionSummary = summary.truncate(120)
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun String.truncate(maxLength: Int): String {
        return if (length > maxLength) take(maxLength) + "…" else this
    }
}
