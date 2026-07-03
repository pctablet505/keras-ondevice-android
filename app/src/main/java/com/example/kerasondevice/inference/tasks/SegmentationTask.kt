package com.example.kerasondevice.inference.tasks

import android.graphics.Bitmap
import com.example.kerasondevice.data.preprocessing.ImagePreprocessor
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.InputType
import com.example.kerasondevice.domain.task.MaskResult
import com.example.kerasondevice.domain.task.OutputType
import com.example.kerasondevice.domain.task.Task
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.LiteRTInterpreter
import com.example.kerasondevice.inference.createFloatArray
import java.io.File

/**
 * Semantic segmentation task.
 *
 * Supports TFLite models that output per-pixel class logits with shape
 * `[H, W, C]` or `[1, H, W, C]`. The argmax over the class dimension is taken
 * to produce a coloured mask and per-class pixel counts.
 */
class SegmentationTask : Task<Bitmap, MaskResult> {

    override val id = "segmentation"
    override val name = "Segmentation"
    override val description = "Pixel-level classes"
    override val inputType = InputType.Gallery
    override val outputType = OutputType.Mask
    override val preferredModels = listOf("deeplabv3.tflite")

    override suspend fun run(model: ModelHandle, input: Bitmap): TaskResult<MaskResult> {
        val config = model.config ?: return TaskResult.Error("Missing sidecar config")
        val interpreter = LiteRTInterpreter(model.file)
        val buffer = ImagePreprocessor().preprocess(input, config)

        val shape = interpreter.getOutputShape(0)
        val output = createFloatArray(shape)
        val outputs = mutableMapOf<Int, Any>(0 to output)

        val start = System.currentTimeMillis()
        interpreter.run(arrayOf(buffer), outputs)
        val latency = System.currentTimeMillis() - start

        val labels = config.labels?.let { labelFile ->
            File(model.file.parent, labelFile).takeIf { it.exists() }?.readLines()
        }.orEmpty()

        val (mask, width, height, counts) = when (shape.size) {
            4 -> parse4D(output as Array<Array<Array<FloatArray>>>, labels)
            3 -> parse3D(output as Array<Array<FloatArray>>, labels)
            2 -> parse2D(output as Array<FloatArray>, labels)
            else -> {
                interpreter.close()
                return TaskResult.Error("Unsupported segmentation output shape: ${shape.toList()}")
            }
        }
        interpreter.close()
        return TaskResult.Success(MaskResult(mask, width, height, counts), latency)
    }

    private fun parse4D(
        arr: Array<Array<Array<FloatArray>>>,
        labels: List<String>
    ): ParsedMask {
        val height = arr[0].size
        val width = arr[0][0].size
        val classes = arr[0][0][0].size
        val mask = IntArray(width * height)
        val counts = mutableMapOf<String, Int>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                var bestClass = 0
                var bestScore = arr[0][y][x][0]
                for (c in 1 until classes) {
                    val score = arr[0][y][x][c]
                    if (score > bestScore) {
                        bestScore = score
                        bestClass = c
                    }
                }
                val idx = y * width + x
                mask[idx] = bestClass
                val label = labels.getOrElse(bestClass) { "class_$bestClass" }
                counts[label] = counts.getOrDefault(label, 0) + 1
            }
        }
        return ParsedMask(mask, width, height, counts)
    }

    private fun parse3D(
        arr: Array<Array<FloatArray>>,
        labels: List<String>
    ): ParsedMask {
        val height = arr.size
        val width = arr[0].size
        val classes = arr[0][0].size
        val mask = IntArray(width * height)
        val counts = mutableMapOf<String, Int>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                var bestClass = 0
                var bestScore = arr[y][x][0]
                for (c in 1 until classes) {
                    val score = arr[y][x][c]
                    if (score > bestScore) {
                        bestScore = score
                        bestClass = c
                    }
                }
                val idx = y * width + x
                mask[idx] = bestClass
                val label = labels.getOrElse(bestClass) { "class_$bestClass" }
                counts[label] = counts.getOrDefault(label, 0) + 1
            }
        }
        return ParsedMask(mask, width, height, counts)
    }

    private fun parse2D(
        arr: Array<FloatArray>,
        labels: List<String>
    ): ParsedMask {
        val height = arr.size
        val width = arr[0].size
        val mask = IntArray(width * height)
        val counts = mutableMapOf<String, Int>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val bestClass = arr[y][x].toInt()
                val idx = y * width + x
                mask[idx] = bestClass
                val label = labels.getOrElse(bestClass) { "class_$bestClass" }
                counts[label] = counts.getOrDefault(label, 0) + 1
            }
        }
        return ParsedMask(mask, width, height, counts)
    }

    private data class ParsedMask(
        val mask: IntArray,
        val width: Int,
        val height: Int,
        val counts: Map<String, Int>
    )

}
