package com.example.kerasondevice

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kerasondevice.domain.config.ModelConfig
import com.example.kerasondevice.inference.language.LiteRTTextEngine
import com.example.kerasondevice.inference.sampler.GreedySampler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LiteRTTextE2ETest {

    companion object {
        private const val TAG = "LiteRTTextE2ETest"
    }

    @Test
    fun textModelGeneratesTokens() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val (modelFile, vocabFile) = findTextModel(context)
            ?: run {
                Log.i(TAG, "No text generation model + vocab found; skipping")
                assumeTrue("No text generation model + vocab found", false)
                return@runBlocking
            }

        Log.i(TAG, "Model: ${modelFile.absolutePath}, vocab: ${vocabFile.absolutePath}")

        val engine = LiteRTTextEngine(context, modelFile, vocabFile)
        val result = engine.generate(
            prompt = "hi",
            maxLength = 8,
            sampler = GreedySampler(),
            stripPrompt = true
        )
        engine.close()

        Log.i(TAG, "Generated: '${result.text}', tokens=${result.tokenCount}, latency=${result.latencyMs}ms")
        assertNotNull("Generated text should not be null", result.text)
        assertTrue("Expected tokenCount > 0", result.tokenCount > 0)
    }

    private fun findTextModel(context: Context): Pair<File, File>? {
        val candidates = mutableListOf<File>()
        candidates += context.filesDir.listFiles()?.toList().orEmpty()
        candidates += context.getExternalFilesDir(null)?.listFiles()?.toList().orEmpty()
        candidates += File("/data/local/tmp").listFiles()?.toList().orEmpty()

        for (file in candidates) {
            if (!file.isFile || !file.name.endsWith(".tflite", ignoreCase = true)) continue

            val sidecar = File(file.parentFile, file.nameWithoutExtension + ".json")
            val isTextModel = file.name.equals("gemma3_270m.tflite", ignoreCase = true) ||
                (sidecar.exists() && isTextGenerationSidecar(sidecar))

            if (!isTextModel) continue

            val vocabFile = File(file.parentFile, "vocabulary.spm")
                .takeIf { it.exists() }
                ?: continue

            return file to vocabFile
        }
        return null
    }

    private fun isTextGenerationSidecar(sidecar: File): Boolean =
        try {
            ModelConfig.parse(sidecar.readText()).task == "text_generation"
        } catch (_: Exception) {
            false
        }
}
