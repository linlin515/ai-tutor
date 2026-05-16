package com.aitutor.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aitutor.app.ui.navigation.AppNavGraph
import com.aitutor.app.ui.theme.AiTutorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Used to propagate conversation_id from notification click
     * to the Compose navigation graph, handling both fresh launch
     * (onCreate) and re-launch (onNewIntent).
     */
    private val pendingConversationId = MutableStateFlow(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent.getLongExtra("conversation_id", -1L).takeIf { it > 0 }?.let {
            pendingConversationId.value = it
        }

        setContent {
            val flow = remember { pendingConversationId }
            AiTutorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(pendingConversationId = flow)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getLongExtra("conversation_id", -1L).takeIf { it > 0 }?.let {
            pendingConversationId.value = it
        }
    }
}
