package com.example.kerasondevice.inference.tasks

import android.graphics.Bitmap
import com.example.kerasondevice.data.preprocessing.ImagePreprocessor
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.DepthResult
import com.example.kerasondevice.domain.task.InputType
import com.example.kerasondevice.domain.task.OutputType
import com.example.kerasondevice.domain.task.Task
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.LiteRTInterpreter
import com.example.kerasondevice.inference.createFloatArray

/**
 * Monocular depth estimation task.
 *
 * Supports TFLite models that output a single-channel depth map with shape
 * `[H, W]`, `[1, H, W]`, or `[1, H, W, 1]`. The raw float values are returned
 * unnormalised so the renderer can map them to a grayscale bitmap.
 */
class DepthEstimationTask : Task<Bitmap, DepthResult> {

    override val id = "depth"
    override val name = "Depth"
    override val description = "Monocular depth"
    override val inputType = InputType.Gallery
    override val outputType = OutputType.DepthMap
    override val preferredModels = listOf("midas.tflite")

    override suspend fun run(model: ModelHandle, input: Bitmap): TaskResult<DepthResult> {
        val config = model.config

        return try {
            LiteRTInterpreter(model.file).use { interpreter ->
                val buffer = ImagePreprocessor().preprocess(input, config)

                val shape = interpreter.getOutputShape(0)
                val output = createFloatArray(shape)
                val outputs = mutableMapOf<Int, Any>(0 to output)

                val start = System.currentTimeMillis()
                interpreter.run(arrayOf(buffer), outputs)
                val latency = System.currentTimeMillis() - start

                val (depthMap, width, height) = flattenDepthOutput(output, shape)
                TaskResult.Success(DepthResult(depthMap, width, height), latency)
            }
        } catch (e: Exception) {
            TaskResult.Error(e.message ?: "Inference failed")
        }
    }

    private fun flattenDepthOutput(output: Any, shape: IntArray): Triple<FloatArray, Int, Int> {
        return when (shape.size) {
            2 -> {
                val arr = output as Array<FloatArray>
                val depth = arr.flatMap { it.asIterable() }.toFloatArray()
                Triple(depth, shape[1], shape[0])
            }
            3 -> {
                val arr = output as Array<Array<FloatArray>>
                val depth = arr[0].flatMap { it.asIterable() }.toFloatArray()
                Triple(depth, shape[2], shape[1])
            }
            4 -> {
                val arr = output as Array<Array<Array<FloatArray>>>
                val depth = arr[0].flatMap { row -> row.flatMap { it.asIterable() } }.toFloatArray()
                Triple(depth, shape[2], shape[1])
            }
            else -> {
                val depth = output as FloatArray
                val height = shape.getOrElse(0) { 1 }
                val width = depth.size / height
                Triple(depth, width, height)
            }
        }
    }
}
