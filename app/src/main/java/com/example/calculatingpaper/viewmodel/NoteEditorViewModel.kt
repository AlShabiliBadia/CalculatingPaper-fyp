package com.example.calculatingpaper.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.view.components.evaluateExpression
import com.example.calculatingpaper.view.components.formatResult
import java.math.BigDecimal

data class CalculationResult(
    val updatedContent: String,
    val newSelection: TextRange
)

class NoteEditorViewModel(private val appPreferences: AppPreferences) : ViewModel() {
    private val variables = mutableMapOf<String, BigDecimal>()

    fun performCalculation(text: String, selection: TextRange, isDegreesMode: Boolean): CalculationResult {
        try {
            val precision = AppPreferences.decimalPrecision
            val lines = text.split("\n").toMutableList()
            val startLineIndex = text.substring(0, selection.start).count { it == '\n' }
            val endLineIndex = text.substring(0, selection.end).count { it == '\n' }
            val indicesToProcess = (startLineIndex..endLineIndex).toList()
            val results = mutableListOf<Pair<Int, String>>()
            for (index in indicesToProcess) {
                val line = lines[index].trim()
                if (line.matches(Regex("^[a-zA-Z]+\\s*=.*"))) {
                    val parts = line.split("=")
                    if (parts.size == 2) {
                        val variableName = parts[0].trim()
                        val expression = parts[1].trim()
                        val result = evaluateExpression(line, variables, isDegreesMode, precision)
                        variables[variableName] = result
                    }
                }
            }
            for (index in indicesToProcess) {
                val line = lines[index].trim()
                if (!line.matches(Regex("^[a-zA-Z]+\\s*=.*")) && line.isNotEmpty()) {
                    val result = evaluateExpression(line, variables, isDegreesMode, precision)
                    results.add(index to formatResult(result, precision))
                }
            }
            for ((index, result) in results.asReversed()) {
                lines.add(index + 1, result)
            }
            val updatedContent = lines.joinToString("\n")
            val newSelection = TextRange(updatedContent.length)
            return CalculationResult(updatedContent, newSelection)
        } catch (e: Exception) {
            throw Exception(e.message ?: "Unknown error during calculation")
        }
    }




}
