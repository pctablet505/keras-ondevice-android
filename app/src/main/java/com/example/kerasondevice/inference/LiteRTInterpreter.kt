package com.example.kerasondevice.inference

import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Generic wrapper around the TensorFlow Lite [Interpreter].
 *
 * The wrapper loads the model file into a memory-mapped buffer and exposes
 * [run] so task implementations can drive inference with their own input/output
 * buffers. It also exposes [getOutputShape] so tasks can allocate output buffers
 * whose dimensions match the loaded model signature.
 */
class LiteRTInterpreter(modelFile: File) : Closeable {

    private val interpreter: Interpreter

    init {
        val buffer = loadModelFile(modelFile)
        interpreter = Interpreter(
            buffer,
            Interpreter.Options().apply { numThreads = 4 }
        )
    }

    fun run(inputs: Array<Any>, outputs: MutableMap<Int, Any>) {
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
    }

    fun getOutputShape(outputIndex: Int): IntArray {
        return interpreter.getOutputTensor(outputIndex).shape()
    }

    override fun close() {
        interpreter.close()
    }

    private fun loadModelFile(file: File): MappedByteBuffer {
        FileInputStream(file).use { stream ->
            val channel = stream.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size()
            )
        }
    }
}
