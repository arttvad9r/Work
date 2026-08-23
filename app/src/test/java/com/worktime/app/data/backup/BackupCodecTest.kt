package com.worktime.app.data.backup

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BackupCodecTest {
    private val entries = listOf(
        WorkEntry(LocalDate.of(2026, 8, 9), 480, 10_000_000),
        WorkEntry(
            date = LocalDate.of(2026, 8, 10),
            workedMinutes = 420,
            hourlyRateMicros = 11_000_000,
            bonusMicros = 2_000_000,
            penaltyMicros = 500_000,
            note = "double shift",
        ),
    )
    private val preferences = UserPreferences(
        defaultHourlyRateMicros = 12_500_000,
        themeMode = ThemeMode.DARK,
    )

    @Test
    fun `encode decode round trip preserves entries and preferences`() {
        val text = BackupCodec.encode(entries, preferences)

        val restored = BackupCodec.decode(text)

        assertEquals(entries, restored.entries)
        assertEquals(preferences, restored.preferences)
    }

    @Test
    fun `decode accepts an empty entry list`() {
        val text = BackupCodec.encode(emptyList(), preferences)

        val restored = BackupCodec.decode(text)

        assertEquals(emptyList<WorkEntry>(), restored.entries)
        assertEquals(preferences, restored.preferences)
    }

    @Test
    fun `decode rejects malformed json`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("not json at all")
        }
    }

    @Test
    fun `decode rejects unsupported version`() {
        val text = BackupCodec.encode(entries, preferences)
            .replaceFirst("\"version\":1", "\"version\":99")

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }

    @Test
    fun `decode rejects an unparsable entry date`() {
        val text = BackupCodec.encode(entries, preferences)
            .replaceFirst("\"date\":\"2026-08-09\"", "\"date\":\"09.08.2026\"")

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }

    @Test
    fun `decode rejects entry values violating domain limits`() {
        val text = BackupCodec.encode(entries, preferences)
            .replaceFirst("\"workedMinutes\":480", "\"workedMinutes\":1500")

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }
}
