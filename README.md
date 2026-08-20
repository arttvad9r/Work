# WorkTime

Modern Android app for tracking worked hours and expected salary with a calendar-first workflow.

> **Core loop:** open month → tap a day → enter worked time → save → immediately see updated monthly earnings.

WorkTime is intentionally small. It is a personal work-hours and salary calculator, not a project tracker, HR system, payroll suite, or shift-planning platform.

## Status

The repository contains a **functionally complete MVP implementation**, but it is **not yet a release candidate** because the Android build and device/emulator QA have not been executed in this environment.

Implemented:

- fixed 6×7 month calendar and month navigation;
- persistent create/edit/delete day entries;
- hours/minutes + 4/6/8/10/12-hour quick entry;
- rate snapshots, bonuses, penalties and notes;
- salary/hours/shift monthly summary;
- Room work-entry source of truth;
- DataStore default rate/currency/theme;
- system/light/dark theme;
- EN/RU resources;
- inline validation and recoverable persistence errors;
- deterministic integer-micros money calculation;
- launcher icon placeholder;
- privacy-oriented backup/transfer exclusion rules;
- JVM tests plus instrumented Room test source;
- CI definition for static audit, tests, lint and APK target compilation.

Remaining release gates: [`docs/ANDROID_QA.md`](docs/ANDROID_QA.md) and [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

## Product rules

- One aggregate work entry per calendar date in MVP.
- `shiftCount` increments only when `workedMinutes > 0`.
- Bonus/penalty-only entries are valid and do not count as shifts.
- Worked time requires a positive hourly rate in the editor.
- A saved entry keeps its hourly-rate snapshot; changing the default rate does not rewrite history.
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

The version matrix was rechecked against official upstream documentation during the 20 August 2026 static audit. See [`docs/STATIC_AUDIT.md`](docs/STATIC_AUDIT.md).

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

## Verification without Android tooling

```bash
python3 scripts/static_audit.py
```

The audit checks XML/resource consistency, EN/RU string parity, privacy-sensitive manifest/backup rules, binary floating-point regressions in domain/data and destructive Room fallback usage.

## Android build

See [`docs/BUILD.md`](docs/BUILD.md).

Until Gradle Wrapper bootstrap is completed, command-line verification requires trusted Gradle 9.5.0 on `PATH`:

```bash
./scripts/verify.sh
```

Windows PowerShell:

```powershell
./scripts/verify.ps1
```

These run the static audit and then:

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
:app:assembleDebugAndroidTest
```

### Gradle Wrapper note

`gradle/wrapper/gradle-wrapper.properties` pins 9.5.0, but wrapper scripts/JAR are not yet committed. The binary wrapper JAR cannot be safely generated or retrieved in the current execution environment. This is tracked explicitly instead of committing a fake/broken binary. CI installs Gradle 9.5.0 through `gradle/actions/setup-gradle`.

## Documentation

- [Documentation index](docs/README.md)
- [Product](docs/PRODUCT.md)
- [Research](docs/RESEARCH.md)
- [UX](docs/UX.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Decisions](docs/DECISIONS.md)
- [Roadmap](docs/ROADMAP.md)
- [Backlog](docs/BACKLOG.md)
- [Testing](docs/TESTING.md)
- [Build & CI](docs/BUILD.md)
- [Static audit](docs/STATIC_AUDIT.md)
- [Android QA](docs/ANDROID_QA.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)
- [Privacy](docs/PRIVACY.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## Quality bar

A feature is not release-ready until business invariants are tested where possible, docs match implementation, money remains exact, persistence is migration-safe, Android build/lint passes, and critical flows pass device/emulator/accessibility QA.

## License

No open-source license has been selected. Until a license is added, the repository is proprietary / all rights reserved.
