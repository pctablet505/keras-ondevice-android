package com.example.kerasondevice.domain.catalog

object DemoCatalog {
    val entries = listOf(
        DemoEntry.VisionEntry(
            id = "classification",
            title = "Classification",
            description = "Classify images with MobileNetV3 (ImageNet).",
            taskId = "classification"
        ),
        DemoEntry.VisionEntry(
            id = "detection",
            title = "Object Detection",
            description = "Detect COCO objects with D-Fine.",
            taskId = "detection"
        ),
        DemoEntry.VisionEntry(
            id = "segmentation",
            title = "Segmentation",
            description = "Pixel-level labels with DeepLabV3 (PASCAL VOC).",
            taskId = "segmentation"
        ),
        DemoEntry.VisionEntry(
            id = "depth",
            title = "Depth",
            description = "Monocular depth with DepthAnything v2.",
            taskId = "depth"
        ),
        DemoEntry.LanguageEntry(
            id = "language",
            title = "Language",
            description = "Gemma3 text generation: LiteRT vs LiteRT-LM."
        ),
        DemoEntry.MultimodalEntry(
            id = "multimodal",
            title = "Multimodal",
            description = "Validate Gemma4-style multimodal engine init."
        )
    )
}
