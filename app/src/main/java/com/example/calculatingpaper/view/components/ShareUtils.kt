package com.example.calculatingpaper.view.components

import android.content.Context
import android.content.Intent
import com.example.calculatingpaper.data.Note

fun shareNoteContent(context: Context, note: Note) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Note via"))
}
