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
}

private class FakeWorkEntryDao : WorkEntryDao {
    private val entries = MutableStateFlow<List<WorkEntryEntity>>(emptyList())

    override fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<WorkEntryEntity>> =
        entries.map { current ->
            current
                .filter { it.dateEpochDay in startEpochDay..endEpochDay }
                .sortedBy(WorkEntryEntity::dateEpochDay)
        }

    override suspend fun upsert(entry: WorkEntryEntity) {
        entries.value = entries.value
            .filterNot { it.dateEpochDay == entry.dateEpochDay } + entry
    }

    override suspend fun deleteByDate(dateEpochDay: Long) {
        entries.value = entries.value.filterNot { it.dateEpochDay == dateEpochDay }
    }
}
