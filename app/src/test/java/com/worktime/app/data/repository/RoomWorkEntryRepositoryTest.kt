package com.worktime.app.data.repository

import com.worktime.app.data.db.WorkEntryDao
import com.worktime.app.data.db.WorkEntryEntity
import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomWorkEntryRepositoryTest {
    @Test
    fun `save observe and delete preserve month boundary`() = runTest {
        val dao = FakeWorkEntryDao()
        val repository = RoomWorkEntryRepository(dao)
        val august = YearMonth.of(2026, 8)
        val augustEntry = WorkEntry(
            date = LocalDate.of(2026, 8, 20),
            workedMinutes = 480,
            hourlyRateMicros = 12_500_000,
            bonusMicros = 5_000_000,
        )
        val septemberEntry = WorkEntry(
            date = LocalDate.of(2026, 9, 1),
            workedMinutes = 300,
            hourlyRateMicros = 12_500_000,
        )

        repository.save(augustEntry)
        repository.save(septemberEntry)

        assertEquals(listOf(augustEntry), repository.observeMonth(august).first())

        repository.delete(augustEntry.date)
        assertTrue(repository.observeMonth(august).first().isEmpty())
    }

    @Test
    fun `bulk hourly rate updates inclusive range and returns original records`() = runTest {
        val dao = FakeWorkEntryDao()
        val repository = RoomWorkEntryRepository(dao)
        val before = listOf(
            WorkEntry(LocalDate.of(2026, 8, 9), 480, 10_000_000),
            WorkEntry(LocalDate.of(2026, 8, 10), 420, 11_000_000, bonusMicros = 2_000_000),
            WorkEntry(LocalDate.of(2026, 8, 12), 360, 12_000_000, penaltyMicros = 1_000_000, note = "late"),
            WorkEntry(LocalDate.of(2026, 8, 13), 300, 13_000_000),
        )
        before.forEach { repository.save(it) }

        val changed = repository.updateHourlyRate(
            startDate = LocalDate.of(2026, 8, 10),
            endDate = LocalDate.of(2026, 8, 12),
            hourlyRateMicros = 20_000_000,
        )

        assertEquals(before.subList(1, 3), changed)
        assertEquals(
            before.map { entry ->
                if (entry.date in LocalDate.of(2026, 8, 10)..LocalDate.of(2026, 8, 12)) {
                    entry.copy(hourlyRateMicros = 20_000_000)
                } else {
                    entry
                }
            },
            repository.observeMonth(YearMonth.of(2026, 8)).first(),
        )
    }
}

private class FakeWorkEntryDao : WorkEntryDao {
    private val entries = MutableStateFlow<List<WorkEntryEntity>>(emptyList())

    override fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<WorkEntryEntity>> =
        entries.map { current ->
            current
                .filter { it.dateEpochDay in startEpochDay..endEpochDay }
                .sortedBy(WorkEntryEntity::dateEpochDay)
        }

    override suspend fun getRange(startEpochDay: Long, endEpochDay: Long): List<WorkEntryEntity> =
        entries.value
            .filter { it.dateEpochDay in startEpochDay..endEpochDay }
            .sortedBy(WorkEntryEntity::dateEpochDay)

    override suspend fun upsert(entry: WorkEntryEntity) {
        entries.value = entries.value
            .filterNot { it.dateEpochDay == entry.dateEpochDay } + entry
    }

    override suspend fun upsert(entries: List<WorkEntryEntity>) {
        entries.forEach { upsert(it) }
    }

    override suspend fun deleteByDate(dateEpochDay: Long) {
        entries.value = entries.value.filterNot { it.dateEpochDay == dateEpochDay }
    }
}
