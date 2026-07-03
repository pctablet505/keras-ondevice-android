package com.example.kerasondevice.ui.components.renderers

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.kerasondevice.domain.task.MaskResult
import kotlin.math.absoluteValue
import kotlin.math.absoluteValue

@Composable
fun MaskRenderer(
    result: MaskResult,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(result) {
        val palette = listOf(
            Color.Black,
            Color.Red,
            Color.Green,
            Color.Blue,
            Color.Yellow,
            Color.Cyan,
            Color.Magenta,
            Color(0xFFFFA500),
            Color(0xFF800080),
            Color(0xFF00FF00),
            Color(0xFF008080),
            Color(0xFF808000)
        )
        Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888).apply {
            result.mask.forEachIndexed { index, classIndex ->
                val x = index % result.width
                val y = index / result.width
                val color = palette[classIndex.absoluteValue % palette.size].toArgb()
                setPixel(x, y, color)
            }
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Segmentation mask",
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
