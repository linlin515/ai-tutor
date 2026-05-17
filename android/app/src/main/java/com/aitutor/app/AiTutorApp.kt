package com.aitutor.app

import android.app.Application
import com.aitutor.app.data.local.AppLifecycleTracker
import com.aitutor.app.data.local.CrashHandler
import com.aitutor.app.data.local.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiTutorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化全局崩溃处理器
        CrashHandler.init(this)
        NotificationChannels.create(this)
        registerActivityLifecycleCallbacks(AppLifecycleTracker)
    }
}
