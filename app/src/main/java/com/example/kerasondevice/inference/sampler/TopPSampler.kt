package com.example.kerasondevice.inference.sampler

import kotlin.random.Random

/**
 * Top-P (nucleus) sampler: restrict sampling to the smallest set of tokens
 * whose cumulative probability exceeds [p], then sample proportionally.
 */
class TopPSampler(
    private val p: Float = 0.9f,
    temperature: Float = 1.0f
) : Sampler(temperature) {

    init {
        require(p in 0.0f..1.0f) { "p must be in [0, 1]" }
    }

    override fun getNextToken(probabilities: FloatArray, random: Random): Int {
        val sorted = probabilities
            .withIndex()
            .sortedByDescending { it.value }

        var cumulative = 0.0
        val nucleus = mutableListOf<IndexedValue<Float>>()
        for (entry in sorted) {
            cumulative += entry.value
            nucleus.add(entry)
            if (cumulative >= p) break
        }

        if (nucleus.isEmpty()) {
            return sorted.firstOrNull()?.index ?: 0
        }

        val sum = nucleus.sumOf { it.value.toDouble() }
        if (sum <= 0.0) {
            return nucleus.first().index
        }

        val threshold = random.nextDouble() * sum
        var running = 0.0
        for ((index, prob) in nucleus) {
            running += prob
            if (threshold < running) {
                return index
            }
        }
        return nucleus.last().index
    }
}
