package com.example.calculatingpaper.view.components

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay

@Composable
fun NoteEditorErrorHandling(
    showErrorState: MutableState<Boolean>,
    errorMessage: MutableState<String>,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onNoteContentChange: (String) -> Unit
) {
    if (showErrorState.value) {
        LaunchedEffect(key1 = showErrorState.value) {
            val currentText = textFieldValue.text
            val cursorPosition = textFieldValue.selection.start
            val lineEnd = currentText.indexOf('\n', startIndex = cursorPosition).let {
                if (it == -1) currentText.length else it
            }

            val errorWithNewLine = "\nError: ${errorMessage.value}\n"
            val newText = StringBuilder(currentText)
                .insert(lineEnd, errorWithNewLine)
                .toString()

            val newTextFieldValue = textFieldValue.copy(
                text = newText,
                selection = TextRange(lineEnd + errorWithNewLine.length)
            )
            onTextFieldValueChange(newTextFieldValue)
            onNoteContentChange(newText)

            delay(1000)

            if (newTextFieldValue.text.contains(errorWithNewLine)) {
                val cleanText = newTextFieldValue.text.replace(errorWithNewLine, "")
                onTextFieldValueChange(newTextFieldValue.copy(
                    text = cleanText,
                    selection = TextRange(maxOf(0, lineEnd))
                ))
                onNoteContentChange(cleanText)
            }

            showErrorState.value = false
        }
    }
}