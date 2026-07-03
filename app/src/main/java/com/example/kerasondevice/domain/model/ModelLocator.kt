package com.example.kerasondevice.domain.model

import android.content.Context
import com.example.kerasondevice.domain.config.ModelConfig
import java.io.File

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
        }.map { file ->
            val format = when (file.extension) {
                "tflite" -> ModelFormat.TFLITE
                "litertlm" -> ModelFormat.LITERTLM
                else -> error("unreachable")
            }
            val configFile = File(file.parent, "${file.nameWithoutExtension}.json")
            val config = if (configFile.exists()) {
                ModelConfig.parse(configFile.readText())
            } else null
            ModelHandle(file, format, config)
        }
    }
}
