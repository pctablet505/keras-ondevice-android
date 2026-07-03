package com.example.kerasondevice.inference

/**
 * Allocates a nested FloatArray structure matching a TensorFlow Lite output
 * tensor shape so it can be passed to [LiteRTInterpreter.run].
 */
fun createFloatArray(shape: IntArray): Any {
    return when (shape.size) {
        2 -> Array(shape[0]) { FloatArray(shape[1]) }
        3 -> Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
        4 -> Array(shape[0]) {
            Array(shape[1]) { Array(shape[2]) { FloatArray(shape[3]) } }
        }
        else -> FloatArray(shape.fold(1) { acc, v -> acc * v })
    }
}
