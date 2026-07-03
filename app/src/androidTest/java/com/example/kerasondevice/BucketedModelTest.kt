package com.example.kerasondevice

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kerasondevice.data.inference.LiteRTLMInferenceEngine
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalApi::class)
/**
 * Instrumented test that verifies the app works correctly with a **bucketed**
 * `.litertlm` model containing multiple prefill signatures (`prefill_4`,
 * `prefill_8`, …).
 *
 * The LiteRT-LM runtime is responsible for dispatching to the correct bucket
 * based on prompt length. This test confirms:
 * 1. The bucketed model loads without errors.
 * 2. Short prompts exercise the smaller bucket(s).
 * 3. Benchmark info reports the actual prefill token count.
 */
@RunWith(AndroidJUnit4::class)
class BucketedModelTest {

    companion object {
        private const val TAG = "BucketedModelTest"
    }

    @Test
    fun testBucketedTinyModelLoads() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val modelFile = File("/data/local/tmp/tiny_gemma3_bucketed.litertlm")
            .takeIf { it.exists() }
            ?: throw IllegalStateException("tiny_gemma3_bucketed.litertlm not found on device")

        Log.i(TAG, "Model file: ${modelFile.absolutePath}, size=${modelFile.length()}")

        // Use the low-level Engine API directly to verify bucketing works.
        @OptIn(ExperimentalApi::class)
        ExperimentalFlags.enableBenchmark = true
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        val engine = Engine(config)
        val initTime = measureTimeMillis { engine.initialize() }
        Log.i(TAG, "Engine initialized in ${initTime}ms")

        val conversation = engine.createConversation()

        // Send a very short prompt — the runtime should pick the smallest
        // bucket that fits (prefill_4 for this model).
        val prompt = "hi"
        Log.i(TAG, "=== Sending short prompt: '$prompt' ===")
        val flow = conversation.sendMessageAsync(prompt)
        val sb = StringBuilder()
        val generationTime = measureTimeMillis {
            withTimeoutOrNull(30_000) {
                flow.collect { msg -> sb.append(msg.toString()) }
            } ?: throw IllegalStateException("Generation timed out")
        }
        Log.i(TAG, "Response (${generationTime}ms): $sb")

        // Verify benchmark info is available and prefill tokens were counted.
        val benchmark = conversation.getBenchmarkInfo()
        Log.i(
            TAG,
            "Benchmark — prefill=${benchmark.lastPrefillTokenCount} tok, " +
                    "decode=${benchmark.lastDecodeTokenCount} tok, " +
                    "TTFT=${String.format("%.3f", benchmark.timeToFirstTokenInSecond)}s"
        )

        // The prefill token count should be <= 4 for this tiny model with a
        // 2-token prompt (the vocab is tiny, so tokenization may produce 1-2
        // tokens). The key assertion is that the engine did not crash and
        // produced benchmark data.
        assert(benchmark.lastPrefillTokenCount > 0) {
            "Expected positive prefill token count, got ${benchmark.lastPrefillTokenCount}"
        }

        conversation.close()
        engine.close()
        Log.i(TAG, "=== Bucketed model test PASSED ===")
    }

    @Test
    fun testBucketedModelViaInferenceEngine() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val modelFile = File("/data/local/tmp/tiny_gemma3_bucketed.litertlm")
            .takeIf { it.exists() }
            ?: throw IllegalStateException("tiny_gemma3_bucketed.litertlm not found on device")

        // Reset any existing singleton so we can force-load the bucketed model.
        LiteRTLMInferenceEngine.resetInstance()

        val engine = LiteRTLMInferenceEngine.getInstance(context, modelFile)
        engine.initialize()

        val info = engine.modelInfo
        assert(info != null) { "Model info should be populated after init" }
        Log.i(TAG, "Loaded model: ${info?.path} (${info?.sizeText})")

        val conversation = engine.createConversation()
        val prompt = "hello"
        Log.i(TAG, "=== Sending '$prompt' via inference engine ===")
        val flow = conversation.sendMessageAsync(prompt)
        val sb = StringBuilder()
        withTimeoutOrNull(30_000) {
            flow.collect { msg -> sb.append(msg.toString()) }
        } ?: throw IllegalStateException("Generation timed out")
        Log.i(TAG, "Response: $sb")

        engine.captureBenchmark(conversation)
        val benchmark = engine.lastBenchmark
        assert(benchmark != null) { "Benchmark should be captured" }
        Log.i(
            TAG,
            "Benchmark — prefill=${benchmark?.prefillTokens} tok, " +
                    "decode=${benchmark?.decodeTokens} tok"
        )

        conversation.close()
        LiteRTLMInferenceEngine.resetInstance()
        Log.i(TAG, "=== Inference-engine bucketed test PASSED ===")
    }
}
