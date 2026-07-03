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

/**
 * End-to-end test for the custom LiteRT text-generation path.
 *
 * The test expects a `.tflite` text model, a matching `.json` sidecar, and a
 * sibling `vocabulary.spm` file to be available on the device. It checks the
 * standard locations used by the demo (app files dir, external files dir, and
 * `/data/local/tmp`) and falls back to well-known filenames.
 */
@RunWith(AndroidJUnit4::class)
class LiteRTTextE2ETest {

    companion object {
        private const val TAG = "LiteRTTextE2ETest"

        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/gemma3_270m.tflite",
            "/data/local/tmp/gemma3_270m_it_torch_wi8afp32.tflite",
            "/data/local/tmp/gemma3_270m_it_torch.tflite"
        )

        private val KNOWN_VOCAB_PATHS = listOf(
            "/data/local/tmp/vocabulary.spm"
        )
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
        // Collect candidate model files from all standard locations.
        val candidates = mutableListOf<File>()
        context.filesDir.listFiles()?.filterTo(candidates) { it.name.endsWith(".tflite", ignoreCase = true) }
        context.getExternalFilesDir(null)?.listFiles()?.filterTo(candidates) { it.name.endsWith(".tflite", ignoreCase = true) }
        File("/data/local/tmp").listFiles()?.filterTo(candidates) { it.name.endsWith(".tflite", ignoreCase = true) }
        KNOWN_MODEL_PATHS.mapTo(candidates) { File(it) }

        for (file in candidates.distinctBy { it.absolutePath }) {
            if (!file.isFile) continue

            val sidecar = File(file.parentFile, file.nameWithoutExtension + ".json")
            val isTextModel = file.name.equals("gemma3_270m.tflite", ignoreCase = true) ||
                file.name.contains("gemma3_270m", ignoreCase = true) ||
                (sidecar.exists() && isTextGenerationSidecar(sidecar))

            if (!isTextModel) continue

            val vocabFile = findVocab(file.parentFile)
                ?: continue

            return file to vocabFile
        }
        return null
    }

    private fun findVocab(parentDir: File?): File? {
        if (parentDir == null) return null
        val local = File(parentDir, "vocabulary.spm")
        if (local.exists()) return local
        return KNOWN_VOCAB_PATHS.map { File(it) }.firstOrNull { it.exists() }
    }

    private fun isTextGenerationSidecar(sidecar: File): Boolean =
        try {
            ModelConfig.parse(sidecar.readText()).task == "text_generation"
        } catch (_: Exception) {
            false
        }
}
