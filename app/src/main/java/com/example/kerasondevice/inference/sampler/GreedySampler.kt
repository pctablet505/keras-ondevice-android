package com.example.kerasondevice.inference.sampler

import kotlin.random.Random

/**
 * Greedy sampler: always picks the token with the highest probability.
 */
class GreedySampler(temperature: Float = 1.0f) : Sampler(temperature) {
    override fun getNextToken(probabilities: FloatArray, random: Random): Int {
        var maxIdx = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }
        return maxIdx
    }
}
