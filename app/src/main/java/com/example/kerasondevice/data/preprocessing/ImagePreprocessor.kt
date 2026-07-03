package com.example.kerasondevice.data.preprocessing

import android.graphics.Bitmap
import com.example.kerasondevice.domain.config.ModelConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Converts a [Bitmap] into a direct [ByteBuffer] of normalized float32 RGB values.
 *
 * The target size and normalization coefficients are read from the model sidecar
 * [config]. The default normalization matches the common "divide by 127.5 then
 * subtract 1" MobileNet preprocessing when the sidecar omits explicit values.
 */
class ImagePreprocessor {

    fun preprocess(
        bitmap: Bitmap,
        config: ModelConfig,
        order: ChannelOrder = ChannelOrder.RGB
    ): ByteBuffer {
        val width = config.image_width ?: DEFAULT_SIZE
        val height = config.image_height ?: DEFAULT_SIZE
        val normalized = config.normalization ?: ModelConfig.Normalization(
            mean = listOf(DEFAULT_MEAN, DEFAULT_MEAN, DEFAULT_MEAN),
            std = listOf(DEFAULT_STD, DEFAULT_STD, DEFAULT_STD)
        )

        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val buffer = ByteBuffer.allocateDirect(4 * width * height * CHANNELS)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()
            val channels = when (order) {
                ChannelOrder.RGB -> listOf(r, g, b)
                ChannelOrder.BGR -> listOf(b, g, r)
            }
            for ((i, v) in channels.withIndex()) {
                buffer.putFloat((v - normalized.mean[i]) / normalized.std[i])
            }
        }
        buffer.rewind()
        return buffer
    }

    enum class ChannelOrder { RGB, BGR }

    companion object {
        private const val DEFAULT_SIZE = 224
        private const val DEFAULT_MEAN = 127.5f
        private const val DEFAULT_STD = 127.5f
        private const val CHANNELS = 3
    }
}
