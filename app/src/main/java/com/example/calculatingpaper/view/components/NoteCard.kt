package com.example.calculatingpaper.view.components
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculatingpaper.data.Note
import com.example.calculatingpaper.view.components.shareNoteContent
import com.example.calculatingpaper.ui.theme.SwipeArchiveColor
import com.example.calculatingpaper.ui.theme.SwipeDeleteColor
import com.example.calculatingpaper.ui.theme.SwipePinColor
import com.example.calculatingpaper.ui.theme.SwipeShareColor
import com.example.calculatingpaper.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onEdit: ((Note) -> Unit)? = null,
    onRename: ((Long, String) -> Unit)? = null,
    onDelete: ((Note) -> Unit)? = null,
    onArchive: ((Note) -> Unit)? = null,
    onPin: ((Note) -> Unit)? = null,
    onRestore: ((Note) -> Unit)? = null,
    onDeletePermanently: ((Note) -> Unit)? = null,
    onShare: ((Note) -> Unit)? = null,
    isArchived: Boolean = false,
    isInTrash: Boolean = false,
    currentlySwipedNoteId: Long?,
    onSwipe: (Long?) -> Unit,
    isRenaming: Boolean,
    onRenameStart: (Long) -> Unit,
    isRenamingAllowed: Boolean,
    onMoveRequest: () -> Unit
) {
    var newTitle by remember { mutableStateOf(note.title) }
    var offsetX by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(newTitle)) }
    LaunchedEffect(offsetX) {
        if (offsetX != 0f && isRenaming) {
            onRenameStart(-1)
        }
    }
    LaunchedEffect(isRenaming) {
        if (isRenaming) {
            offsetX = 0f
            onSwipe(null)
        }
    }
    LaunchedEffect(currentlySwipedNoteId) {
        if (currentlySwipedNoteId != note.id) {
            offsetX = 0f
        }
    }
    LaunchedEffect(isRenaming) {
        if (isRenaming) {
            textFieldValue = TextFieldValue(newTitle, selection = TextRange(0, newTitle.length))
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val swipeRightLength = if (!isArchived && !isInTrash && onPin != null) 150f else 0f
    val swipeLeftLength = when {
        isArchived || isInTrash -> 300f
        else -> 450f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (offsetX < 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(85.dp)
                    .width(swipeLeftLength.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.End
            ) {
                if (onShare != null && !isInTrash && !isArchived) {
                    SwipeButton(
                        icon = Icons.Default.Share,
                        onClick = {
                            onShare?.invoke(note)
                            offsetX = 0f
                            onSwipe(null)
                        },
                        backgroundColor = SwipeShareColor
                    )
                }
                if (onArchive != null && !isArchived && !isInTrash) {
                    SwipeButton(
                        icon = Icons.Default.Archive,
                        onClick = {
                            onArchive?.invoke(note)
                            offsetX = 0f
                        },
                        backgroundColor = SwipeArchiveColor
                    )
                }
                if (onDelete != null && !isInTrash) {
                    SwipeButton(
                        icon = Icons.Default.Delete,
                        onClick = {
                            onDelete?.invoke(note)
                            offsetX = 0f
                        },
                        backgroundColor = SwipeDeleteColor
                    )
                }
                if (onDeletePermanently != null && isInTrash) {
                    SwipeButton(
                        icon = Icons.Default.DeleteForever,
                        onClick = {
                            onDeletePermanently?.invoke(note)
                            offsetX = 0f
                        },
                        backgroundColor = SwipeDeleteColor
                    )
                }
                if (onRestore != null && (isInTrash || isArchived)) {
                    SwipeButton(
                        icon = Icons.Default.Undo,
                        onClick = {
                            onRestore?.invoke(note)
                            offsetX = 0f
                        },
                        backgroundColor = SwipeArchiveColor
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
                SwipeButton(
                    icon = if (note.isPinned) Icons.Filled.Star else Icons.Default.StarOutline,
                    onClick = {
                        if (onPin != null) {
                            if (note.isPinned) {
                                onPin(note.copy(isPinned = false))
                            } else {
                                onPin(note.copy(isPinned = true))
                            }
                        }
                        offsetX = 0f
                    },
                    backgroundColor = SwipePinColor
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-swipeLeftLength, swipeRightLength)
                        if (offsetX != 0f) {
                            onSwipe(note.id)
                        }
                    },
                    onDragStopped = {
                        when {
                            offsetX > swipeRightLength / 2 -> {
                                offsetX = swipeRightLength
                            }
                            offsetX < -swipeLeftLength / 2 -> {
                                offsetX = -swipeLeftLength
                            }
                            else -> {
                                offsetX = 0f
                                onSwipe(null)
                            }
                        }
                    }
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(note.id, isRenaming, offsetX, isArchived, isInTrash) {
                    detectTapGestures(
                        onTap = {
                            if (offsetX != 0f) {
                                offsetX = 0f
                                onSwipe(null)
                            } else if (!isRenaming && onEdit != null){
                                onEdit(note)
                            }
                        },
                        onLongPress = {
                            if (!isRenaming && offsetX == 0f && !isArchived && !isInTrash) {
                                onMoveRequest()
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isRenaming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (newValue.text.length <= 24) {
                                    textFieldValue = newValue
                                    newTitle = newValue.text
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newTitle.isNotEmpty()) {
                                        onRename?.invoke(note.id, newTitle)
                                        onRenameStart(-1)
                                    }
                                }
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newTitle.isNotEmpty()) {
                                    onRename?.invoke(note.id, newTitle)
                                    onRenameStart(-1)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save Title",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(26.dp))
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
                    Box(
                        modifier = Modifier
                            .clickable(enabled = isRenamingAllowed) {
                                if(isRenamingAllowed) {
                                    onRenameStart(note.id)
                                }
                            }
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
                Text(
                    text = "Last edited: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(note.timestamp)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}
@Composable
fun SwipeButton(
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(50.dp)
            .height(85.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = White,
            modifier = Modifier.size(24.dp)
        )
    }
}