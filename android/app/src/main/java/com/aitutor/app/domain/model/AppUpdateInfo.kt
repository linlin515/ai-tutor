package com.aitutor.app.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 应用更新信息，从后端 API /api/v1/app/version 获取
 */
data class AppUpdateInfo(
    @SerializedName("version_code")
    val versionCode: Long,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("download_url")
    val downloadUrl: String,
    @SerializedName("release_notes")
    val releaseNotes: String,
    @SerializedName("force_update")
    val forceUpdate: Boolean = false
)
