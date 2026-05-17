package com.aitutor.app.domain.repository

import com.aitutor.app.domain.model.ReportData
import com.aitutor.app.domain.model.ReportType

interface StudyReportRepository {
    suspend fun getReportData(reportType: ReportType = ReportType.WEEKLY, nickname: String = ""): ReportData
}
