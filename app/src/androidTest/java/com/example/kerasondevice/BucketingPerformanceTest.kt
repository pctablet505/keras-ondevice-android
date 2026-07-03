package com.example.kerasondevice

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Measures the real performance difference between a single-signature model
 * (fixed prefill length) and a bucketed model (multiple prefill signatures).
 *
 * Hypothesis: for short prompts, the bucketed model should have a lower
 * time-to-first-token (TTFT) because it dispatches to a smaller prefill
 * signature (e.g. prefill_32) instead of padding to the fixed length.
 */
@OptIn(ExperimentalApi::class)
@RunWith(AndroidJUnit4::class)
class BucketingPerformanceTest {

    companion object {
        private const val TAG = "BucketingPerfTest"
        private const val TIMEOUT_MS = 120_000L
    }

    data class Result(
        val modelName: String,
        val prompt: String,
        val initTimeMs: Long,
        val generationTimeMs: Long,
        val ttftMs: Double,
        val prefillTokens: Int,
        val decodeTokens: Int,
    )

    private fun loadAndGenerate(modelFile: File, prompt: String): Result {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Log.i(TAG, "Loading ${modelFile.name} (${modelFile.length()} bytes)")

        ExperimentalFlags.enableBenchmark = true
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        val engine = Engine(config)
        val initTime = measureTimeMillis { engine.initialize() }
        Log.i(TAG, "Engine init: ${initTime}ms")

        val conversation = engine.createConversation()
        val sb = StringBuilder()

        val generationTime = measureTimeMillis {
            val flow = conversation.sendMessageAsync(prompt)
            runBlocking {
                withTimeoutOrNull(TIMEOUT_MS) {
                    flow.collect { msg -> sb.append(msg.toString()) }
                } ?: throw IllegalStateException("Generation timed out")
            }
        }

        val benchmark = conversation.getBenchmarkInfo()
        val result = Result(
            modelName = modelFile.name,
            prompt = prompt,
            initTimeMs = initTime,
            generationTimeMs = generationTime,
            ttftMs = benchmark.timeToFirstTokenInSecond * 1000.0,
            prefillTokens = benchmark.lastPrefillTokenCount,
            decodeTokens = benchmark.lastDecodeTokenCount,
        )

        Log.i(
            TAG,
            "Result: model=${result.modelName}, " +
                    "prefill=${result.prefillTokens} tok, decode=${result.decodeTokens} tok, " +
                    "TTFT=${String.format("%.1f", result.ttftMs)}ms, " +
                    "gen=${result.generationTimeMs}ms, " +
                    "response=${sb.take(40)}"
        )

        conversation.close()
        engine.close()
        return result
    }

    @Test
    fun compareShortPrompt() = runBlocking<Unit> {
        val shortPrompt = "Hello"
        Log.i(TAG, "========== SHORT PROMPT: '$shortPrompt' ==========")

        val fixed128 = File("/data/local/tmp/gemma3_270m_it.litertlm")
            .takeIf { it.exists() } ?: throw IllegalStateException("fixed 128 model not found")

        val bucketed = File("/data/local/tmp/gemma3_270m_it_bucketed.litertlm")
            .takeIf { it.exists() } ?: throw IllegalStateException("bucketed model not found")

        val r1 = loadAndGenerate(fixed128, shortPrompt)
        delay(2000)
        val r2 = loadAndGenerate(bucketed, shortPrompt)

        Log.i(TAG, "========== COMPARISON ==========")
        Log.i(TAG, "Fixed-128:  TTFT=${String.format("%.1f", r1.ttftMs)}ms, gen=${r1.generationTimeMs}ms")
        Log.i(TAG, "Bucketed:   TTFT=${String.format("%.1f", r2.ttftMs)}ms, gen=${r2.generationTimeMs}ms")
        val ttftDiff = r1.ttftMs - r2.ttftMs
        val ttftPct = if (r1.ttftMs > 0) (ttftDiff / r1.ttftMs * 100) else 0.0
        Log.i(TAG, "TTFT delta: ${String.format("%.1f", ttftDiff)}ms (${String.format("%.1f", ttftPct)}% faster with bucketing)")

        assert(r2.ttftMs <= r1.ttftMs * 1.20) {
            "Bucketed TTFT (${r2.ttftMs}ms) should not be more than 20% slower than fixed-128 (${r1.ttftMs}ms)"
        }
        Log.i(TAG, "========== PASSED ==========")
    }

