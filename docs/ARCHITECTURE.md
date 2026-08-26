# Architecture

## Layers

```text
Compose UI -> CalendarViewModel -> domain repository interfaces -> Room / DataStore
                      `-------> domain calculations
```

- `domain` owns work-entry invariants and exact calculations.
- `data` implements Room and DataStore repositories.
- `ui` renders immutable state and sends user intents.
- The app composition root wires implementations to interfaces.

## State

`CalendarViewModel` combines the requested month, the matching Room month snapshot, preferences, selected date, settings visibility and recoverable operation error.

Month and entries are emitted together to avoid showing one month title with another month's rows. `isReady` blocks editing until Room/DataStore have emitted real state.

## Persistence

### Room

One row per date:

```text
dateEpochDay, workedMinutes, hourlyRateMicros, bonusMicros, penaltyMicros, note
```

The legacy `note` column is retained to avoid a destructive schema change. The compact UI does not expose notes and preserves an existing stored value when editing.

### DataStore

Current preferences:

- default hourly rate micros;
- theme mode;
- whether the first-entry default-rate adoption has already been decided.

An old stored currency key may remain on upgraded installations but is not read or written.

## Amount model

Amounts are stored as `Long` micros. The UI formats them with the fixed `₽`/`₽/h` presentation strings; there is no selectable currency or exchange-rate model.

```text
ratePay = roundHalfUp(workedMinutes x hourlyRateMicros / 60)
entryTotal = ratePay + bonus - penalty
monthTotal = sum(entryTotal)
```

Parsing rejects malformed/exponent input. `MoneyLimits` bounds user-entered components; checked integer arithmetic protects overflow.

## UI surfaces

- `CalendarScreen`: fixed calendar, fixed summary, standard draggable report sheet.
- `DayEditorSheet`: draft validation and create/edit/delete actions.
- `SettingsScreen`: default rate, theme and data operations.
- `YearSummaryScreen`: view-only yearly statistics.
- `MoneyFormatting` and `DurationFormatting`: presentation-boundary formatting only.

Write failures keep the relevant editor/settings surface open where applicable and expose a generic localized error without logging personal values. Import is validated before confirmation and compensated across Room/DataStore: a failure after replacement attempts to restore both snapshots; rollback failure is reported separately.
