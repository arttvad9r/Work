package com.worktime.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkTimeDatabaseTest {
    private lateinit var database: WorkTimeDatabase
    private lateinit var dao: WorkEntryDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkTimeDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.workEntryDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertObserveAndDelete() = runTest {
        val date = LocalDate.of(2026, 8, 20)
        val entity = WorkEntryEntity(
            dateEpochDay = date.toEpochDay(),
            workedMinutes = 510,
            hourlyRateMicros = 12_500_000,
            bonusMicros = 15_000_000,
            penaltyMicros = 2_500_000,
            note = "Night shift",
        )

        dao.upsert(entity)
        val observed = dao.observeRange(date.toEpochDay(), date.toEpochDay()).first()
        assertEquals(listOf(entity), observed)

        dao.deleteByDate(date.toEpochDay())
        assertTrue(dao.observeRange(date.toEpochDay(), date.toEpochDay()).first().isEmpty())
    }
}
