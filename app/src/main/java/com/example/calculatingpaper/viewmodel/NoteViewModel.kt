package com.example.calculatingpaper.viewmodel
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.calculatingpaper.data.*
import com.example.calculatingpaper.view.screens.SpecialFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.ArrayDeque
@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val db = NoteDatabase.getDatabase(application)
    private val noteDao = db.noteDao()
    private val appPreferences = AppPreferences(application)
    private val firestoreSyncManager = FirestoreSyncManager(noteDao, appPreferences, viewModelScope, db)
    private val backupRestoreManager = BackupRestoreManager(noteDao, appPreferences, viewModelScope, application.applicationContext, db)
    private val currentFolderIdFlow = MutableStateFlow<Long>(SpecialFolders.ROOT)
    val pinnedNotes: StateFlow<List<Note>> = noteDao.getPinnedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedNotes: StateFlow<List<Note>> = noteDao.getAllArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trashNotes: StateFlow<List<Note>> = noteDao.getAllTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedFolders: StateFlow<List<Folder>> = noteDao.getAllArchivedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trashedFolders: StateFlow<List<Folder>> = noteDao.getAllTrashedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentFolderContents: StateFlow<Pair<List<Note>, List<Folder>>> = currentFolderIdFlow
        .flatMapLatest { folderId ->
            if (!SpecialFolders.isSpecial(folderId)) {
                combine(
                    noteDao.getNotesByParent(folderId),
                    noteDao.getFoldersByParent(folderId)
                ) { notes, folders -> Pair(notes, folders) }
            } else {
                flowOf(Pair(emptyList<Note>(), emptyList<Folder>()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Pair(emptyList(), emptyList())
        )
    val currentFolderTotalItemCounts: StateFlow<Map<Long, Long>> = currentFolderContents
        .map { (_, folders) -> folders.map { it.id } }
        .transformLatest { folderIds ->
            val counts = folderIds.associateWith { id ->
                val noteCount = noteDao.getNoteCountInFolder(id)
                val folderCount = noteDao.getFolderCountInFolder(id)
                noteCount + folderCount
            }
            emit(counts)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap<Long, Long>()
        )
    val syncCheckState: StateFlow<SyncCheckState> = firestoreSyncManager.syncCheckState
    val syncActivationState: StateFlow<SyncActivationState> = firestoreSyncManager.syncActivationState
    val importState: StateFlow<DataImportState> = backupRestoreManager.importState
    private val _resetState = MutableStateFlow<DataResetState>(DataResetState.Idle)
    val resetState: StateFlow<DataResetState> = _resetState.asStateFlow()
    private val _folderRequiringReparentingOnRestore = MutableStateFlow<Folder?>(null)
    val folderRequiringReparentingOnRestore: StateFlow<Folder?> = _folderRequiringReparentingOnRestore.asStateFlow()
    private val _noteRequiringReparentingOnRestore = MutableStateFlow<Note?>(null)
    val noteRequiringReparentingOnRestore: StateFlow<Note?> = _noteRequiringReparentingOnRestore.asStateFlow()
    init {
        viewModelScope.launch {
            ensureRootFolderExists()
            if (appPreferences.isLoggedIn() && appPreferences.isRealtimeSyncEnabled()) {
                firestoreSyncManager.startListeners()
            }
        }
    }
    fun checkFirestoreStatusAndInitiateSync(userId: String) = firestoreSyncManager.checkFirestoreStatusAndInitiateSync(userId)
    fun proceedWithSyncActivation(userId: String) = firestoreSyncManager.proceedWithSyncActivation(userId)
    fun activateSyncAndUploadLocalData(userId: String) = firestoreSyncManager.activateSyncAndUploadLocalData(userId)
    fun startSyncListeners() = firestoreSyncManager.startListeners()
    fun disableSyncing() = firestoreSyncManager.disableSyncing()
    fun resetSyncCheckState() = firestoreSyncManager.resetSyncCheckState()
    fun resetSyncActivationState() = firestoreSyncManager.resetSyncActivationState()
    suspend fun prepareBackupData(): BackupData? = backupRestoreManager.prepareBackupData()
    suspend fun writeBackupToFile(backupData: BackupData, targetUri: Uri): Boolean = backupRestoreManager.writeBackupToFile(backupData, targetUri)
    fun createBackupFilename(): String = backupRestoreManager.createBackupFilename()
    fun importBackupData(backupUri: Uri, clearExistingDataFirst: Boolean) {
        if (appPreferences.isRealtimeSyncEnabled()) {
            firestoreSyncManager.disableSyncing()
        }
        backupRestoreManager.importBackupData(backupUri, clearExistingDataFirst)
    }
    fun clearImportState() = backupRestoreManager.clearImportState()
    suspend fun hasUserData(): Boolean = backupRestoreManager.hasUserData()
    suspend fun addNote(note: Note): Long {
        val generatedRoomId = noteDao.insertNote(note)
        val noteWithId = note.copy(id = generatedRoomId)
        firestoreSyncManager.syncNoteChange(noteWithId)
        return generatedRoomId
    }
    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        firestoreSyncManager.syncNoteChange(note)
    }
    suspend fun createFolder(title: String, parentId: Long = SpecialFolders.ROOT): Long {
        val folderToInsert = Folder(title = title, parentId = parentId)
        var generatedRoomId = -1L
        generatedRoomId = noteDao.insertFolder(folderToInsert)
        val folderWithId = folderToInsert.copy(id = generatedRoomId)
        firestoreSyncManager.syncFolderChange(folderWithId)
        return generatedRoomId
    }
    suspend fun updateFolder(folder: Folder) {
        noteDao.updateFolder(folder)
        firestoreSyncManager.syncFolderChange(folder)
    }
    fun deleteNoteToTrash(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isInTrash = true, isArchived = false, isPinned = false, timestamp = System.currentTimeMillis())
        noteDao.updateNote(updatedNote)
        firestoreSyncManager.syncNoteChange(updatedNote)
    }
    fun archiveNote(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isArchived = true, isInTrash = false, isPinned = false, timestamp = System.currentTimeMillis())
        noteDao.updateNote(updatedNote)
        firestoreSyncManager.syncNoteChange(updatedNote)
    }
    fun restoreNoteFromArchive(note: Note) = viewModelScope.launch {
        var needsReparenting = false
        if (note.isArchived) {
            if (!SpecialFolders.isSpecial(note.parentId) && note.parentId != SpecialFolders.ROOT) {
                val parent = noteDao.getFolderById(note.parentId)
                if (parent == null || parent.isArchived || parent.isInTrash) {
                    needsReparenting = true
                }
            } else {
                needsReparenting = true
            }
        }
        if (note.parentId == SpecialFolders.ARCHIVE) {
            needsReparenting = true
        } else if (note.parentId == SpecialFolders.ROOT && note.isArchived) {
            needsReparenting = true
        } else if (!SpecialFolders.isSpecial(note.parentId) && note.parentId != SpecialFolders.ROOT) {
            val parent = noteDao.getFolderById(note.parentId)
            if (parent == null || parent.isArchived || parent.isInTrash) {
                needsReparenting = true
            }
        }
        if (needsReparenting) {
            _noteRequiringReparentingOnRestore.value = note.copy(isArchived = false)
        } else {
            val restoredNote = note.copy(isArchived = false, timestamp = System.currentTimeMillis())
            noteDao.updateNote(restoredNote)
            firestoreSyncManager.syncNoteChange(restoredNote)
        }
    }
    fun restoreNoteFromTrash(note: Note) = viewModelScope.launch {
        var needsReparenting = false
        if (!SpecialFolders.isSpecial(note.parentId) && note.parentId != SpecialFolders.ROOT) {
            val parent = noteDao.getFolderById(note.parentId)
            if (parent == null || parent.isArchived || parent.isInTrash) {
                needsReparenting = true
            }
        } else if (note.parentId == SpecialFolders.TRASH) {
            needsReparenting = true
        }
        if (needsReparenting) {
            _noteRequiringReparentingOnRestore.value = note.copy(isInTrash = false)
        } else {
            val restoredNote = note.copy(isInTrash = false, timestamp = System.currentTimeMillis())
            noteDao.updateNote(restoredNote)
            firestoreSyncManager.syncNoteChange(restoredNote)
        }
    }
    fun deleteFolderToTrash(folder: Folder) = viewModelScope.launch {
        val updatedFolder = folder.copy(isInTrash = true, isArchived = false, timestamp = System.currentTimeMillis())
        noteDao.updateFolder(updatedFolder)
        firestoreSyncManager.syncFolderChange(updatedFolder)
    }
    fun archiveFolder(folder: Folder) = viewModelScope.launch {
        val updatedFolder = folder.copy(isArchived = true, isInTrash = false, timestamp = System.currentTimeMillis())
        noteDao.updateFolder(updatedFolder)
        firestoreSyncManager.syncFolderChange(updatedFolder)
    }
    fun restoreFolder(folder: Folder) = viewModelScope.launch {
        var needsReparenting = false
        if (folder.isArchived || folder.isInTrash) {
            if (folder.parentId == SpecialFolders.ARCHIVE ||
                folder.parentId == SpecialFolders.TRASH ||
                folder.parentId == SpecialFolders.ROOT ||
                SpecialFolders.isSpecial(folder.parentId) ) {
                needsReparenting = true
            } else {
                val parent = noteDao.getFolderById(folder.parentId)
                if (parent == null || parent.isArchived || parent.isInTrash) {
                    needsReparenting = true
                }
            }
        }
        if (needsReparenting) {
            _folderRequiringReparentingOnRestore.value = folder.copy(isArchived = false, isInTrash = false)
        } else {
            val restoredFolder = folder.copy(isArchived = false, isInTrash = false, timestamp = System.currentTimeMillis())
            noteDao.updateFolder(restoredFolder)
            firestoreSyncManager.syncFolderChange(restoredFolder)
        }
    }
    suspend fun deleteNotePermanently(note: Note) {
        noteDao.deleteNote(note)
        firestoreSyncManager.syncNoteDeletion(note)
    }
    suspend fun deleteFolderPermanently(folder: Folder) {
        if (folder.isArchived || folder.isInTrash) {
            processFolderForPermanentDeletion(folder.id)
            orphanArchivedChildren(folder.id)
            noteDao.deleteFolder(folder)
            firestoreSyncManager.syncFolderDeletion(folder)
        }
    }

    fun emptyRecycleBin() = viewModelScope.launch {
        val notesToDelete = trashNotes.value
        val foldersToDelete = trashedFolders.value

        notesToDelete.forEach { noteDao.deleteNote(it) }

        foldersToDelete.forEach { folder ->
            processFolderForPermanentDeletion(folder.id)
            orphanArchivedChildren(folder.id)
            noteDao.deleteFolder(folder)
        }
        notesToDelete.forEach { firestoreSyncManager.syncNoteDeletion(it) }
        foldersToDelete.forEach { firestoreSyncManager.syncFolderDeletion(it) }
    }
    fun toggleNotePin(note: Note) = viewModelScope.launch {
        if (note.isArchived || note.isInTrash) return@launch
        val updatedNote = note.copy(isPinned = !note.isPinned, timestamp = System.currentTimeMillis())
        noteDao.updateNote(updatedNote)
        firestoreSyncManager.syncNoteChange(updatedNote)
    }
    suspend fun renameNoteTitle(noteId: Long, newTitle: String) {
        noteDao.getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(title = newTitle, timestamp = System.currentTimeMillis())
            noteDao.updateNote(updatedNote)
            firestoreSyncManager.syncNoteChange(updatedNote)
        }
    }
    suspend fun renameFolderTitle(folderId: Long, newTitle: String) {
        noteDao.getFolderById(folderId)?.let { folder ->
            val updatedFolder = folder.copy(title = newTitle, timestamp = System.currentTimeMillis())
            noteDao.updateFolder(updatedFolder)
            firestoreSyncManager.syncFolderChange(updatedFolder)
        }
    }
    suspend fun moveNote(noteId: Long, destinationFolderId: Long) {
        if (SpecialFolders.isSpecial(destinationFolderId)) return
        val note = noteDao.getNoteById(noteId) ?: return
        if (note.parentId == destinationFolderId) return
        val destinationFolder = if (destinationFolderId == SpecialFolders.ROOT) null else noteDao.getFolderById(destinationFolderId)
        if (destinationFolderId != SpecialFolders.ROOT && (destinationFolder == null || destinationFolder.isArchived || destinationFolder.isInTrash)) return
        val updatedNote = note.copy(
            parentId = destinationFolderId,
            timestamp = System.currentTimeMillis(),
            isArchived = false,
            isInTrash = false
        )
        noteDao.updateNote(updatedNote)
        firestoreSyncManager.syncNoteChange(updatedNote)
    }
    suspend fun moveFolder(folderId: Long, destinationFolderId: Long) {
        if (SpecialFolders.isSpecial(folderId) || folderId == SpecialFolders.ROOT) return
        if (SpecialFolders.isSpecial(destinationFolderId)) return
        if (folderId == destinationFolderId) return
        val folder = noteDao.getFolderById(folderId) ?: return
        if (folder.parentId == destinationFolderId) return
        val destinationFolder = if (destinationFolderId == SpecialFolders.ROOT) null else noteDao.getFolderById(destinationFolderId)
        if (destinationFolderId != SpecialFolders.ROOT && (destinationFolder == null || destinationFolder.isArchived || destinationFolder.isInTrash)) return
        val destinationPath = getFolderPath(destinationFolderId)
        if (destinationPath.any { it.id == folderId }) return
        val updatedFolder = folder.copy(
            parentId = destinationFolderId,
            timestamp = System.currentTimeMillis(),
            isArchived = false,
            isInTrash = false
        )
        noteDao.updateFolder(updatedFolder)
        firestoreSyncManager.syncFolderChange(updatedFolder)
    }
    fun completeFolderRestoreWithNewParent(folder: Folder, newParentId: Long) = viewModelScope.launch {
        if (folder.id == _folderRequiringReparentingOnRestore.value?.id) {
            val restoredFolder = folder.copy(parentId = newParentId, isInTrash = false, isArchived = false, timestamp = System.currentTimeMillis())
            noteDao.updateFolder(restoredFolder)
            firestoreSyncManager.syncFolderChange(restoredFolder)
            _folderRequiringReparentingOnRestore.value = null
        }
    }
    fun cancelFolderRestoreReparenting() { _folderRequiringReparentingOnRestore.value = null }
    fun completeNoteRestoreWithNewParent(note: Note, newParentId: Long) = viewModelScope.launch {
        if (note.id == _noteRequiringReparentingOnRestore.value?.id) {
            val restoredNote = note.copy(
                parentId = newParentId,
                isArchived = false,
                isInTrash = false,
                timestamp = System.currentTimeMillis()
            )
            noteDao.updateNote(restoredNote)
            firestoreSyncManager.syncNoteChange(restoredNote)
            _noteRequiringReparentingOnRestore.value = null
        }
    }

    fun cancelNoteRestoreReparenting() {
        _noteRequiringReparentingOnRestore.value = null
    }
    fun resetApplicationData() {
        viewModelScope.launch(Dispatchers.IO) {
            _resetState.value = DataResetState.Running
            try {
                if (appPreferences.isRealtimeSyncEnabled()) {
                    firestoreSyncManager.disableSyncing()
                }
                performDataResetOperations()
                _resetState.value = DataResetState.Success
            } catch (e: Exception) {
                _resetState.value = DataResetState.Error("Failed to reset data: ${e.message}")
            }
        }
    }
    fun clearResetState() { _resetState.value = DataResetState.Idle }
    fun switchCurrentFolder(folderId: Long) { currentFolderIdFlow.value = folderId }
    suspend fun getNoteById(noteId: Long): Note? = noteDao.getNoteById(noteId)
    suspend fun getFolderById(folderId: Long): Folder? = noteDao.getFolderById(folderId)
    suspend fun getFolderPath(folderId: Long): List<Folder> {
        val path = mutableListOf<Folder>()
        var currentId: Long = folderId
        val visitedIds = mutableSetOf<Long>()
        var safetyBreak = 100
        var shouldContinue = true
        while (currentId != SpecialFolders.ROOT && currentId !in visitedIds && shouldContinue && safetyBreak > 0) {
            visitedIds.add(currentId); safetyBreak--
            val currentFolder = noteDao.getFolderById(currentId)
            if (currentFolder != null) {
                path.add(currentFolder)
                if (currentFolder.parentId in visitedIds || currentFolder.parentId == currentFolder.id) {
                    shouldContinue = false
                } else {
                    currentId = currentFolder.parentId
                    if (SpecialFolders.isSpecial(currentId) && currentId != SpecialFolders.ROOT) {
                        shouldContinue = false
                    }
                }
            } else {
                shouldContinue = false
            }
        }
        return path.reversed()
    }
    fun getFoldersFlow(parentId: Long): Flow<List<Folder>> = noteDao.getFoldersByParent(parentId)
    private suspend fun ensureRootFolderExists() {
        if (noteDao.getRootFolder() == null) {
            try {
                val rootFolder = Folder(id = SpecialFolders.ROOT, title = "Main", parentId = SpecialFolders.ROOT, isRoot = true, timestamp = System.currentTimeMillis())
                noteDao.insertFolder(rootFolder)
                val archiveFolder = Folder(id = SpecialFolders.ARCHIVE, title = "Archive", parentId = SpecialFolders.ROOT, timestamp = System.currentTimeMillis())
                noteDao.insertFolder(archiveFolder)
                val trashFolder = Folder(id = SpecialFolders.TRASH, title = "Recycle Bin", parentId = SpecialFolders.ROOT, timestamp = System.currentTimeMillis())
                noteDao.insertFolder(trashFolder)
            } catch (e: Exception) {  }
        }
    }
    private suspend fun performDataResetOperations() {
        db.withTransaction {
            noteDao.deleteAllNotes()
            noteDao.deleteAllUserFolders()
        }
        appPreferences.clearLastOpenedItem()
        ensureRootFolderExists()
        switchCurrentFolder(SpecialFolders.ROOT)
    }
    private suspend fun processFolderForPermanentDeletion(folderId: Long) {
        val foldersToScan = ArrayDeque<Folder>()
        val notesToDelete = mutableListOf<Note>()
        noteDao.getNonArchivedNotesByParent(folderId).toCollection(notesToDelete)
        noteDao.getNonArchivedFoldersByParent(folderId).forEach { foldersToScan.add(it) }
        val foldersToDeleteOrder = mutableListOf<Folder>()
        val visitedSubFolderIds = mutableSetOf<Long>()
        while (foldersToScan.isNotEmpty()) {
            val currentSubFolder = foldersToScan.removeFirst()
            if (currentSubFolder.id in visitedSubFolderIds) continue
            visitedSubFolderIds.add(currentSubFolder.id)
            foldersToDeleteOrder.add(currentSubFolder)
            noteDao.getNonArchivedNotesByParent(currentSubFolder.id).toCollection(notesToDelete)
            noteDao.getNonArchivedFoldersByParent(currentSubFolder.id).forEach { subSubFolder ->
                if (subSubFolder.id !in visitedSubFolderIds) {
                    foldersToScan.add(subSubFolder)
                }
            }
        }
        notesToDelete.forEach { note ->
            noteDao.deleteNote(note)
            firestoreSyncManager.syncNoteDeletion(note)
        }
        foldersToDeleteOrder.reversed().forEach { subFolderToDelete ->
            orphanArchivedChildren(subFolderToDelete.id)
            noteDao.deleteFolder(subFolderToDelete)
            firestoreSyncManager.syncFolderDeletion(subFolderToDelete)
        }
    }

    private suspend fun orphanArchivedChildren(folderId: Long) {
        noteDao.getArchivedNotesByParent(folderId).forEach { note ->
            val updatedNote = note.copy(parentId = SpecialFolders.ARCHIVE, timestamp = System.currentTimeMillis())
            noteDao.updateNote(updatedNote)
            firestoreSyncManager.syncNoteChange(updatedNote)
        }
        noteDao.getArchivedFoldersByParent(folderId).forEach { folder ->
            val updatedFolder = folder.copy(parentId = SpecialFolders.ARCHIVE, timestamp = System.currentTimeMillis())
            noteDao.updateFolder(updatedFolder)
            firestoreSyncManager.syncFolderChange(updatedFolder)
        }
    }
    override fun onCleared() {
        super.onCleared()
        firestoreSyncManager.stopListeners()
    }
}
sealed class DataResetState {
    object Idle : DataResetState()
    object Running : DataResetState()
    object Success : DataResetState()
    data class Error(val message: String) : DataResetState()
}