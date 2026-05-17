package com.aitutor.app.data.repository

import com.aitutor.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for reading and managing user grade/profile information.
 * Provides grade-aware configuration for adaptive step-by-step teaching (F41).
 *
 * Grade levels: 小学, 初中, 高中, 大学
 */
@Singleton
class UserProfileRepository @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Get the current user's grade level.
     * Returns null if not set or unavailable.
     */
    suspend fun getUserGrade(): String? {
        return authRepository.getProfile().getOrNull()?.grade
    }

    /**
     * Get the display name for a grade level.
     */
    fun getGradeDisplayName(grade: String): String {
        return when (grade) {
            "小学" -> "小学"
            "初中" -> "初中"
            "高中" -> "高中"
            "大学" -> "大学"
            else -> "自动检测"
        }
    }

    /**
     * Get all available grade levels.
     */
    fun getAvailableGrades(): List<String> {
        return listOf("auto", "小学", "初中", "高中", "大学")
    }

    /**
     * Get the system prompt hint for a given grade level.
     * This adjusts the AI's language complexity.
     */
    fun getGradeSystemPrompt(grade: String): String {
        return when (grade) {
            "小学" -> "学生当前年级：小学。请使用极其简单的语言，多用比喻和例子，避免专业术语。"
            "初中" -> "学生当前年级：初中。请使用适中的语言难度，适当使用教材中的术语并进行解释。"
            "高中" -> "学生当前年级：高中。请使用标准的学术语言，可以引入专业概念。"
            "大学" -> "学生当前年级：大学。请深入讲解原理，可以使用专业术语和公式推导。"
            else -> "" // auto — determined by user's actual grade
        }
    }
}
