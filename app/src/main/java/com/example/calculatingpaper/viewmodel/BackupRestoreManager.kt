package com.example.calculatingpaper.viewmodel
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.calculatingpaper.data.*
import com.example.calculatingpaper.view.screens.SpecialFolders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class BackupRestoreManager(
    private val noteDao: NoteDao,
    private val appPreferences: AppPreferences,
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val db: NoteDatabase
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val _importState = MutableStateFlow<DataImportState>(DataImportState.Idle)
    val importState: StateFlow<DataImportState> = _importState.asStateFlow()
    suspend fun prepareBackupData(): BackupData? {
        return try {
            val allNotes = noteDao.getAllNotesForBackup()
            val allFolders = noteDao.getAllFoldersForBackup()
            val settings = AppSettings(decimalPrecision = AppPreferences.decimalPrecision)
            BackupData(notes = allNotes, folders = allFolders, appSettings = settings)
        } catch (e: Exception) {
            null
        }
    }
    suspend fun writeBackupToFile(backupData: BackupData, targetUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    val jsonString = json.encodeToString(backupData)
                    outputStream.write(jsonString.toByteArray())
                } ?: throw Exception("Could not open output stream for URI: $targetUri")
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    fun createBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "CalculatingPaper_Backup_${timestamp}.json"
    }
    fun importBackupData(backupUri: Uri, clearExistingDataFirst: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            _importState.value = DataImportState.Running("Starting import...")
            try {
                if (clearExistingDataFirst) {
                    _importState.value = DataImportState.Running("Clearing existing data...")
                    try {
                        db.withTransaction {
                            noteDao.deleteAllNotes()
                            noteDao.deleteAllUserFolders()
                        }
                        appPreferences.clearLastOpenedItem()
                    } catch (e: Exception) {
                        _importState.value = DataImportState.Error("Failed to clear existing data: ${e.message}")
                        return@launch
                    }
                }
                _importState.value = DataImportState.Running("Reading file...")
                val backupJson = readTextFromUri(backupUri)
                if (backupJson == null) {
                    _importState.value = DataImportState.Error("Failed to read backup file.")
                    return@launch
                }
                _importState.value = DataImportState.Running("Parsing data...")
                val backupData: BackupData = try {
                    json.decodeFromString(backupJson)
                } catch (e: Exception) {
                    _importState.value = DataImportState.Error("Could not parse backup data: ${e.message}")
                    return@launch
                }
                _importState.value = DataImportState.Running("Importing items...")
                val folderIdMap = mutableMapOf<Long, Long>()
                folderIdMap[SpecialFolders.ROOT] = SpecialFolders.ROOT
                val existingTitlesMap = mutableMapOf<Long, Pair<MutableSet<String>, MutableSet<String>>>()
                if (!clearExistingDataFirst) {
                    populateExistingTitles(existingTitlesMap)
                }
                db.withTransaction {
                    recursiveImport(backupData, SpecialFolders.ROOT, SpecialFolders.ROOT, folderIdMap, existingTitlesMap)
                }
                _importState.value = DataImportState.Running("Applying settings...")
                appPreferences.saveDecimalPrecision(backupData.appSettings.decimalPrecision)
                ensureRootFolderExists()
                _importState.value = DataImportState.Success("Import complete!")
            } catch (e: Exception) {
                _importState.value = DataImportState.Error("Import failed: ${e.message}")
            }
        }
    }
    private suspend fun populateExistingTitles(map: MutableMap<Long, Pair<MutableSet<String>, MutableSet<String>>>) {
        val allDbFolders = noteDao.getAllFoldersForBackup()
        val allDbNotes = noteDao.getAllNotesForBackup()
        allDbFolders.groupBy { it.parentId }.forEach { (parentId, folders) ->
            val titles = map.getOrPut(parentId) { Pair(mutableSetOf(), mutableSetOf()) }
            folders.mapTo(titles.first) { it.title }
        }
        allDbNotes.groupBy { it.parentId }.forEach { (parentId, notes) ->
            val titles = map.getOrPut(parentId) { Pair(mutableSetOf(), mutableSetOf()) }
            notes.mapTo(titles.second) { it.title }
        }
    }
    private suspend fun recursiveImport(
        backupData: BackupData, backupParentFolderId: Long, dbParentFolderId: Long,
        folderIdMap: MutableMap<Long, Long>, existingTitlesMap: MutableMap<Long, Pair<MutableSet<String>, MutableSet<String>>>
    ) {
        val existingDbFoldersInParent = noteDao.getAllFoldersByParentForMerge(dbParentFolderId)
        val existingDbFolderMapByTitle = existingDbFoldersInParent.associateBy { it.title }
        val (folderTitlesInDbParent, noteTitlesInDbParent) = existingTitlesMap.getOrPut(dbParentFolderId) { Pair(mutableSetOf(), mutableSetOf()) }
        val foldersToImport = backupData.folders.filter { it.parentId == backupParentFolderId }
        for (backupFolder in foldersToImport) {
            if (SpecialFolders.isSpecial(backupFolder.id)) continue
            val originalTitle = backupFolder.title
            val existingFolderMatch: Folder? = existingDbFolderMapByTitle[originalTitle]
            var targetDbFolderIdForChildren: Long
            if (existingFolderMatch != null) {
                val updatedExistingFolder = existingFolderMatch.copy(
                    isArchived = backupFolder.isArchived,
                    isInTrash = backupFolder.isInTrash,
                    timestamp = backupFolder.timestamp
                )
                noteDao.updateFolder(updatedExistingFolder)
                targetDbFolderIdForChildren = existingFolderMatch.id
                folderIdMap[backupFolder.id] = existingFolderMatch.id
            } else {
                val finalTitle = findUniqueName(originalTitle, folderTitlesInDbParent)
                folderTitlesInDbParent.add(finalTitle)
                val newFolderObject = Folder(
                    title = finalTitle, parentId = dbParentFolderId, timestamp = backupFolder.timestamp,
                    isArchived = backupFolder.isArchived, isInTrash = backupFolder.isInTrash
                )
                val newDbFolderId = noteDao.insertFolder(newFolderObject)
                targetDbFolderIdForChildren = newDbFolderId
                folderIdMap[backupFolder.id] = newDbFolderId
            }
            recursiveImport(backupData, backupFolder.id, targetDbFolderIdForChildren, folderIdMap, existingTitlesMap)
        }
        val notesToImport = backupData.notes.filter { it.parentId == backupParentFolderId }
        for (backupNote in notesToImport) {
            val originalTitle = backupNote.title
            val finalTitle = findUniqueName(originalTitle, noteTitlesInDbParent)
            noteTitlesInDbParent.add(finalTitle)
            val newNote = Note(
                title = finalTitle, content = backupNote.content, parentId = dbParentFolderId,
                timestamp = backupNote.timestamp, isPinned = backupNote.isPinned,
                isArchived = backupNote.isArchived, isInTrash = backupNote.isInTrash
            )
            noteDao.insertNote(newNote)
        }
    }
    fun clearImportState() { _importState.value = DataImportState.Idle }
    suspend fun hasUserData(): Boolean = withContext(Dispatchers.IO) {
        noteDao.getAllNotesForBackup().isNotEmpty() || noteDao.getAllFoldersForBackup().isNotEmpty()
    }
    private suspend fun readTextFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader -> reader.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
    private fun findUniqueName(originalTitle: String, existingTitles: MutableSet<String>): String {
        if (!existingTitles.contains(originalTitle)) return originalTitle
        var counter = 1
        var currentTitle = "${originalTitle}_copy"
        while (existingTitles.contains(currentTitle)) {
            counter++
            currentTitle = "${originalTitle}_copy${counter}"
            if (counter > 999) {
                currentTitle = "${originalTitle}_${System.currentTimeMillis()}"
                break;
            }
        }
        return currentTitle
    }
    private suspend fun ensureRootFolderExists() {
        if (noteDao.getRootFolder() == null) {
            try {
                val rootFolder = Folder(id = SpecialFolders.ROOT, title = "Main", parentId = SpecialFolders.ROOT, isRoot = true)
                noteDao.insertFolder(rootFolder)
            } catch (e: Exception) {  }
        }
    }
}
sealed class DataImportState {
    object Idle : DataImportState()
    data class Running(val message: String) : DataImportState()
    data class Success(val message: String) : DataImportState()
    data class Error(val message: String) : DataImportState()
}