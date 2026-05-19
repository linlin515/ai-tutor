package com.aitutor.app.data.local.reminder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aitutor.app.data.local.NotificationHelper
import com.aitutor.app.data.local.dao.WrongAnswerDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager Worker：每天定时检查待复习错题并发送通知提醒。
 *
 * 触发条件：用户开启"每日学习提醒"后，由 WorkManager 在指定时间调度。
 * 工作内容：
 *   1. 查询 WrongAnswerDao 中待复习错题数量
 *   2. 如果有待复习题目，调用 NotificationHelper.sendReviewReminder()
 *   3. 如果没有待复习题目，发送一句鼓励通知
 */
@HiltWorker
class StudyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wrongAnswerDao: WrongAnswerDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "StudyReminderWorker"
        const val WORK_NAME = "daily_study_reminder"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "StudyReminderWorker started")

        return try {
            val dueCount = wrongAnswerDao.getDueCount().first()

            if (dueCount > 0) {
                NotificationHelper.sendReviewReminder(applicationContext, dueCount)
                Log.d(TAG, "Sent review reminder with $dueCount due items")
            } else {
                // 发送鼓励通知 — 所有错题都已复习完成
                NotificationHelper.sendEncouragementNotification(applicationContext)
                Log.d(TAG, "Sent encouragement notification (no items due)")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "StudyReminderWorker failed", e)
            Result.retry()
        }
    }
}
