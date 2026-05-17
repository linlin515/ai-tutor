package com.aitutor.app.util

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Long.formatTimestamp(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        diff < 604_800_000 -> "${diff / 86_400_000}天前"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(this))
    }
}

fun String.isValidPhone(): Boolean {
    return length == 11 && all { it.isDigit() }
}

fun String.isValidPassword(): Boolean {
    return length in 6..20
}

fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (length > maxLength) {
        take(maxLength) + ellipsis
    } else {
        this
    }
}
