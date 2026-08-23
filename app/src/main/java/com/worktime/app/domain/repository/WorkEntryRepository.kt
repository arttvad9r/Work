package com.worktime.app.domain.repository

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface WorkEntryRepository {
    fun observeMonth(month: YearMonth): Flow<List<WorkEntry>>
    suspend fun save(entry: WorkEntry)
    suspend fun delete(date: LocalDate)
    suspend fun restore(entries: List<WorkEntry>)
    suspend fun replaceAll(entries: List<WorkEntry>)
    suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry>
}
