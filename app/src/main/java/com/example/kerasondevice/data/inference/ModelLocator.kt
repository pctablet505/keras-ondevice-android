package com.example.kerasondevice.data.inference

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Discovers LiteRT-LM model assets in the app's internal/external files
 * directories or on-device test paths.
 *
 * When [filename] is provided, that exact file is searched for. When
 * omitted (or null), the locator returns the **first** `.litertlm` file
 * found, preferring internal storage, then external storage, then
 * `/data/local/tmp`.
 */
object ModelLocator {
    private const val TAG = "ModelLocator"
    private const val FALLBACK_DIR = "/data/local/tmp"

    /** Search locations in priority order. */
    private fun searchDirs(context: Context): List<File> = listOf(
        context.filesDir,
        context.getExternalFilesDir(null) ?: File("/dev/null"),
        File(FALLBACK_DIR)
    )

    private val PREFERRED_DEFAULTS = listOf(
        "gemma3_270m_it_wi8afp32.litertlm",
        "gemma3_270m_it.litertlm",
        "gemma3_270m_it_int4_bucketed_32_64_128.litertlm"
    )

    /**
     * Find a model file.
     *
     * @param filename Optional explicit filename. When null, the locator
     *   first searches for known default model names, then falls back to
     *   the first (alphabetically) `.litertlm` file found.
     */
    fun findModelFile(context: Context, filename: String? = null): File? {
        val dirs = searchDirs(context)

        // Explicit filename requested — look for it directly.
        if (filename != null) {
            for (dir in dirs) {
                val file = File(dir, filename)
                if (file.exists() && file.canRead()) {
                    Log.d(TAG, "Found model: ${file.absolutePath} (${formatBytes(file.length())})")
                    return file
                }
            }
            Log.e(TAG, "Model '$filename' not found in: ${dirs.map { it.absolutePath }}")
            return null
        }

        // No filename — try preferred defaults first.
        for (preferred in PREFERRED_DEFAULTS) {
            for (dir in dirs) {
                val file = File(dir, preferred)
                if (file.exists() && file.canRead()) {
                    Log.d(TAG, "Found model: ${file.absolutePath} (${formatBytes(file.length())})")
                    return file
                }
            }
        }

        // Fallback: first readable .litertlm in any search dir (sorted for determinism).
        val all = dirs.flatMap { dir ->
            (dir.listFiles { f -> f.isFile && f.extension == "litertlm" }?.toList()
                ?: emptyList()).filter { it.canRead() }
        }.sortedBy { it.name }

        all.firstOrNull()?.let {
            Log.d(TAG, "Found model: ${it.absolutePath} (${formatBytes(it.length())})")
            return it
        }

        Log.e(TAG, "No .litertlm model found in: ${dirs.map { it.absolutePath }}")
        return null
    }

    /** Known model filenames to check when directory listing is unavailable. */
    private val KNOWN_MODELS = listOf(
        "gemma3_270m_it_wi8afp32.litertlm",
        "gemma3_270m_it.litertlm",
        "gemma3_270m_it_int4_bucketed_32_64_128.litertlm",
        "gemma3_270m_it_bucketed.litertlm",
        "gemma3_270m_it_prefill_256.litertlm",
        "tiny_gemma3_bucketed.litertlm",
        "tiny_gemma3n.litertlm"
    )

    /** List all readable `.litertlm` files across search locations. */
    fun listAvailableModels(context: Context): List<File> {
        val found = mutableSetOf<File>()
        for (dir in searchDirs(context)) {
            // Try directory listing first.
            val listed = dir.listFiles { f -> f.isFile && f.extension == "litertlm" }
                ?.filter { it.canRead() }
                ?: emptyList()
            found.addAll(listed)

            // Fallback: check known filenames individually (needed for /data/local/tmp
            // where apps can read files but may not have directory-list permission).
            if (listed.isEmpty()) {
                for (name in KNOWN_MODELS) {
                    val file = File(dir, name)
                    if (file.exists() && file.canRead()) {
                        found.add(file)
                    }
                }
            }
        }
        return found.toList()
    }

    /** Format byte count as human-readable string. */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}
