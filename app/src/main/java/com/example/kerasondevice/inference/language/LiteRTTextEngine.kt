package com.example.kerasondevice.inference.language

import android.content.Context
import android.util.Log
import com.example.kerasondevice.inference.sampler.Sampler
import com.example.kerasondevice.inference.tokenizer.GemmaTokenizer
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-level custom LiteRT text-generation engine.
 *
 * Uses the stock [org.tensorflow.lite.Interpreter] from TensorFlow Lite 2.16.1,
 * a SentencePiece tokenizer, and a pluggable sampler. This path is intentionally
 * kept side-by-side with the optimized [LiteRTLMTextEngine] for comparison.
 */
class LiteRTTextEngine(
    context: Context,
    private val modelFile: File,
    vocabFile: File
) : Closeable {
    private val interpreter: Interpreter
    private val tokenizer = GemmaTokenizer(vocabFile)
    private val closed = AtomicBoolean(false)

    init {
        val buffer = loadModelFile(modelFile)
        interpreter = Interpreter(
            buffer,
            Interpreter.Options().apply { setNumThreads(4) }
        )
    }

    /**
     * Run autoregressive generation.
     *
     * @param prompt Raw user prompt.
     * @param maxLength Max tokens to generate.
     * @param sampler Sampling strategy.
     * @param stripPrompt If true, remove prompt tokens from the returned string.
     * @return Generated text and timing statistics.
     */
    fun generate(
        prompt: String,
        maxLength: Int,
        sampler: Sampler,
        stripPrompt: Boolean
    ): GenerationResult {
        val start = System.currentTimeMillis()
        val text = runGeneration(prompt, maxLength, sampler, stripPrompt)
        val latency = System.currentTimeMillis() - start
        return GenerationResult(text, tokenizer.tokenize(text).size, latency)
    }

    private fun runGeneration(
        prompt: String,
        maxLength: Int,
        sampler: Sampler,
        stripPrompt: Boolean
    ): String {
        val bosTokenId = tokenizer.bosId
        val padTokenId = tokenizer.padId

        val inputTensor = interpreter.getInputTensor(0)
        val shape = inputTensor.shape()
        val seqLength = if (shape.size >= 2 && shape[1] > 1) shape[1] else GemmaTokenizer.SEQUENCE_LENGTH
        Log.d(TAG, "Model input shape=${shape.contentToString()}, using seqLength=$seqLength")

        val promptTokens = listOf(bosTokenId) + tokenizer.tokenize(prompt)
        val initialLength = minOf(promptTokens.size, seqLength)
        Log.d(TAG, "Prompt tokens: $promptTokens")

        val tokenIds = IntArray(seqLength) { padTokenId }
        for (i in promptTokens.indices) {
            if (i < seqLength) tokenIds[i] = promptTokens[i]
        }

        val shortNames = interpreter.getSignatureInputs("serving_default").toList()
        Log.d(TAG, "Signature inputs (short): $shortNames")

        val shortNamesSet = shortNames.toSet()
        val (maskShort, tokenShort) = when {
            "padding_mask" in shortNamesSet && "token_ids" in shortNamesSet ->
                "padding_mask" to "token_ids"
            shortNamesSet == setOf("args_0", "args_1") ->
                "args_1" to "args_0"
            shortNamesSet == setOf("args_0", "args_0_1") ->
                "args_0" to "args_0_1"
            shortNames.size >= 2 ->
                shortNames[0] to shortNames[1]
            else -> throw IllegalStateException("Unexpected signature inputs: $shortNames")
        }
        Log.d(TAG, "Using maskShort=$maskShort, tokenShort=$tokenShort")

        val maskTensorIdx = findTensorIndex(interpreter, maskShort)
        val maskIsBool = interpreter.getInputTensor(maskTensorIdx).dataType() ==
            org.tensorflow.lite.DataType.BOOL
        val inputPaddingMask: Any = if (maskIsBool) {
            Array(1) { BooleanArray(seqLength) }
        } else {
            Array(1) { IntArray(seqLength) }
        }
        val inputTokenIds = Array(1) { tokenIds.copyOf() }
        val outputBuffer = Array(1) { Array(seqLength) { FloatArray(tokenizer.vocabSize) } }

        val inputs = mutableMapOf<String, Any>(
            maskShort to inputPaddingMask,
            tokenShort to inputTokenIds
        )
        val outputs = mapOf<String, Any>(
            "output_0" to outputBuffer
        )

        val stopTokens = setOf(tokenizer.eosId, tokenizer.endOfTurnId)
        var currentLength = initialLength
        val generatedTokens = mutableListOf<Int>()

        while (currentLength < seqLength && generatedTokens.size < maxLength) {
            for (i in 0 until seqLength) {
                if (maskIsBool) {
                    (inputPaddingMask as Array<BooleanArray>)[0][i] = i < currentLength
                } else {
                    (inputPaddingMask as Array<IntArray>)[0][i] = if (i < currentLength) 1 else 0
                }
            }
            System.arraycopy(tokenIds, 0, inputTokenIds[0], 0, seqLength)

            interpreter.runSignature(inputs, outputs, "serving_default")

            val logits = outputBuffer[0][currentLength - 1]
            val probabilities = sampler.computeProbabilities(logits)
            val nextTokenId = sampler.getNextToken(probabilities)

            if (nextTokenId in stopTokens) break

            tokenIds[currentLength] = nextTokenId
            generatedTokens.add(nextTokenId)
            currentLength++
        }

        val allTokenIds = if (stripPrompt) generatedTokens else promptTokens + generatedTokens
        Log.d(TAG, "Generated token IDs: $allTokenIds")
        val result = tokenizer.detokenize(allTokenIds)
        Log.d(TAG, "Generated: $result")
        return result
    }

    private fun findTensorIndex(interpreter: Interpreter, shortName: String): Int {
        for (i in 0 until interpreter.inputTensorCount) {
            val name = interpreter.getInputTensor(i).name()
            if (name.contains(shortName)) return i
        }
        throw IllegalArgumentException("Tensor for $shortName not found")
    }

    private fun loadModelFile(file: File): MappedByteBuffer {
        FileInputStream(file).use {
            val channel = it.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        }
    }

    data class GenerationResult(
        val text: String,
        val tokenCount: Int,
        val latencyMs: Long
    )

    override fun close() {
        if (closed.getAndSet(true)) return
        interpreter.close()
    }

    companion object {
        private const val TAG = "LiteRTTextEngine"
    }
}
