package com.example.kerasondevice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kerasondevice.ui.theme.KerasRed
import com.example.kerasondevice.ui.theme.OnSurfaceDark
import com.example.kerasondevice.ui.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KerasTopBar(
    modelName: String? = null,
    modelSize: String? = null,
    availableModels: List<Pair<String, String>> = emptyList(),
    onModelSelected: ((Int) -> Unit)? = null,
    onNewChat: (() -> Unit)? = null
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(KerasRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        color = OnSurfaceDark,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = "LiteRT-LM Demo",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (modelName != null) {
                        Text(
                            text = buildString {
                                append(modelName)
                                if (modelSize != null) append(" · $modelSize")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            // Model selector dropdown
            if (availableModels.isNotEmpty() && onModelSelected != null) {
                Box {
                    IconButton(onClick = { modelMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select model",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        availableModels.forEachIndexed { index, (name, size) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = size,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    modelMenuExpanded = false
                                    onModelSelected(index)
                                }
                            )
                        }
                    }
                }
            }

            if (onNewChat != null) {
                IconButton(onClick = onNewChat) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
