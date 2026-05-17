package com.aitutor.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * P1-3: Compose Error Boundary.
 *
 * Wraps child composables and provides error boundary semantics.
 * Child composables can report errors via [LocalErrorHandler].
 *
 * When an error is reported, the boundary shows a fallback UI
 * instead of letting the whole screen go blank.
 *
 * Usage:
 * ```kotlin
 * ErrorBoundary {
 *     // child composable
 *     val errorHandler = LocalErrorHandler.current
 *     LaunchedEffect(Unit) {
 *         try { api.call() }
 *         catch (e: Exception) { errorHandler.reportError(e) }
 *     }
 * }
 * ```
 *
 * Supports nesting — inner boundaries handle their children first.
 */
@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    key: Any? = null,
    onError: ((Throwable) -> Unit)? = null,
    fallback: @Composable (error: Throwable?, retry: () -> Unit) -> Unit = { error, retry ->
        DefaultErrorFallback(error = error, onRetry = retry)
    },
    content: @Composable () -> Unit
) {
    var hasError by remember(key) { mutableStateOf(false) }
    var capturedError by remember(key) { mutableStateOf<Throwable?>(null) }

    val retry: () -> Unit = {
        hasError = false
        capturedError = null
    }

    // Create an error handler that child composables can use
    val errorHandler = remember(key) {
        ErrorHandler { throwable ->
            if (!hasError) {
                hasError = true
                capturedError = throwable
                onError?.invoke(throwable)
                Log.w(
                    "ErrorBoundary",
                    "Composition error caught: ${throwable.javaClass.simpleName}: ${throwable.message}"
                )
            }
        }
    }

    // Expose via CompositionLocal so children can report errors
    CompositionLocalProvider(
        LocalErrorHandler provides errorHandler
    ) {
        if (hasError) {
            Box(modifier = modifier) {
                fallback(capturedError, retry)
            }
        } else {
            Box(modifier = modifier) {
                content()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Error Handler
// ═══════════════════════════════════════════════════════════════

/**
 * Error handler callback used by [ErrorBoundary].
 * Child composables obtain the current handler via [LocalErrorHandler]
 * and call [reportError] when they encounter a non-recoverable error.
 */
fun interface ErrorHandler {
    fun reportError(throwable: Throwable)
}

/**
 * CompositionLocal that provides the nearest [ErrorBoundary]'s error handler.
 *
 * Child composables use:
 * ```kotlin
 * val errorHandler = LocalErrorHandler.current
 * errorHandler.reportError(someException)
 * ```
 */
val LocalErrorHandler = compositionLocalOf<ErrorHandler> {
    // Default: no-op handler when outside any ErrorBoundary
    ErrorHandler { throwable ->
        Log.e("ErrorBoundary", "Unhandled error outside boundary: ${throwable.message}", throwable)
    }
}

// ═══════════════════════════════════════════════════════════════
// Default Fallback UI
// ═══════════════════════════════════════════════════════════════

/**
 * Default fallback UI shown when a child component reports an error.
 */
@Composable
fun DefaultErrorFallback(
    error: Throwable?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "组件出现异常",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (error != null) {
                    "${error.javaClass.simpleName}: ${error.message ?: "未知错误"}"
                } else {
                    "发生了意外错误"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("重试")
                }
            }
        }
    }
}
