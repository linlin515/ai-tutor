package com.aitutor.app.domain.repository

import android.net.Uri

interface ReportRepository {
    suspend fun exportReport(
        reportType: String,
        format: String = "pdf",
        startDate: String? = null,
        endDate: String? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Uri>

    fun shareFile(uri: Uri): android.content.Intent
}
