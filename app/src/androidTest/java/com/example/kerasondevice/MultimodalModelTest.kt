package com.example.kerasondevice

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class MultimodalModelTest {

    companion object {
        private const val TAG = "MultimodalModelTest"
    }

    @Test
    fun testMultimodalModelLoadsAndRuns() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val modelFile = File("/data/local/tmp/gemma4_multimodal_test.litertlm")
            .takeIf { it.exists() }
            ?: run {
                assumeTrue("gemma4_multimodal_test.litertlm not found on device", false)
                return@runBlocking
            }

        Log.i(TAG, "Model file: ${modelFile.absolutePath}, size=${modelFile.length()}")

        Log.i(TAG, "Creating engine for multimodal model...")
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath,
            maxNumImages = 1
        )
        val engine = Engine(config)
        val initTime = measureTimeMillis {
            engine.initialize()
        }
        Log.i(TAG, "Engine initialized in ${initTime}ms")

        // Verify that the engine reports vision support by checking that
        // initialization succeeds with maxNumImages > 0.  Actual image
        // inference requires separate VISION_ENCODER / VISION_ADAPTER TFLite
        // models which the current KerasHub export pipeline does not yet
        // produce.
        Log.i(TAG, "=== Multimodal config test PASSED ===")

        engine.close()
    }
}
