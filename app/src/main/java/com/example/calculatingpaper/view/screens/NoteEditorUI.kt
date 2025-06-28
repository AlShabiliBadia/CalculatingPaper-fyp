package com.example.calculatingpaper.view.screens

import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.calculatingpaper.view.components.MathKeyboard
import kotlinx.coroutines.launch

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.navigation.NavController
import com.example.calculatingpaper.AppDestinations

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
    onGraphRequest: () -> Unit

) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    var hasFocus by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    var keyboardHeightDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    LaunchedEffect(isMathKeyboardVisible) {
        if (isMathKeyboardVisible) {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(textFieldValue.selection, isMathKeyboardVisible) {
        if (isMathKeyboardVisible) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    val pointerInputModifier = Modifier.pointerInput(isMathKeyboardVisible) {
        detectTapGestures(
            onTap = { offset ->
                if (isMathKeyboardVisible) {
                    keyboardController?.hide()
                } else {
                    focusRequester.requestFocus()
                }
            }
        )
    }

    LaunchedEffect(interactionSource, isMathKeyboardVisible) {
        interactionSource.interactions.collect { interaction ->
            if (isMathKeyboardVisible) {
                keyboardController?.hide()
            }
        }
    }

    LaunchedEffect(hasFocus, isMathKeyboardVisible) {
        if (hasFocus && isMathKeyboardVisible) {
            keyboardController?.hide()
        }
    }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isMathKeyboardVisible) keyboardHeightDp else 0.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    enabled = true,
                    placeholder = { Text("Enter your notes and calculations...") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = if (isMathKeyboardVisible) ImeAction.None else ImeAction.Default
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            hasFocus = focusState.isFocused
                            if (isMathKeyboardVisible && focusState.isFocused) {
                                keyboardController?.hide()
                            }
                        },
                    interactionSource = interactionSource,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onUpdateOffsets(offsetX + dragAmount.x, offsetY + dragAmount.y)
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isMathKeyboardVisible) {
                    Button(
                        onClick = performCalculation,
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Calculate")
                    }
                    Button(
                        onClick = { showMathKeyboard() },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Keyboard")
                    }
                }
                Button(
                    onClick = { onGraphRequest() },
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("Graph")
                }
            }

            if (isMathKeyboardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            keyboardHeightDp = with(density) { coordinates.size.height.toDp() }
                        }
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
}
