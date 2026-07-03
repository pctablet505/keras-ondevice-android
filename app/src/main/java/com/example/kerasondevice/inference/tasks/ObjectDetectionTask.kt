package com.example.kerasondevice.inference.tasks

import android.graphics.Bitmap
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.BoundingBox
import com.example.kerasondevice.domain.task.InputType
import com.example.kerasondevice.domain.task.OutputType
import com.example.kerasondevice.domain.task.Task
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.LiteRTInterpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Stock COCO SSD MobileNet V1 quantized object detection task.
 *
 * Model input: 300×300 uint8 RGB.
 * Model outputs (in order): detection_boxes, detection_classes,
 * detection_scores, num_detections. Box coordinates are normalized
 * [ymin, xmin, ymax, xmax].
 */
class ObjectDetectionTask(
    private val labels: (File) -> List<String>
) : Task<Bitmap, List<BoundingBox>> {

    override val id = "detection"
    override val name = "Object Detection"
    override val description = "Detect objects"
    override val inputType = InputType.Gallery
    override val outputType = OutputType.BoundingBoxList
    override val preferredModels = listOf("ssd_mobilenet_v1_detect.tflite")

    override suspend fun run(
        model: ModelHandle,
        input: Bitmap
    ): TaskResult<List<BoundingBox>> {
        val config = model.config
        val labelList = labels(model.file)

        return try {
            LiteRTInterpreter(model.file).use { interpreter ->
                val width = config.image_width ?: DEFAULT_SIZE
                val height = config.image_height ?: DEFAULT_SIZE
                val scaled = Bitmap.createScaledBitmap(input, width, height, true)
                val inputBuffer = rgbBytesToBuffer(scaled)

                val outputLocations = Array(1) { Array(DETECTION_COUNT) { FloatArray(4) } }
                val outputClasses = Array(1) { FloatArray(DETECTION_COUNT) }
                val outputScores = Array(1) { FloatArray(DETECTION_COUNT) }
                val numDetections = FloatArray(1)
                val outputs = mutableMapOf<Int, Any>(
                    0 to outputLocations,
                    1 to outputClasses,
                    2 to outputScores,
                    3 to numDetections
                )

                val start = System.currentTimeMillis()
                interpreter.run(arrayOf(inputBuffer), outputs)
                val latency = System.currentTimeMillis() - start

                val boxes = parseOutputs(
                    outputLocations,
                    outputClasses,
                    outputScores,
                    numDetections,
                    labelList
                )
                TaskResult.Success(boxes, latency)
            }
        } catch (e: Exception) {
            TaskResult.Error(e.message ?: "Inference failed")
        }
    }

    private fun rgbBytesToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * CHANNELS)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (pixel in pixels) {
            buffer.put((pixel shr 16 and 0xFF).toByte())
            buffer.put((pixel shr 8 and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }
        buffer.rewind()
        return buffer
    }

    private fun parseOutputs(
        locations: Array<Array<FloatArray>>,
        classes: Array<FloatArray>,
        scores: Array<FloatArray>,
        numDetections: FloatArray,
        labels: List<String>
    ): List<BoundingBox> {
        val count = numDetections[0].toInt().coerceAtMost(DETECTION_COUNT)
        val result = mutableListOf<BoundingBox>()
        for (i in 0 until count) {
            val score = scores[0][i]
            if (score < SCORE_THRESHOLD) continue
            val classIdx = classes[0][i].toInt()
            if (classIdx == BACKGROUND_CLASS_ID) continue
            val box = locations[0][i]
            val label = labels.getOrElse(classIdx - 1) { "class_$classIdx" }
            result.add(
                BoundingBox(
                    label = label,
                    score = score,
                    x1 = box[1],
                    y1 = box[0],
                    x2 = box[3],
                    y2 = box[2]
                )
            )
        }
        return result
    }

    companion object {
        private const val DEFAULT_SIZE = 300
        private const val DETECTION_COUNT = 10
        private const val CHANNELS = 3
        private const val SCORE_THRESHOLD = 0.5f
        private const val BACKGROUND_CLASS_ID = 0
    }
}
