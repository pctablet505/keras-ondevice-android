package com.example.kerasondevice.ui.task

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.model.ModelLocator
import com.example.kerasondevice.domain.task.BoundingBox
import com.example.kerasondevice.domain.task.Classification
import com.example.kerasondevice.domain.task.DepthResult
import com.example.kerasondevice.domain.task.MaskResult
import com.example.kerasondevice.domain.task.OutputType
import com.example.kerasondevice.domain.task.Task
import com.example.kerasondevice.domain.task.TaskRegistry
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.ui.components.renderers.ClassificationRenderer
import com.example.kerasondevice.ui.components.renderers.DepthRenderer
import com.example.kerasondevice.ui.components.renderers.DetectionRenderer
import com.example.kerasondevice.ui.components.renderers.MaskRenderer
import com.example.kerasondevice.ui.theme.KerasRed
import com.example.kerasondevice.ui.theme.SurfaceDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Generic vision task screen.
 *
 * @param task The task to run. Input must be [com.example.kerasondevice.domain.task.InputType.Gallery].
 * @param onBack Invoked when the user presses the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    task: Task<*, *>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    @Suppress("UNCHECKED_CAST")
    val typedTask = task as Task<Bitmap, Any>

    val viewModel: TaskViewModel = viewModel(
        factory = TaskViewModel.Factory(context.applicationContext as Application, typedTask)
    )
    val uiState by viewModel.uiState.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmap(context.contentResolver, it)
            bitmap?.let(viewModel::setImage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(task.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModelSelector(
                models = viewModel.models,
                selectedIndex = uiState.selectedModelIndex,
                onSelect = viewModel::selectModel
            )

            InputCard(
                image = uiState.selectedImage,
                onPickImage = { galleryLauncher.launch("image/*") },
                onClear = viewModel::clearImage
            )

            Button(
                onClick = { viewModel.run() },
                enabled = uiState.selectedImage != null &&
                    viewModel.models.getOrNull(uiState.selectedModelIndex) != null &&
                    !uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KerasRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Run ${task.name}")
                }
            }

            OutputArea(task = task, result = uiState.result)

            StatsFooter(task = task, result = uiState.result)
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<ModelHandle>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = models.getOrNull(selectedIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Model",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selected?.file?.name ?: "No model available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (models.size > 1) {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select model"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        models.forEachIndexed { index, model ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = model.file.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onSelect(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    image: Bitmap?,
    onPickImage: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "No image selected",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = if (image != null) "Change image" else "Pick from gallery",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (image != null) {
                    Button(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputArea(
    task: Task<*, *>,
    result: TaskResult<Any>?
) {
    when (result) {
        is TaskResult.Success -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                when (task.outputType) {
                    is OutputType.ClassificationList -> {
                        @Suppress("UNCHECKED_CAST")
                        ClassificationRenderer(result.output as List<Classification>)
                    }
                    is OutputType.BoundingBoxList -> {
                        @Suppress("UNCHECKED_CAST")
                        DetectionRenderer(result.output as List<BoundingBox>)
                    }
                    is OutputType.Mask -> MaskRenderer(result.output as MaskResult)
                    is OutputType.DepthMap -> DepthRenderer(result.output as DepthResult)
                }
            }
        }
        is TaskResult.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = result.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        null -> { }
    }
}

@Composable
private fun StatsFooter(
    task: Task<*, *>,
    result: TaskResult<Any>?
) {
    val text = when (result) {
        is TaskResult.Success -> {
            val shapeText = when (task.outputType) {
                is OutputType.ClassificationList -> {
                    @Suppress("UNCHECKED_CAST")
                    "${(result.output as List<*>).size} classes"
                }
                is OutputType.BoundingBoxList -> {
                    @Suppress("UNCHECKED_CAST")
                    "${(result.output as List<*>).size} detections"
                }
                is OutputType.Mask -> {
                    val mask = result.output as MaskResult
                    "${mask.width}x${mask.height}"
                }
                is OutputType.DepthMap -> {
                    val depth = result.output as DepthResult
                    "${depth.width}x${depth.height}"
                }
            }
            "Latency: ${result.latencyMs} ms · Output: $shapeText"
        }
        is TaskResult.Error -> "Error"
        null -> "Ready"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

class TaskViewModel(
    application: Application,
    private val task: Task<Bitmap, Any>
) : AndroidViewModel(application) {

    val models: List<ModelHandle>

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val selectedModelIndex: Int = 0,
        val selectedImage: Bitmap? = null,
        val result: TaskResult<Any>? = null,
        val isRunning: Boolean = false
    )

    init {
        models = ModelLocator(application).discover().filter { TaskRegistry.matches(task, it) }
    }

    fun selectModel(index: Int) {
        _uiState.update {
            it.copy(
                selectedModelIndex = index.coerceIn(0, (models.size - 1).coerceAtLeast(0)),
                result = null
            )
        }
    }

    fun setImage(bitmap: Bitmap) {
        _uiState.update { it.copy(selectedImage = bitmap, result = null) }
    }

    fun clearImage() {
        _uiState.update { it.copy(selectedImage = null, result = null) }
    }

    fun run() {
        val current = _uiState.value
        val model = models.getOrNull(current.selectedModelIndex)
        val image = current.selectedImage
        if (model == null || image == null || current.isRunning) return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isRunning = true, result = null) }
            val result = task.run(model, image)
            _uiState.update { it.copy(isRunning = false, result = result) }
        }
    }

    class Factory(
        private val application: Application,
        private val task: Task<Bitmap, Any>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(application, task) as T
        }
    }
}

private fun loadBitmap(contentResolver: android.content.ContentResolver, uri: Uri): Bitmap? =
    try {
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }
