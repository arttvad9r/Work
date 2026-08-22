package com.worktime.app.data.repository

import com.worktime.app.data.db.WorkEntryDao
import com.worktime.app.data.db.toDomain
import com.worktime.app.data.db.toEntity
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkEntryRepository(
    private val dao: WorkEntryDao,
) : WorkEntryRepository {
    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> {
        val start = month.atDay(1).toEpochDay()
        val end = month.atEndOfMonth().toEpochDay()
        return dao.observeRange(start, end).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun save(entry: WorkEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun delete(date: LocalDate) {
        dao.deleteByDate(date.toEpochDay())
    }

    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> {
        require(startDate <= endDate) { "startDate must not be after endDate" }
        require(hourlyRateMicros in 1..MoneyLimits.MAX_COMPONENT_MICROS) {
            "hourlyRateMicros is outside the supported range"
        }
        return dao.updateHourlyRate(startDate.toEpochDay(), endDate.toEpochDay(), hourlyRateMicros)
            .map { it.toDomain() }
    }
}
