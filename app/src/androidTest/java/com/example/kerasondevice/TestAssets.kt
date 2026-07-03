package com.example.kerasondevice

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Copies an asset to the app's files directory, creating parent directories as
 * needed. Returns true if the asset existed and was copied.
 */
fun copyAssetToFiles(context: Context, assetPath: String, dest: File): Boolean {
    dest.parentFile?.mkdirs()
    return try {
        context.assets.open(assetPath).use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Skips the current test gracefully if the requested asset is not bundled.
 */
fun assumeAssetExists(context: Context, assetPath: String): Boolean {
    val dir = assetPath.substringBeforeLast("/", "")
    val fileName = assetPath.substringAfterLast("/")
    val names = if (dir.isEmpty()) {
        context.assets.list("")?.toList().orEmpty()
    } else {
        context.assets.list(dir)?.toList().orEmpty()
    }
    assumeTrue("Asset not found: $assetPath", fileName in names)
    return true
}
