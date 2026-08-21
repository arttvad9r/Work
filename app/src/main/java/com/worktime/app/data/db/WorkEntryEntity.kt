package com.worktime.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate

@Entity(tableName = "work_entries")
data class WorkEntryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val workedMinutes: Int,
    val hourlyRateMicros: Long,
    val bonusMicros: Long,
    val penaltyMicros: Long,
    val note: String,
)

fun WorkEntryEntity.toDomain(): WorkEntry = WorkEntry(
    date = LocalDate.ofEpochDay(dateEpochDay),
    workedMinutes = workedMinutes,
    hourlyRateMicros = hourlyRateMicros,
    bonusMicros = bonusMicros,
    penaltyMicros = penaltyMicros,
    note = note,
)

fun WorkEntry.toEntity(): WorkEntryEntity = WorkEntryEntity(
    dateEpochDay = date.toEpochDay(),
    workedMinutes = workedMinutes,
    hourlyRateMicros = hourlyRateMicros,
    bonusMicros = bonusMicros,
    penaltyMicros = penaltyMicros,
    note = note,
)
