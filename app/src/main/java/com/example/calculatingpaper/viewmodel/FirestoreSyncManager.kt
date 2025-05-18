package com.example.calculatingpaper.viewmodel
import android.util.Log
import com.example.calculatingpaper.data.*
import com.example.calculatingpaper.view.screens.SpecialFolders
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.room.withTransaction


class FirestoreSyncManager(
    private val noteDao: NoteDao,
    private val appPreferences: AppPreferences,
    private val coroutineScope: CoroutineScope,
    private val db: NoteDatabase
) {
    private val firestore = Firebase.firestore
    private var noteListenerRegistration: ListenerRegistration? = null
    private var folderListenerRegistration: ListenerRegistration? = null
    private val _syncCheckState = MutableStateFlow<SyncCheckState>(SyncCheckState.Idle)
    val syncCheckState: StateFlow<SyncCheckState> = _syncCheckState.asStateFlow()
    private val _syncActivationState = MutableStateFlow<SyncActivationState>(SyncActivationState.Idle)
    val syncActivationState: StateFlow<SyncActivationState> = _syncActivationState.asStateFlow()
    fun startListeners() {
        val userId = appPreferences.getUserId() ?: return
        if (!appPreferences.isRealtimeSyncEnabled()) return
        stopListeners()
        setupNoteListener(userId)
        setupFolderListener(userId)
    }
    fun stopListeners() {
        noteListenerRegistration?.remove()
        folderListenerRegistration?.remove()
        noteListenerRegistration = null
        folderListenerRegistration = null
    }
    fun disableSyncing() {
        stopListeners()
        appPreferences.saveRealtimeSyncEnabled(false)
    }
    fun checkFirestoreStatusAndInitiateSync(userId: String) {
        _syncCheckState.value = SyncCheckState.Idle
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val notesCollection = firestore.collection("users").document(userId).collection("notes")
                val foldersCollection = firestore.collection("users").document(userId).collection("folders")
                val notesSnapshot = notesCollection.limit(1).get().await()
                val foldersSnapshot = foldersCollection.limit(1).get().await()
                val cloudDataExists = !notesSnapshot.isEmpty || !foldersSnapshot.isEmpty

                val localUserNotes = noteDao.getAllNotesForBackup()
                val allLocalFolders = noteDao.getAllFoldersForBackup()
                val localUserFolders = allLocalFolders.filter { folder ->
                    !SpecialFolders.isSpecial(folder.id) && folder.id != SpecialFolders.ROOT && !folder.isRoot
                }
                val localUserDataExists = localUserNotes.isNotEmpty() || localUserFolders.isNotEmpty()

                if (localUserDataExists && cloudDataExists) {
                    _syncCheckState.value = SyncCheckState.RequiresDownloadConfirmation
                } else if (localUserDataExists && !cloudDataExists) {
                    _syncCheckState.value = SyncCheckState.RequiresUploadConfirmation
                } else {
                    _syncCheckState.value = SyncCheckState.CanEnableDirectly
                }
            } catch (e: Exception) {
                _syncCheckState.value = SyncCheckState.Error("Failed to check Firestore: ${e.message}")
            }
        }
    }
    fun proceedWithSyncActivation(userId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            _syncActivationState.value = SyncActivationState.Running("Deleting local data...")
            try {
                db.withTransaction {
                    noteDao.deleteAllNotes()
                    noteDao.deleteAllUserFolders()
                }
                appPreferences.clearLastOpenedItem()
                _syncActivationState.value = SyncActivationState.Running("Loading cloud data...")
                val notesCollection = firestore.collection("users").document(userId).collection("notes")
                val foldersCollection = firestore.collection("users").document(userId).collection("folders")
                val notesSnapshot = notesCollection.get().await()
                val foldersSnapshot = foldersCollection.get().await()
                db.withTransaction {
                    val cloudFolderIdToRoomIdMap = mutableMapOf<String, Long>()
                    foldersSnapshot.documents.forEach { doc ->
                        val cloudData = doc.data
                        if (cloudData != null) {
                            try {
                                val title = cloudData["title"] as? String ?: "Untitled Folder"
                                val rawTimestamp = cloudData["timestamp"]
                                val timestamp = (rawTimestamp as? Timestamp)?.toDate()?.time
                                    ?: (rawTimestamp as? Long)
                                    ?: System.currentTimeMillis()
                                val isArchived = cloudData["isArchived"] as? Boolean ?: false
                                val isInTrash = cloudData["isInTrash"] as? Boolean ?: false
                                val parentCloudId = cloudData["parentCloudId"] as? String
                                val localParentId = if (parentCloudId != null) cloudFolderIdToRoomIdMap[parentCloudId] ?: SpecialFolders.ROOT else SpecialFolders.ROOT
                                val folderToInsert = Folder(
                                    id = 0,
                                    title = title,
                                    parentId = localParentId,
                                    timestamp = timestamp,
                                    isArchived = isArchived,
                                    isInTrash = isInTrash,
                                    isRoot = false,
                                    cloudId = doc.id
                                )
                                val newRoomFolderId = noteDao.insertFolder(folderToInsert)
                                cloudFolderIdToRoomIdMap[doc.id] = newRoomFolderId
                            } catch (e: Exception) {
                            }
                        }
                    }
                    notesSnapshot.documents.forEach { doc ->
                        val cloudData = doc.data
                        if (cloudData != null) {
                            try {
                                val title = cloudData["title"] as? String ?: "Untitled"
                                val content = cloudData["content"] as? String
                                val rawTimestamp = cloudData["timestamp"]
                                val timestamp = (rawTimestamp as? Timestamp)?.toDate()?.time
                                    ?: (rawTimestamp as? Long)
                                    ?: System.currentTimeMillis()
                                val isPinned = cloudData["isPinned"] as? Boolean ?: false
                                val isArchived = cloudData["isArchived"] as? Boolean ?: false
                                val isInTrash = cloudData["isInTrash"] as? Boolean ?: false
                                val parentCloudId = cloudData["parentCloudId"] as? String
                                val localParentId = if (parentCloudId != null) cloudFolderIdToRoomIdMap[parentCloudId] ?: SpecialFolders.ROOT else SpecialFolders.ROOT
                                val noteToInsert = Note(
                                    id = 0,
                                    title = title,
                                    content = content,
                                    timestamp = timestamp,
                                    isPinned = isPinned,
                                    isArchived = isArchived,
                                    isInTrash = isInTrash,
                                    parentId = localParentId,
                                    cloudId = doc.id
                                )
                                noteDao.insertNote(noteToInsert)
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
                _syncActivationState.value = SyncActivationState.Running("Starting sync listeners...")
                appPreferences.saveRealtimeSyncEnabled(true)
                startListeners()
                _syncActivationState.value = SyncActivationState.Success
            } catch (e: Exception) {
                _syncActivationState.value = SyncActivationState.Error("Sync activation failed: ${e.message}")
                appPreferences.saveRealtimeSyncEnabled(false)
            }
        }
    }
    fun activateSyncAndUploadLocalData(userId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            _syncActivationState.value = SyncActivationState.Running("Uploading local data...")
            try {
                val localNotes = noteDao.getAllNotesForBackup()
                val allLocalFolders = noteDao.getAllFoldersForBackup()
                val localUserFoldersToUpload = allLocalFolders.filter { folder ->
                    !SpecialFolders.isSpecial(folder.id) && folder.id != SpecialFolders.ROOT && !folder.isRoot
                }

                val notesCollection = firestore.collection("users").document(userId).collection("notes")
                val foldersCollection = firestore.collection("users").document(userId).collection("folders")

                _syncActivationState.value = SyncActivationState.Running("Uploading folders...")
                val folderIdMap = mutableMapOf<Long, String>()
                localUserFoldersToUpload.forEach { folder ->
                    try {
                        val parentCloudId = if (folder.parentId != SpecialFolders.ROOT && !SpecialFolders.isSpecial(folder.parentId)) folderIdMap[folder.parentId] else null
                        val firestoreData = createFolderFirestoreMap(folder, parentCloudId)
                        val docRef = foldersCollection.add(firestoreData).await()
                        noteDao.updateFolderCloudId(folder.id, docRef.id)
                        folderIdMap[folder.id] = docRef.id
                    } catch (e: Exception) {
                        Log.e("FirestoreSync", "Error uploading folder: ${folder.id}", e)
                    }
                }

                _syncActivationState.value = SyncActivationState.Running("Uploading notes...")
                localNotes.forEach { note ->
                    try {
                        val parentCloudId = if (note.parentId != SpecialFolders.ROOT && !SpecialFolders.isSpecial(note.parentId)) folderIdMap[note.parentId] else null
                        val firestoreData = createNoteFirestoreMap(note, parentCloudId)
                        val docRef = notesCollection.add(firestoreData).await()
                        noteDao.updateNoteCloudId(note.id, docRef.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreSync", "Error uploading note: ${note.id}", e)
                    }
                }

                appPreferences.saveRealtimeSyncEnabled(true)
                _syncActivationState.value = SyncActivationState.Running("Starting sync listeners...")
                startListeners()
                _syncActivationState.value = SyncActivationState.Success
            } catch (e: Exception) {
                _syncActivationState.value = SyncActivationState.Error("Initial sync activation failed: ${e.message}")
                Log.e("FirestoreSync", "Initial sync activation failed", e)
                appPreferences.saveRealtimeSyncEnabled(false)
            }
        }
    }

    fun resetSyncCheckState() { _syncCheckState.value = SyncCheckState.Idle }
    fun resetSyncActivationState() { _syncActivationState.value = SyncActivationState.Idle }
    private fun setupNoteListener(userId: String) {
        val notesCollection = firestore.collection("users").document(userId).collection("notes")
        noteListenerRegistration = notesCollection.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) {
                return@addSnapshotListener
            }
            coroutineScope.launch(Dispatchers.IO) { processNoteChanges(snapshots.documentChanges) }
        }
    }
    private fun setupFolderListener(userId: String) {
        val foldersCollection = firestore.collection("users").document(userId).collection("folders")
        folderListenerRegistration = foldersCollection.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) {
                return@addSnapshotListener
            }
            coroutineScope.launch(Dispatchers.IO) { processFolderChanges(snapshots.documentChanges) }
        }
    }
    private suspend fun processNoteChanges(changes: List<DocumentChange>) {
        for (change in changes) {
            val cloudId = change.document.id
            val cloudData = change.document.data ?: continue
            try {
                val title = cloudData["title"] as? String ?: "Untitled"
                val content = cloudData["content"] as? String
                val rawTimestamp = cloudData["timestamp"]
                val timestamp = (rawTimestamp as? Timestamp)?.toDate()?.time
                    ?: (rawTimestamp as? Long) ?: System.currentTimeMillis()
                val isPinned = cloudData["isPinned"] as? Boolean ?: false
                val isArchived = cloudData["isArchived"] as? Boolean ?: false
                val isInTrash = cloudData["isInTrash"] as? Boolean ?: false
                val parentCloudId = cloudData["parentCloudId"] as? String
                val localParentId = getLocalParentId(parentCloudId)
                val incomingNote = Note(
                    id = 0, title = title, content = content, timestamp = timestamp,
                    isPinned = isPinned, isArchived = isArchived, isInTrash = isInTrash,
                    parentId = localParentId, cloudId = cloudId
                )
                when (change.type) {
                    DocumentChange.Type.ADDED -> handleNoteAdded(incomingNote)
                    DocumentChange.Type.MODIFIED -> handleNoteModified(incomingNote)
                    DocumentChange.Type.REMOVED -> handleNoteRemoved(cloudId)
                }
            } catch (dbEx: Exception) {
            }
        }
    }
    private suspend fun processFolderChanges(changes: List<DocumentChange>) {
        for (change in changes) {
            val cloudId = change.document.id
            val cloudData = change.document.data ?: continue
            try{
                val title = cloudData["title"] as? String ?: "Untitled Folder"
                val rawTimestamp = cloudData["timestamp"]
                val timestamp = (rawTimestamp as? Timestamp)?.toDate()?.time
                    ?: (rawTimestamp as? Long) ?: System.currentTimeMillis()
                val isArchived = cloudData["isArchived"] as? Boolean ?: false
                val isInTrash = cloudData["isInTrash"] as? Boolean ?: false
                val parentCloudId = cloudData["parentCloudId"] as? String
                val localParentId = getLocalParentId(parentCloudId)
                val incomingFolder = Folder(
                    id = 0, title = title, timestamp = timestamp,
                    isArchived = isArchived, isInTrash = isInTrash,
                    parentId = localParentId, cloudId = cloudId,
                    isRoot = false
                )
                when (change.type) {
                    DocumentChange.Type.ADDED -> handleFolderAdded(incomingFolder)
                    DocumentChange.Type.MODIFIED -> handleFolderModified(incomingFolder)
                    DocumentChange.Type.REMOVED -> handleFolderRemoved(cloudId)
                }
            } catch (dbEx: Exception) {
            }
        }
    }
    private suspend fun handleNoteAdded(incomingNote: Note) {
        val potentialLocalMatch = noteDao.findPendingNote(
            incomingNote.title, incomingNote.parentId, incomingNote.timestamp
        )
        if (potentialLocalMatch != null) {
            noteDao.updateNoteCloudId(potentialLocalMatch.id, incomingNote.cloudId)
        } else {
            val existingNote = noteDao.getNoteByCloudId(incomingNote.cloudId!!)
            if (existingNote == null) {
                noteDao.insertNote(incomingNote.copy(id = 0))
            } else {
                if (incomingNote.timestamp >= existingNote.timestamp) {
                    noteDao.updateNote(incomingNote.copy(id = existingNote.id))
                }
            }
        }
    }
    private suspend fun handleFolderAdded(incomingFolder: Folder) {
        val potentialLocalMatch = noteDao.findPendingFolder(
            incomingFolder.title, incomingFolder.parentId, incomingFolder.timestamp
        )
        if (potentialLocalMatch != null) {
            noteDao.updateFolderCloudId(potentialLocalMatch.id, incomingFolder.cloudId)
        } else {
            val existingFolder = noteDao.getFolderByCloudId(incomingFolder.cloudId!!)
            if (existingFolder == null) {
                noteDao.insertFolder(incomingFolder.copy(id = 0))
            } else {
                if (incomingFolder.timestamp >= existingFolder.timestamp) {
                    noteDao.updateFolder(incomingFolder.copy(id = existingFolder.id))
                }
            }
        }
    }
    private suspend fun handleNoteModified(incomingNote: Note) {
        val existingNote = noteDao.getNoteByCloudId(incomingNote.cloudId!!)
        if (existingNote != null) {
            if (incomingNote.timestamp >= existingNote.timestamp) {
                noteDao.updateNote(incomingNote.copy(id = existingNote.id))
            }
        } else {
            noteDao.insertNote(incomingNote.copy(id = 0))
        }
    }
    private suspend fun handleFolderModified(incomingFolder: Folder) {
        val existingFolder = noteDao.getFolderByCloudId(incomingFolder.cloudId!!)
        if (existingFolder != null) {
            if (incomingFolder.timestamp >= existingFolder.timestamp) {
                noteDao.updateFolder(incomingFolder.copy(id = existingFolder.id))
            }
        } else {
            noteDao.insertFolder(incomingFolder.copy(id = 0))
        }
    }
    private suspend fun handleNoteRemoved(cloudId: String) {
        noteDao.getNoteByCloudId(cloudId)?.let { noteDao.deleteNote(it) }
    }
    private suspend fun handleFolderRemoved(cloudId: String) {
        val folderToDelete = noteDao.getFolderByCloudId(cloudId)
        if (folderToDelete != null) {
            try {
                val archivedNotesInFolder = noteDao.getArchivedNotesByParent(folderToDelete.id)
                archivedNotesInFolder.forEach { note ->
                    val updatedNote = note.copy(parentId = SpecialFolders.ARCHIVE, timestamp = System.currentTimeMillis())
                    noteDao.updateNote(updatedNote)
                }
                val archivedSubFolders = noteDao.getArchivedFoldersByParent(folderToDelete.id)
                archivedSubFolders.forEach { subFolder ->
                    val updatedSubFolder = subFolder.copy(parentId = SpecialFolders.ARCHIVE, timestamp = System.currentTimeMillis())
                    noteDao.updateFolder(updatedSubFolder)
                }
                val nonArchivedNotesInFolder = noteDao.getNonArchivedNotesByParent(folderToDelete.id)
                nonArchivedNotesInFolder.forEach { note ->
                    noteDao.deleteNote(note)
                }
                val nonArchivedSubFolders = noteDao.getNonArchivedFoldersByParent(folderToDelete.id)
                nonArchivedSubFolders.forEach { subFolder ->
                    val updatedSubFolder = subFolder.copy(parentId = SpecialFolders.ROOT, timestamp = System.currentTimeMillis())
                    noteDao.updateFolder(updatedSubFolder)
                }
                noteDao.deleteFolder(folderToDelete)
            } catch (e: Exception) {
            }
        }
    }
    fun syncNoteChange(note: Note) {
        if (!appPreferences.isRealtimeSyncEnabled()) return
        coroutineScope.launch(Dispatchers.IO) { writeNoteToFirestore(note) }
    }
    fun syncFolderChange(folder: Folder) {
        if (!appPreferences.isRealtimeSyncEnabled() || SpecialFolders.isSpecial(folder.id) || folder.isRoot) return
        coroutineScope.launch(Dispatchers.IO) { writeFolderToFirestore(folder) }
    }
    fun syncNoteDeletion(note: Note) {
        if (!appPreferences.isRealtimeSyncEnabled() || note.cloudId == null) return
        coroutineScope.launch(Dispatchers.IO) { deleteNoteFromFirestore(note.cloudId!!) }
    }
    fun syncFolderDeletion(folder: Folder) {
        if (!appPreferences.isRealtimeSyncEnabled() || folder.cloudId == null || SpecialFolders.isSpecial(folder.id) || folder.isRoot) return
        coroutineScope.launch(Dispatchers.IO) { deleteFolderFromFirestore(folder.cloudId!!) }
    }
    private suspend fun writeNoteToFirestore(note: Note) {
        val userId = appPreferences.getUserId() ?: return
        try {
            val parentCloudId = getParentCloudId(note.parentId)
            val firestoreData = createNoteFirestoreMap(note, parentCloudId)
            val notesCollection = firestore.collection("users").document(userId).collection("notes")
            val noteCloudId = note.cloudId
            if (noteCloudId == null) {
                val currentDbNote = noteDao.getNoteById(note.id)
                if (currentDbNote?.cloudId != null) {
                    notesCollection.document(currentDbNote.cloudId!!).set(firestoreData, SetOptions.merge()).await()
                    return
                }
                val docRef = notesCollection.add(firestoreData).await()
                noteDao.updateNoteCloudId(note.id, docRef.id)
            } else {
                notesCollection.document(noteCloudId).set(firestoreData, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
        }
    }
    private suspend fun writeFolderToFirestore(folder: Folder) {
        val userId = appPreferences.getUserId() ?: return
        try {
            val parentCloudId = getParentCloudId(folder.parentId)
            val firestoreData = createFolderFirestoreMap(folder, parentCloudId)
            val foldersCollection = firestore.collection("users").document(userId).collection("folders")
            val folderCloudId = folder.cloudId
            if (folderCloudId == null) {
                val currentDbFolder = noteDao.getFolderById(folder.id)
                if (currentDbFolder?.cloudId != null) {
                    foldersCollection.document(currentDbFolder.cloudId!!).set(firestoreData, SetOptions.merge()).await()
                    return
                }
                val docRef = foldersCollection.add(firestoreData).await()
                noteDao.updateFolderCloudId(folder.id, docRef.id)
            } else {
                foldersCollection.document(folderCloudId).set(firestoreData, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
        }
    }
    private suspend fun deleteNoteFromFirestore(cloudId: String) {
        val userId = appPreferences.getUserId() ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("notes").document(cloudId)
                .delete()
                .await()
        } catch (e: Exception) {
        }
    }
    private suspend fun deleteFolderFromFirestore(cloudId: String) {
        val userId = appPreferences.getUserId() ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("folders").document(cloudId)
                .delete()
                .await()
        } catch (e: Exception) {
        }
    }
    private suspend fun getLocalParentId(parentCloudId: String?): Long {
        if (parentCloudId == null) return SpecialFolders.ROOT
        return noteDao.getFolderByCloudId(parentCloudId)?.id ?: SpecialFolders.ROOT
    }
    private suspend fun getParentCloudId(localParentId: Long): String? {
        if (localParentId == SpecialFolders.ROOT || SpecialFolders.isSpecial(localParentId)) {
            return null
        }
        return noteDao.getFolderById(localParentId)?.cloudId
    }
    private fun createNoteFirestoreMap(note: Note, parentCloudId: String?): Map<String, Any?> {
        return mapOf(
            "title" to note.title,
            "content" to note.content,
            "timestamp" to FieldValue.serverTimestamp(),
            "isPinned" to note.isPinned,
            "isArchived" to note.isArchived,
            "isInTrash" to note.isInTrash,
            "parentCloudId" to parentCloudId
        )
    }
    private fun createFolderFirestoreMap(folder: Folder, parentCloudId: String?): Map<String, Any?> {
        return mapOf(
            "title" to folder.title,
            "timestamp" to FieldValue.serverTimestamp(),
            "isArchived" to folder.isArchived,
            "isInTrash" to folder.isInTrash,
            "parentCloudId" to parentCloudId
        )
    }
}