# WorkTime

Modern Android app for tracking worked hours and expected salary with a calendar-first workflow.

> **Core loop:** open month → tap a day → enter worked time → save → immediately see updated monthly earnings.

WorkTime is intentionally small. It is a personal work-hours and salary calculator, not a project tracker, HR system, payroll suite, or shift-planning platform.

## Status

The repository currently contains a **functionally complete MVP implementation**, but it is **not yet a release candidate** because the Android build and device/emulator QA have not been executed in this environment.

What is already implemented:

- fixed 6×7 month calendar;
- previous/next month navigation;
- monthly earnings, worked-time and shift-count summary;
- persistent create/edit/delete day entries;
- hours/minutes and 4/6/8/10/12-hour quick entry;
- hourly-rate snapshots on saved entries;
- bonuses, penalties and notes;
- Room as the work-entry source of truth;
- DataStore for default rate, currency and theme;
- system/light/dark theme;
- English and Russian resources;
- inline validation and persistence error states;
- deterministic integer-micros money calculation;
- launcher icon and local-only privacy posture;
- JVM unit tests plus an instrumented Room test source;
- CI definition for tests, lint and APK compilation.

The remaining release gates are documented in [`docs/ANDROID_QA.md`](docs/ANDROID_QA.md) and [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

## Product rules

- One aggregate work entry per calendar date in MVP.
- `shiftCount` increments only when `workedMinutes > 0`.
- Bonus/penalty-only entries are allowed and do not count as shifts.
- Worked time requires a positive hourly rate in the editor.
- A saved entry keeps its own hourly-rate snapshot; changing the default rate does not rewrite history.
- Currency is a global display/accounting unit. Changing the ISO code **does not perform FX conversion**; existing numeric amounts are relabelled.
- Core functionality is offline and does not require an account.

## Technical stack

- **Language:** Kotlin 2.4.x
- **UI:** Jetpack Compose + Material 3
- **Build:** AGP 9.3.1, Gradle 9.5.0, compile/target SDK 37
- **Persistence:** Room 2.8.4 + DataStore 1.2.1
- **State:** ViewModel + immutable `StateFlow`
- **Concurrency:** coroutines + Flow
- **Money:** `Long` micros; no `Float`/`Double` in domain/data
- **Testing:** JUnit 5 JVM tests, AndroidX Test/Room instrumentation source

The version matrix was rechecked against official Android/Gradle documentation during the 20 August 2026 static audit. See [`docs/STATIC_AUDIT.md`](docs/STATIC_AUDIT.md).

## Architecture

```text
Compose UI
    ↓ user intent / immutable state
CalendarViewModel
    ↓
domain repository interfaces + business rules
    ↓
Room / DataStore implementations
```

```text
app/src/main/java/com/worktime/app/
├── data/
│   ├── db/
│   ├── preferences/
│   └── repository/
├── domain/
│   ├── calculation/
│   ├── calendar/
│   ├── model/
│   ├── preferences/
│   └── repository/
└── ui/
    ├── calendar/
    ├── dayeditor/
    ├── format/
    ├── settings/
    └── theme/
```

## Salary model

```text
basePayMicros = roundHalfUp(workedMinutes × hourlyRateMicros / 60)
entryPayMicros = basePayMicros + bonusMicros - penaltyMicros
monthPayMicros = Σ entryPayMicros
shiftCount = count(entries where workedMinutes > 0)
```

Inputs are bounded defensively so checked `Long` arithmetic cannot be driven into overflow by normal UI input.

## Verification without an Android device

Run the repository-only audit:

```bash
python3 scripts/static_audit.py
```

It checks XML/resource consistency, EN/RU string-key parity, privacy-sensitive manifest properties, domain/data floating-point regressions and destructive Room fallback usage.

## Android build

See [`docs/BUILD.md`](docs/BUILD.md).

The repository currently requires an installed **Gradle 9.5.0** for command-line builds:

```bash
./scripts/verify.sh
```

or, on Windows PowerShell:

```powershell
./scripts/verify.ps1
```

The scripts run the static audit and then:

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
:app:assembleDebugAndroidTest
```

### Gradle Wrapper note

`gradle/wrapper/gradle-wrapper.properties` pins 9.5.0, but the wrapper scripts/JAR are not yet committed. The binary wrapper JAR cannot be safely generated or retrieved in the current execution environment. This is tracked explicitly rather than committing a fake/broken wrapper. CI installs Gradle 9.5.0 through `gradle/actions/setup-gradle` and does not depend on the wrapper.

## Documentation

- [Documentation index](docs/README.md)
- [Product specification](docs/PRODUCT.md)
- [Competitor research](docs/RESEARCH.md)
- [UX specification](docs/UX.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Architecture/product decisions](docs/DECISIONS.md)
- [Roadmap](docs/ROADMAP.md)
- [Prioritized backlog](docs/BACKLOG.md)
- [Testing strategy](docs/TESTING.md)
- [Build & CI](docs/BUILD.md)
- [Static audit](docs/STATIC_AUDIT.md)
- [Android QA checklist](docs/ANDROID_QA.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)
- [Privacy/data handling](docs/PRIVACY.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## Quality bar

A feature is not considered release-ready until:

- business invariants are covered by JVM tests where possible;
- user-facing strings are resources;
- money does not enter domain/data as binary floating point;
- persistence is accessed behind repository boundaries;
- schema changes have migration tests;
- no destructive database fallback is introduced;
- Android build/lint passes;
- critical flows pass device/emulator QA and accessibility checks.

## License

No open-source license has been selected. Until a license is added, the repository is proprietary / all rights reserved.
