package com.aitutor.app.data.local

import android.app.Activity
import android.app.Application

/**
 * 追踪 App 的前景/背景状态，通过 Activity 生命周期回调实现。
 *
 * 使用 started/stopped 计数来判断 App 是否在前台：
 *  - 当至少有一个 Activity 处于 started 状态时，App 在前台。
 *  - 当所有 Activity 都 stopped 时，App 在后台。
 */
object AppLifecycleTracker : Application.ActivityLifecycleCallbacks {

    private var startedCount = 0

    /**
     * App 是否处于前台（至少有一个 Activity 处于 started 状态）。
     */
    val isInForeground: Boolean
        get() = startedCount > 0

    override fun onActivityStarted(activity: Activity) {
        startedCount++
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount--
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
