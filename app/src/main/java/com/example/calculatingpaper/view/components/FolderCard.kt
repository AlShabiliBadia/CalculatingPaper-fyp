package com.example.calculatingpaper.view.components
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculatingpaper.data.Folder
import com.example.calculatingpaper.ui.theme.SwipeArchiveColor
import com.example.calculatingpaper.ui.theme.SwipeDeleteColor
import com.example.calculatingpaper.view.screens.SpecialFolders
import kotlin.math.roundToInt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCard(
    folder: Folder,
    itemCount: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    isArchived: Boolean = false,
    isInTrash: Boolean = false,
    currentlySwipedFolderId: Long?,
    onSwipe: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    isRenaming: Boolean = false,
    onRenameStart: (Long) -> Unit = { _ -> },
    onRename: (String) -> Unit = { _ -> },
    isRenamingAllowed: Boolean = true,
    onMoveRequest: () -> Unit
) {
    var newTitle by remember { mutableStateOf(folder.title) }
    var offsetX by remember { mutableStateOf(0f) }
    val swipeRightLength = 0f
    val swipeLeftLength = when {
        SpecialFolders.isSpecial(folder.id) -> 0f
        else -> 300f
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember(folder.title) { mutableStateOf(TextFieldValue(folder.title)) }
    LaunchedEffect(folder.title) {
        newTitle = folder.title
        if (!isRenaming) {
            textFieldValue = TextFieldValue(folder.title)
        }
    }
    LaunchedEffect(offsetX) {
        if (offsetX != 0f && isRenaming) {
            onRenameStart(-1)
        }
    }
    LaunchedEffect(isRenaming) {
        if (isRenaming) {
            offsetX = 0f
            onSwipe(null)
            textFieldValue = TextFieldValue(newTitle, selection = TextRange(0, newTitle.length))
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }
    LaunchedEffect(currentlySwipedFolderId) {
        if (currentlySwipedFolderId != folder.id) {
            offsetX = 0f
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
    ) {
        if (offsetX < 0 && !SpecialFolders.isSpecial(folder.id)) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(70.dp)
                    .width((-offsetX).coerceAtMost(swipeLeftLength).dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp
                        )
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInTrash) {
                    SwipeButton(
                        icon = Icons.Default.Undo,
                        onClick = {
                            onRestore()
                            offsetX = 0f
                        },
                        backgroundColor = SwipeArchiveColor
                    )
                    SwipeButton(
                        icon = Icons.Default.DeleteForever,
                        onClick = {
                            onDeletePermanently()
                            offsetX = 0f
                        },
                        backgroundColor = SwipeDeleteColor
                    )
                } else if (isArchived) {
                    SwipeButton(
                        icon = Icons.Default.Undo,
                        onClick = {
                            onRestore()
                            offsetX = 0f
                        },
                        backgroundColor = SwipeArchiveColor
                    )
                    SwipeButton(
                        icon = Icons.Default.Delete,
                        onClick = {
                            onDelete()
                            offsetX = 0f
                        },
                        backgroundColor = SwipeDeleteColor
                    )
                } else {
                    SwipeButton(
                        icon = Icons.Default.Archive,
                        onClick = {
                            onArchive()
                            offsetX = 0f
                        },
                        backgroundColor = SwipeArchiveColor
                    )
                    SwipeButton(
                        icon = Icons.Default.Delete,
                        onClick = {
                            onDelete()
                            offsetX = 0f
                        },
                        backgroundColor = SwipeDeleteColor
                    )
                }
            }
        }
        if (offsetX > 0 && !isArchived && !isInTrash) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(85.dp)
                    .width(swipeRightLength.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.Start
            ) {
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val targetOffset = (offsetX + delta).coerceIn(-swipeLeftLength, swipeRightLength)
                        offsetX = if (SpecialFolders.isSpecial(folder.id) && targetOffset < 0) 0f else targetOffset
                        if (offsetX != 0f) {
                            onSwipe(folder.id)
                        } else {
                            if(currentlySwipedFolderId == folder.id) onSwipe(null)
                        }
                    },
                    onDragStopped = {
                        when {
                            offsetX < -swipeLeftLength / 2 -> offsetX = -swipeLeftLength
                            else -> {
                                offsetX = 0f
                                if(currentlySwipedFolderId == folder.id) onSwipe(null)
                            }
                        }
                    },
                    enabled = !SpecialFolders.isSpecial(folder.id) && !isRenaming
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(folder.id, isRenaming, offsetX, isArchived, isInTrash) {
                    detectTapGestures(
                        onTap = {
                            if (offsetX != 0f) {
                                offsetX = 0f
                                onSwipe(null)
                            } else if (!isRenaming) {
                                onClick()
                            }
                        },
                        onLongPress = {
                            if (!isRenaming && offsetX == 0f && !isArchived && !isInTrash && !SpecialFolders.isSpecial(folder.id)) {
                                onMoveRequest()
                            }
                        }
                    )
                }
                .fillMaxWidth()
                .height(70.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder Icon",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                if (isRenaming) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 24) {
                                    textFieldValue = newValue
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val finalTitle = textFieldValue.text.trim()
                                    if (finalTitle.isNotEmpty()) {
                                        onRename(finalTitle)
                                    }
                                    onRenameStart(-1)
                                }
                            )
                        )
                        IconButton(
                            onClick = {
                                val finalTitle = textFieldValue.text.trim()
                                if (finalTitle.isNotEmpty()) {
                                    onRename(finalTitle)
                                }
                                onRenameStart(-1)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save Rename",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onRenameStart(-1) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel Rename",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.clickable(
                                enabled = isRenamingAllowed && !SpecialFolders.isSpecial(folder.id),
                                onClick = {
                                    if (isRenamingAllowed && !SpecialFolders.isSpecial(folder.id)) {
                                        onRenameStart(folder.id)
                                    }
                                }
                            )
                        ) {
                            Text(
                                text = folder.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$itemCount items",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                    if (!SpecialFolders.isSpecial(folder.id)) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open folder",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}