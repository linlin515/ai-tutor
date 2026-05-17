package com.aitutor.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyticsDto(
    val today: TodayStatsDto? = null,
    val trend: List<TrendPointDto>? = null,
    val knowledge: List<KnowledgeDto>? = null
)

data class TodayStatsDto(
    @SerializedName("solve_count") val solveCount: Int = 0,
    @SerializedName("correct_rate") val correctRate: Float = 0f,
    val duration: Int = 0
)

data class TrendPointDto(
    val date: String,
    val duration: Int = 0,
    @SerializedName("solve_count") val solveCount: Int = 0
)

data class KnowledgeDto(
    val id: String,
    val name: String,
    val status: String = "weak",
    val confidence: Float = 0f
)
