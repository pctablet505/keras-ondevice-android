package com.example.kerasondevice.domain.model

import android.content.Context
import android.util.Log
import com.example.kerasondevice.domain.config.ModelConfig
import java.io.File

/**
 * Discovers on-device model files and their required JSON sidecars.
 *
 * Searches the app's files directory, external files directory, and
 * `/data/local/tmp`. Models without a sidecar are ignored and logged so the
 * user knows why they do not appear in the UI.
 */
class ModelLocator(private val context: Context) {

    fun discover(): List<ModelHandle> = discover(
        listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null),
            File("/data/local/tmp")
        )
    )

    internal fun discover(dirs: List<File>): List<ModelHandle> {
        return dirs.flatMap { dir ->
            dir.listFiles { f ->
                f.isFile && (f.extension == "tflite" || f.extension == "litertlm")
            }?.toList().orEmpty()
        }.mapNotNull { file ->
            val format = when (file.extension) {
                "tflite" -> ModelFormat.TFLITE
                "litertlm" -> ModelFormat.LITERTLM
                else -> return@mapNotNull null
            }
            val configFile = File(file.parent, "${file.nameWithoutExtension}.json")
            if (!configFile.exists()) {
                Log.w(TAG, "Skipping ${file.name}: missing sidecar ${configFile.name}")
                return@mapNotNull null
            }
            val config = try {
                ModelConfig.parse(configFile.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Skipping ${file.name}: failed to parse sidecar", e)
                return@mapNotNull null
            }
            ModelHandle(file, format, config)
        }
    }

    companion object {
        private const val TAG = "ModelLocator"
    }
}
