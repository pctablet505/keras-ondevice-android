package com.example.kerasondevice

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SegmentationE2ETest {

    @Before
    fun setUp() {
        assumeAssetExists("models/deeplabv3.tflite")
    }

    @Test
    fun deeplabProducesMask() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = File(ctx.filesDir, "deeplabv3.tflite")
        val sidecarFile = File(ctx.filesDir, "deeplabv3.json")
        val labelsFile = File(ctx.filesDir, "pascal_labels.txt")

        copyAssetToFiles(ctx, "models/deeplabv3.tflite", modelFile)
        copyAssetToFiles(ctx, "models/deeplabv3.json", sidecarFile)
        copyAssetToFiles(ctx, "models/pascal_labels.txt", labelsFile)

        assumeTrue("Sidecar missing", sidecarFile.exists())
        assumeTrue("Test image missing", ctx.assets.list("images")?.contains("test_image.jpg") == true)

        val config = ModelConfig.parse(sidecarFile.readText())
        val handle = ModelHandle(modelFile, ModelFormat.TFLITE, config)

        val bitmap = BitmapFactory.decodeStream(ctx.assets.open("images/test_image.jpg"))
            ?: throw AssertionError("Failed to decode test image")

        val task = SegmentationTask()
        val result = runBlocking { task.run(handle, bitmap) }

        assertTrue("Expected success but got $result", result is TaskResult.Success)
        val success = result as TaskResult.Success
        assertTrue("Mask should not be empty", success.output.mask.isNotEmpty())
        assertTrue("Mask dimensions should be positive", success.output.width > 0 && success.output.height > 0)
    }
}
