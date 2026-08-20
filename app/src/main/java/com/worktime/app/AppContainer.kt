package com.worktime.app

import android.content.Context
import androidx.room.Room
import com.worktime.app.data.db.WorkTimeDatabase
import com.worktime.app.data.preferences.DataStoreUserPreferencesRepository
import com.worktime.app.data.repository.RoomWorkEntryRepository
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository

class AppContainer(context: Context) {
    private val database: WorkTimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        WorkTimeDatabase::class.java,
        "worktime.db",
    ).build()

    val workEntryRepository: WorkEntryRepository = RoomWorkEntryRepository(database.workEntryDao())
    val userPreferencesRepository: UserPreferencesRepository =
        DataStoreUserPreferencesRepository(context.applicationContext)
}
