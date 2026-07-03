package com.example.kerasondevice.ui.components.renderers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.kerasondevice.domain.task.DepthResult

@Composable
fun DepthRenderer(
    result: DepthResult,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(result) {
        val min = result.depthMap.minOrNull() ?: 0f
        val max = result.depthMap.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0f } ?: 1f
        Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888).apply {
            result.depthMap.forEachIndexed { index, value ->
                val x = index % result.width
                val y = index / result.width
                val normalized = ((value - min) / range).coerceIn(0f, 1f)
                val gray = (normalized * 255).toInt()
                setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Depth map",
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
