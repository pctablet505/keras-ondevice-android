package com.example.kerasondevice.domain.model

import com.example.kerasondevice.domain.config.ModelConfig
import java.io.File

data class ModelHandle(
    val file: File,
    val format: ModelFormat,
    val config: ModelConfig? = null
)
