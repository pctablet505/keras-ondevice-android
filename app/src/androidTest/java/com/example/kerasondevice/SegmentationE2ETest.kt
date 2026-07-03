package com.example.kerasondevice

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.tasks.SegmentationTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end test for semantic segmentation.
 *
 * Uses a bundled `deeplabv3.tflite` asset if available; otherwise falls back to
 * a model staged in `/data/local/tmp`.
 */
@RunWith(AndroidJUnit4::class)
class SegmentationE2ETest {

    companion object {
        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/deeplabv3.tflite"
        )
        private const val SIDECAR_ASSET = "models/deeplabv3.json"
        private const val LABELS_ASSET = "models/pascal_labels.txt"
    }

    @Test
    fun deeplabProducesMask() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val testCtx = InstrumentationRegistry.getInstrumentation().context

        val (modelFile, sidecarFile, labelsFile) = resolveModel(ctx, testCtx)
            ?: throw AssertionError(
                "No segmentation model available. " +
                    "Bundle models/deeplabv3.tflite or push a .tflite to /data/local/tmp."
            )

        assumeTrue("Sidecar missing", sidecarFile.exists())
        assumeTrue("Test image missing", testCtx.assets.list("images")?.contains("test_image.jpg") == true)

        val config = ModelConfig.parse(sidecarFile.readText())
        val handle = ModelHandle(modelFile, ModelFormat.TFLITE, config)

        val bitmap = BitmapFactory.decodeStream(testCtx.assets.open("images/test_image.jpg"))
            ?: throw AssertionError("Failed to decode test image")

        val task = SegmentationTask()
        val result = runBlocking { task.run(handle, bitmap) }

        assertTrue("Expected success but got $result", result is TaskResult.Success)
        val success = result as TaskResult.Success
        assertTrue("Mask should not be empty", success.output.mask.isNotEmpty())
        assertTrue("Mask dimensions should be positive", success.output.width > 0 && success.output.height > 0)
    }

    private fun resolveModel(ctx: Context, testCtx: Context): Triple<File, File, File>? {
        val bundledModel = File(ctx.filesDir, "deeplabv3.tflite")
        val bundledSidecar = File(ctx.filesDir, "deeplabv3.json")
        val bundledLabels = File(ctx.filesDir, "pascal_labels.txt")
        if (copyAssetToFiles(testCtx, "models/deeplabv3.tflite", bundledModel) &&
            copyAssetToFiles(testCtx, SIDECAR_ASSET, bundledSidecar) &&
            copyAssetToFiles(testCtx, LABELS_ASSET, bundledLabels)
        ) {
            return Triple(bundledModel, bundledSidecar, bundledLabels)
        }

        for (path in KNOWN_MODEL_PATHS) {
            val model = File(path)
            if (!model.exists()) continue
            val sidecar = File(model.parentFile, model.nameWithoutExtension + ".json")
            if (!sidecar.exists()) continue
            val labels = File(model.parentFile, "pascal_labels.txt")
                .takeIf { it.exists() }
                ?: continue
            return Triple(model, sidecar, labels)
        }
        return null
    }
}
