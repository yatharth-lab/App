package com.example.wakeword

import java.util.Locale

class WakeWordDetector {

    // Extended phonetics and common variations/mishearings of "Mr. Stark" by speech engines
    private val activationPhrases = listOf(
        "mr stark",
        "mr. stark",
        "mister stark",
        "hey stark",
        "ok stark",
        "okay stark",
        "stark",
        "mr start",
        "mr spark",
        "mr dark",
        "mr stalk",
        "mister stock",
        "starck",
        "starke"
    )

    data class DetectionResult(
        val isWakeWordDetected: Boolean,
        val extractedCommand: String
    )

    /**
     * Checks if the transcript contains the wake word phrase.
     * If detected, strips the wake word and returns the remaining command.
     */
    fun processTranscript(transcript: String): DetectionResult {
        val lower = transcript.trim().lowercase(Locale.getDefault())

        for (phrase in activationPhrases) {
            if (lower.startsWith(phrase)) {
                val remainder = lower.removePrefix(phrase)
                    .trim()
                    .removePrefix(",")
                    .removePrefix(".")
                    .trim()
                return DetectionResult(isWakeWordDetected = true, extractedCommand = remainder)
            }
            if (lower.contains(phrase)) {
                val index = lower.indexOf(phrase)
                val remainder = lower.substring(index + phrase.length)
                    .trim()
                    .removePrefix(",")
                    .removePrefix(".")
                    .trim()
                return DetectionResult(isWakeWordDetected = true, extractedCommand = remainder)
            }
        }

        return DetectionResult(isWakeWordDetected = false, extractedCommand = transcript.trim())
    }
}
