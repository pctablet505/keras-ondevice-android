package com.example.kerasondevice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.inference.language.MultimodalValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end test that verifies a LiteRT-LM multimodal model can be loaded with
 * vision support enabled.
 *
 * Uses a bundled `models/gemma4_multimodal_test.litertlm` asset if available;
 * otherwise falls back to a model staged in `/data/local/tmp`.
 */
@RunWith(AndroidJUnit4::class)
class MultimodalConfigTest {

    companion object {
        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/gemma4_multimodal_test.litertlm",
            "/data/local/tmp/gemma3_multimodal_test.litertlm"
        )
        private const val SIDECAR_ASSET = "models/gemma4_multimodal_test.json"
    }

    @Test
    fun gemma4MultimodalInitializes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val modelFile = resolveModel(context)
            ?: throw AssertionError(
                "No multimodal model available. " +
                    "Bundle models/gemma4_multimodal_test.litertlm or push a " +
                    ".litertlm to /data/local/tmp."
            )

        val configText = if (context.assets.list("models")?.contains("gemma4_multimodal_test.json") == true) {
            context.assets.open(SIDECAR_ASSET).bufferedReader().use { it.readText() }
        } else {
            File(modelFile.parentFile, modelFile.nameWithoutExtension + ".json")
                .takeIf { it.exists() }
                ?.readText()
                ?: "{\"task\":\"multimodal\"}"
        }
        val config = ModelConfig.parse(configText)
        val handle = ModelHandle(modelFile, ModelFormat.LITERTLM, config)
        val result = MultimodalValidator(context).validate(handle)

        assertTrue(
            "Engine should initialize: ${result.error}",
            result.initialized
        )
        assertTrue(
            "maxNumImages should be > 0",
            result.maxNumImages > 0
        )
    }

    private fun resolveModel(context: Context): File? {
        val bundled = File(context.filesDir, "gemma4_multimodal_test.litertlm")
        if (copyAssetToFiles(context, "models/gemma4_multimodal_test.litertlm", bundled)) {
            return bundled
        }
        return KNOWN_MODEL_PATHS.map { File(it) }.firstOrNull { it.exists() }
    }
}
