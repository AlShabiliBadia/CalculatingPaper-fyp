package com.example.calculatingpaper.view.components

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun NoteEditorErrorHandling(
    showErrorState: MutableState<Boolean>,
    errorMessage: MutableState<String>,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onNoteContentChange: (String) -> Unit
) {
    var errorText by remember { mutableStateOf("") }

    LaunchedEffect(showErrorState.value) {
        if (showErrorState.value) {
            val currentText = textFieldValue.text
            val cursorPosition = textFieldValue.selection.start
            val lineEnd = currentText.indexOf('\n', startIndex = cursorPosition).let {
                if (it == -1) currentText.length else it
            }

            val insertedErrorText = "\nError: ${errorMessage.value}\n"
            val newTextWithHighlight = StringBuilder(currentText).insert(lineEnd, insertedErrorText).toString()

            onTextFieldValueChange(
                TextFieldValue(
                    text = newTextWithHighlight,
                    selection = TextRange(lineEnd + insertedErrorText.length)
                )
            )
            onNoteContentChange(newTextWithHighlight)

            delay(3000)

            val currentTextWithHighlight = textFieldValue.text
            val cleanText = currentTextWithHighlight.replace(insertedErrorText, "")

            onTextFieldValueChange(
                TextFieldValue(
                    text = cleanText,
                    selection = TextRange(maxOf(0, lineEnd).coerceAtMost(cleanText.length))
                )
            )
            onNoteContentChange(cleanText)

            showErrorState.value = false
        }
    }
}