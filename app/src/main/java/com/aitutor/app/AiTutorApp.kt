package com.aitutor.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiTutorApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