    @Test
    fun compareMediumPrompt() = runBlocking<Unit> {
        val mediumPrompt = "Can you explain the concept of machine learning " +
                "in simple terms for a beginner who has no background in computer science?"
        Log.i(TAG, "========== MEDIUM PROMPT ==========")

        val fixed128 = File("/data/local/tmp/gemma3_270m_it.litertlm")
            .takeIf { it.exists() } ?: throw IllegalStateException("fixed 128 model not found")

        val bucketed = File("/data/local/tmp/gemma3_270m_it_bucketed.litertlm")
            .takeIf { it.exists() } ?: throw IllegalStateException("bucketed model not found")

        val r1 = loadAndGenerate(fixed128, mediumPrompt)
        delay(2000)
        val r2 = loadAndGenerate(bucketed, mediumPrompt)

        Log.i(TAG, "========== COMPARISON ==========")
        Log.i(TAG, "Fixed-128:  TTFT=${String.format("%.1f", r1.ttftMs)}ms, prefill=${r1.prefillTokens} tok")
        Log.i(TAG, "Bucketed:   TTFT=${String.format("%.1f", r2.ttftMs)}ms, prefill=${r2.prefillTokens} tok")
        val ttftDiff = r1.ttftMs - r2.ttftMs
        val ttftPct = if (r1.ttftMs > 0) (ttftDiff / r1.ttftMs * 100) else 0.0
        Log.i(TAG, "TTFT delta: ${String.format("%.1f", ttftDiff)}ms (${String.format("%.1f", ttftPct)}% faster with bucketing)")

        assert(r2.ttftMs <= r1.ttftMs * 1.20) {
            "Bucketed TTFT (${r2.ttftMs}ms) should not be more than 20% slower than fixed-128 (${r1.ttftMs}ms)"
        }
        Log.i(TAG, "========== PASSED ==========")
    }

    @Test
    fun compareFixed256() = runBlocking<Unit> {
        val shortPrompt = "Hello"
        Log.i(TAG, "========== FIXED-256 vs BUCKETED (short prompt) ==========")

        val fixed256 = File("/data/local/tmp/gemma3_270m_it_prefill_256.litertlm")
            .takeIf { it.exists() } ?: throw IllegalStateException("fixed 256 model not found")

        val bucketed = File("/data/local/tmp/gemma3_270m_it_bucketed.litertlm")
            .takeIf { it.exists() } ?: throw IllegalStateException("bucketed model not found")

        val r1 = loadAndGenerate(fixed256, shortPrompt)
        delay(2000)
        val r2 = loadAndGenerate(bucketed, shortPrompt)

        Log.i(TAG, "========== COMPARISON ==========")
        Log.i(TAG, "Fixed-256:  TTFT=${String.format("%.1f", r1.ttftMs)}ms")
        Log.i(TAG, "Bucketed:   TTFT=${String.format("%.1f", r2.ttftMs)}ms")
        val ttftDiff = r1.ttftMs - r2.ttftMs
        val ttftPct = if (r1.ttftMs > 0) (ttftDiff / r1.ttftMs * 100) else 0.0
        Log.i(TAG, "TTFT delta: ${String.format("%.1f", ttftDiff)}ms (${String.format("%.1f", ttftPct)}% faster with bucketing)")
        Log.i(TAG, "========== PASSED ==========")
    }
}
