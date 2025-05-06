
package com.example.calculatingpaper.data
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentId"]
        )
    ],
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["isPinned"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String?,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "isInTrash")
    val isInTrash: Boolean = false,
    @ColumnInfo(name = "parentId")
    val parentId: Long = 0,
    @ColumnInfo(name = "cloudId")
    var cloudId: String? = null
)
@Serializable
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["isRoot"])
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "parentId")
    val parentId: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "isInTrash")
    val isInTrash: Boolean = false,
    @ColumnInfo(name = "isRoot")
    val isRoot: Boolean = false,
    @ColumnInfo(name = "cloudId")
    var cloudId: String? = null
)