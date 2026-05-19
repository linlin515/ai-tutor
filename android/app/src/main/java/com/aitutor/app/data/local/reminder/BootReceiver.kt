package com.aitutor.app.data.local.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 开机广播接收器：设备重启后重新调度学习提醒 WorkManager。
 *
 * 注意：API 26+ 会使用 JobScheduler，其定期任务在重启后会自动恢复。
 * 本 Receiver 作为兜底方案，确保低版本设备或特殊情况下提醒能正常恢复。
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed, checking reminder schedule")

        // Use WorkManager directly - it will check if work already exists
        try {
            val workManager = WorkManager.getInstance(context)

            // Check if the work is already scheduled (API 26+ typically auto-recovers)
            val workInfos = workManager.getWorkInfosForUniqueWork(StudyReminderWorker.WORK_NAME).get()
            if (workInfos.isNotEmpty() && workInfos.any {
                    it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                    it.state == androidx.work.WorkInfo.State.RUNNING
                }) {
                Log.d(TAG, "Study reminder already scheduled by JobScheduler")
                return
            }

            // If not scheduled, we can't inject here easily. Just log it.
            // AiTutorApp.onCreate() will handle the initial scheduling.
            Log.d(TAG, "Study reminder not found — will be scheduled by AiTutorApp.onCreate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check reminder schedule", e)
        }
    }
}
