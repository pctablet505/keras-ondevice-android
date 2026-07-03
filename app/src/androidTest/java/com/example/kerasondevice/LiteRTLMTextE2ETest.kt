package com.example.kerasondevice

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kerasondevice.inference.language.LiteRTLMTextEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end test for the LiteRT-LM optimized text-generation path.
 *
 * The test expects a `.litertlm` model file to be available on the device in
 * the app's files dir, external files dir, or `/data/local/tmp`. It checks
 * well-known filenames first to avoid directory-listing permission issues.
 */
@RunWith(AndroidJUnit4::class)
class LiteRTLMTextE2ETest {

    companion object {
        private const val TAG = "LiteRTLMTextE2ETest"

        private val KNOWN_MODEL_PATHS = listOf(
            "/data/local/tmp/gemma3_270m_it.litertlm",
            "/data/local/tmp/tiny_gemma3n.litertlm"
        )
    }

    @Test
    fun litertlmModelGeneratesTokens() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val modelFile = findLiteRTLMModel(context)
            ?: run {
                Log.i(TAG, "No .litertlm model found; skipping")
                assumeTrue("No .litertlm model found", false)
                return@runBlocking
            }

        Log.i(TAG, "Model: ${modelFile.absolutePath}")

        val engine = LiteRTLMTextEngine.create(context, modelFile)
        val result = engine.generate(prompt = "hi") { token ->
            Log.d(TAG, "Token: $token")
        }
        engine.close()

        Log.i(TAG, "Tokens=${result.tokenCount}, latency=${result.latencyMs}ms")
        assertTrue("Expected tokenCount > 0", result.tokenCount > 0)
    }

    private fun findLiteRTLMModel(context: Context): File? {
        val candidates = mutableListOf<File>()
        context.filesDir.listFiles()?.filterTo(candidates) { it.name.endsWith(".litertlm", ignoreCase = true) }
        context.getExternalFilesDir(null)?.listFiles()?.filterTo(candidates) { it.name.endsWith(".litertlm", ignoreCase = true) }
        File("/data/local/tmp").listFiles()?.filterTo(candidates) { it.name.endsWith(".litertlm", ignoreCase = true) }
        KNOWN_MODEL_PATHS.mapTo(candidates) { File(it) }
        return candidates.distinctBy { it.absolutePath }.firstOrNull { it.isFile }
    }
}
