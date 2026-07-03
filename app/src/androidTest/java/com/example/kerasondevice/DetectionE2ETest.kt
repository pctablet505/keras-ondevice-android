package com.example.kerasondevice

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.BoundingBox
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.tasks.ObjectDetectionTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end test for object detection.
 *
 * Uses a bundled SSD MobileNet V1 model if available; otherwise falls back to a
 * D-Fine model staged in /data/local/tmp.
 */
@RunWith(AndroidJUnit4::class)
class DetectionE2ETest {

    companion object {
        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/dfine.tflite"
        )
        private const val SSD_MODEL_ASSET = "models/ssd_mobilenet_v1_detect.tflite"
        private const val SSD_SIDECAR_ASSET = "models/ssd_mobilenet_v1_detect.json"
        private const val DFINE_SIDECAR_ASSET = "models/dfine.json"
        private const val LABELS_ASSET = "models/coco_labels.txt"
        private const val IOU_THRESHOLD = 0.5f
    }

    @Test
    fun detectorDetectsObjects() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val testCtx = InstrumentationRegistry.getInstrumentation().context

        val (modelFile, sidecarFile, labelsFile) = resolveModel(ctx, testCtx)
            ?: throw AssertionError(
                "No detection model available. " +
                    "Bundle $SSD_MODEL_ASSET or push a .tflite to /data/local/tmp."
            )

        assumeTrue("Sidecar missing", sidecarFile.exists())
        assumeTrue("Test image missing", testCtx.assets.list("images")?.contains("test_image.jpg") == true)

        val config = ModelConfig.parse(sidecarFile.readText())
        val handle = ModelHandle(modelFile, ModelFormat.TFLITE, config)

        val bitmap = BitmapFactory.decodeStream(testCtx.assets.open("images/test_image.jpg"))
            ?: throw AssertionError("Failed to decode test image")

        val task = ObjectDetectionTask { file ->
            file.resolveSibling(labelsFile.name).readLines()
        }
        val result = runBlocking { task.run(handle, bitmap) }

        assertTrue("Expected success but got $result", result is TaskResult.Success)
        val success = result as TaskResult.Success

        val goldenBoxes = loadGoldenIfExists(testCtx)
        if (goldenBoxes != null) {
            assertGoldenMatches(success.output, goldenBoxes)
        } else {
            assertTrue("Expected at least one detection", success.output.isNotEmpty())
        }
    }

    private fun resolveModel(ctx: Context, testCtx: Context): Triple<File, File, File>? {
        // 1. Try bundled SSD MobileNet V1 assets.
        val bundledModel = File(ctx.filesDir, "ssd_mobilenet_v1_detect.tflite")
        val bundledSidecar = File(ctx.filesDir, "ssd_mobilenet_v1_detect.json")
        val bundledLabels = File(ctx.filesDir, "coco_labels.txt")
        if (copyAssetToFiles(testCtx, SSD_MODEL_ASSET, bundledModel) &&
            copyAssetToFiles(testCtx, SSD_SIDECAR_ASSET, bundledSidecar) &&
            copyAssetToFiles(testCtx, LABELS_ASSET, bundledLabels)
        ) {
            return Triple(bundledModel, bundledSidecar, bundledLabels)
        }

        // 2. Fall back to D-Fine staged in /data/local/tmp.
        for (path in KNOWN_MODEL_PATHS) {
            val model = File(path)
            if (!model.exists()) continue
            val sidecar = File(model.parentFile, model.nameWithoutExtension + ".json")
            if (!sidecar.exists()) continue
            val labels = File(model.parentFile, "coco_labels.txt")
                .takeIf { it.exists() }
                ?: continue
            return Triple(model, sidecar, labels)
        }
        return null
    }

    private fun loadGoldenIfExists(ctx: android.content.Context): List<BoundingBox>? {
        if (ctx.assets.list("golden")?.contains("detection_golden.json") != true) return null
        return try {
            val json = ctx.assets.open("golden/detection_golden.json").bufferedReader().readText()
            parseGoldenBoxes(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseGoldenBoxes(json: String): List<BoundingBox> {
        val regex = """\{\s*"label"\s*:\s*"([^"]+)"\s*,\s*"score"\s*:\s*([0-9.]+)\s*,\s*"x1"\s*:\s*([0-9.]+)\s*,\s*"y1"\s*:\s*([0-9.]+)\s*,\s*"x2"\s*:\s*([0-9.]+)\s*,\s*"y2"\s*:\s*([0-9.]+)\s*\}""".toRegex()
        return regex.findAll(json).map { match ->
            BoundingBox(
                label = match.groupValues[1],
                score = match.groupValues[2].toFloat(),
                x1 = match.groupValues[3].toFloat(),
                y1 = match.groupValues[4].toFloat(),
                x2 = match.groupValues[5].toFloat(),
                y2 = match.groupValues[6].toFloat()
            )
        }.toList()
    }

    private fun assertGoldenMatches(
        actual: List<BoundingBox>,
        golden: List<BoundingBox>
    ) {
        assertTrue("Expected ${golden.size} golden detections, found ${actual.size}", actual.size >= golden.size)
        golden.forEach { goldenBox ->
            val match = actual.any { box ->
                box.label == goldenBox.label &&
                    iou(box, goldenBox) >= IOU_THRESHOLD
            }
            assertTrue("Missing golden detection: $goldenBox", match)
        }
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)
        val intersection = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        val union = areaA + areaB - intersection
        return if (union > 0f) intersection / union else 0f
    }
}
