package com.example.calculatingpaper.view.screens
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.data.Note
import com.example.calculatingpaper.utils.*
import com.example.calculatingpaper.view.components.NoteEditorErrorHandling
import com.example.calculatingpaper.view.components.NoteEditorLifecycleObserver
import com.example.calculatingpaper.viewmodel.NoteEditorViewModel
import com.example.calculatingpaper.viewmodel.NoteViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import androidx.navigation.NavController
import com.example.calculatingpaper.AppDestinations
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun findPrecedingNumber(text: String, cursorPos: Int): Pair<String, Int>? {
    if (cursorPos == 0) return null
    var numStartIndex = -1
    var i = cursorPos - 1
    while (i >= 0) {
        val char = text[i]
        if (char.isDigit() || char == '.') {
            numStartIndex = i
            i--
        } else {
            break
        }
    }
    if (numStartIndex != -1) {
        val numberStr = text.substring(numStartIndex, cursorPos)
        if (numberStr.toBigDecimalOrNull() != null) {
            return Pair(numberStr, numStartIndex)
        }
    }
    return null
}

@Composable
fun NoteEditorScreen(
    note: Note?,
    currentFolderId: Long,
    viewModel: NoteViewModel,
    onSaveAction: suspend (Note) -> Unit,
    onCloseAction: () -> Unit,
    onCalculate: () -> Unit,
    navController: NavController,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    appPreferences: AppPreferences = AppPreferences(LocalContext.current)
) {
    val noteEditorViewModel: NoteEditorViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(appPreferences) as T
        }
    })
    val scope = rememberCoroutineScope()
    var textFieldValue by remember {
        val initialContent = note?.content ?: ""
        mutableStateOf(TextFieldValue(initialContent, TextRange(initialContent.length)))
    }
    var noteContent by remember { mutableStateOf(note?.content ?: "") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isDegreesModeState = remember {
        mutableStateOf(appPreferences.getCalculationMode())
    }
    var isDegreesMode by isDegreesModeState
    val isMathKeyboardVisibleState = remember { mutableStateOf(false) }
    var isMathKeyboardVisible by isMathKeyboardVisibleState

    var isInitialSetupComplete by remember { mutableStateOf(true) }

    var offsetX by rememberSaveable { mutableStateOf(0f) }
    var offsetY by rememberSaveable { mutableStateOf(0f) }
    val showErrorState: MutableState<Boolean> = remember { mutableStateOf(false) }
    val errorMessageState: MutableState<String> = remember { mutableStateOf("") }
    val originalContent = remember(note?.id) { note?.content ?: "" }
    val originalTitle = remember(note?.id) { note?.title ?: "" }
    var noteTitle by remember(note?.id) {
        mutableStateOf(note?.title ?: NoteUtils.generateTitleFromContent(noteContent))
    }
    val noteContentState = rememberUpdatedState(noteContent)
    val noteTitleState = rememberUpdatedState(noteTitle)
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current



    NoteEditorErrorHandling(
        showErrorState = showErrorState,
        errorMessage = errorMessageState,
        textFieldValue = textFieldValue,
        onTextFieldValueChange = { textFieldValue = it },
        onNoteContentChange = { noteContent = it }
    )

    LaunchedEffect(isMathKeyboardVisible) {
        if (isMathKeyboardVisible) {
            keyboardController?.hide()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel, note, appPreferences) {
        val observer = NoteEditorLifecycleObserver(
            noteViewModel = viewModel,
            note = note,
            noteTitleState = noteTitleState,
            noteContentState = noteContentState,
            originalTitle = originalTitle,
            originalContent = originalContent,
            currentFolderId = currentFolderId,
            appPreferences = appPreferences
        )
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    fun handleGraphRequest() {
        val graphResult = noteEditorViewModel.prepareGraphDataFromSelection(
            textFieldValue.text,
            textFieldValue.selection,
            isDegreesMode
        )

        if (graphResult != null) {

            val stringVariables = graphResult.variables.mapValues { it.value.toPlainString() }
            val variablesJson = Json.encodeToString(stringVariables)

            navController.navigate(
                AppDestinations.createGraphRoute(
                    graphResult.equation,
                    note?.id ?: 0L,
                    variablesJson
                )
            )
        } else {
            errorMessageState.value = "Invalid equation or selection for graphing."
            showErrorState.value = true
        }
    }


    fun showMathKeyboard() {
        if (isInitialSetupComplete) {
            scope.launch {
                keyboardController?.hide()
                focusManager.clearFocus()
                delay(100)
                isMathKeyboardVisible = true
                delay(50)
                focusRequester.requestFocus()
            }
        }
    }

    fun showSystemKeyboard() {
        if (isInitialSetupComplete) {
            scope.launch {
                isMathKeyboardVisible = false
                delay(50)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    val onCloseKeyboard: () -> Unit = {
        if (isInitialSetupComplete) {
            scope.launch {
                keyboardController?.hide()
                delay(50)
                isMathKeyboardVisible = false
                focusManager.clearFocus()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isMathKeyboardVisible) {
                keyboardController?.hide()
            }
        }
    }

    fun performCalculation() {
        try {
            val result = noteEditorViewModel.performCalculation(
                textFieldValue.text,
                textFieldValue.selection,
                isDegreesMode
            )
            noteContent = result.updatedContent
            textFieldValue = textFieldValue.copy(
                text = result.updatedContent,
                selection = result.newSelection
            )
            if (originalTitle.isBlank() && (note?.id == null || note.id == 0L)) {
                noteTitle = NoteUtils.generateTitleFromContent(noteContent)
            }
            showErrorState.value = false
            onCalculate()
        } catch (e: Exception) {
            errorMessageState.value = e.message ?: "Calculation error"
            showErrorState.value = true
        }
    }
    suspend fun handleCloseAndSave() {
        val lastOpened = appPreferences.getLastOpenedItem()
        val noteIdToCheck = note?.id ?: if (lastOpened.first == AppPreferences.TYPE_NOTE) lastOpened.second else 0L
        val actualCurrentNote = if (noteIdToCheck > 0L) viewModel.getNoteById(noteIdToCheck) else null
        val isNewNote = actualCurrentNote == null
        val currentContent = textFieldValue.text
        val currentTitleText = noteTitle
        val trimmedContent = currentContent.trimEnd { it == '\n' || it == '\r' }
        val finalTitle = if (currentTitleText.isBlank()) NoteUtils.generateTitleFromContent(trimmedContent) else currentTitleText
        if (trimmedContent.isBlank()) {
            if (!isNewNote && actualCurrentNote != null) {
                viewModel.deleteNotePermanently(actualCurrentNote)
                if (lastOpened.first == AppPreferences.TYPE_NOTE && lastOpened.second == actualCurrentNote.id) {
                    appPreferences.clearLastOpenedItem()
                }
            }
            onCloseAction()
            return
        }
        val isContentChanged = trimmedContent != originalContent
        val isTitleChanged = finalTitle != originalTitle
        val isMeaningfulChange = isContentChanged || isTitleChanged
        if (!isMeaningfulChange && !isNewNote) {
            onCloseAction()
            return
        }
        val noteToSave = Note(
            id = actualCurrentNote?.id ?: 0,
            title = finalTitle,
            content = trimmedContent,
            timestamp = System.currentTimeMillis(),
            isPinned = actualCurrentNote?.isPinned ?: false,
            isArchived = actualCurrentNote?.isArchived ?: false,
            isInTrash = actualCurrentNote?.isInTrash ?: false,
            parentId = actualCurrentNote?.parentId ?: currentFolderId,
            cloudId = actualCurrentNote?.cloudId
        )
        try {
            var savedNoteId = noteToSave.id
            if (isNewNote) {
                savedNoteId = viewModel.addNote(noteToSave)
                if (savedNoteId > 0L) {
                    appPreferences.saveLastOpenedItem(AppPreferences.TYPE_NOTE, savedNoteId)
                }
            } else {
                viewModel.updateNote(noteToSave)
                if (noteToSave.id > 0L) {
                    appPreferences.saveLastOpenedItem(AppPreferences.TYPE_NOTE, noteToSave.id)
                }
            }
        } catch (e: Exception) {
            println("Error during manual save: ${e.message}")
        }
        focusManager.clearFocus()
        if (keyboardController != null) {
            keyboardController.hide()
        }
        onCloseAction()
    }

    var titleUpdateJob by remember { mutableStateOf<Job?>(null) }
    val debounceDelay = 750L

    fun handleValueChange(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = newValue.text
        val oldCursor = textFieldValue.selection.start
        val newCursor = newValue.selection.start

        if (newText.length < oldText.length && newCursor < oldCursor) {
            if (oldText.length - newText.length == 1) {
                val specialSequence = MathKeyboardUtils.findSpecialSequenceBeforeCursor(oldText, oldCursor)

                if (specialSequence != null) {
                    val (seqStart, seqLength) = specialSequence
                    val textWithSequenceRemoved = oldText.removeRange(seqStart, seqStart + seqLength)
                    textFieldValue = TextFieldValue(textWithSequenceRemoved, TextRange(seqStart))
                    noteContent = textWithSequenceRemoved
                    return
                }
            }
        }

        textFieldValue = newValue
        val newContent = newValue.text
        noteContent = newContent

        titleUpdateJob?.cancel()
        titleUpdateJob = scope.launch {
            delay(debounceDelay)
            if (originalTitle.isBlank() && (note?.id == null || note.id == 0L)) {
                val currentTitle = noteTitleState.value
                if (currentTitle.isBlank() || currentTitle == NoteUtils.generateTitleFromContent(originalContent)) {
                    val generatedTitle = NoteUtils.generateTitleFromContent(newContent)
                    if(generatedTitle.isNotBlank()){
                        noteTitle = generatedTitle
                    }
                }
            }
        }
    }
    fun handleKeyPress(key: String) {
        val selection = textFieldValue.selection
        val currentText = textFieldValue.text
        val start = min(selection.start, selection.end)
        val end = max(selection.start, selection.end)
        val newText = currentText.replaceRange(start, end, key)
        val newCursorPosition = start + key.length
        noteContent = newText
        textFieldValue = TextFieldValue(newText, TextRange(newCursorPosition))
    }


    fun handleSpecialKeyPress(key: String) {
        val currentTFV = textFieldValue
        var newTFV = currentTFV
        when (key) {
            UIConstants.LEFT_ARROW -> {
                val newPos = (currentTFV.selection.start - 1).coerceAtLeast(0)
                newTFV = currentTFV.copy(selection = TextRange(newPos))
            }
            UIConstants.RIGHT_ARROW -> {
                val currentLength = currentTFV.text.length
                val newPos = (currentTFV.selection.start + 1).coerceAtMost(currentLength)
                newTFV = currentTFV.copy(selection = TextRange(newPos))
            }
            UIConstants.RETURN -> {
                val start = min(currentTFV.selection.start, currentTFV.selection.end)
                val end = max(currentTFV.selection.start, currentTFV.selection.end)
                val newText = currentTFV.text.replaceRange(start, end, "\n")
                val newPos = start + 1
                newTFV = TextFieldValue(newText, TextRange(newPos))
            }
            UIConstants.BACKSPACE -> {
                val sel = currentTFV.selection
                val text = currentTFV.text
                val start = min(sel.start, sel.end)
                val end = max(sel.start, sel.end)

                if (start == end && start > 0) {
                    val specialSequence = MathKeyboardUtils.findSpecialSequenceBeforeCursor(text, start)

                    if (specialSequence != null) {
                        val (seqStart, seqLength) = specialSequence
                        val newText = text.removeRange(seqStart, seqStart + seqLength)
                        val newPos = seqStart
                        newTFV = TextFieldValue(newText, TextRange(newPos))
                    } else {
                        val newText = text.removeRange(start - 1, start)
                        val newPos = start - 1
                        newTFV = TextFieldValue(newText, TextRange(newPos))
                    }
                } else if (start < end) {
                    val newText = text.removeRange(start, end)
                    val newPos = start
                    newTFV = TextFieldValue(newText, TextRange(newPos))
                }
            }
        }
        if (newTFV != currentTFV) {
            noteContent = newTFV.text
            textFieldValue = newTFV
        }
    }


    NoteEditorUI(
        noteTitle = noteTitle,
        textFieldValue = textFieldValue,
        isMathKeyboardVisible = isMathKeyboardVisible,
        isDegreesModeState = isDegreesModeState,
        offsetX = offsetX,
        offsetY = offsetY,
        focusRequester = focusRequester,
        focusManager = focusManager,
        showError = showErrorState.value,
        errorMessage = errorMessageState.value,
        onSave = { coroutineScope.launch { handleCloseAndSave() } },
        onClose = onCloseAction,
        onValueChange = { handleValueChange(it) },
        performCalculation = { performCalculation() },
        showMathKeyboard = ::showMathKeyboard,
        showSystemKeyboard = ::showSystemKeyboard,
        onKeyPress = { handleKeyPress(it) },
        onSpecialKeyPress = { handleSpecialKeyPress(it) },
        onCloseKeyboard = onCloseKeyboard,
        onSetDegreesMode = { newMode ->
            isDegreesMode = newMode
            appPreferences.saveCalculationMode(newMode)
        },
        onUpdateOffsets = { newX, newY ->
            offsetX = newX
            offsetY = newY
        },
        onGraphRequest = { handleGraphRequest() }
    )
}

