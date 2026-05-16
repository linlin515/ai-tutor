package com.aitutor.app.data.repository

import com.aitutor.app.data.remote.api.AiTutorApi
import com.aitutor.app.data.remote.api.SolveApi
import com.aitutor.app.data.remote.dto.SolveRetryRequest
import com.aitutor.app.data.remote.dto.SolveStepsRequest
import com.aitutor.app.data.remote.dto.SolveStepsResponse
import com.aitutor.app.domain.model.SolveEvent
import com.aitutor.app.domain.repository.SolveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolveRepositoryImpl @Inject constructor(
    private val solveApi: SolveApi,
    private val aiTutorApi: AiTutorApi
) : SolveRepository {

    override fun streamSolvePhoto(
        imageBytes: ByteArray,
        subject: String,
        grade: String?
    ): Flow<SolveEvent> {
        return solveApi.streamSolve(
            imageBytes = imageBytes,
            subject = subject,
            grade = grade
        )
    }

    override suspend fun retryStep(
        solveId: String,
        stepIndex: Int,
        question: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = SolveRetryRequest(
                solveId = solveId,
                stepIndex = stepIndex,
                question = question
            )
            val response = aiTutorApi.solveRetryStep(request)
            val body = response.body()
            if (response.isSuccessful && body?.code == 0) {
                Result.success(body.message ?: "重试成功")
            } else {
                Result.failure(Exception(body?.message ?: "重试失败 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("重试请求失败: ${e.message}", e))
        }
    }

    override suspend fun getSolveSteps(
        question: String,
        grade: String,
        subject: String
    ): Result<SolveStepsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SolveStepsRequest(
                question = question,
                grade = grade,
                subject = subject
            )
            val response = aiTutorApi.solveSteps(request)
            val body = response.body()
            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.message ?: "获取讲解失败 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("获取讲解请求失败: ${e.message}", e))
        }
    }
}
