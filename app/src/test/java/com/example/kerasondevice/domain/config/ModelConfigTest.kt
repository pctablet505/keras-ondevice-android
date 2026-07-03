package com.example.kerasondevice.domain.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelConfigTest {

    @Test
    fun parseClassificationConfig() {
        val json = """
            {
              "task": "image_classification",
              "image_width": 224,
              "image_height": 224,
              "normalization": {"mean": [127.5, 127.5, 127.5], "std": [127.5, 127.5, 127.5]},
              "labels": "imagenet_labels.txt",
              "num_classes": 1000
            }
        """.trimIndent()
        val cfg = ModelConfig.parse(json)
        assertEquals("image_classification", cfg.task)
        assertEquals(224, cfg.image_width)
        assertEquals(224, cfg.image_height)
        assertNotNull(cfg.normalization)
        assertEquals(3, cfg.normalization?.mean?.size)
        assertEquals(3, cfg.normalization?.std?.size)
        assertEquals("imagenet_labels.txt", cfg.labels)
        assertEquals(1000, cfg.num_classes)
    }

    @Test
    fun parseIgnoresUnknownKeys() {
        val json = """
            {
              "task": "object_detection",
              "foo": "bar"
            }
        """.trimIndent()
        val cfg = ModelConfig.parse(json)
        assertEquals("object_detection", cfg.task)
    }

    @Test
    fun parseMinimalConfig() {
        val json = """
            {
              "task": "depth_estimation"
            }
        """.trimIndent()
        val cfg = ModelConfig.parse(json)
        assertEquals("depth_estimation", cfg.task)
        assertEquals(null, cfg.image_width)
        assertEquals(null, cfg.labels)
    }
}
