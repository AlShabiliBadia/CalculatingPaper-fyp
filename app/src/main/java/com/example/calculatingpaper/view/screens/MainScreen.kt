package com.example.calculatingpaper.view.screens
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calculatingpaper.AppDestinations
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.data.Folder
import com.example.calculatingpaper.data.Note
import com.example.calculatingpaper.view.components.*
import com.example.calculatingpaper.view.components.RestoreDestinationSelector
import com.example.calculatingpaper.view.components.RestorableItem
import com.example.calculatingpaper.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import android.util.Log
sealed class ItemToMove {
    data class NoteItem(val note: Note) : ItemToMove()
    data class FolderItem(val folder: Folder) : ItemToMove()
}
object SpecialFolders {
    const val ARCHIVE: Long = -1L
    const val TRASH: Long = -2L
    const val ROOT: Long = 0L
    fun isSpecial(id: Long) = id == ARCHIVE || id == TRASH
    fun isArchive(id: Long) = id == ARCHIVE
    fun isTrash(id: Long) = id == TRASH
    fun isRoot(id: Long) = id == ROOT
}
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NoteViewModel,
    context: Context,
    navController: NavController
) {
    var currentFolderId by remember { mutableStateOf(SpecialFolders.ROOT) }
    var folderPath by remember { mutableStateOf<List<Folder>>(emptyList()) }
    val currentFolderContents by viewModel.currentFolderContents.collectAsState()
    val currentFolderTotalItemCounts by viewModel.currentFolderTotalItemCounts.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val archivedFolders by viewModel.archivedFolders.collectAsState()
    val trashNotes by viewModel.trashNotes.collectAsState()
    val trashedFolders by viewModel.trashedFolders.collectAsState()
    val appPreferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current
    val folderRequiringReparenting by viewModel.folderRequiringReparentingOnRestore.collectAsState()
    var showFolderRestoreSheet by remember { mutableStateOf(false) }
    val folderRestoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val noteRequiringReparenting by viewModel.noteRequiringReparentingOnRestore.collectAsState()
    var showNoteRestoreSheet by remember { mutableStateOf(false) }
    val noteRestoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var listRefreshKey by remember { mutableStateOf(false) }
    val currentRegularNotes = remember(currentFolderContents, listRefreshKey) { currentFolderContents.first }
    val currentRegularFolders = remember(currentFolderContents, listRefreshKey) { currentFolderContents.second }
    fun shareNote(note: Note) {
        shareNoteContent(localContext, note)
    }
    val displayedNotes = remember(currentRegularNotes, currentFolderId, archivedNotes, trashNotes) {
        when {
            SpecialFolders.isArchive(currentFolderId) -> archivedNotes
            SpecialFolders.isTrash(currentFolderId) -> trashNotes
            else -> currentRegularNotes.filter { !it.isArchived && !it.isInTrash && it.parentId == currentFolderId }
        }
    }
    val displayedFolders = remember(currentRegularFolders, currentFolderId, archivedFolders, trashedFolders) {
        when {
            SpecialFolders.isArchive(currentFolderId) -> archivedFolders
            SpecialFolders.isTrash(currentFolderId) -> trashedFolders
            else -> currentRegularFolders.filter { !it.isArchived && !it.isInTrash && it.parentId == currentFolderId }
        }
    }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf<Note?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var currentlySwipedNoteId by remember { mutableStateOf<Long?>(null) }
    var currentlySwipedFolderId by remember { mutableStateOf<Long?>(null) }
    var currentlyRenamingNoteId by remember { mutableStateOf<Long?>(null) }
    var currentlyRenamingFolderId by remember { mutableStateOf<Long?>(null) }
    var showConfirmEmptyTrashDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<ItemToMove?>(null) }
    val moveSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(folderRequiringReparenting) {
        if (folderRequiringReparenting != null) {
            showFolderRestoreSheet = true
            scope.launch { folderRestoreSheetState.show() }
        } else {
            if (showFolderRestoreSheet) {
                scope.launch { folderRestoreSheetState.hide() }.invokeOnCompletion {
                    if (!folderRestoreSheetState.isVisible) showFolderRestoreSheet = false
                }
            }
        }
    }
    LaunchedEffect(noteRequiringReparenting) {
        if (noteRequiringReparenting != null) {
            Log.d("MainScreen", "Note requiring reparenting: ${noteRequiringReparenting!!.title}")
            showNoteRestoreSheet = true
            scope.launch { noteRestoreSheetState.show() }
        } else {
            if (showNoteRestoreSheet) {
                scope.launch { noteRestoreSheetState.hide() }.invokeOnCompletion {
                    if (!noteRestoreSheetState.isVisible) {
                        showNoteRestoreSheet = false
                    }
                }
            }
        }
    }
    fun clearSelectionAndRenameState() {
        currentlySwipedNoteId = null
        currentlySwipedFolderId = null
        currentlyRenamingNoteId = null
        currentlyRenamingFolderId = null
    }
    val navigateToFolderLambda: (Long) -> Unit = { folderId ->
        currentFolderId = folderId
        appPreferences.saveLastOpenedItem(AppPreferences.TYPE_FOLDER, folderId)
        searchQuery = ""
        clearSelectionAndRenameState()
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    LaunchedEffect(currentFolderId) {
        viewModel.switchCurrentFolder(currentFolderId)
        if (!SpecialFolders.isSpecial(currentFolderId)) {
            scope.launch {
                folderPath = viewModel.getFolderPath(currentFolderId)
            }
        } else {
            folderPath = emptyList()
        }
    }
    LaunchedEffect(Unit) {
        val (type, id) = appPreferences.getLastOpenedItem()
        when (type) {
            AppPreferences.TYPE_NOTE -> {
                scope.launch {
                    val note = viewModel.getNoteById(id)
                    if (note != null && !note.isArchived && !note.isInTrash) {
                        currentNote = note
                        showNoteEditor = true
                        currentFolderId = note.parentId
                    } else {
                        appPreferences.clearLastOpenedItem()
                        currentFolderId = SpecialFolders.ROOT
                    }
                }
            }
            AppPreferences.TYPE_FOLDER -> {
                val folderExists = id == SpecialFolders.ROOT ||
                        SpecialFolders.isSpecial(id) ||
                        (viewModel.getFolderById(id) != null)
                if (folderExists) {
                    currentFolderId = id
                } else {
                    appPreferences.clearLastOpenedItem()
                    currentFolderId = SpecialFolders.ROOT
                }
            }
            else -> {
                currentFolderId = SpecialFolders.ROOT
            }
        }
        viewModel.switchCurrentFolder(currentFolderId)
        clearSelectionAndRenameState()
    }
    fun handleFolderArchive(folder: Folder) = viewModel.archiveFolder(folder)
    fun handleFolderDelete(folder: Folder) = viewModel.deleteFolderToTrash(folder)
    fun handleFolderDeletePermanently(folder: Folder) = scope.launch { viewModel.deleteFolderPermanently(folder) }
    fun handleFolderRestore(folder: Folder) = viewModel.restoreFolder(folder)
    val navigateToFolder = navigateToFolderLambda
    fun navigateUp() {
        clearSelectionAndRenameState()
        searchQuery = ""
        keyboardController?.hide()
        focusManager.clearFocus()
        val targetFolderId = if (SpecialFolders.isSpecial(currentFolderId)) {
            SpecialFolders.ROOT
        } else {
            if (folderPath.isNotEmpty()) {
                folderPath.firstOrNull()?.parentId ?: SpecialFolders.ROOT
            } else {
                SpecialFolders.ROOT
            }
        }
        navigateToFolder(targetFolderId)
    }
    val handleNoteMoveRequest: (Note) -> Unit = { note ->
        if (!note.isArchived && !note.isInTrash) {
            clearSelectionAndRenameState()
            itemToMove = ItemToMove.NoteItem(note)
            showMoveDialog = true
        }
    }
    val handleFolderMoveRequest: (Folder) -> Unit = { folder ->
        if (!SpecialFolders.isSpecial(folder.id) && !folder.isRoot) {
            clearSelectionAndRenameState()
            itemToMove = ItemToMove.FolderItem(folder)
            showMoveDialog = true
        }
    }
    val handleNoteRenameStart: (Long) -> Unit = { noteId ->
        clearSelectionAndRenameState()
        currentlyRenamingNoteId = noteId
    }
    val handleFolderRenameStart: (Long) -> Unit = { folderId ->
        clearSelectionAndRenameState()
        currentlyRenamingFolderId = folderId
    }
    fun cancelRename() {
        currentlyRenamingNoteId = null
        currentlyRenamingFolderId = null
    }
    LaunchedEffect(showMoveDialog) {
        if (showMoveDialog) {
            scope.launch { moveSheetState.show() }
        } else {
            if (moveSheetState.isVisible) {
                scope.launch { moveSheetState.hide() }
            }
        }
    }
    if (showConfirmEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEmptyTrashDialog = false },
            title = { Text("Confirm Empty Recycle Bin") },
            text = { Text("Are you sure you want to permanently delete all items in the Recycle Bin? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyRecycleBin()
                        showConfirmEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showFolderDialog) {
        var dialogFolderName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                TextField(
                    value = dialogFolderName,
                    onValueChange = { dialogFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = dialogFolderName.isNotBlank(),
                    onClick = {
                        val nameToCreate = dialogFolderName.trim()
                        scope.launch {
                            Log.d("MainScreen", "Creating folder with title: '${nameToCreate}'")
                            viewModel.createFolder(nameToCreate, currentFolderId)
                        }
                        showFolderDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showMoveDialog) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { moveSheetState.hide() }.invokeOnCompletion {
                    if (!moveSheetState.isVisible) {
                        showMoveDialog = false
                        itemToMove = null
                    }
                }
            },
            sheetState = moveSheetState,
        ) {
            itemToMove?.let { currentItem ->
                MoveDestinationSelector(
                    itemToMove = currentItem,
                    viewModel = viewModel,
                    onDismissRequest = {
                        scope.launch { moveSheetState.hide() }.invokeOnCompletion {
                            if (!moveSheetState.isVisible) {
                                showMoveDialog = false
                                itemToMove = null
                            }
                        }
                    },
                    onDestinationSelected = { destinationFolderId: Long ->
                        scope.launch { moveSheetState.hide() }.invokeOnCompletion {
                            if (!moveSheetState.isVisible) {
                                when (currentItem) {
                                    is ItemToMove.NoteItem -> scope.launch {
                                        viewModel.moveNote(currentItem.note.id, destinationFolderId)
                                    }
                                    is ItemToMove.FolderItem -> scope.launch {
                                        viewModel.moveFolder(currentItem.folder.id, destinationFolderId)
                                    }
                                }
                                showMoveDialog = false
                                itemToMove = null
                            }
                        }
                    }
                )
            }
                ?: run {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Error: No item selected for move.") }
                    LaunchedEffect(Unit) {
                        moveSheetState.hide()
                        showMoveDialog = false
                        itemToMove = null
                    }
                }
        }
    }
    if (showFolderRestoreSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { folderRestoreSheetState.hide() }.invokeOnCompletion {
                    if (!folderRestoreSheetState.isVisible) {
                        viewModel.cancelFolderRestoreReparenting()
                        showFolderRestoreSheet = false
                    }
                }
            },
            sheetState = folderRestoreSheetState,
        ) {
            folderRequiringReparenting?.let { folderToRestore ->
                RestoreDestinationSelector(
                    itemToRestore = RestorableItem.FolderItem(folderToRestore),
                    viewModel = viewModel,
                    onDismissRequest = {
                        scope.launch { folderRestoreSheetState.hide() }.invokeOnCompletion {
                            if (!folderRestoreSheetState.isVisible) {
                                viewModel.cancelFolderRestoreReparenting()
                                showFolderRestoreSheet = false
                            }
                        }
                    },
                    onDestinationSelected = { destinationFolderId: Long ->
                        scope.launch { folderRestoreSheetState.hide() }.invokeOnCompletion {
                            if (!folderRestoreSheetState.isVisible) {
                                viewModel.completeFolderRestoreWithNewParent(folderToRestore, destinationFolderId)
                                showFolderRestoreSheet = false
                            }
                        }
                    }
                )
            }
                ?: run {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Error: No folder requiring reparenting found.") }
                    LaunchedEffect(Unit) {
                        folderRestoreSheetState.hide()
                        showFolderRestoreSheet = false
                        viewModel.cancelFolderRestoreReparenting()
                    }
                }
        }
    }
    if (showNoteRestoreSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { noteRestoreSheetState.hide() }.invokeOnCompletion {
                    if (!noteRestoreSheetState.isVisible) {
                        viewModel.cancelNoteRestoreReparenting()
                        showNoteRestoreSheet = false
                    }
                }
            },
            sheetState = noteRestoreSheetState,
        ) {
            noteRequiringReparenting?.let { noteToRestore ->
                RestoreDestinationSelector(
                    itemToRestore = RestorableItem.NoteItem(noteToRestore),
                    viewModel = viewModel,
                    onDismissRequest = {
                        scope.launch { noteRestoreSheetState.hide() }.invokeOnCompletion {
                            if (!noteRestoreSheetState.isVisible) {
                                viewModel.cancelNoteRestoreReparenting()
                                showNoteRestoreSheet = false
                            }
                        }
                    },
                    onDestinationSelected = { destinationFolderId: Long ->
                        scope.launch { noteRestoreSheetState.hide() }.invokeOnCompletion {
                            if (!noteRestoreSheetState.isVisible) {
                                viewModel.completeNoteRestoreWithNewParent(noteToRestore, destinationFolderId)
                                showNoteRestoreSheet = false
                            }
                        }
                    }
                )
            }
                ?: run {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Error: No note requiring reparenting found.") }
                    LaunchedEffect(Unit) {
                        noteRestoreSheetState.hide()
                        showNoteRestoreSheet = false
                        viewModel.cancelNoteRestoreReparenting()
                    }
                }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        clearSelectionAndRenameState()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )
            }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                Column(
                    modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = (currentFolderId != SpecialFolders.ROOT) || SpecialFolders.isSpecial(currentFolderId),
                                onClick = { navigateUp() }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allowNavigateUp = (currentFolderId != SpecialFolders.ROOT) || SpecialFolders.isSpecial(currentFolderId)
                            if (allowNavigateUp) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                            Text(
                                text = when {
                                    SpecialFolders.isRoot(currentFolderId) -> "Main"
                                    SpecialFolders.isArchive(currentFolderId) -> "Archive"
                                    SpecialFolders.isTrash(currentFolderId) -> "Recycle Bin"
                                    else -> folderPath.lastOrNull()?.title ?: "Folder"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            if (SpecialFolders.isRoot(currentFolderId)) {
                                IconButton(onClick = {
                                    clearSelectionAndRenameState()
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    navController.navigate(AppDestinations.SETTINGS_SCREEN)
                                }) {
                                    Icon(Icons.Default.Settings, "Settings")
                                }
                            } else {
                                Spacer(Modifier.width(48.dp))
                            }
                        }
                    }
                    SearchBar(
                        onSearch = { searchQuery = it },
                        onSearchBarClicked = { clearSelectionAndRenameState() }
                    )
                }
            },
            floatingActionButton = {
                when {
                    SpecialFolders.isTrash(currentFolderId) -> {
                        val isTrashEmpty = trashNotes.isEmpty() && trashedFolders.isEmpty()
                        if (!isTrashEmpty) {
                            FloatingActionButton(
                                onClick = { showConfirmEmptyTrashDialog = true },
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, "Empty Recycle Bin")
                            }
                        }
                    }
                    !SpecialFolders.isSpecial(currentFolderId) -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    clearSelectionAndRenameState()
                                    currentNote = null
                                    showNoteEditor = true
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Add, "New Note")
                            }
                            FloatingActionButton(
                                onClick = {
                                    clearSelectionAndRenameState()
                                    showFolderDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Icon(Icons.Default.CreateNewFolder, "New Folder")
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                val handleNoteSwipe: (Long?) -> Unit = { noteId ->
                    if (noteId != null) {
                        clearSelectionAndRenameState()
                        currentlySwipedNoteId = noteId
                    } else {
                        if(currentlySwipedNoteId != null) currentlySwipedNoteId = null
                    }
                }
                val handleFolderSwipe: (Long?) -> Unit = { folderId ->
                    if (folderId != null) {
                        clearSelectionAndRenameState()
                        currentlySwipedFolderId = folderId
                    } else {
                        if(currentlySwipedFolderId != null) currentlySwipedFolderId = null
                    }
                }
                when {
                    SpecialFolders.isRoot(currentFolderId) -> {
                        FolderContentsList(
                            folders = displayedFolders.filterNot { SpecialFolders.isSpecial(it.id) || it.isRoot },
                            notes = displayedNotes,
                            onShare = ::shareNote,
                            folderItemCounts = currentFolderTotalItemCounts,
                            onFolderClick = navigateToFolder,
                            onNoteEdit = { noteFromList ->
                                clearSelectionAndRenameState()
                                appPreferences.saveLastOpenedItem(AppPreferences.TYPE_NOTE, noteFromList.id)
                                scope.launch {
                                    currentNote = viewModel.getNoteById(noteFromList.id)
                                    showNoteEditor = true
                                }
                            },
                            onNoteRename = { id, newTitle ->
                                scope.launch { viewModel.renameNoteTitle(id, newTitle) }
                                cancelRename()
                            },
                            onFolderRename = { id, newTitle ->
                                scope.launch { viewModel.renameFolderTitle(id, newTitle) }
                                cancelRename()
                            },
                            onNoteDelete = viewModel::deleteNoteToTrash,
                            onFolderDelete = ::handleFolderDelete,
                            onFolderArchive = ::handleFolderArchive,
                            onFolderRestore = ::handleFolderRestore,
                            onFolderDeletePermanently = { folder -> scope.launch { viewModel.deleteFolderPermanently(folder) } },
                            onArchive = viewModel::archiveNote,
                            onPin = viewModel::toggleNotePin,
                            currentlySwipedNoteId = currentlySwipedNoteId,
                            currentlySwipedFolderId = currentlySwipedFolderId,
                            onNoteSwipe = handleNoteSwipe,
                            onFolderSwipe = handleFolderSwipe,
                            currentlyRenamingNoteId = currentlyRenamingNoteId,
                            currentlyRenamingFolderId = currentlyRenamingFolderId,
                            onNoteRenameStart = handleNoteRenameStart,
                            onFolderRenameStart = handleFolderRenameStart,
                            onNoteMoveRequest = handleNoteMoveRequest,
                            onFolderMoveRequest = handleFolderMoveRequest,
                            searchQuery = searchQuery,
                            isRootFolder = true,
                            showSystemFolders = true,
                            systemFolders = listOf(
                                Folder(id = SpecialFolders.ARCHIVE, title = "Archive", parentId = SpecialFolders.ROOT),
                                Folder(id = SpecialFolders.TRASH, title = "Recycle Bin", parentId = SpecialFolders.ROOT)
                            ),
                            systemFolderCounts = mapOf(
                                SpecialFolders.ARCHIVE to (archivedNotes.size + archivedFolders.size).toLong(),
                                SpecialFolders.TRASH to (trashNotes.size + trashedFolders.size).toLong()
                            )
                        )
                    }
                    SpecialFolders.isArchive(currentFolderId) -> {
                        ArchivedNotesList(
                            notes = displayedNotes,
                            folders = displayedFolders,
                            onShare = ::shareNote,
                            onRestore = viewModel::restoreNoteFromArchive,
                            onDelete = viewModel::deleteNoteToTrash,
                            onEdit = { noteFromList ->
                                clearSelectionAndRenameState()
                                appPreferences.saveLastOpenedItem(AppPreferences.TYPE_NOTE, noteFromList.id)
                                scope.launch {
                                    currentNote = viewModel.getNoteById(noteFromList.id)
                                    showNoteEditor = true
                                }
                            },
                            onRename = { id, newTitle ->
                                scope.launch { viewModel.renameNoteTitle(id, newTitle) }
                                cancelRename()
                            },
                            onFolderRestore = ::handleFolderRestore,
                            onFolderDelete = ::handleFolderDelete,
                            currentlySwipedNoteId = currentlySwipedNoteId,
                            currentlySwipedFolderId = currentlySwipedFolderId,
                            onNoteSwipe = handleNoteSwipe,
                            onFolderSwipe = handleFolderSwipe,
                            currentlyRenamingNoteId = currentlyRenamingNoteId,
                            currentlyRenamingFolderId = currentlyRenamingFolderId,
                            onNoteRenameStart = handleNoteRenameStart,
                            onFolderRenameStart = handleFolderRenameStart,
                            searchQuery = searchQuery,
                            onFolderRename = { id, newTitle ->
                                scope.launch { viewModel.renameFolderTitle(id, newTitle) }
                                cancelRename()
                            }
                        )
                    }
                    SpecialFolders.isTrash(currentFolderId) -> {
                        TrashNotesList(
                            notes = displayedNotes,
                            folders = displayedFolders,
                            onShare = ::shareNote,
                            onRestore = viewModel::restoreNoteFromTrash,
                            onDeletePermanently = { note -> scope.launch { viewModel.deleteNotePermanently(note) } },
                            onEdit = {},
                            onRename = { _, _ -> cancelRename() },
                            onFolderRestore = ::handleFolderRestore,
                            onFolderDeletePermanently = { folder -> scope.launch { viewModel.deleteFolderPermanently(folder) } },
                            currentlySwipedNoteId = currentlySwipedNoteId,
                            currentlySwipedFolderId = currentlySwipedFolderId,
                            onNoteSwipe = handleNoteSwipe,
                            onFolderSwipe = handleFolderSwipe,
                            currentlyRenamingNoteId = currentlyRenamingNoteId,
                            currentlyRenamingFolderId = currentlyRenamingFolderId,
                            onNoteRenameStart = { _ -> },
                            onFolderRenameStart = { _ -> },
                            searchQuery = searchQuery
                        )
                    }
                    else -> {
                        FolderContentsList(
                            folders = displayedFolders,
                            notes = displayedNotes,
                            onShare = ::shareNote,
                            folderItemCounts = currentFolderTotalItemCounts,
                            onFolderClick = navigateToFolder,
                            onNoteEdit = { note ->
                                clearSelectionAndRenameState()
                                currentNote = note
                                appPreferences.saveLastOpenedItem(AppPreferences.TYPE_NOTE, note.id)
                                showNoteEditor = true
                            },
                            onNoteRename = { id, newTitle ->
                                scope.launch { viewModel.renameNoteTitle(id, newTitle) }
                                cancelRename()
                            },
                            onFolderRename = { id, newTitle ->
                                scope.launch { viewModel.renameFolderTitle(id, newTitle) }
                                cancelRename()
                            },
                            onNoteDelete = viewModel::deleteNoteToTrash,
                            onFolderDelete = ::handleFolderDelete,
                            onFolderArchive = ::handleFolderArchive,
                            onFolderRestore = ::handleFolderRestore,
                            onFolderDeletePermanently = { folder -> scope.launch { viewModel.deleteFolderPermanently(folder) } },
                            onArchive = viewModel::archiveNote,
                            onPin = viewModel::toggleNotePin,
                            currentlySwipedNoteId = currentlySwipedNoteId,
                            currentlySwipedFolderId = currentlySwipedFolderId,
                            onNoteSwipe = handleNoteSwipe,
                            onFolderSwipe = handleFolderSwipe,
                            currentlyRenamingNoteId = currentlyRenamingNoteId,
                            currentlyRenamingFolderId = currentlyRenamingFolderId,
                            onNoteRenameStart = handleNoteRenameStart,
                            onFolderRenameStart = handleFolderRenameStart,
                            onNoteMoveRequest = handleNoteMoveRequest,
                            onFolderMoveRequest = handleFolderMoveRequest,
                            searchQuery = searchQuery,
                            isRootFolder = false,
                            showSystemFolders = false
                        )
                    }
                }
            }
        }
        if (showNoteEditor) {
            NoteEditorScreen(
                note = currentNote,
                viewModel = viewModel,
                currentFolderId = currentFolderId,
                onSaveAction = { noteToSave ->
                    scope.launch {
                        if (noteToSave.id == 0L) {
                            viewModel.addNote(noteToSave.copy(parentId = currentFolderId))
                        } else {
                            viewModel.updateNote(noteToSave)
                        }
                    }
                },
                onCloseAction = {
                    appPreferences.saveLastOpenedItem(AppPreferences.TYPE_FOLDER, currentFolderId)
                    showNoteEditor = false
                    currentNote = null
                },
                onCalculate = {  },
                appPreferences = appPreferences
            )
        }
    }
}