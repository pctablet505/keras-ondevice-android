package com.example.kerasondevice.inference.language

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Optimised text-generation engine backed by the LiteRT-LM runtime.
 *
 * This wraps [com.google.ai.edge.litertlm.Engine] and [Conversation] and
 * exposes a simple streaming [generate] API that can be compared against the
 * low-level custom [LiteRTTextEngine] path.
 */
class LiteRTLMTextEngine private constructor(private val engine: Engine) {

    private val conversation: Conversation = engine.createConversation()

    /**
     * Streams a response for [prompt]. [onToken] is invoked for every decoded
     * token. The returned [GenerationResult] contains the token count and
     * latency; the full text is accumulated by the caller.
     */
    suspend fun generate(
        prompt: String,
        onToken: (String) -> Unit
    ): GenerationResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var tokenCount = 0
        conversation.sendMessageAsync(prompt).collect { token ->
            tokenCount++
            onToken(token.toString())
        }
        val latency = System.currentTimeMillis() - start
        GenerationResult("", tokenCount, latency)
    }

    fun close() {
        try {
            conversation.close()
            engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing engine", e)
        }
    }

    data class GenerationResult(
        val text: String,
        val tokenCount: Int,
        val latencyMs: Long
    )

    companion object {
        private const val TAG = "LiteRTLMTextEngine"

        /**
         * Creates a new engine instance for the given model file.
         * Callers are responsible for closing the returned engine.
         */
        fun create(context: Context, modelFile: File): LiteRTLMTextEngine {
            @OptIn(ExperimentalApi::class)
            ExperimentalFlags.enableBenchmark = true

            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )
            Log.d(TAG, "Creating Engine for ${modelFile.absolutePath}")
            val engine = Engine(config)
            engine.initialize()
            Log.d(TAG, "Engine initialized")
            return LiteRTLMTextEngine(engine)
        }
    }
}
