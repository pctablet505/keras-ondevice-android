package com.example.kerasondevice

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Backend
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class LiteRTLMEngineTest {

    companion object {
        private const val TAG = "LiteRTLMEngineTest"
    }

    @Test
    fun testTinyGemma3nModelLoads() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val modelFile = File("/data/local/tmp/tiny_gemma3n.litertlm")
            .takeIf { it.exists() }
            ?: run {
                assumeTrue("tiny_gemma3n.litertlm not found on device", false)
                return@runBlocking
            }

        Log.i(TAG, "Model file: ${modelFile.absolutePath}, size=${modelFile.length()}")

        Log.i(TAG, "Creating engine for tiny Gemma3n...")
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        val engine = Engine(config)
        val initTime = measureTimeMillis {
            engine.initialize()
        }
        Log.i(TAG, "Engine initialized in ${initTime}ms")

        Log.i(TAG, "Creating conversation...")
        val conversation = engine.createConversation()

        val prompt = "hi"
        Log.i(TAG, "=== Sending '$prompt' ===")
        val flow = conversation.sendMessageAsync(prompt)
        val sb = StringBuilder()
        val generationTime = measureTimeMillis {
            withTimeoutOrNull(60_000) {
                flow.collect { msg ->
                    sb.append(msg.toString())
                }
            } ?: throw IllegalStateException("Generation timed out")
        }
        Log.i(TAG, "Response (${generationTime}ms): $sb")

        conversation.close()
        engine.close()
        Log.i(TAG, "=== Tiny Gemma3n test PASSED ===")
    }

    @Test
    fun testMultiTurnConversation() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Find model file in various locations
        val modelFile = File(context.getExternalFilesDir(null), "gemma3_270m_it.litertlm")
            .takeIf { it.exists() }
            ?: File(context.filesDir, "gemma3_270m_it.litertlm")
                .takeIf { it.exists() }
            ?: File("/data/local/tmp/gemma3_270m_it.litertlm")
                .takeIf { it.exists() }
            ?: run {
                assumeTrue("gemma3_270m_it.litertlm not found on device", false)
                return@runBlocking
            }

        Log.i(TAG, "Model file: ${modelFile.absolutePath}, size=${modelFile.length()}")

        Log.i(TAG, "Creating engine...")
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        val engine = Engine(config)
        val initTime = measureTimeMillis {
            engine.initialize()
        }
        Log.i(TAG, "Engine initialized in ${initTime}ms")

        Log.i(TAG, "Creating conversation...")
        val conversation = engine.createConversation()

        val prompts = listOf("Hello", "How are you?")
        for ((i, prompt) in prompts.withIndex()) {
            Log.i(TAG, "=== Turn ${i + 1}: sending '$prompt' ===")
            try {
                val flow = conversation.sendMessageAsync(prompt)
                val sb = StringBuilder()
                val generationTime = measureTimeMillis {
                    withTimeoutOrNull(120_000) {
                        flow.collect { msg ->
                            sb.append(msg.toString())
                        }
                    } ?: throw IllegalStateException("Generation timed out")
                }
                Log.i(TAG, "Turn ${i + 1} response (${generationTime}ms): $sb")
            } catch (e: Exception) {
                Log.e(TAG, "Turn ${i + 1} FAILED", e)
                throw e
            }
        }

        Log.i(TAG, "=== All turns completed successfully ===")
        conversation.close()
        engine.close()
    }
}
