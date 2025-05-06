package com.example.calculatingpaper.view.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calculatingpaper.data.Folder
import com.example.calculatingpaper.data.Note
import com.example.calculatingpaper.view.screens.SpecialFolders

@Composable
fun FolderContentsList(
    folders: List<Folder>,
    notes: List<Note>,
    onShare: (Note) -> Unit = {},
    folderItemCounts: Map<Long, Long>,
    onFolderClick: (Long) -> Unit,
    onNoteEdit: (Note) -> Unit,
    onNoteRename: (Long, String) -> Unit,
    onFolderRename: (Long, String) -> Unit,
    onNoteDelete: (Note) -> Unit,
    onFolderDelete: (Folder) -> Unit,
    onFolderArchive: (Folder) -> Unit,
    onFolderRestore: (Folder) -> Unit,
    onFolderDeletePermanently: (Folder) -> Unit,
    onArchive: (Note) -> Unit,
    onPin: (Note) -> Unit,
    currentlySwipedNoteId: Long?,
    currentlySwipedFolderId: Long?,
    onNoteSwipe: (Long?) -> Unit,
    onFolderSwipe: (Long?) -> Unit,
    currentlyRenamingNoteId: Long?,
    currentlyRenamingFolderId: Long?,
    onNoteRenameStart: (Long) -> Unit,
    onFolderRenameStart: (Long) -> Unit,
    onNoteMoveRequest: (Note) -> Unit,
    onFolderMoveRequest: (Folder) -> Unit,
    searchQuery: String = "",
    isRootFolder: Boolean = false,
    showSystemFolders: Boolean = false,
    systemFolders: List<Folder> = emptyList(),
    systemFolderCounts: Map<Long, Long> = emptyMap()
) {
    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        val filteredFolders = folders.filter { it.title.contains(searchQuery, ignoreCase = true) }
        val hasFoldersToShow = filteredFolders.isNotEmpty()
        val hasAnyFolders = folders.isNotEmpty()
        val filteredNotes = notes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        val hasNotesToShow = filteredNotes.isNotEmpty()
        val hasAnyNotes = notes.isNotEmpty()

        if (hasAnyFolders) {
            item {
//                Text(
//                    text = "Folders",
//                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
//                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
//                )
                if (!hasFoldersToShow && searchQuery.isNotEmpty()) {
                    Text(
                        text = "No folders match your search",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
            if (hasFoldersToShow) {
                items(filteredFolders.sortedBy { it.title }, key = { folder -> "folder_${folder.id}" }) { folder ->
                    FolderCard(
                        folder = folder,
                        itemCount = folderItemCounts[folder.id] ?: 0,
                        onClick = { onFolderClick(folder.id) },
                        onDelete = { onFolderDelete(folder) },
                        onArchive = { onFolderArchive(folder) },
                        onRestore = { onFolderRestore(folder) },
                        onDeletePermanently = { onFolderDeletePermanently(folder) },
                        isArchived = folder.isArchived,
                        isInTrash = folder.isInTrash,
                        currentlySwipedFolderId = currentlySwipedFolderId,
                        onSwipe = onFolderSwipe,
                        isRenaming = currentlyRenamingFolderId == folder.id,
                        onRenameStart = onFolderRenameStart,
                        onRename = { newTitle -> onFolderRename(folder.id, newTitle) },
                        isRenamingAllowed = !SpecialFolders.isSpecial(folder.id),
                        onMoveRequest = { onFolderMoveRequest(folder) }
                    )
                }
            }
        }

        if (hasAnyNotes) {
            item {
                Spacer(modifier = Modifier.height(if (hasAnyFolders) 16.dp else 0.dp))
//                Text(
//                    text = "Notes",
//                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
//                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
//                )
                if (!hasNotesToShow && searchQuery.isNotEmpty()) {
                    Text(
                        text = "No notes match your search",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
            if (hasNotesToShow) {
                val pinnedNotes = filteredNotes.filter { it.isPinned }
                if (pinnedNotes.isNotEmpty()) {
                    items(pinnedNotes.sortedByDescending { it.timestamp }, key = { note -> "note_${note.id}" }) { note ->
                        NoteCard(
                            note = note,
                            onShare = onShare,
                            onEdit = { onNoteEdit(note) },
                            onRename = onNoteRename,
                            onDelete = { onNoteDelete(note) },
                            onArchive = { onArchive(note) },
                            onPin = { onPin(note) },
                            onRestore = { },
                            onDeletePermanently = { },
                            currentlySwipedNoteId = currentlySwipedNoteId,
                            onSwipe = onNoteSwipe,
                            isRenaming = currentlyRenamingNoteId == note.id,
                            onRenameStart = onNoteRenameStart,
                            isRenamingAllowed = true,
                            onMoveRequest = { onNoteMoveRequest(note) }
                        )
                    }
                }

                val unpinnedNotes = filteredNotes.filter { !it.isPinned }
                if (unpinnedNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                    items(unpinnedNotes.sortedByDescending { it.timestamp }, key = { note -> "note_${note.id}" }) { note ->
                        NoteCard(
                            note = note,
                            onShare = onShare,
                            onEdit = { onNoteEdit(note) },
                            onRename = onNoteRename,
                            onDelete = { onNoteDelete(note) },
                            onArchive = { onArchive(note) },
                            onPin = { onPin(note) },
                            onRestore = { },
                            onDeletePermanently = { },
                            currentlySwipedNoteId = currentlySwipedNoteId,
                            onSwipe = onNoteSwipe,
                            isRenaming = currentlyRenamingNoteId == note.id,
                            onRenameStart = onNoteRenameStart,
                            isRenamingAllowed = true,
                            onMoveRequest = { onNoteMoveRequest(note) }
                        )
                    }
                }
            }
        }

        if (isRootFolder && showSystemFolders && systemFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(if (hasAnyFolders || hasAnyNotes) 16.dp else 0.dp))
                Text(
                    text = "System Folders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(systemFolders, key = { folder -> "folder_${folder.id}" }) { folder ->
                FolderCard(
                    folder = folder,
                    itemCount = systemFolderCounts[folder.id] ?: 0,
                    onClick = { onFolderClick(folder.id) },
                    onDelete = { },
                    onArchive = { },
                    onRestore = { },
                    onDeletePermanently = { },
                    isArchived = folder.isArchived,
                    isInTrash = folder.isInTrash,
                    currentlySwipedFolderId = null,
                    onSwipe = { },
                    isRenaming = false,
                    isRenamingAllowed = false,
                    onMoveRequest = { }
                )
            }
        }

        if (!hasAnyFolders && !hasAnyNotes && searchQuery.isEmpty() && !isRootFolder) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This folder is empty",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (isRootFolder && !hasAnyFolders && !hasAnyNotes && !showSystemFolders && searchQuery.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes or folders",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ArchivedNotesList(
    notes: List<Note>,
    folders: List<Folder>,
    onShare: (Note) -> Unit = {},
    onRestore: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onRename: (Long, String) -> Unit,
    onFolderRestore: (Folder) -> Unit,
    onFolderDelete: (Folder) -> Unit,
    currentlySwipedNoteId: Long?,
    currentlySwipedFolderId: Long?,
    onNoteSwipe: (Long?) -> Unit,
    onFolderSwipe: (Long?) -> Unit,
    currentlyRenamingNoteId: Long?,
    currentlyRenamingFolderId: Long?,
    onNoteRenameStart: (Long) -> Unit,
    onFolderRenameStart: (Long) -> Unit,
    searchQuery: String = "",
    onFolderRename: (Long, String) -> Unit = { _, _ -> }
) {
    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        val filteredFolders = folders.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (filteredFolders.isNotEmpty()) {
            item {
                Text(
                    text = "Archived Folders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(filteredFolders.sortedBy { it.title }, key = { folder -> "folder_${folder.id}" }) { folder ->
                FolderCard(
                    folder = folder,
                    itemCount = 0,
                    onClick = {  },
                    onDelete = { onFolderDelete(folder) },
                    onArchive = { },
                    onRestore = { onFolderRestore(folder) },
                    onDeletePermanently = { },
                    isArchived = true,
                    isInTrash = false,
                    currentlySwipedFolderId = currentlySwipedFolderId,
                    onSwipe = onFolderSwipe,
                    isRenaming = currentlyRenamingFolderId == folder.id,
                    onRenameStart = onFolderRenameStart,
                    onRename = { newTitle -> onFolderRename(folder.id, newTitle) },
                    isRenamingAllowed = true,
                    onMoveRequest = { }
                )
            }
        }

        val filteredNotes = notes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (filteredNotes.isNotEmpty()) {
            if (filteredFolders.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            item {
                Text(
                    text = "Archived Notes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(filteredNotes.sortedByDescending { it.timestamp }, key = { note -> "note_${note.id}" }) { note ->
                NoteCard(
                    note = note,
                    onShare = onShare,
                    onRestore = { onRestore(note) },
                    onEdit = onEdit,
                    onRename = onRename,
                    onDelete = { onDelete(note) },
                    onArchive = { },
                    onPin = { },
                    onDeletePermanently = { },
                    currentlySwipedNoteId = currentlySwipedNoteId,
                    onSwipe = onNoteSwipe,
                    isRenaming = currentlyRenamingNoteId == note.id,
                    onRenameStart = onNoteRenameStart,
                    isRenamingAllowed = true,
                    isArchived = true,
                    onMoveRequest = { }
                )
            }
        }

        if (filteredFolders.isEmpty() && filteredNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Archive is empty"
                        else "No archived items match your search",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun TrashNotesList(
    notes: List<Note>,
    folders: List<Folder>,
    onShare: (Note) -> Unit = {},
    onRestore: (Note) -> Unit,
    onDeletePermanently: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onRename: (Long, String) -> Unit,
    onFolderRestore: (Folder) -> Unit,
    onFolderDeletePermanently: (Folder) -> Unit,
    currentlySwipedNoteId: Long?,
    currentlySwipedFolderId: Long?,
    onNoteSwipe: (Long?) -> Unit,
    onFolderSwipe: (Long?) -> Unit,
    currentlyRenamingNoteId: Long?,
    currentlyRenamingFolderId: Long?,
    onNoteRenameStart: (Long) -> Unit,
    onFolderRenameStart: (Long) -> Unit,
    searchQuery: String = ""
) {
    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        val filteredFolders = folders.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (filteredFolders.isNotEmpty()) {
            item {
                Text(
                    text = "Deleted Folders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(filteredFolders.sortedBy { it.title }, key = { folder -> "folder_${folder.id}" }) { folder ->
                FolderCard(
                    folder = folder,
                    itemCount = 0,
                    onClick = {  },
                    onDelete = { },
                    onArchive = { },
                    onRestore = { onFolderRestore(folder) },
                    onDeletePermanently = { onFolderDeletePermanently(folder) },
                    isArchived = false,
                    isInTrash = true,
                    currentlySwipedFolderId = currentlySwipedFolderId,
                    onSwipe = onFolderSwipe,
                    isRenaming = false,
                    onRenameStart = { _ -> },
                    isRenamingAllowed = false,
                    onMoveRequest = { }
                )
            }
        }

        val filteredNotes = notes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        if (filteredNotes.isNotEmpty()) {
            if (filteredFolders.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            item {
                Text(
                    text = "Deleted Notes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(filteredNotes.sortedByDescending { it.timestamp }, key = { note -> "note_${note.id}" }) { note ->
                NoteCard(
                    note = note,
                    onShare = { },
                    onRestore = { onRestore(note) },
                    onEdit = { },
                    onRename = { _, _ -> },
                    onDelete = { },
                    onArchive = { },
                    onPin = { },
                    onDeletePermanently = { onDeletePermanently(note) },
                    currentlySwipedNoteId = currentlySwipedNoteId,
                    onSwipe = onNoteSwipe,
                    isRenaming = false,
                    onRenameStart = { _ -> },
                    isRenamingAllowed = false,
                    isInTrash = true,
                    onMoveRequest = { }
                )
            }
        }

        if (filteredFolders.isEmpty() && filteredNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Recycle Bin is empty"
                        else "No deleted items match your search",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}