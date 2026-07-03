package com.example.kerasondevice

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.tasks.DepthEstimationTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end test for monocular depth estimation.
 *
 * Uses a bundled `midas.tflite` asset if available; otherwise falls back to a
 * model staged in `/data/local/tmp`.
 */
@RunWith(AndroidJUnit4::class)
class DepthE2ETest {

    companion object {
        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/midas.tflite",
            "/data/local/tmp/midas_v2_1_small.tflite"
        )
        private const val SIDECAR_ASSET = "models/midas.json"
    }

    @Test
    fun midasProducesDepthMap() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val testCtx = InstrumentationRegistry.getInstrumentation().context

        val (modelFile, sidecarFile) = resolveModel(ctx, testCtx)
            ?: throw AssertionError(
                "No depth model available. " +
                    "Bundle models/midas.tflite or push a .tflite to /data/local/tmp."
            )

        assumeTrue("Sidecar missing", sidecarFile.exists())
        assumeTrue("Test image missing", testCtx.assets.list("images")?.contains("test_image.jpg") == true)

        val config = ModelConfig.parse(sidecarFile.readText())
        val handle = ModelHandle(modelFile, ModelFormat.TFLITE, config)

        val bitmap = BitmapFactory.decodeStream(testCtx.assets.open("images/test_image.jpg"))
            ?: throw AssertionError("Failed to decode test image")

        val task = DepthEstimationTask()
        val result = runBlocking { task.run(handle, bitmap) }

        assertTrue("Expected success but got $result", result is TaskResult.Success)
        val success = result as TaskResult.Success
        assertTrue("Depth map should not be empty", success.output.depthMap.isNotEmpty())
        assertTrue("Depth dimensions should be positive", success.output.width > 0 && success.output.height > 0)
    }

    private fun resolveModel(ctx: Context, testCtx: Context): Pair<File, File>? {
        val bundledModel = File(ctx.filesDir, "midas.tflite")
        val bundledSidecar = File(ctx.filesDir, "midas.json")
        if (copyAssetToFiles(testCtx, "models/midas.tflite", bundledModel) &&
            copyAssetToFiles(testCtx, SIDECAR_ASSET, bundledSidecar)
        ) {
            return bundledModel to bundledSidecar
        }

        for (path in KNOWN_MODEL_PATHS) {
            val model = File(path)
            if (!model.exists()) continue
            val sidecar = File(model.parentFile, model.nameWithoutExtension + ".json")
            if (!sidecar.exists()) continue
            return model to sidecar
        }
        return null
    }
}
