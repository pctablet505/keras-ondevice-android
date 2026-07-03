package com.example.kerasondevice.inference.tasks

import android.graphics.Bitmap
import com.example.kerasondevice.data.preprocessing.ImagePreprocessor
import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.domain.task.Classification
import com.example.kerasondevice.domain.task.InputType
import com.example.kerasondevice.domain.task.OutputType
import com.example.kerasondevice.domain.task.Task
import com.example.kerasondevice.domain.task.TaskResult
import com.example.kerasondevice.inference.LiteRTInterpreter
import java.io.File
import java.util.PriorityQueue

/**
 * ImageNet-style image classification task.
 *
 * Expects a float32 TFLite model whose output is a 1-D logits/probability array
 * of length [ModelConfig.num_classes]. Returns the top-5 classes by score.
 */
class ImageClassificationTask(
    private val labels: (File) -> List<String>
) : Task<Bitmap, List<Classification>> {

    override val id = "classification"
    override val name = "Classification"
    override val description = "ImageNet classification"
    override val inputType = InputType.Gallery
    override val outputType = OutputType.ClassificationList
    override val preferredModels = listOf("mobilenetv3.tflite")

    override suspend fun run(
        model: ModelHandle,
        input: Bitmap
    ): TaskResult<List<Classification>> {
        val config = model.config
        val labelList = labels(model.file)

        return try {
            LiteRTInterpreter(model.file).use { interpreter ->
                val buffer = ImagePreprocessor().preprocess(input, config)

                val classCount = config.num_classes ?: labelList.size
                val outputArray = FloatArray(classCount)
                val outputs = mutableMapOf<Int, Any>(0 to outputArray)

                val start = System.currentTimeMillis()
                interpreter.run(arrayOf(buffer), outputs)
                val latency = System.currentTimeMillis() - start

                val probs = outputs[0] as? FloatArray
                    ?: return@use TaskResult.Error("Unexpected classification output type")

                val top5 = PriorityQueue(compareByDescending<Classification> { it.score })
                probs.forEachIndexed { idx, score ->
                    if (score.isFinite()) {
                        val label = labelList.getOrElse(idx) { "class_$idx" }
                        top5.add(Classification(label, score))
                        if (top5.size > 5) top5.poll()
                    }
                }
                TaskResult.Success(top5.sortedByDescending { it.score }, latency)
            }
        } catch (e: Exception) {
            TaskResult.Error(e.message ?: "Inference failed")
        }
    }
}
