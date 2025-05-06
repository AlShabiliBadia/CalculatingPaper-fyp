package com.example.calculatingpaper.view.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import kotlinx.coroutines.awaitCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.calculatingpaper.view.components.MathKeyboard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorUI(
    noteTitle: String,
    textFieldValue: TextFieldValue,
    isMathKeyboardVisible: Boolean,
    isDegreesModeState: MutableState<Boolean>,
    offsetX: Float,
    offsetY: Float,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    showError: Boolean,
    errorMessage: String,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    performCalculation: () -> Unit,
    showMathKeyboard: () -> Unit,
    showSystemKeyboard: () -> Unit,
    onKeyPress: (String) -> Unit,
    onSpecialKeyPress: (String) -> Unit,
    onCloseKeyboard: () -> Unit,
    onSetDegreesMode: (Boolean) -> Unit,
    onUpdateOffsets: (Float, Float) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val customKeyboardHeight = 420.dp
    val scrollState = rememberScrollState()
    var hasFocus by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text(noteTitle, maxLines = 1) },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Save and Close Editor",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = if (isMathKeyboardVisible) customKeyboardHeight else 0.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {
                    TextField(
                        value = textFieldValue,
                        onValueChange = onValueChange,
                        enabled = true,
                        placeholder = { Text("Enter your notes and calculations...") },
                        keyboardOptions = KeyboardOptions.Default,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                hasFocus = focusState.isFocused
                                if (focusState.isFocused && isMathKeyboardVisible) {
                                    scope.launch {
                                        keyboardController?.hide()
                                        kotlinx.coroutines.delay(10)
                                        keyboardController?.hide()
                                    }
                                }
                            }
                            .pointerInput(isMathKeyboardVisible) {
                                detectTapGestures(
                                    onTap = {
                                        if (isMathKeyboardVisible) {
                                            focusRequester.requestFocus()
                                            scope.launch {
                                                keyboardController?.hide()
                                                kotlinx.coroutines.delay(10)
                                                keyboardController?.hide()
                                            }
                                        }
                                    }
                                )
                            },
                        interactionSource = remember { MutableInteractionSource() },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (!isMathKeyboardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onUpdateOffsets(offsetX + dragAmount.x, offsetY + dragAmount.y)
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = performCalculation) {
                            Text("Calculate")
                        }
                        Button(onClick = {
                            showMathKeyboard()
                        }) {
                            Text("Keyboard")
                        }
                    }
                }
            }
            if (isMathKeyboardVisible) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    MathKeyboard(
                        onKeyPress = onKeyPress,
                        onSpecialKeyPress = onSpecialKeyPress,
                        onCalculate = performCalculation,
                        onCloseKeyboard = onCloseKeyboard,
                        onToggleMode = onSetDegreesMode,
                        isDegreesMode = isDegreesModeState,
                        keyboardController = keyboardController,
                        onToggleToSystemKeyboard = showSystemKeyboard
                    )
                }
            }
        }
    }
