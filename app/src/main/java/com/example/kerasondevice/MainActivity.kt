package com.example.kerasondevice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kerasondevice.ui.components.KerasTopBar
import com.example.kerasondevice.ui.screen.ChatScreen
import com.example.kerasondevice.ui.theme.LiteRTLMDemoTheme
import com.example.kerasondevice.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiteRTLMDemoTheme(darkTheme = true) {
                val viewModel: ChatViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val modelInfo = uiState.modelInfo

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        KerasTopBar(
                            modelName = modelInfo?.path?.substringAfterLast("/"),
                            modelSize = modelInfo?.sizeText,
                            availableModels = uiState.availableModels.map {
                                it.name to it.sizeText
                            },
                            onModelSelected = { index ->
                                val model = uiState.availableModels.getOrNull(index)
                                if (model != null) {
                                    viewModel.switchModel(model)
                                }
                            },
                            onNewChat = { viewModel.clearConversation() }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    ChatScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
