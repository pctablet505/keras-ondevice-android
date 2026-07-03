package com.example.kerasondevice.ui.language

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.model.ModelLocator
import com.example.kerasondevice.inference.language.LiteRTLMTextEngine
import com.example.kerasondevice.inference.language.LiteRTTextEngine
import com.example.kerasondevice.inference.sampler.GreedySampler
import com.example.kerasondevice.inference.sampler.TopKSampler
import com.example.kerasondevice.inference.sampler.TopPSampler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the side-by-side language surface.
 *
 * Supports two modes:
 * - [Mode.LITERT]: low-level custom interpreter + SentencePiece + hand-rolled samplers.
 * - [Mode.LITERTLM]: optimized LiteRT-LM engine.
 *
 * History is isolated per mode because the two runtimes have incompatible
 * tokenization and cache state.
 */
class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    enum class Mode { LITERT, LITERTLM }
    enum class SamplerType { GREEDY, TOP_K, TOP_P }

    data class UiState(
        val mode: Mode = Mode.LITERTLM,
        val prompt: String = "",
        val output: String = "",
        val isGenerating: Boolean = false,
        val stats: String = "",
        val maxLength: Int = 32,
        val samplerType: SamplerType = SamplerType.GREEDY,
        val temperature: Float = 1.0f,
        val topK: Int = 50,
        val topP: Float = 0.9f,
        val stripPrompt: Boolean = true,
        val availableModels: List<ModelHandle> = emptyList(),
        val selectedModelIndex: Int = 0,
        val modelError: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val historyLitert = mutableListOf<String>()
    private val historyLitertlm = mutableListOf<String>()

    init {
        val context = getApplication<Application>()
        val allModels = ModelLocator(context).discover()
        val languageModels = allModels.filter { isLanguageModel(it) }
        val initialIndex = selectIndexForMode(languageModels, _state.value.mode)
        _state.value = _state.value.copy(
            availableModels = languageModels,
            selectedModelIndex = initialIndex
        ).withModelError()
    }

    fun setMode(mode: Mode) {
        val newIndex = selectIndexForMode(_state.value.availableModels, mode)
        _state.value = _state.value.copy(
            mode = mode,
            output = "",
            stats = "",
            error = null,
            selectedModelIndex = newIndex
        ).withModelError()
    }

    fun setPrompt(prompt: String) {
        _state.value = _state.value.copy(prompt = prompt, error = null)
    }

    fun setMaxLength(value: Int) {
        _state.value = _state.value.copy(maxLength = value.coerceAtLeast(1))
    }

    fun setSamplerType(type: SamplerType) {
        _state.value = _state.value.copy(samplerType = type)
    }

    fun setTemperature(value: Float) {
        _state.value = _state.value.copy(temperature = value.coerceAtLeast(0.01f))
    }

    fun setTopK(value: Int) {
        _state.value = _state.value.copy(topK = value.coerceAtLeast(1))
    }

    fun setTopP(value: Float) {
        _state.value = _state.value.copy(topP = value.coerceIn(0.0f, 1.0f))
    }

    fun setStripPrompt(strip: Boolean) {
        _state.value = _state.value.copy(stripPrompt = strip)
    }

    fun selectModel(index: Int) {
        val validIndex = index.coerceIn(0, (_state.value.availableModels.size - 1).coerceAtLeast(0))
        _state.value = _state.value.copy(
            selectedModelIndex = validIndex,
            output = "",
            stats = "",
            error = null
        ).withModelError()
    }

    fun generate() {
        val current = _state.value
        if (current.prompt.isBlank() || current.isGenerating) return
        if (current.availableModels.getOrNull(current.selectedModelIndex) == null) return
        if (current.modelError != null) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = current.copy(isGenerating = true, output = "", stats = "", error = null)
            try {
                when (current.mode) {
                    Mode.LITERT -> generateLitert(current)
                    Mode.LITERTLM -> generateLitertlm(current)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                _state.value = current.copy(
                    isGenerating = false,
                    error = e.message ?: "Generation error"
                )
            }
        }
    }

    private fun generateLitert(current: UiState) {
        val context = getApplication<Application>()
        val model = current.availableModels[current.selectedModelIndex]
        val vocabFile = model.file.resolveSibling("vocabulary.spm")

        if (!model.file.exists()) {
            throw IllegalStateException("Model not found: ${model.file.name}. Push it to the app files dir.")
        }
        if (!vocabFile.exists()) {
            throw IllegalStateException("Missing vocabulary.spm")
        }

        val sampler = when (current.samplerType) {
            SamplerType.GREEDY -> GreedySampler(current.temperature)
            SamplerType.TOP_K -> TopKSampler(current.topK, current.temperature)
            SamplerType.TOP_P -> TopPSampler(current.topP, current.temperature)
        }

        LiteRTTextEngine(context, model.file, vocabFile).use { engine ->
            val result = engine.generate(
                prompt = current.prompt,
                maxLength = current.maxLength,
                sampler = sampler,
                stripPrompt = current.stripPrompt
            )

            historyLitert.add(result.text)
            _state.value = current.copy(
                output = result.text,
                isGenerating = false,
                stats = "${result.tokenCount} tokens in ${result.latencyMs} ms"
            )
        }
    }

    private suspend fun generateLitertlm(current: UiState) {
        val context = getApplication<Application>()
        val model = current.availableModels[current.selectedModelIndex]

        if (!model.file.exists()) {
            throw IllegalStateException("Model not found: ${model.file.name}. Push it to the app files dir.")
        }

        LiteRTLMTextEngine.create(context, model.file).use { engine ->
            val sb = StringBuilder()
            val result = engine.generate(current.prompt) { token ->
                sb.append(token)
                _state.value = current.copy(output = sb.toString())
            }

            historyLitertlm.add(sb.toString())
            _state.value = current.copy(
                isGenerating = false,
                stats = "${result.tokenCount} tokens in ${result.latencyMs} ms"
            )
        }
    }

    private fun isLanguageModel(handle: ModelHandle): Boolean {
        if (handle.format == ModelFormat.LITERTLM) return true
        if (handle.format == ModelFormat.TFLITE) {
            if (handle.config.task.equals("text_generation", ignoreCase = true)) return true
            if (handle.file.name.equals("gemma3_270m.tflite", ignoreCase = true)) return true
        }
        return false
    }

    private fun matchesMode(handle: ModelHandle, mode: Mode): Boolean = when (mode) {
        Mode.LITERT -> handle.format == ModelFormat.TFLITE
        Mode.LITERTLM -> handle.format == ModelFormat.LITERTLM
    }

    private fun selectIndexForMode(models: List<ModelHandle>, mode: Mode): Int {
        val modeIndex = models.indexOfFirst { matchesMode(it, mode) }
        return if (modeIndex >= 0) modeIndex else 0
    }

    private fun UiState.withModelError(): UiState {
        val model = availableModels.getOrNull(selectedModelIndex)
        val error = when {
            model == null -> null
            mode == Mode.LITERT && model.format == ModelFormat.TFLITE -> {
                val vocabFile = model.file.resolveSibling("vocabulary.spm")
                if (!vocabFile.exists()) "Missing vocabulary.spm" else null
            }
            else -> null
        }
        return copy(modelError = error)
    }

    companion object {
        private const val TAG = "LanguageViewModel"
    }
}
