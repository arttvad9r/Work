# Architecture

## Goals

The architecture optimizes for a small codebase now without blocking Room persistence, settings, export, or multiple jobs later.

The key rule is that **UI never owns business truth**. Salary calculation, work-entry invariants, and persistence boundaries live outside composables.

## System context

```text
┌──────────────────────── Android app ────────────────────────┐
│                                                             │
│   Compose UI                                                 │
│      │ user intents / immutable UI state                     │
│      ▼                                                       │
│   ViewModel                                                  │
│      │                                                       │
│      ▼                                                       │
│   Domain                                                     │
│   ├─ WorkEntry                                               │
│   ├─ SalaryCalculator                                        │
│   ├─ MonthGrid                                               │
│   └─ use cases (next slice)                                  │
│      │                                                       │
│      ▼                                                       │
│   Repository interfaces                                      │
│      │                                                       │
│      ├────────── Room database (work entries)                 │
│      └────────── DataStore (rate, currency, theme)            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Layer boundaries

### UI

Responsibilities:

- render state;
- collect user intent;
- perform ephemeral presentation logic;
- accessibility semantics and localization.

The UI must not query Room/DataStore directly and must not calculate persisted salary values independently.

### Domain

Responsibilities:

- work-entry invariants;
- salary calculations and rounding;
- monthly aggregation;
- calendar date layout;
- future use cases such as SaveWorkEntry and ObserveMonth.

Domain code stays Android-free where practical and is unit tested on the JVM.

### Data

Planned next slice:

- `WorkEntryEntity` and DAO in Room;
- mapper between entity and domain model;
- `WorkEntryRepository` as the single persistence boundary;
- DataStore-backed `UserPreferencesRepository`;
- Room is the source of truth for work entries.

## Data model

MVP logical record:

```text
WorkEntry
- date: LocalDate                 // unique for one-job MVP
- workedMinutes: Int              // 0..1440
- hourlyRateMicros: Long          // snapshot at save time
- bonusMicros: Long               // >= 0
- penaltyMicros: Long             // >= 0
- note: String
```

Planned Room representation uses an ISO date string or epoch day with a unique index. The exact storage representation must be migration-tested before beta.

## Historical rate rule

`defaultHourlyRate` is only an input when creating a new entry. Saving copies it into `hourlyRateMicros` on that entry. Updating settings later never triggers a historical recalculation.

## Money

Money is represented by `Long` micros. Domain operations use checked arithmetic where practical. Multiplication by minutes is rounded deterministically to the nearest micro using half-up rounding.

Why micros rather than cents:

- hourly rates can contain more than two decimal places;
- minute conversion can produce fractions of a cent;
- aggregation remains deterministic;
- formatting to locale-specific decimal places happens only at the presentation boundary.

## State and UDF

```text
UI event → ViewModel → domain/repository → StateFlow<UiState> → UI
```

`CalendarUiState` is immutable. The current first slice uses an in-memory map inside `CalendarViewModel`; this is intentionally temporary and tracked as technical debt until Room is connected.

## Dependency direction

```text
ui  ─────► domain
data ────► domain

domain ─X─► Android UI / Room / DataStore
```

## Module strategy

Start with a single `:app` module and package-by-layer/feature. Do not introduce multi-module overhead before build times or team boundaries justify it.

A future split can be:

```text
:app
:core:model
:core:database
:core:datastore
:core:designsystem
:feature:calendar
:feature:settings
```

## Persistence roadmap

1. Add Room and schema export.
2. Add `WorkEntryEntity`, DAO, database.
3. Add repository interface and Room implementation.
4. Replace in-memory ViewModel map with repository Flow.
5. Add DataStore preferences.
6. Add migration tests before database version 2 exists.

## Security and privacy

The MVP has no account, analytics SDK, advertising SDK, location access, contacts, or background network requirement. Data remains on-device. Backup policy must be explicitly decided before production release.

## Architectural fitness rules

- Domain tests run without an Android device.
- Composables contain no direct persistence calls.
- Money never enters the domain as `Double`/`Float`.
- Database schema changes require migration tests.
- A default rate change has no side effect on existing entries.
- New feature dependencies point inward, not from domain to UI/framework code.
