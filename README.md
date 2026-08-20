# WorkTime

Modern Android app for tracking worked hours and expected salary with a calendar-first workflow.

> **Core loop:** open month → tap a day → enter worked time → save → immediately see updated monthly earnings.

WorkTime is intentionally small. It is a personal work-hours and salary calculator, not a project tracker, HR system, payroll suite, or shift-planning platform.

## Product goals

- Make a typical shift entry possible in under 10 seconds.
- Keep monthly earnings, worked time, and shift count visible without opening reports.
- Preserve historical correctness when the default hourly rate changes.
- Work fully offline without mandatory registration.
- Keep the core flow free from interstitial ads and dark patterns.

## Current implementation

The first foundation slice now includes:

- Android/Compose project scaffold;
- Material 3 theme;
- fixed 6×7 month calendar prototype;
- monthly earnings/hours/shift summary;
- day editor bottom sheet with quick durations, hourly rate, bonus, penalty and note;
- deterministic salary calculations using integer micros;
- golden salary tests and month-grid tests;
- CI, Dependabot, issue templates and PR template.

Persistence is intentionally the next slice: the current prototype keeps entries in ViewModel memory while Room/DataStore boundaries are implemented.

## MVP scope

- Month calendar as the main screen.
- Worked hours and minutes per day.
- Default hourly rate with a per-day rate snapshot.
- Bonus and penalty adjustments attached to a day.
- Monthly totals: earnings, worked time, shift count.
- Create, edit, and delete a day entry.
- Light, dark, and system themes.
- Local-first persistence.

### Explicit non-goals for v1.0

Timer/clock-in, GPS, clients, projects, tasks, invoices, taxes, cloud accounts, team features, complex overtime rules, and shift-pattern generation.

## Technical direction

- **Language:** Kotlin 2.4.x
- **UI:** Jetpack Compose + Material 3
- **Build:** AGP 9.3.x, Gradle 9.5, compileSdk 37
- **Architecture:** layered, unidirectional data flow; UI → domain → data
- **State:** immutable UI state exposed by ViewModels
- **Persistence:** Room for work entries, DataStore for preferences
- **Money:** integer micros; never `Float` or `Double` in domain/data
- **Concurrency:** coroutines + Flow
- **Testing:** domain unit tests first, then repository and Compose UI tests

```text
app/
└── src/main/java/com/worktime/app/
    ├── domain/
    │   ├── calculation/
    │   ├── calendar/
    │   └── model/
    └── ui/
        ├── calendar/
        ├── dayeditor/
        └── theme/

docs/
├── PRODUCT.md
├── ARCHITECTURE.md
├── ROADMAP.md
├── TESTING.md
└── DECISIONS.md
```

## Salary model

For a work entry:

```text
basePay = round(workedMinutes × hourlyRateMicros / 60)
entryPay = basePay + bonusMicros - penaltyMicros
monthPay = Σ entryPay
```

The hourly rate is copied into each saved work entry as a **snapshot**. Changing the default rate later must not silently rewrite historical months.

## Getting started

Requirements:

- Android Studio Quail 3 or newer recommended;
- JDK 17;
- Android SDK 37.

Until the Gradle wrapper binary is added, either open the project in Android Studio or use Gradle 9.5 installed locally:

```bash
gradle testDebugUnitTest lintDebug
```

## Documentation

- [Product specification](docs/PRODUCT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Testing strategy](docs/TESTING.md)
- [Architecture & product decisions](docs/DECISIONS.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## Quality bar

A feature is not done until:

- acceptance criteria are met;
- business logic has unit tests;
- user-facing strings are localizable;
- the UI works in light/dark themes and with large font scale;
- no database or settings API is accessed directly from composables;
- no blocking work runs on the main thread;
- money calculations are deterministic and covered by golden cases.

## Source specification

The initial product scope, competitor research, UX rules, business rules, architecture, and roadmap were consolidated in **WorkTime Product & Technical Specification v1.0**, dated 20 August 2026. Repository documentation is the implementation-oriented source of truth from this point forward.

## License

No open-source license has been selected yet. Until a license is added, the repository should be treated as proprietary / all rights reserved.
