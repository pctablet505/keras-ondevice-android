package com.example.kerasondevice.domain.model

import com.example.kerasondevice.domain.config.ModelConfig
import java.io.File

/**
 * A discovered model file together with its parsed sidecar configuration.
 *
 * The sidecar is **required**: the [ModelLocator] only produces handles when a
 * matching `.json` sidecar is present. This guarantees that every task has the
 * metadata it needs (input size, normalisation, labels) without falling back to
 * silent defaults.
 */
data class ModelHandle(
    val file: File,
    val format: ModelFormat,
    val config: ModelConfig
)
