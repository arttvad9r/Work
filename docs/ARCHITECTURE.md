# Architecture

## Goals

WorkTime is intentionally small, offline-first and calendar-first. The architecture optimizes for deterministic money calculations and persistence correctness without adding premature module/framework complexity.

The key rule is: **UI never owns business truth**.

## System context

```text
Compose UI
   │ user intent / immutable state
   ▼
CalendarViewModel
   │
   ├──────────────► domain business rules
   │                ├─ WorkEntry invariants
   │                ├─ MoneyLimits
   │                ├─ SalaryCalculator
   │                └─ MonthGrid
   │
   ▼
domain repository interfaces
   │
   ├──────────────► Room implementation → SQLite work entries
   └──────────────► DataStore implementation → user preferences
```

## Dependency direction

```text
ui   ─────► domain
data ─────► domain
app composition root ─► data + domain

domain ─X─► Compose / Android UI / Room / DataStore
```

Repository interfaces live in `domain/repository`; UI does not depend on concrete Room/DataStore classes.

## UI/state

`CalendarViewModel` combines:

- the currently requested `YearMonth`;
- Room's month `Flow`;
- DataStore preferences;
- selected editor date;
- settings visibility;
- recoverable write-error state.

The month and its Room entries are emitted as one snapshot to avoid showing one month's title with another month's data during rapid navigation.

`CalendarUiState.isReady` remains false until real persistence/preferences data has emitted. This prevents a new editor from snapshotting placeholder rate/currency values during cold start.

Write failures do not log user financial data. The relevant modal remains open with the draft intact and displays a generic operation error.

## Domain model

```text
WorkEntry
- date: LocalDate
- workedMinutes: Int              // 0..1440
- hourlyRateMicros: Long
- bonusMicros: Long
- penaltyMicros: Long
- note: String                    // <= 200 chars
```

One aggregate record per date is an MVP constraint. `dateEpochDay` is the Room primary key.

An entry must contain worked time or at least one adjustment. Bonus/penalty-only records are valid. The editor requires a positive hourly rate when worked time is greater than zero.

## Money

Domain/data money is `Long` micros:

```text
1 major currency unit = 1,000,000 micros
```

```text
basePay = roundHalfUp(workedMinutes × hourlyRateMicros / 60)
entryPay = basePay + bonusMicros - penaltyMicros
monthPay = Σ entryPay
```

`SalaryCalculator` uses checked integer arithmetic. `MoneyLimits.MAX_COMPONENT_MICROS` bounds each user-entered rate/bonus/penalty defensively so normal UI input cannot drive the checked arithmetic into `Long` overflow.

The upper bound is a safety implementation limit, not a payroll/product entitlement limit.

Decimal parsing and localized currency formatting exist only at the presentation boundary. Exponent notation is rejected by user-input parsing.

## Historical rate

The default hourly rate is an editor default only. Saving a work entry copies the effective rate into `WorkEntry.hourlyRateMicros`. Changing settings later does not rewrite existing records.

## Currency semantics

MVP has one global ISO currency code in DataStore. Work entries do not store a currency code.

Changing the currency code changes presentation/accounting labels for all existing numeric amounts; **no FX conversion occurs**. This is explicit in settings. Multi-currency support would require a new product/data-model decision and migration.

## Persistence

### Room

- `WorkEntryEntity` maps one-to-one with the MVP domain record.
- `WorkEntryDao.observeRange()` exposes ordered month-range `Flow` data.
- `@Upsert` enforces one aggregate record per date through the primary key.
- no destructive migration fallback is configured;
- schema export is enabled;
- generated schema JSON must be committed after the first verified Android build.

### DataStore

Stores:

- default hourly rate;
- ISO currency code;
- theme mode.

Malformed stored preferences are normalized to safe defaults where possible. I/O errors during DataStore reads fall back to empty preferences; non-I/O failures still surface.

## Composition root

`WorkTimeApplication` owns `AppContainer`. `AppContainer` constructs the singleton Room database plus concrete repository implementations. Composables receive behavior through the ViewModel rather than service-locating persistence.

## Module strategy

Remain single-module (`:app`) until team/build constraints justify splitting. Package boundaries already make future extraction possible:

```text
:core:model
:core:database
:core:datastore
:core:designsystem
:feature:calendar
:feature:settings
```

## Privacy/security posture

- no account/backend in MVP;
- no Internet/dangerous permissions required;
- Android cloud backup disabled for v1;
- no analytics/ad SDK;
- no production logging of work entries, notes, rates or salary totals.

## Architectural fitness rules

- Domain tests run without Android.
- Composables contain no direct persistence calls.
- Domain/data contain no `Float`/`Double` money representation.
- Database schema changes require migration tests.
- No `fallbackToDestructiveMigration` for user work-history data.
- Default-rate changes do not mutate existing records.
- Persistence errors must not discard the user's open draft.
- Currency behavior must remain explicit; never silently introduce FX conversion.
