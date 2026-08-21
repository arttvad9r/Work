# WorkTime

WorkTime is a compact offline Android timesheet for recording worked time and calculating an expected monthly amount.

The core flow is intentionally short:

```text
open month -> tap a day -> enter duration and hourly rate -> save
```

## Current interface

- fixed Monday-first 6 x 7 calendar that does not scroll or jump;
- faint adjacent-month dates for calendar continuity;
- centered worked duration and daily amount inside filled cells;
- compact fixed monthly card with work days, hours worked and monthly income;
- a separate draggable bottom report with days, hours, optional bonus/penalty and total;
- one day-editor sheet with duration and hourly rate on one row;
- optional bonus above optional penalty;
- compact settings sheet with hourly rate and system/light/dark theme;
- controlled light/dark color palettes and consistent Material 3 shapes.

Currency selection, currency symbols, notes and quick-duration presets are intentionally not part of the product. Numeric amounts are shown as neutral values. A fractional part is shown only when it is non-zero. Durations are displayed as `0`, `15` or `15:30`.

## Product rules

- One aggregate entry per date.
- Worked time is limited to `0..24:00`.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only entries are valid but do not count as work days.
- A saved entry keeps its hourly-rate snapshot; changing the default rate does not rewrite history.
- Core functionality is local and requires neither an account nor network access.

```text
ratePayMicros = roundHalfUp(workedMinutes x hourlyRateMicros / 60)
entryTotalMicros = ratePayMicros + bonusMicros - penaltyMicros
monthTotalMicros = sum(entryTotalMicros)
workDays = count(entries where workedMinutes > 0)
```

Amounts use integer micros in domain/data code. `Float` and `Double` are not used for persisted calculations.

## Stack

- Kotlin 2.4.x
- Jetpack Compose + Material 3
- AGP 9.3.1 / Gradle 9.5.0 / Java 17
- Room 2.8.4
- DataStore 1.2.1
- coroutines and `StateFlow`
- JUnit 5 and AndroidX Test

## Verification

Preferred command:

```bash
./scripts/verify.sh
```

It runs the static audit, JVM tests, lint, debug APK assembly and debug instrumentation APK assembly. Device execution remains a separate release gate.

The current feature branch has passed targeted source/resource checks, but the latest GitHub Actions job ended before executing any step. Therefore this repository does not claim a successful CI build or physical-device verification for the newest interface commit. See [the current audit](docs/STATIC_AUDIT.md) and [Android QA checklist](docs/ANDROID_QA.md).

## Documentation

- [Product](docs/PRODUCT.md)
- [UX](docs/UX.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Decisions](docs/DECISIONS.md)
- [Testing](docs/TESTING.md)
- [Build and CI](docs/BUILD.md)
- [Current audit](docs/STATIC_AUDIT.md)
- [Android QA](docs/ANDROID_QA.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)
- [Privacy](docs/PRIVACY.md)
- [Changelog](CHANGELOG.md)

## License

No open-source license has been selected. The repository is all rights reserved until a license is added.
