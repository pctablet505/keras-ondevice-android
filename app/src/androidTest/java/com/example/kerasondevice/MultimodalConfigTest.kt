package com.example.kerasondevice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.domain.model.ModelFormat
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.inference.language.MultimodalValidator
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MultimodalConfigTest {

    @Test
    fun gemma4MultimodalInitializes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val modelAssetName = "models/gemma4_multimodal_test.litertlm"
        val configAssetName = "models/gemma4_multimodal_test.json"

        // Skip gracefully when the bundled tiny model is not present. The model
        // asset is prepared by the separate model_assets task.
        assumeAssetExists(modelAssetName)
        assumeAssetExists(configAssetName)

        val modelFile = File(context.filesDir, "gemma4_multimodal_test.litertlm")
        copyAssetToFiles(context, modelAssetName, modelFile)

        val configText = context.assets.open(configAssetName).bufferedReader().use { it.readText() }
        val config = ModelConfig.parse(configText)
        val handle = ModelHandle(modelFile, ModelFormat.LITERTLM, config)
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
}
