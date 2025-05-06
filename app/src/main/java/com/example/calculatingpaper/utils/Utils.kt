package com.example.calculatingpaper.utils
object NoteUtils {
    private const val DEFAULT_NOTE_TITLE = "New Note"
    private const val MAX_TITLE_WORDS = 3
    private const val DEFAULT_MAX_TITLE_LENGTH = 20
    fun generateTitleFromContent(
        content: String,
        maxLength: Int = DEFAULT_MAX_TITLE_LENGTH
    ): String {
        val firstLine = content
            .split("\n", limit = 2)
            .firstOrNull()
            ?.trim()
            ?: ""
        return if (firstLine.isNotEmpty()) {
            firstLine.split(" ")
                .take(MAX_TITLE_WORDS)
                .joinToString(" ")
                .take(maxLength)
        } else {
            DEFAULT_NOTE_TITLE
        }
    }
}
object UIConstants {
    const val LEFT_ARROW = "←"
    const val RIGHT_ARROW = "→"
    const val RETURN = "↵"
    const val BACKSPACE = "⌫"
    //const val ABC = "abc"
}