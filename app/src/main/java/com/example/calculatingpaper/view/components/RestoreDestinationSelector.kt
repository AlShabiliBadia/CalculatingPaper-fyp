package com.example.calculatingpaper.view.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.calculatingpaper.data.Folder
import com.example.calculatingpaper.data.Note
import com.example.calculatingpaper.view.screens.SpecialFolders
import com.example.calculatingpaper.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
sealed class RestorableItem {
    data class NoteItem(val note: Note) : RestorableItem()
    data class FolderItem(val folder: Folder) : RestorableItem()
    val title: String
        get() = when (this) {
            is NoteItem -> note.title
            is FolderItem -> folder.title
        }
    val typeName: String
        get() = when (this) {
            is NoteItem -> "note"
            is FolderItem -> "folder"
        }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreDestinationSelector(
    itemToRestore: RestorableItem,
    viewModel: NoteViewModel,
    onDismissRequest: () -> Unit,
    onDestinationSelected: (destinationFolderId: Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var dialogCurrentFolderId by remember { mutableStateOf(SpecialFolders.ROOT) }
    var dialogCurrentPath by remember { mutableStateOf<List<Folder>>(emptyList()) }
    val foldersInCurrentDialogFolder by viewModel.getFoldersFlow(dialogCurrentFolderId)
        .collectAsState(initial = emptyList())
    LaunchedEffect(dialogCurrentFolderId) {
        scope.launch {
            dialogCurrentPath = viewModel.getFolderPath(dialogCurrentFolderId)
        }
    }
    val displayableFolders = remember(foldersInCurrentDialogFolder) {
        foldersInCurrentDialogFolder.filter { folder ->
            !SpecialFolders.isSpecial(folder.id) && !folder.isRoot
        }
    }
    val isValidDestination = remember(dialogCurrentFolderId) {
        !SpecialFolders.isSpecial(dialogCurrentFolderId)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = "Select Restore Location",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Original parent folder not found. Select a new location for restoring ${itemToRestore.typeName} '${itemToRestore.title}'.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(
                onClick = {
                    val parentId = dialogCurrentPath.firstOrNull()?.parentId ?: SpecialFolders.ROOT
                    if (dialogCurrentFolderId != SpecialFolders.ROOT) {
                        dialogCurrentFolderId = parentId
                    }
                },
                enabled = dialogCurrentFolderId != SpecialFolders.ROOT
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Up Folder")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (dialogCurrentFolderId == SpecialFolders.ROOT) "Main" else dialogCurrentPath.lastOrNull()?.title ?: "Main",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Divider()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 300.dp)
        ) {
            if (displayableFolders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No subfolders", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(displayableFolders, key = { it.id }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dialogCurrentFolderId = folder.id }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = folder.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onDestinationSelected(dialogCurrentFolderId) },
                enabled = isValidDestination
            ) {
                Text("Restore Here")
            }
        }
    }
}