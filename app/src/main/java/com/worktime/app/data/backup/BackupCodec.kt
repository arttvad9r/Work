package com.worktime.app.data.backup

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupData(
    val entries: List<WorkEntry>,
    val preferences: UserPreferences,
    val defaultRateInitialized: Boolean,
)

/**
 * JSON backup format:
 * `{"version":2,"preferences":{...},"entries":[{...}]}` — see [encode].
 * Version 1 remains import-compatible.
 */
object BackupCodec {
    const val VERSION = 2
    private const val LEGACY_VERSION = 1
    const val MAX_BACKUP_SIZE_BYTES = 4 * 1024 * 1024

    fun encode(
        entries: List<WorkEntry>,
        preferences: UserPreferences,
        defaultRateInitialized: Boolean = true,
    ): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put(
            "preferences",
            JSONObject()
                .put("defaultHourlyRateMicros", preferences.defaultHourlyRateMicros)
                .put("themeMode", preferences.themeMode.name)
                .put("defaultRateInitialized", defaultRateInitialized),
        )
        val jsonEntries = JSONArray()
        entries.forEach { entry ->
            jsonEntries.put(
                JSONObject()
                    .put("date", entry.date.toString())
                    .put("workedMinutes", entry.workedMinutes)
                    .put("hourlyRateMicros", entry.hourlyRateMicros)
                    .put("bonusMicros", entry.bonusMicros)
                    .put("penaltyMicros", entry.penaltyMicros)
                    .put("note", entry.note),
            )
        }
        root.put("entries", jsonEntries)
        return root.toString()
    }

    fun decode(text: String): BackupData = try {
        require(text.isNotBlank()) { "Malformed backup file" }
        require(text.toByteArray(StandardCharsets.UTF_8).size <= MAX_BACKUP_SIZE_BYTES) {
            "Backup file is too large"
        }
        val root = JSONObject(text)
        val version = root.getInt("version")
        require(version == LEGACY_VERSION || version == VERSION) {
            "Unsupported backup version: $version"
        }

        val jsonPreferences = root.getJSONObject("preferences")
        val preferences = UserPreferences(
            defaultHourlyRateMicros = jsonPreferences.getLong("defaultHourlyRateMicros"),
            themeMode = ThemeMode.valueOf(jsonPreferences.getString("themeMode")),
        )

        val jsonEntries = root.getJSONArray("entries")
        val dates = HashSet<LocalDate>(jsonEntries.length())
        val entries = List(jsonEntries.length()) { index ->
            val jsonEntry = jsonEntries.getJSONObject(index)
            val entry = WorkEntry(
                date = LocalDate.parse(jsonEntry.getString("date")),
                workedMinutes = jsonEntry.getInt("workedMinutes"),
                hourlyRateMicros = jsonEntry.getLong("hourlyRateMicros"),
                bonusMicros = jsonEntry.getLong("bonusMicros"),
                penaltyMicros = jsonEntry.getLong("penaltyMicros"),
                note = jsonEntry.optString("note", ""),
            )
            require(dates.add(entry.date)) { "Duplicate entry date: ${entry.date}" }
            entry
        }
        val defaultRateInitialized = if (version >= VERSION) {
            jsonPreferences.getBoolean("defaultRateInitialized")
        } else {
            inferLegacyDefaultRateInitialized(entries, preferences)
        }
        BackupData(
            entries = entries,
            preferences = preferences,
            defaultRateInitialized = defaultRateInitialized,
        )
    } catch (error: JSONException) {
        throw IllegalArgumentException("Malformed backup file", error)
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Malformed backup file", error)
    } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("Malformed backup file", error)
    }

    private fun inferLegacyDefaultRateInitialized(
        entries: List<WorkEntry>,
        preferences: UserPreferences,
    ): Boolean = preferences.defaultHourlyRateMicros != 0L || entries.any { it.workedMinutes > 0 }
}
