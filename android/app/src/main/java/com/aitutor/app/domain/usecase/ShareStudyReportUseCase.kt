package com.aitutor.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aitutor.app.domain.model.ReportData
import com.aitutor.app.domain.model.ReportType
import com.aitutor.app.domain.repository.StudyReportRepository
import com.aitutor.app.ui.report.PdfReportRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareStudyReportUseCase @Inject constructor(
    private val studyReportRepository: StudyReportRepository,
    private val pdfReportRenderer: PdfReportRenderer,
    @ApplicationContext private val context: Context
) {
    /**
     * Generate the study report PDF and return the content URI for sharing.
     */
    suspend fun generateReport(
        reportType: ReportType = ReportType.WEEKLY,
        nickname: String = "",
        onProgress: (String) -> Unit = {}
    ): Result<Uri> {
        return try {
            onProgress("正在获取数据...")
            val reportData = studyReportRepository.getReportData(reportType, nickname)

            onProgress("正在生成 PDF...")
            val pdfDocument = pdfReportRenderer.render(reportData)

            onProgress("正在保存...")
            val cacheDir = File(context.cacheDir, "study_report")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "report_${dateFormat.format(Date())}.pdf"
            val pdfFile = File(cacheDir, fileName)

            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            onProgress("准备分享...")
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            Result.success(contentUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a share intent for the PDF file.
     */
    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "学习报告")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
