package com.example.kerasondevice.domain.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ModelConfig(
    val task: String,
    val image_width: Int? = null,
    val image_height: Int? = null,
    val normalization: Normalization? = null,
    val labels: String? = null,
    val num_classes: Int? = null
) {
    @Serializable
    data class Normalization(
        val mean: List<Float>,
        val std: List<Float>
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(content: String): ModelConfig = json.decodeFromString(serializer(), content)
    }
}
