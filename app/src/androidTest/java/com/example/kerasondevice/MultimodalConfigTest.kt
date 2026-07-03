package com.example.kerasondevice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.inference.language.MultimodalValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MultimodalConfigTest {

    @Test
    fun gemma4MultimodalInitializes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetName = "models/gemma4_multimodal_test.litertlm"

        // Skip gracefully when the bundled tiny model is not present. The model
        // asset is prepared by the separate model_assets task.
        val assetExists = context.assets.list("models")?.contains("gemma4_multimodal_test.litertlm") ?: false
        assumeTrue("$assetName not bundled; skipping multimodal config test", assetExists)

        val modelFile = File(context.filesDir, "gemma4_multimodal_test.litertlm")
        copyAssetToFiles(context, assetName, modelFile)

        val handle = ModelHandle(modelFile, ModelFormat.LITERTLM, config = null)
        val result = MultimodalValidator(context).validate(handle)

        org.junit.Assert.assertTrue(
            "Engine should initialize: ${result.error}",
            result.initialized
        )
        org.junit.Assert.assertTrue(
            "maxNumImages should be > 0",
            result.maxNumImages > 0
        )
    }

    private fun copyAssetToFiles(context: Context, assetPath: String, outFile: File) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}
