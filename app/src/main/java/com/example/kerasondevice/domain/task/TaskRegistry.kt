package com.example.kerasondevice.domain.task

import com.example.kerasondevice.domain.model.ModelHandle
import com.example.kerasondevice.inference.tasks.DepthEstimationTask
import com.example.kerasondevice.inference.tasks.ImageClassificationTask
import com.example.kerasondevice.inference.tasks.ObjectDetectionTask
import com.example.kerasondevice.inference.tasks.SegmentationTask

/**
 * Registry of every vision task demo in the catalog.
 *
 * Tasks are keyed by the [DemoEntry.VisionEntry.taskId] used by the catalog,
 * and know how to locate their label files relative to the model file.
 */
object TaskRegistry {

    private val tasks = listOf(
        ImageClassificationTask { file -> file.resolveSibling("imagenet_labels.txt").readLines() },
        ObjectDetectionTask { file -> file.resolveSibling("coco_labels.txt").readLines() },
        SegmentationTask(),
        DepthEstimationTask()
    )

    private val taskMap = tasks.associateBy { it.id }

    fun get(id: String): Task<*, *>? = taskMap[id]

    /**
     * Returns true when [model] is usable for [task].
     *
     * A model matches when its sidecar config names the task (with a few
     * common aliases) or when the model filename appears in the task's
     * [Task.preferredModels] list.
     */
    fun matches(task: Task<*, *>, model: ModelHandle): Boolean {
        val configTask = model.config?.task
        if (configTask != null && taskIdMatches(configTask, task.id)) {
            return true
        }
        return task.preferredModels.any { preferred ->
            model.file.name.equals(preferred, ignoreCase = true)
        }
    }

    private fun taskIdMatches(configTask: String, taskId: String): Boolean {
        val normalizedConfig = configTask.lowercase().replace(Regex("[- ]"), "_")
        val normalizedId = taskId.lowercase()
        return normalizedConfig == normalizedId ||
            normalizedConfig == "image_$normalizedId" ||
            normalizedConfig == "object_$normalizedId" ||
            normalizedConfig == "semantic_$normalizedId" ||
            normalizedConfig == "depth_estimation" && normalizedId == "depth"
    }
}
