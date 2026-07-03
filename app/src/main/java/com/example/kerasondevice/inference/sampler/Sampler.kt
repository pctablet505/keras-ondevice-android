package com.example.kerasondevice.inference.sampler

import kotlin.math.exp
import kotlin.random.Random

/**
 * Base sampler class matching keras-hub's Sampler interface.
 * Subclasses implement [getNextToken] to define the sampling strategy.
 */
abstract class Sampler(val temperature: Float = 1.0f) {

    /**
     * Convert logits to a probability distribution with temperature scaling.
     * Always done in float32 precision.
     */
    fun computeProbabilities(logits: FloatArray): FloatArray {
        val scaled = if (temperature != 1.0f) {
            FloatArray(logits.size) { i -> logits[i] / temperature }
        } else {
            logits.copyOf()
        }
        val maxLogit = scaled.maxOrNull() ?: 0f
        var sum = 0.0
        val expScaled = DoubleArray(scaled.size) { i ->
            val e = exp((scaled[i] - maxLogit).toDouble())
            sum += e
            e
        }
        return FloatArray(expScaled.size) { i -> (expScaled[i] / sum).toFloat() }
    }

    /**
     * Select the next token ID given a probability distribution.
     */
    abstract fun getNextToken(probabilities: FloatArray, random: Random = Random.Default): Int
}
