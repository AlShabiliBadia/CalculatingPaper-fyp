package com.example.calculatingpaper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Note::class, Folder::class], version = 4)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                db.execSQL("""
                                    INSERT OR IGNORE INTO folders 
                                    (id, title, parentId, timestamp, isArchived, isInTrash, isRoot) 
                                    VALUES 
                                    (0, 'Main', 0, ${System.currentTimeMillis()}, 0, 0, 1),
                                    (-1, 'Archive', 0, ${System.currentTimeMillis()}, 1, 0, 0),
                                    (-2, 'Recycle Bin', 0, ${System.currentTimeMillis()}, 0, 1, 0)
                                """)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}