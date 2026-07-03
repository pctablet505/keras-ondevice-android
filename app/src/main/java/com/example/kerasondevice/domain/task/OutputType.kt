package com.example.kerasondevice.domain.task

sealed class OutputType<O> {
    data object ClassificationList : OutputType<List<Classification>>()
    data object BoundingBoxList : OutputType<List<BoundingBox>>()
    data object Mask : OutputType<MaskResult>()
    data object DepthMap : OutputType<DepthResult>()
}

data class Classification(val label: String, val score: Float)

data class BoundingBox(
    val label: String,
    val score: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

data class MaskResult(
    val mask: IntArray,
    val width: Int,
    val height: Int,
    val classPixelCounts: Map<String, Int>
)

data class DepthResult(
    val depthMap: FloatArray,
    val width: Int,
    val height: Int
)
