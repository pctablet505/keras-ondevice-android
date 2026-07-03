package com.example.kerasondevice.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kerasondevice.ui.theme.KerasRed
import com.example.kerasondevice.ui.theme.OnSurfaceDark
import com.example.kerasondevice.ui.theme.SurfaceDark
import com.example.kerasondevice.ui.theme.SurfaceVariantDark
import com.example.kerasondevice.ui.viewmodel.ChatMessage
import com.example.kerasondevice.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    viewModel: ChatViewModel = viewModel()
) {
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
            if (uiState.isLoading && uiState.messages.isNotEmpty() && !uiState.messages.last().isGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .background(SurfaceVariantDark, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = OnSurfaceDark,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        uiState.error?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp)
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!uiState.isEngineReady && uiState.error == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = KerasRed,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading model...",
                        color = OnSurfaceDark,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::onInputChange,
                placeholder = { Text("Message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KerasRed,
                    focusedLabelColor = KerasRed,
                    cursorColor = KerasRed
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    viewModel.sendMessage()
                }),
                singleLine = false,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = uiState.inputText.isNotBlank() && !uiState.isLoading && uiState.isEngineReady
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (uiState.inputText.isNotBlank() && uiState.isEngineReady) KerasRed else SurfaceVariantDark
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) KerasRed else SurfaceVariantDark
            ),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.text,
                    color = if (isUser) OnSurfaceDark else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                )
                // Show benchmark stats for completed assistant messages
                if (!isUser && !msg.isGenerating && msg.benchmark != null) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = buildString {
                            append("${msg.benchmark.prefillTokens} prefill tok")
                            append(" · ${msg.benchmark.decodeTokens} decode tok")
                            append(" · TTFT ${String.format("%.2f", msg.benchmark.timeToFirstTokenSec)}s")
                            append(" · ${String.format("%.1f", msg.benchmark.decodeTps)} tok/s")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp)
                    )
                }
                msg.error?.let { err ->
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
