package com.example.kerasondevice.data.inference

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.BenchmarkInfo
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Information about the loaded model file.
 */
data class ModelInfo(
    val path: String,
    val sizeBytes: Long,
    val sizeText: String
)

/**
 * Benchmark stats captured after a generation turn.
 */
data class BenchmarkStats(
    val prefillTokens: Int,
    val decodeTokens: Int,
    val prefillTps: Double,
    val decodeTps: Double,
    val timeToFirstTokenSec: Double
)

/**
 * Singleton-friendly wrapper around the LiteRT-LM Engine.
 *
 * The engine is created once and can vend multiple [Conversation] instances
 * (or a single long-lived one) via [createConversation].
 *
 * **Bucketing support:** This engine works with both single-signature
 * (legacy `prefill` / `decode`) and multi-signature bucketed models
 * (e.g. `prefill_32`, `prefill_64`, …). Signature selection is handled
 * transparently by the LiteRT-LM runtime. We enable
 * [ExperimentalFlags.enableBenchmark] so that [getLastBenchmark] can report
 * the actual prefill token count, which indirectly confirms which bucket
 * was active.
 */
class LiteRTLMInferenceEngine private constructor(
    private val appContext: Context,
    private val modelFile: File
) {

    private var engine: Engine? = null
    private var _modelInfo: ModelInfo? = null
    private var _lastBenchmark: BenchmarkStats? = null

    val isReady: Boolean
        get() = engine != null

    val modelInfo: ModelInfo?
        get() = _modelInfo

    val lastBenchmark: BenchmarkStats?
        get() = _lastBenchmark

    /**
     * Initialises the underlying native engine. Safe to call multiple times;
     * subsequent calls are no-ops once [isReady] is true.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isReady) return@withContext

        Log.d(TAG, "initialize() start")
        Log.d(TAG, "Model file: ${modelFile.absolutePath}, size=${ModelLocator.formatBytes(modelFile.length())}")

        _modelInfo = ModelInfo(
            path = modelFile.absolutePath,
            sizeBytes = modelFile.length(),
            sizeText = ModelLocator.formatBytes(modelFile.length())
        )

        // Enable benchmark collection so we can verify bucketing via token counts.
        @OptIn(ExperimentalApi::class)
        ExperimentalFlags.enableBenchmark = true
        Log.d(TAG, "Benchmark mode enabled")

        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = appContext.cacheDir.absolutePath
        )
        Log.d(TAG, "Creating Engine...")

        val eng = Engine(config)
        Log.d(TAG, "Engine created, calling initialize()...")
        eng.initialize()
        Log.d(TAG, "Engine initialized.")

        engine = eng
    }

    /**
     * Creates a new [com.google.ai.edge.litertlm.Conversation] backed by this engine.
     * The caller is responsible for closing the conversation when done.
     */
    fun createConversation(): com.google.ai.edge.litertlm.Conversation {
        val eng = engine ?: throw IllegalStateException("Engine not initialized")
        Log.d(TAG, "Creating conversation...")
        return eng.createConversation().also {
            Log.d(TAG, "Conversation created.")
        }
    }

    /**
     * Capture benchmark info from a [conversation] after generation completes.
     * Call this after [com.google.ai.edge.litertlm.Conversation.sendMessageAsync]
     * finishes to populate [lastBenchmark].
     */
    @OptIn(ExperimentalApi::class)
    fun captureBenchmark(conversation: com.google.ai.edge.litertlm.Conversation) {
        try {
            val info: BenchmarkInfo = conversation.getBenchmarkInfo()
            _lastBenchmark = BenchmarkStats(
                prefillTokens = info.lastPrefillTokenCount,
                decodeTokens = info.lastDecodeTokenCount,
                prefillTps = info.lastPrefillTokensPerSecond,
                decodeTps = info.lastDecodeTokensPerSecond,
                timeToFirstTokenSec = info.timeToFirstTokenInSecond
            )
            Log.d(
                TAG,
                "Benchmark — prefill=${info.lastPrefillTokenCount} tok " +
                        "(${String.format("%.1f", info.lastPrefillTokensPerSecond)} tps), " +
                        "decode=${info.lastDecodeTokenCount} tok " +
                        "(${String.format("%.1f", info.lastDecodeTokensPerSecond)} tps), " +
                        "TTFT=${String.format("%.2f", info.timeToFirstTokenInSecond)}s"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to capture benchmark info", e)
        }
    }

    fun close() {
        Log.d(TAG, "close() called")
        try {
            engine?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing engine", e)
        }
        engine = null
        _lastBenchmark = null
    }

    companion object {
        private const val TAG = "LiteRTLMInferenceEngine"

        @Volatile
        private var INSTANCE: LiteRTLMInferenceEngine? = null

        /**
         * Returns the singleton instance, creating it if necessary.
         *
         * @param context Application context.
         * @param modelFile Optional explicit model file. When null, the engine
         *   auto-discovers the first available `.litertlm` file.
         */
        fun getInstance(context: Context, modelFile: File? = null): LiteRTLMInferenceEngine {
            val existing = INSTANCE
            if (existing != null && (modelFile == null || existing.modelFile == modelFile)) {
                return existing
            }
            return synchronized(this) {
                val current = INSTANCE
                if (current != null && (modelFile == null || current.modelFile == modelFile)) {
                    current
                } else {
                    // If switching models, tear down old instance first.
                    current?.close()
                    val resolved = modelFile
                        ?: ModelLocator.findModelFile(context.applicationContext)
                        ?: throw IllegalStateException("No .litertlm model found on device")
                    LiteRTLMInferenceEngine(context.applicationContext, resolved).also {
                        INSTANCE = it
                    }
                }
            }
        }

        /** Closes and drops the singleton instance. */
        fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
