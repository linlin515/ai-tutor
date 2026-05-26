package com.aitutor.app.data.remote.dto

data class ReportExportRequest(
    val reportType: String, // "weekly" or "monthly"
    val format: String = "pdf",
    val startDate: String? = null,
    val endDate: String? = null
)

data class ReportExportResponse(
    val downloadUrl: String = ""
)
