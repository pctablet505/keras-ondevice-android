package com.example.kerasondevice.domain.catalog

object DemoCatalog {
    val entries = listOf(
        DemoEntry.VisionEntry(
            id = "classification",
            title = "Classification",
            description = "ImageNet classification",
            taskId = "classification"
        ),
        DemoEntry.VisionEntry(
            id = "detection",
            title = "Object Detection",
            description = "Detect objects",
            taskId = "detection"
        ),
        DemoEntry.VisionEntry(
            id = "segmentation",
            title = "Segmentation",
            description = "Pixel-level classes",
            taskId = "segmentation"
        ),
        DemoEntry.VisionEntry(
            id = "depth",
            title = "Depth",
            description = "Monocular depth",
            taskId = "depth"
        ),
        DemoEntry.LanguageEntry(
            id = "language",
            title = "Language",
            description = "LLM text generation"
        ),
        DemoEntry.MultimodalEntry(
            id = "multimodal",
            title = "Multimodal",
            description = "Multimodal config validation"
        )
    )
}
