package com.example.kerasondevice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.kerasondevice.domain.catalog.DemoEntry
import com.example.kerasondevice.domain.task.TaskRegistry
import com.example.kerasondevice.ui.catalog.CatalogScreen
import com.example.kerasondevice.ui.components.KerasTopBar
import com.example.kerasondevice.ui.language.LanguageScreen
import com.example.kerasondevice.ui.multimodal.MultimodalScreen
import com.example.kerasondevice.ui.task.TaskScreen
import com.example.kerasondevice.ui.theme.KerasOnDeviceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KerasOnDeviceTheme(darkTheme = true) {
                var currentEntry by rememberSaveable { mutableStateOf<DemoEntry?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        KerasTopBar(
                            onNewChat = { currentEntry = null }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (val entry = currentEntry) {
                        null -> CatalogScreen(
                            onSelect = { currentEntry = it },
                            modifier = modifier
                        )
                        is DemoEntry.VisionEntry -> {
                            val task = TaskRegistry.get(entry.taskId)
                            if (task != null) {
                                TaskScreen(
                                    task = task,
                                    onBack = { currentEntry = null },
                                    modifier = modifier
                                )
                            } else {
                                currentEntry = null
                            }
                        }
                        is DemoEntry.LanguageEntry -> LanguageScreen(
                            modifier = modifier
                        )
                        is DemoEntry.MultimodalEntry -> androidx.compose.foundation.layout.Box(
                            modifier = modifier.fillMaxSize()
                        ) {
                            MultimodalScreen(onBack = { currentEntry = null })
                        }
                    }
                }
            }
        }
    }
}
