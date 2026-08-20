package com.worktime.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WorkTimeDatabase : RoomDatabase() {
    abstract fun workEntryDao(): WorkEntryDao
}
