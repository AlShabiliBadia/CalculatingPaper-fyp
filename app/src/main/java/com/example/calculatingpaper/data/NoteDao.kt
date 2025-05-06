package com.example.calculatingpaper.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isInTrash = 0 AND parentId = :parentId")
    fun getNotesByParent(parentId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isArchived = 0 AND isInTrash = 0")
    fun getPinnedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isInTrash = 0")
    fun getAllArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 1 AND isArchived = 0")
    fun getAllTrashedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): Note?

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): Folder?

    @Query("SELECT * FROM folders WHERE isRoot = 1 LIMIT 1")
    suspend fun getRootFolder(): Folder?

    @Query("""
        SELECT * FROM folders 
        WHERE parentId = :parentId 
        AND isRoot = 0
        AND isArchived = 0 
        AND isInTrash = 0
    """)
    fun getFoldersByParent(parentId: Long): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId")
    suspend fun getAllFoldersByParentForMerge(parentId: Long): List<Folder>

    @Query("SELECT * FROM folders WHERE isArchived = 1 AND isInTrash = 0")
    fun getAllArchivedFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE isInTrash = 1 AND isArchived = 0")
    fun getAllTrashedFolders(): Flow<List<Folder>>

    @Query("""
        SELECT COUNT(*) FROM notes 
        WHERE parentId = :folderId 
        AND isInTrash = 0 
        AND isArchived = 0
    """)
    suspend fun getNoteCountInFolder(folderId: Long): Long

    @Query("""
        SELECT COUNT(*) FROM folders 
        WHERE parentId = :folderId 
        AND isInTrash = 0 
        AND isArchived = 0
    """)
    suspend fun getFolderCountInFolder(folderId: Long): Long


    @Query("SELECT * FROM notes WHERE parentId = :parentId AND isArchived = 0 AND isInTrash = 0")
    suspend fun getNonArchivedNotesByParent(parentId: Long): List<Note>

    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isArchived = 0 AND isInTrash = 0")
    suspend fun getNonArchivedFoldersByParent(parentId: Long): List<Folder>

    @Query("SELECT * FROM notes WHERE parentId = :parentId AND isArchived = 1")
    suspend fun getArchivedNotesByParent(parentId: Long): List<Note>

    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isArchived = 1")
    suspend fun getArchivedFoldersByParent(parentId: Long): List<Folder>




    // backup!
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForBackup(): List<Note>

    @Query("SELECT * FROM folders WHERE id > 0")
    suspend fun getAllFoldersForBackup(): List<Folder>


    // reset
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM folders WHERE id > 0")
    suspend fun deleteAllUserFolders()


    //syncing
    @Query("SELECT * FROM notes WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getNoteByCloudId(cloudId: String): Note?

    @Query("SELECT * FROM folders WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getFolderByCloudId(cloudId: String): Folder?

    @Query("UPDATE notes SET cloudId = :cloudId WHERE id = :id")
    suspend fun updateNoteCloudId(id: Long, cloudId: String?)

    @Query("UPDATE folders SET cloudId = :cloudId WHERE id = :id")
    suspend fun updateFolderCloudId(id: Long, cloudId: String?)

    @Query("SELECT * FROM folders WHERE cloudId IS NULL AND title = :title AND parentId = :parentId AND ABS(timestamp - :approxTimestamp) < 10000 ORDER BY timestamp DESC LIMIT 1")
    suspend fun findPendingFolder(title: String, parentId: Long, approxTimestamp: Long): Folder?

    @Query("SELECT * FROM notes WHERE cloudId IS NULL AND title = :title AND parentId = :parentId AND ABS(timestamp - :approxTimestamp) < 10000 ORDER BY timestamp DESC LIMIT 1")
    suspend fun findPendingNote(title: String, parentId: Long, approxTimestamp: Long): Note?

}