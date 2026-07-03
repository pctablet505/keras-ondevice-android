package com.example.kerasondevice.domain.catalog

sealed class DemoEntry(
    val id: String,
    val title: String,
    val description: String
) {
    class VisionEntry(
        id: String,
        title: String,
        description: String,
        val taskId: String
    ) : DemoEntry(id, title, description)

    class LanguageEntry(
        id: String,
        title: String,
        description: String
    ) : DemoEntry(id, title, description)

    class MultimodalEntry(
        id: String,
        title: String,
        description: String
    ) : DemoEntry(id, title, description)
}
