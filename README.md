# WorkTime

WorkTime is a compact offline Android timesheet for recording worked time by date and calculating expected income.

The core flow is intentionally short:

```text
open month -> tap a day -> enter duration and hourly rate -> save
```

## Current interface

- fixed Monday-first 6 × 7 calendar with direct month navigation and a month picker;
- worked days show date, worked duration and daily income with a restrained visual hierarchy;
- contextual `Fill today` action appears only when today belongs to the visible month and has no entry;
- compact monthly summary strip opens a draggable detailed month report;
- day editor is a modal sheet with duration, hourly rate and optional bonus/penalty inline fields;
- settings contain the default rate, bulk rate change, theme selection and JSON/CSV data operations;
- year summary is a full-screen, view-only yearly report opened from the monthly report;
- optional home-screen month-summary widget;
- controlled light/dark Material 3 palettes and portrait-only layout.

There is no currency selector or conversion model; presentation uses fixed `₽` labels. Notes and quick-duration presets are intentionally not part of the product. Detailed monetary values show a fractional part only when it is non-zero; dense calendar cells and the compact summary strip round to whole rubles to preserve fixed geometry. Durations are displayed compactly (`0`, `15`, `15:30`).

The interface follows one shared component/dimension contract ([UI system](docs/UI_SYSTEM.md)): navigation, value, editable, segmented and action controls use the same semantics and sizing across screens and sheets.

## Product rules

- One aggregate entry per date.
- Worked time is limited to `0..24:00`.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only entries are valid but do not count as work days.
- The first saved shift can initialize the default hourly rate when no default exists.
- Once initialized, the default rate is not overwritten by a different rate entered for an individual day.
- A saved entry keeps its hourly-rate snapshot; changing the default rate does not rewrite history.
- Bulk rate changes affect only entries inside the selected period and leave the default rate unchanged.
- Core functionality is local and requires neither an account nor network access.

```text
ratePayMicros = roundHalfUp(workedMinutes × hourlyRateMicros / 60)
entryTotalMicros = ratePayMicros + bonusMicros - penaltyMicros
monthTotalMicros = sum(entryTotalMicros)
workDays = count(entries where workedMinutes > 0)
```

Amounts use integer micros in domain/data code. `Float` and `Double` are not used for persisted calculations.

## Stack

- Kotlin 2.4.x
- Jetpack Compose + Material 3
- AGP 9.3.2 / Gradle Wrapper 9.7.1 / Java 17
- Room 2.8.4
- DataStore 1.2.1
- coroutines and `StateFlow`
- JUnit 6 and AndroidX Test

## Verification

Preferred local command:

```bash
./scripts/verify.sh
```

It uses the repository Gradle Wrapper and runs the static audit, JVM tests, lint, debug APK assembly and debug instrumentation APK assembly. Physical-device interaction testing remains a separate release gate, especially for IME focus, modal-sheet behavior, calendar gestures and launcher/widget integration.

GitHub Actions runs that terminate before executing workflow steps are treated as runner/account infrastructure failures, not as successful or failed Gradle verification.

See the [Android QA checklist](docs/ANDROID_QA.md) and [release checklist](docs/RELEASE_CHECKLIST.md).

## Documentation

- [Documentation index](docs/README.md)
- [Product](docs/PRODUCT.md)
- [UX](docs/UX.md)
- [UI system](docs/UI_SYSTEM.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Build and CI](docs/BUILD.md)
- [Testing](docs/TESTING.md)
- [Android QA](docs/ANDROID_QA.md)
- [Roadmap](docs/ROADMAP.md)
- [Backlog](docs/BACKLOG.md)
- [Privacy](docs/PRIVACY.md)
- [Changelog](CHANGELOG.md)

## License

No open-source license has been selected. The repository is all rights reserved until a license is added.
