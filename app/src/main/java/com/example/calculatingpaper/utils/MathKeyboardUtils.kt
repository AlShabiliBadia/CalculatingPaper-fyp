package com.example.calculatingpaper.utils

object MathKeyboardUtils {
    private val SPECIAL_SEQUENCES = listOf(
        "sin(", "arcsin(",
        "cos(", "arccos(",
        "tan(", "arctan(",
        "cot(", "sec(", "csc(",

        "ln(", "log₂(", "log₁₀(",

        "f(x)", "exp(", "abs(",
        "e^", "√("
    ).sortedByDescending { it.length }

    fun findSpecialSequenceBeforeCursor(text: String, cursorPosition: Int): Pair<Int, Int>? {
        if (cursorPosition <= 0) return null

        val textBeforeCursor = text.substring(0, cursorPosition)

        for (sequence in SPECIAL_SEQUENCES) {
            if (textBeforeCursor.endsWith(sequence)) {
                val startPos = cursorPosition - sequence.length
                return Pair(startPos, sequence.length)
            }
        }

        return null
    }
}