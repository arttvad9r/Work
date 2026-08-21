package com.worktime.app.data.db

import androidx.room.Dao
import androidx.room.Query
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

    @Upsert
    suspend fun upsert(entry: WorkEntryEntity)

    @Query("DELETE FROM work_entries WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteByDate(dateEpochDay: Long)
}
