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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class QuantizedModelTest {
    @Test
    fun testQuantized270M() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val modelFile = File("/data/local/tmp/gemma3_270m_it_wi8afp32.litertlm")
            .takeIf { it.exists() }
            ?: throw IllegalStateException("Quantized model not found")

        Log.i("QuantizedTest", "Model file: ${modelFile.absolutePath}, size=${modelFile.length()}")
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        val engine = Engine(config)
        val initTime = measureTimeMillis { engine.initialize() }
        Log.i("QuantizedTest", "Engine initialized in ${initTime}ms")

        val conversation = engine.createConversation()
        val prompt = "What is Keras?"
        Log.i("QuantizedTest", "=== Sending '$prompt' ===")
        val flow = conversation.sendMessageAsync(prompt)
        val sb = StringBuilder()
        val generationTime = measureTimeMillis {
            withTimeoutOrNull(60_000) {
                flow.collect { msg -> sb.append(msg.toString()) }
            } ?: throw IllegalStateException("Generation timed out")
        }
        Log.i("QuantizedTest", "Response (${generationTime}ms): $sb")
        conversation.close()
        engine.close()
        Log.i("QuantizedTest", "=== Quantized 270M test PASSED ===")
    }
}
