package com.example.kerasondevice.inference.language

import android.content.Context
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Validates that a `.litertlm` model can be loaded as a multimodal engine with
 * vision support enabled (`maxNumImages > 0`).
 *
 * This performs only configuration/initialization checks; it does **not** run
 * end-to-end image or audio inference, because the current KerasHub export
 * pipeline does not yet produce the separate vision encoder/adapter sub-models.
 */
class MultimodalValidator(private val appContext: Context) {

    data class Result(
        val maxNumImages: Int,
        val initialized: Boolean,
        val error: String?
    )

    /**
     * Attempts to create and initialize a LiteRT-LM [Engine] for [model].
     *
     * @return a [Result] indicating whether initialization succeeded.
     */
    suspend fun validate(model: ModelHandle): Result = withContext(Dispatchers.IO) {
        if (model.format != ModelFormat.LITERTLM) {
            return@withContext Result(
                maxNumImages = 0,
                initialized = false,
                error = "Expected LITERTLM model, got ${model.format}"
            )
        }

        runCatching {
            val maxNumImages = 1
            val config = EngineConfig(
                modelPath = model.file.absolutePath,
                backend = Backend.CPU(),
                cacheDir = appContext.cacheDir.absolutePath,
                maxNumImages = maxNumImages
            )
            val engine = Engine(config)
            try {
                engine.initialize()
                Result(maxNumImages = maxNumImages, initialized = true, error = null)
            } finally {
                engine.close()
            }
        }.getOrElse { e ->
            Result(maxNumImages = 0, initialized = false, error = e.message ?: e.toString())
        }
    }
}
