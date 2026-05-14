package com.example.nr

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Report::class],
    version = 5,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
}
