package com.example.kerasondevice.ui.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kerasondevice.ui.theme.KerasRed

@Composable
fun LanguageScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    viewModel: LanguageViewModel = viewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Language",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            ModeToggle(
                mode = uiState.mode,
                onModeSelected = viewModel::setMode
            )

            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = viewModel::setPrompt,
                label = { Text("Prompt") },
                placeholder = { Text("Type a prompt...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KerasRed,
                    focusedLabelColor = KerasRed,
                    cursorColor = KerasRed
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
            )

            if (uiState.mode == LanguageViewModel.Mode.LITERT) {
                SamplerControls(
                    uiState = uiState,
                    onSamplerTypeChange = viewModel::setSamplerType,
                    onMaxLengthChange = viewModel::setMaxLength,
                    onTemperatureChange = viewModel::setTemperature,
                    onTopKChange = viewModel::setTopK,
                    onTopPChange = viewModel::setTopP,
                    onStripPromptChange = viewModel::setStripPrompt
                )
            }

            Button(
                onClick = { viewModel.generate() },
                enabled = uiState.prompt.isNotBlank() && !uiState.isGenerating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KerasRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Generate"
                    )
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = if (uiState.isGenerating) "Generating..." else "Generate")
            }

            uiState.error?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.output.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Output",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.output,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (uiState.stats.isNotBlank()) {
                Text(
                    text = uiState.stats,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModeToggle(
    mode: LanguageViewModel.Mode,
    onModeSelected: (LanguageViewModel.Mode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LanguageViewModel.Mode.entries.forEach { entry ->
            val selected = entry == mode
            val label = when (entry) {
                LanguageViewModel.Mode.LITERT -> "LiteRT (custom)"
                LanguageViewModel.Mode.LITERTLM -> "LiteRT-LM (optimised)"
            }
            val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                Text(
                    text = label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            if (selected) {
                Button(
                    onClick = { onModeSelected(entry) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KerasRed),
                    shape = RoundedCornerShape(8.dp),
                    content = content
                )
            } else {
                OutlinedButton(
                    onClick = { onModeSelected(entry) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SamplerControls(
    uiState: LanguageViewModel.UiState,
    onSamplerTypeChange: (LanguageViewModel.SamplerType) -> Unit,
    onMaxLengthChange: (Int) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopKChange: (Int) -> Unit,
    onTopPChange: (Float) -> Unit,
    onStripPromptChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sampler settings",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageViewModel.SamplerType.entries.forEach { type ->
                val selected = type == uiState.samplerType
                OutlinedButton(
                    onClick = { onSamplerTypeChange(type) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = type.name.replace("_", "-"),
                        color = if (selected) KerasRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        NumberField(
            label = "Max length",
            value = uiState.maxLength.toString(),
            onValueChange = { onMaxLengthChange(it.toIntOrNull() ?: 32) },
            keyboardType = KeyboardType.Number
        )

        NumberField(
            label = "Temperature",
            value = "%.2f".format(uiState.temperature),
            onValueChange = { onTemperatureChange(it.toFloatOrNull() ?: 1.0f) },
            keyboardType = KeyboardType.Decimal
        )

        if (uiState.samplerType == LanguageViewModel.SamplerType.TOP_K) {
            NumberField(
                label = "Top-K",
                value = uiState.topK.toString(),
                onValueChange = { onTopKChange(it.toIntOrNull() ?: 50) },
                keyboardType = KeyboardType.Number
            )
        }

        if (uiState.samplerType == LanguageViewModel.SamplerType.TOP_P) {
            NumberField(
                label = "Top-P",
                value = "%.2f".format(uiState.topP),
                onValueChange = { onTopPChange(it.toFloatOrNull() ?: 0.9f) },
                keyboardType = KeyboardType.Decimal
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Checkbox(
                checked = uiState.stripPrompt,
                onCheckedChange = onStripPromptChange
            )
            Text(
                text = "Strip prompt from output",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KerasRed,
            focusedLabelColor = KerasRed,
            cursorColor = KerasRed
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        )
    )
}
