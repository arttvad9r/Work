package com.worktime.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {
    @Query(
        """
        SELECT * FROM work_entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY dateEpochDay
        """,
    )
    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<WorkEntryEntity>>

    @Query(
        """
        SELECT * FROM work_entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY dateEpochDay
        """,
    )
    suspend fun getRange(startEpochDay: Long, endEpochDay: Long): List<WorkEntryEntity>

    @Upsert
    suspend fun upsert(entry: WorkEntryEntity)

    @Upsert
    suspend fun upsert(entries: List<WorkEntryEntity>)

    @Transaction
    suspend fun restore(entries: List<WorkEntryEntity>) {
        upsert(entries)
    }

    @Transaction
    suspend fun updateHourlyRate(
        startEpochDay: Long,
        endEpochDay: Long,
        hourlyRateMicros: Long,
    ): List<WorkEntryEntity> = getRange(startEpochDay, endEpochDay).also { entries ->
        upsert(entries.map { it.copy(hourlyRateMicros = hourlyRateMicros) })
    }

    @Query("DELETE FROM work_entries WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteByDate(dateEpochDay: Long)

    @Query("SELECT * FROM work_entries ORDER BY dateEpochDay")
    suspend fun getAll(): List<WorkEntryEntity>

    @Query("DELETE FROM work_entries")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entries: List<WorkEntryEntity>) {
        clearAll()
        upsert(entries)
    }
}
