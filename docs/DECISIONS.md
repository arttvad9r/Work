# Architecture and product decisions

## Active decisions

### Calendar-first navigation

The month calendar is the primary screen. A date opens the editor directly; there is no dashboard or bottom navigation.

### Fixed calendar geometry

The calendar uses six rows and consumes a stable portion of the viewport. Summary/report content adapts below it and never moves the grid.

### Portrait-first, not portrait-locked

The compact portrait phone is the primary visual and physical-QA target, but WorkTime does not depend on `screenOrientation` or runtime orientation locks. Rotation, window resizing and large-screen layouts must remain usable and preserve valid app state. This is adaptive support required by the Android platform; it does not introduce a separate landscape-only product mode.

### Standard draggable report sheet

The compact summary is permanent. A separate `BottomSheetScaffold` sheet exposes the detailed month report through one native drag/tap handle. On a wide adaptive layout the same report content is shown as a supporting pane rather than as a redundant sheet.

### Minimal day entry

The form contains duration, hourly rate, optional bonus and optional penalty. Notes and quick presets were intentionally removed.

### No currency concept

Amounts are neutral numeric values. Currency preference, symbols and exchange-rate copy were removed. This supersedes the former global ISO-currency/no-FX decision.

### Historical rate snapshots

The default rate only initializes a new entry. Each saved entry stores its own rate so settings changes do not rewrite history.

### Session-scoped Undo

Undo snapshots for delete and bulk-rate operations live only in the active `CalendarViewModel`. They are intentionally not persisted to Room, DataStore or saved instance state. A process death therefore clears the available Undo action while already committed repository data remains authoritative. Undo is a short-lived convenience action, not durable user data.

### Integer micros

Domain/data amounts use `Long` micros and checked arithmetic. Optional fractional digits are a presentation concern.

### Local-first privacy

No account, analytics, advertising or network permission is needed for the core product. Backup/transfer exclusion remains explicit.

### Schema compatibility over cleanup

The unused Room `note` column remains in schema v1 until a deliberate migration is designed. UI scope and storage layout are allowed to differ temporarily.
