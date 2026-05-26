package com.aitutor.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aitutor.app.data.remote.api.ReportApi
import com.aitutor.app.data.remote.dto.ReportExportRequest
import com.aitutor.app.domain.repository.ReportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val reportApi: ReportApi,
    @ApplicationContext private val context: Context
) : ReportRepository {

    override suspend fun exportReport(
        reportType: String,
        format: String,
        startDate: String?,
        endDate: String?,
        onProgress: (String) -> Unit
    ): Result<Uri> {
        return try {
            onProgress("正在生成报告...")

            val request = ReportExportRequest(
                reportType = reportType,
                format = format,
                startDate = startDate,
                endDate = endDate
            )

            val response = reportApi.exportReport(request)
            if (!response.isSuccessful) {
                return Result.failure(Exception("服务器错误: ${response.code()}"))
            }

            onProgress("正在下载...")

            val body = response.body() ?: return Result.failure(Exception("响应为空"))

            // Save to cache directory
            val cacheDir = File(context.cacheDir, "study_report")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "report_${dateFormat.format(Date())}.$format"
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            onProgress("准备分享...")

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(contentUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun shareFile(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
