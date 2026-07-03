package com.example.kerasondevice.domain.model

import androidx.test.core.app.ApplicationProvider
import com.example.kerasondevice.domain.config.ModelConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelLocatorTest {

    @Test
    fun discoverFindsTfliteAndLitertlmFilesWithSidecar() {
        val root = File(RuntimeEnvironment.getApplication().filesDir, "locator-test")
        root.deleteRecursively()
        root.mkdirs()

        val modelFile = File(root, "mobilenetv3.tflite")
        modelFile.writeText("dummy tflite")

        val sidecar = File(root, "mobilenetv3.json")
        sidecar.writeText(
            """
            {
              "task": "image_classification",
              "image_width": 224,
              "image_height": 224,
              "labels": "imagenet_labels.txt",
              "num_classes": 1000
            }
            """.trimIndent()
        )

        val litertlmFile = File(root, "gemma.litertlm")
        litertlmFile.writeText("dummy litertlm")
        val litertlmSidecar = File(root, "gemma.json")
        litertlmSidecar.writeText(
            """
            {
              "task": "text_generation"
            }
            """.trimIndent()
        )

        val locator = ModelLocator(ApplicationProvider.getApplicationContext())
        val handles = locator.discover(listOf(root))

        assertEquals(2, handles.size)

        val tfliteHandle = handles.first { it.file.name == "mobilenetv3.tflite" }
        assertEquals(ModelFormat.TFLITE, tfliteHandle.format)
        assertNotNull(tfliteHandle.config)
        assertEquals("image_classification", tfliteHandle.config.task)
        assertEquals(224, tfliteHandle.config.image_width)

        val litertlmHandle = handles.first { it.file.name == "gemma.litertlm" }
        assertEquals(ModelFormat.LITERTLM, litertlmHandle.format)
        assertNotNull(litertlmHandle.config)
        assertEquals("text_generation", litertlmHandle.config.task)
    }

    @Test
    fun discoverOmitsModelsWithoutSidecar() {
        val root = File(RuntimeEnvironment.getApplication().filesDir, "locator-sidecar-required")
        root.deleteRecursively()
        root.mkdirs()

        val modelWithSidecar = File(root, "valid.tflite")
        modelWithSidecar.writeText("dummy")
        File(root, "valid.json").writeText("{\"task\":\"depth_estimation\"}")

        val modelWithoutSidecar = File(root, "orphan.tflite")
        modelWithoutSidecar.writeText("dummy")

        val locator = ModelLocator(ApplicationProvider.getApplicationContext())
        val handles = locator.discover(listOf(root))

        assertEquals(1, handles.size)
        assertEquals("valid.tflite", handles.first().file.name)
    }

    @Test
    fun discoverOmitsUnsupportedExtensions() {
        val root = File(RuntimeEnvironment.getApplication().filesDir, "locator-filter")
        root.deleteRecursively()
        root.mkdirs()

        File(root, "model.tflite").writeText("dummy")
        File(root, "model.json").writeText("{\"task\":\"image_classification\"}")
        File(root, "readme.txt").writeText("ignore me")
        File(root, "model.bin").writeText("ignore me too")

        val locator = ModelLocator(ApplicationProvider.getApplicationContext())
        val handles = locator.discover(listOf(root))

        assertEquals(1, handles.size)
        assertEquals("model.tflite", handles.first().file.name)
    }

    @Test
    fun discoverReturnsEmptyListWhenNoModels() {
        val root = File(RuntimeEnvironment.getApplication().filesDir, "locator-empty")
        root.deleteRecursively()
        root.mkdirs()

        val locator = ModelLocator(ApplicationProvider.getApplicationContext())
        val handles = locator.discover(listOf(root))

        assertTrue(handles.isEmpty())
    }
}
