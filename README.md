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
- full-screen settings pages for hourly rate, statistics, rate history and system/light/dark theme;
- controlled light/dark color palettes and consistent Material 3 shapes;
- portrait-only application layout.

There is no currency selector or conversion model; the current presentation uses the fixed `₽` and `₽/h` labels. Notes and quick-duration presets are intentionally not part of the product. A fractional part is shown only when it is non-zero. Durations are displayed as `0`, `15` or `15:30`.

Numeric validation is deliberately compact: invalid input is indicated by the red field outline without helper text. Persistence failures are separate operational errors and are shown transiently without changing sheet geometry.

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
- AGP 9.3.1 / Gradle Wrapper 9.5.0 / Java 17
- Room 2.8.4
- DataStore 1.2.1
- coroutines and `StateFlow`
- JUnit 5 and AndroidX Test

## Verification

Preferred command:

```bash
./scripts/verify.sh
```

It uses the repository Gradle Wrapper and runs the static audit, JVM tests, lint, debug APK assembly and debug instrumentation APK assembly. Device execution remains a separate release gate.

The `main` baseline has been exercised on physical hardware by the project owner. Interaction changes that affect IME focus, bottom-sheet drag/tap behavior or insets require a fresh focused device pass before merge.

See the [Android QA checklist](docs/ANDROID_QA.md).

## Documentation

- [Product](docs/PRODUCT.md)
- [UX](docs/UX.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Decisions](docs/DECISIONS.md)
- [Testing](docs/TESTING.md)
- [Build and CI](docs/BUILD.md)
- [Android QA](docs/ANDROID_QA.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)
- [Privacy](docs/PRIVACY.md)
- [Changelog](CHANGELOG.md)

## License

No open-source license has been selected. The repository is all rights reserved until a license is added.
