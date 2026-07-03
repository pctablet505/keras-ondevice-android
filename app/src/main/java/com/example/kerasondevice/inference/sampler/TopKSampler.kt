package com.example.kerasondevice.inference.sampler

import kotlin.random.Random

/**
 * Top-K sampler: restrict sampling to the K most likely tokens and sample
 * proportionally within that set.
 */
class TopKSampler(
    private val k: Int = 50,
    temperature: Float = 1.0f
) : Sampler(temperature) {

    override fun getNextToken(probabilities: FloatArray, random: Random): Int {
        require(k >= 1) { "k must be >= 1" }

        val topK = probabilities
            .withIndex()
            .sortedByDescending { it.value }
            .take(k.coerceAtMost(probabilities.size))

        val sum = topK.sumOf { it.value.toDouble() }
        if (sum <= 0.0) {
            return topK.firstOrNull()?.index ?: 0
        }

        val threshold = random.nextDouble() * sum
        var cumulative = 0.0
        for ((index, prob) in topK) {
            cumulative += prob
            if (threshold < cumulative) {
                return index
            }
        }
        return topK.last().index
    }
}
