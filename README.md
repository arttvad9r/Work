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

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** layered, unidirectional data flow; UI → domain → data
- **State:** immutable UI state exposed by ViewModels
- **Persistence:** Room for work entries, DataStore for preferences
- **Money:** integer minor units / micros; never `Float` or `Double`
- **Concurrency:** coroutines + Flow
- **Testing:** domain unit tests first, repository tests, Compose UI tests for critical flows
- **Platform:** Android, compile SDK 37

```text
app/
├── ui/            Compose screens, components, theme
├── domain/        models, calculations, use cases
└── data/          persistence and repositories

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
basePay = workedMinutes × hourlyRate / 60
entryPay = basePay + bonus - penalty
monthPay = Σ entryPay
```

The hourly rate is copied into each saved work entry as a **snapshot**. Changing the default rate later must not silently rewrite historical months.

## Development status

The repository has been bootstrapped and the first implementation slice is being built around three foundations:

1. deterministic salary calculations;
2. calendar-first Compose UI;
3. local persistence and settings.

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for delivery phases and [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for technical boundaries.

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

The initial product scope, competitor research, UX rules, business rules, architecture, and roadmap were consolidated in the **WorkTime Product & Technical Specification v1.0** dated 20 August 2026. Repository documentation is the implementation-oriented source of truth from this point forward.

## License

No open-source license has been selected yet. Until a license is added, the repository should be treated as proprietary / all rights reserved.
