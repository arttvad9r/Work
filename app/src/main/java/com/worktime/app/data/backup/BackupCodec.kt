package com.worktime.app.data.backup

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupData(
    val entries: List<WorkEntry>,
    val preferences: UserPreferences,
)

/**
 * JSON backup format:
 * `{"version":1,"preferences":{...},"entries":[{...}]}` — see [encode].
 */
object BackupCodec {
    const val VERSION = 1

    fun encode(entries: List<WorkEntry>, preferences: UserPreferences): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put(
            "preferences",
            JSONObject()
                .put("defaultHourlyRateMicros", preferences.defaultHourlyRateMicros)
                .put("themeMode", preferences.themeMode.name),
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
        val root = JSONObject(text)
        val version = root.getInt("version")
        require(version == VERSION) { "Unsupported backup version: $version" }

        val jsonPreferences = root.getJSONObject("preferences")
        val preferences = UserPreferences(
            defaultHourlyRateMicros = jsonPreferences.getLong("defaultHourlyRateMicros"),
            themeMode = ThemeMode.valueOf(jsonPreferences.getString("themeMode")),
        )

        val jsonEntries = root.getJSONArray("entries")
        val entries = List(jsonEntries.length()) { index ->
            val jsonEntry = jsonEntries.getJSONObject(index)
            WorkEntry(
                date = LocalDate.parse(jsonEntry.getString("date")),
                workedMinutes = jsonEntry.getInt("workedMinutes"),
                hourlyRateMicros = jsonEntry.getLong("hourlyRateMicros"),
                bonusMicros = jsonEntry.getLong("bonusMicros"),
                penaltyMicros = jsonEntry.getLong("penaltyMicros"),
                note = jsonEntry.getString("note"),
            )
        }
        BackupData(entries = entries, preferences = preferences)
    } catch (error: JSONException) {
        throw IllegalArgumentException("Malformed backup file", error)
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("Malformed backup file", error)
    }
}
