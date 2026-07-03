package com.example.kerasondevice.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kerasondevice.data.inference.BenchmarkStats
import com.example.kerasondevice.data.inference.LiteRTLMInferenceEngine
import com.example.kerasondevice.data.inference.ModelInfo
import com.example.kerasondevice.data.inference.ModelLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,               // "user" or "assistant"
    val text: String = "",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val benchmark: BenchmarkStats? = null
)

data class AvailableModel(
    val name: String,
    val file: File,
    val sizeText: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEngineReady: Boolean = false,
    val modelInfo: ModelInfo? = null,
    val availableModels: List<AvailableModel> = emptyList()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private var engine = LiteRTLMInferenceEngine.getInstance(application)
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val models = ModelLocator.listAvailableModels(application).map { file ->
            AvailableModel(
                name = file.name,
                file = file,
                sizeText = ModelLocator.formatBytes(file.length())
            )
        }
        _uiState.update { it.copy(availableModels = models) }

        viewModelScope.launch {
            initEngine()
        }
    }

    private suspend fun initEngine() {
        try {
            _uiState.update { it.copy(isEngineReady = false, error = null) }
            engine.initialize()
            conversation = engine.createConversation()
            _uiState.update {
                it.copy(
                    isEngineReady = true,
                    modelInfo = engine.modelInfo
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Engine initialization failed", e)
            _uiState.update {
                it.copy(
                    isEngineReady = false,
                    error = e.message ?: "Failed to initialize inference engine"
                )
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isLoading || !engine.isReady) return

        val userMessage = ChatMessage(role = "user", text = prompt)
        val assistantMessage = ChatMessage(role = "assistant", isGenerating = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + assistantMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val sb = StringBuilder()
            try {
                val conv = conversation
                    ?: throw IllegalStateException("Conversation not initialized")
                val flow = conv.sendMessageAsync(prompt)

                flow.collect { token ->
                    sb.append(token.toString())
                    updateLastAssistantMessage(sb.toString(), isGenerating = true)
                }

                // Capture benchmark after generation finishes.
                engine.captureBenchmark(conv)
                val benchmark = engine.lastBenchmark

                updateLastAssistantMessage(
                    text = sb.toString(),
                    isGenerating = false,
                    benchmark = benchmark
                )
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                updateLastAssistantMessage(
                    text = sb.toString(),
                    isGenerating = false,
                    error = e.message ?: "Generation error"
                )
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearConversation() {
        viewModelScope.launch {
            conversation?.close()
            conversation = engine.createConversation()
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    inputText = "",
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Switch to a different model file. Closes the current engine,
     * creates a new one with the selected model, and reinitialises.
     */
    fun switchModel(model: AvailableModel) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isEngineReady = false,
                    messages = emptyList(),
                    inputText = "",
                    isLoading = false,
                    error = null
                )
            }
            conversation?.close()
            conversation = null

            // Tear down old singleton and create new engine with selected file.
            LiteRTLMInferenceEngine.resetInstance()
            engine = LiteRTLMInferenceEngine.getInstance(getApplication(), model.file)

            initEngine()
        }
    }

    private fun updateLastAssistantMessage(
        text: String,
        isGenerating: Boolean,
        error: String? = null,
        benchmark: BenchmarkStats? = null
    ) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            if (messages.isNotEmpty() && messages.last().role == "assistant") {
                messages[messages.lastIndex] = messages.last().copy(
                    text = text,
                    isGenerating = isGenerating,
                    error = error,
                    benchmark = benchmark
                )
            }
            state.copy(messages = messages)
        }
    }

    override fun onCleared() {
        super.onCleared()
        conversation?.close()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
