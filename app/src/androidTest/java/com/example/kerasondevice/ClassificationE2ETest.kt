package com.example.kerasondevice

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.tasks.ImageClassificationTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * End-to-end test for image classification.
 *
 * The test first tries to use a bundled `mobilenetv3.tflite` asset. If that is
 * not present, it falls back to a model file placed in `/data/local/tmp` (the
 * standard on-device model staging location for this demo).
 */
@RunWith(AndroidJUnit4::class)
class ClassificationE2ETest {

    companion object {
        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/mobilenetv3.tflite",
            "/data/local/tmp/mobilenetv3_large_100_224.tflite"
        )
        private const val SIDEACAR_ASSET = "models/mobilenetv3.json"
        private const val LABELS_ASSET = "models/imagenet_labels.txt"
    }

    @Test
    fun mobilenetPredictsTop5Classes() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val testCtx = InstrumentationRegistry.getInstrumentation().context

        val (modelFile, sidecarFile, labelsFile) = resolveModel(ctx, testCtx)
            ?: throw AssertionError(
                "No classification model available. " +
                    "Bundle models/mobilenetv3.tflite or push a .tflite to /data/local/tmp."
            )

        assumeTrue("Sidecar missing", sidecarFile.exists())
        assumeTrue("Test image missing", testCtx.assets.list("images")?.contains("test_image.jpg") == true)

        val config = ModelConfig.parse(sidecarFile.readText())
        val handle = ModelHandle(modelFile, ModelFormat.TFLITE, config)

        val bitmap = BitmapFactory.decodeStream(testCtx.assets.open("images/test_image.jpg"))
            ?: throw AssertionError("Failed to decode test image")

        val task = ImageClassificationTask { file ->
            file.resolveSibling(labelsFile.name).readLines()
        }
        val result = runBlocking { task.run(handle, bitmap) }

        assertTrue("Expected success but got $result", result is TaskResult.Success)
        val success = result as TaskResult.Success
        assertTrue("Expected top-5 results", success.output.size in 1..5)
    }

    private fun resolveModel(ctx: Context, testCtx: Context): Triple<File, File, File>? {
        // 1. Try bundled assets.
        val bundledModel = File(ctx.filesDir, "mobilenetv3.tflite")
        val bundledSidecar = File(ctx.filesDir, "mobilenetv3.json")
        val bundledLabels = File(ctx.filesDir, "imagenet_labels.txt")
        if (copyAssetToFiles(testCtx, "models/mobilenetv3.tflite", bundledModel) &&
            copyAssetToFiles(testCtx, SIDEACAR_ASSET, bundledSidecar) &&
            copyAssetToFiles(testCtx, LABELS_ASSET, bundledLabels)
        ) {
            return Triple(bundledModel, bundledSidecar, bundledLabels)
        }

        // 2. Fall back to on-device model in /data/local/tmp.
        for (path in KNOWN_MODEL_PATHS) {
            val model = File(path)
            if (!model.exists()) continue
            val sidecar = File(model.parentFile, model.nameWithoutExtension + ".json")
            if (!sidecar.exists()) continue
            val labels = File(model.parentFile, "imagenet_labels.txt")
                .takeIf { it.exists() }
                ?: continue
            return Triple(model, sidecar, labels)
        }
        return null
    }
}
