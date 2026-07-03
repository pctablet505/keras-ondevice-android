package com.example.kerasondevice.ui.components.renderers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kerasondevice.domain.task.BoundingBox

@Composable
fun DetectionRenderer(
    boxes: List<BoundingBox>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        if (boxes.isEmpty()) {
            Text(
                text = "No detections",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "${boxes.size} detection${if (boxes.size == 1) "" else "s"}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            boxes.forEach { box ->
                Text(
                    text = buildString {
                        append(box.label)
                        append(" · ${"%.2f".format(box.score)}")
                        append(" · [${"%.2f".format(box.x1)}, ${"%.2f".format(box.y1)}, ")
                        append("${"%.2f".format(box.x2)}, ${"%.2f".format(box.y2)}]")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
