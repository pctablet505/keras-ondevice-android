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

@RunWith(AndroidJUnit4::class)
class LiteRTLMTextE2ETest {

    companion object {
        private const val TAG = "LiteRTLMTextE2ETest"
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
        candidates += context.filesDir.listFiles()?.toList().orEmpty()
        candidates += context.getExternalFilesDir(null)?.listFiles()?.toList().orEmpty()
        candidates += File("/data/local/tmp").listFiles()?.toList().orEmpty()

        return candidates.firstOrNull {
            it.isFile && it.name.endsWith(".litertlm", ignoreCase = true)
        }
    }
}
