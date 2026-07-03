package com.example.kerasondevice

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DetectionE2ETest {

    @Before
    fun setUp() {
        assumeAssetExists("models/ssd_mobilenet_v1_detect.tflite")
    }

    @Test
    fun ssdMobileNetDetectsObjects() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = File(ctx.filesDir, "ssd_mobilenet_v1_detect.tflite")
        val sidecarFile = File(ctx.filesDir, "ssd_mobilenet_v1_detect.json")
        val labelsFile = File(ctx.filesDir, "coco_labels.txt")

        copyAssetToFiles(ctx, "models/ssd_mobilenet_v1_detect.tflite", modelFile)
        copyAssetToFiles(ctx, "models/ssd_mobilenet_v1_detect.json", sidecarFile)
        copyAssetToFiles(ctx, "models/coco_labels.txt", labelsFile)

        assumeTrue("Sidecar missing", sidecarFile.exists())
        assumeTrue("Test image missing", ctx.assets.list("images")?.contains("test_image.jpg") == true)

        val config = ModelConfig.parse(sidecarFile.readText())
        val handle = ModelHandle(modelFile, ModelFormat.TFLITE, config)

        val bitmap = BitmapFactory.decodeStream(ctx.assets.open("images/test_image.jpg"))
            ?: throw AssertionError("Failed to decode test image")

        val task = ObjectDetectionTask { file ->
            file.resolveSibling("coco_labels.txt").readLines()
        }
        val result = runBlocking { task.run(handle, bitmap) }

        assertTrue("Expected success but got $result", result is TaskResult.Success)
        val success = result as TaskResult.Success

        val goldenBoxes = loadGoldenIfExists(ctx)
        if (goldenBoxes != null) {
            assertGoldenMatches(success.output, goldenBoxes)
        } else {
            assertTrue("Expected at least one detection", success.output.isNotEmpty())
        }
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
        // Minimal parser for [{"label":"...","score":0.9,"x1":0.1,"y1":0.1,"x2":0.5,"y2":0.5}, ...]
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

    companion object {
        private const val IOU_THRESHOLD = 0.5f
    }
}
