package com.worktime.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.data.db.WorkTimeDatabase
import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomWorkEntryRepositoryInstrumentedTest {
    private lateinit var database: WorkTimeDatabase
    private lateinit var repository: RoomWorkEntryRepository

    @Before
    fun createRepository() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkTimeDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomWorkEntryRepository(database.workEntryDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun bulkHourlyRateUsesLiveRoomDaoPath() = runTest {
        val original = WorkEntry(
            date = LocalDate.of(2026, 8, 10),
            workedMinutes = 480,
            hourlyRateMicros = 10_000_000,
            bonusMicros = 2_000_000,
            penaltyMicros = 1_000_000,
            note = "historical",
        )
        repository.save(original)

        assertEquals(
            listOf(original),
            repository.updateHourlyRate(original.date, original.date, 20_000_000),
        )
        assertEquals(
            listOf(original.copy(hourlyRateMicros = 20_000_000)),
            repository.observeMonth(java.time.YearMonth.of(2026, 8)).first(),
        )
    }
}
