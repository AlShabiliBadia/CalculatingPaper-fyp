package com.example.calculatingpaper.view.components

import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.data.Note
import com.example.calculatingpaper.utils.NoteUtils
import com.example.calculatingpaper.viewmodel.NoteViewModel
import kotlinx.coroutines.runBlocking

class NoteEditorLifecycleObserver(
    private val noteViewModel: NoteViewModel,
    private val note: Note?,
    private val noteTitleState: State<String>,
    private val noteContentState: State<String>,
    private val originalTitle: String,
    private val originalContent: String,
    private val currentFolderId: Long,
    private val appPreferences: AppPreferences
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        runBlocking {
            saveNote()
        }
    }

    private suspend fun saveNote() {
        val currentContent = noteContentState.value
        val currentTitle = noteTitleState.value

        val trimmedContent = currentContent.trimEnd { it == '\n' || it == '\r' }
        val finalTitle = if (currentTitle.isBlank()) NoteUtils.generateTitleFromContent(trimmedContent) else currentTitle

        val isContentChanged = trimmedContent != originalContent
        val isTitleChanged = finalTitle != originalTitle
        val isMeaningfulChange = isContentChanged || isTitleChanged

        val isNewNote = note == null || note.id == 0L


        val isEffectivelyEmpty = trimmedContent.isBlank()
        val isTitleEffectivelyDefaultOrBlank = finalTitle.isBlank() || finalTitle == NoteUtils.generateTitleFromContent("")
        val isEffectivelyEmptyNewNote = isNewNote && isEffectivelyEmpty && isTitleEffectivelyDefaultOrBlank


        if (!isMeaningfulChange || isEffectivelyEmptyNewNote) {
            return
        }

        val noteToSave = Note(
            id = note?.id ?: 0,
            title = finalTitle,
            content = trimmedContent,
            timestamp = System.currentTimeMillis(),
            isPinned = note?.isPinned ?: false,
            isArchived = note?.isArchived ?: false,
            parentId = note?.parentId ?: currentFolderId
        )

        var savedNoteId = noteToSave.id

        if (isNewNote) {
            savedNoteId = noteViewModel.addNote(noteToSave)
        } else {
            noteViewModel.updateNote(noteToSave)
        }

        if (savedNoteId != 0L) {
            appPreferences.saveLastOpenedItem(AppPreferences.TYPE_NOTE, savedNoteId)
        }
    }
}