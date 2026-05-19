package com.aitutor.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aitutor.app.data.local.AppLifecycleTracker
import com.aitutor.app.data.local.CrashHandler
import com.aitutor.app.data.local.NotificationChannels
import com.aitutor.app.data.local.reminder.StudyReminderWorker
import com.aitutor.app.data.remote.datastore.SettingsDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AiTutorApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 初始化全局崩溃处理器
        CrashHandler.init(this)
        NotificationChannels.create(this)
        registerActivityLifecycleCallbacks(AppLifecycleTracker)

        // F4: 启动时恢复学习提醒调度（如果用户已开启）
        restoreReminderSchedule()
    }

    /**
     * 恢复学习提醒调度。
     * 从 DataStore 读取设置，如果已开启提醒，通过 WorkManager 重新调度。
     * 使用 UPDATE 策略避免重复创建。
     */
    private fun restoreReminderSchedule() {
        appScope.launch {
            try {
                val settings = settingsDataStore.settingsFlow.first()
                if (settings.dailyReminderEnabled) {
                    val workManager = WorkManager.getInstance(this@AiTutorApp)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()

                    // 检查是否已有调度，有则跳过
                    val existingWork = workManager.getWorkInfosForUniqueWork(StudyReminderWorker.WORK_NAME).get()
                    if (existingWork.isNotEmpty() && existingWork.any {
                            it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                            it.state == androidx.work.WorkInfo.State.RUNNING
                        }) {
                        Log.d("AiTutorApp", "Study reminder already scheduled, skipping")
                        return@launch
                    }

                    val now = Calendar.getInstance()
                    val targetTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, settings.reminderHour)
                        set(Calendar.MINUTE, settings.reminderMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (before(now)) {
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    val initialDelayMinutes = (targetTime.timeInMillis - now.timeInMillis) / (1000 * 60)

                    val workRequest = PeriodicWorkRequestBuilder<StudyReminderWorker>(
                        24, TimeUnit.HOURS
                    )
                        .setConstraints(constraints)
                        .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                        .addTag(StudyReminderWorker.WORK_NAME)
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        StudyReminderWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
                    Log.d("AiTutorApp", "Restored daily reminder at ${settings.reminderHour}:${settings.reminderMinute}")
                }
            } catch (e: Exception) {
                Log.e("AiTutorApp", "Failed to restore reminder schedule", e)
            }
        }
    }
}
