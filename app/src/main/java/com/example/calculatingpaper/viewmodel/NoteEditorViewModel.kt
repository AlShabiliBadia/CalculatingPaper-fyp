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

data class GraphResult(
    val equation: String,
    val variables: Map<String, BigDecimal>
)

class NoteEditorViewModel(private val appPreferences: AppPreferences) : ViewModel() {
    private val globalVariables = mutableMapOf<String, BigDecimal>()

    fun performCalculation(text: String, selection: TextRange, isDegreesMode: Boolean): CalculationResult {
        try {
            val precision = AppPreferences.decimalPrecision
            val initialLines = text.split("\n")
            val lines = initialLines.toMutableList()

            val startLineIndexOfSelection = text.substring(0, selection.start).count { it == '\n' }
            val endLineIndexOfSelection = text.substring(0, selection.end).count { it == '\n' }

            val safeStartLine = startLineIndexOfSelection.coerceIn(0, if (lines.isEmpty()) 0 else lines.size - 1)
            val safeEndLine = endLineIndexOfSelection.coerceIn(0, if (lines.isEmpty()) 0 else lines.size - 1)

            val indicesToProcess = if (lines.isEmpty()) emptyList() else (safeStartLine..safeEndLine).toList().distinct()

            val outputLinesToAdd = mutableListOf<Pair<Int, String>>()

            for (index in indicesToProcess) {
                if (index >= lines.size) continue
                val lineContent = lines[index]
                val trimmedLine = lineContent.trim()

                if (trimmedLine.isEmpty()) continue

                val assignmentRegex = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$")
                val incompleteAssignmentRegex = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*$")

                val assignmentMatch = assignmentRegex.matchEntire(trimmedLine)
                val incompleteAssignmentMatch = incompleteAssignmentRegex.matchEntire(trimmedLine)

                if (assignmentMatch != null) {
                    val variableName = assignmentMatch.groupValues[1].trim()
                    val expressionOnRHS = assignmentMatch.groupValues[2].trim()
                    try {
                        val resultValue = evaluateExpression(expressionOnRHS, globalVariables, isDegreesMode, precision)
                        globalVariables[variableName] = resultValue
                    } catch (e: Exception) {
                        outputLinesToAdd.add(index to "Error: ${e.message}")
                    }
                } else if (incompleteAssignmentMatch != null) {
                    val variableName = incompleteAssignmentMatch.groupValues[1].trim()
                    outputLinesToAdd.add(index to "Error: Incomplete assignment for $variableName.")
                } else {
                    val literalValue = trimmedLine.toBigDecimalOrNull()
                    if (literalValue != null) {
                    } else {
                        try {
                            val resultValue = evaluateExpression(trimmedLine, globalVariables, isDegreesMode, precision)
                            outputLinesToAdd.add(index to formatResult(resultValue, precision))
                        } catch (e: Exception) {
                            outputLinesToAdd.add(index to "Error: ${e.message}")
                        }
                    }
                }
            }

            outputLinesToAdd.sortByDescending { it.first }
            for ((originalIndex, stringToInsert) in outputLinesToAdd) {
                lines.add(originalIndex + 1, stringToInsert)
            }

            val updatedContent = lines.joinToString("\n")
            var newCursorPosition = updatedContent.length

            if (outputLinesToAdd.isNotEmpty()) {
                val highestOriginalIndexProducingOutput = outputLinesToAdd.map { it.first }.maxOrNull()
                if (highestOriginalIndexProducingOutput != null) {
                    var finalIndexOfInterest = -1
                    var insertionsSoFar = 0
                    for (i in initialLines.indices) {
                        if (i == highestOriginalIndexProducingOutput) {
                            finalIndexOfInterest = i + 1 + insertionsSoFar
                            break
                        }
                        if (outputLinesToAdd.any { it.first == i }) {
                            insertionsSoFar++
                        }
                    }
                    if (finalIndexOfInterest != -1 && finalIndexOfInterest < lines.size) {
                        newCursorPosition = lines.take(finalIndexOfInterest + 1).joinToString("\n").length
                    } else {
                        newCursorPosition = updatedContent.length
                    }
                }
            } else {
                val lastProcessedOriginalIndex = indicesToProcess.maxOrNull() ?: -1
                if (lastProcessedOriginalIndex != -1) {
                    val targetLineForCursor = if (lastProcessedOriginalIndex + 1 < lines.size) {
                        lastProcessedOriginalIndex + 1
                    } else {
                        lastProcessedOriginalIndex
                    }
                    if (targetLineForCursor < lines.size) {
                        newCursorPosition = lines.take(targetLineForCursor + 1).joinToString("\n").length
                    } else {
                        newCursorPosition = updatedContent.length
                    }
                }
            }

            return CalculationResult(updatedContent, TextRange(newCursorPosition.coerceIn(0, updatedContent.length)))

        } catch (e: Exception) {
            throw Exception(e.message ?: "Unknown error during calculation")
        }
    }


    fun prepareGraphDataFromSelection(text: String, selection: TextRange, isDegreesMode: Boolean): GraphResult? {
        val selectedText = if (selection.collapsed) {
            val lineStart = text.lastIndexOf('\n', selection.start - 1).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', selection.start).let { if (it == -1) text.length else it }
            text.substring(lineStart, lineEnd)
        } else {
            text.substring(selection.start, selection.end)
        }

        val contextLines = selectedText.lines().filter { it.isNotBlank() }
        if (contextLines.isEmpty()) {
            return null
        }

        val equation = contextLines.last()
        val variableDefinitionLines = contextLines.dropLast(1)

        val finalVariables = globalVariables.toMutableMap()
        val precision = AppPreferences.decimalPrecision

        val assignmentRegex = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$")
        variableDefinitionLines.forEach { line ->
            assignmentRegex.matchEntire(line.trim())?.let { match ->
                val name = match.groupValues[1]
                val expr = match.groupValues[2]
                try {
                    val value = evaluateExpression(expr, finalVariables, isDegreesMode, precision)
                    finalVariables[name] = value
                } catch (e: Exception) {
                    return null
                }
            }
        }

        return GraphResult(equation, finalVariables)
    }
}