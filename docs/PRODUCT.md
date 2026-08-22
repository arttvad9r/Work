# Product specification

## Product statement

WorkTime is a personal portrait-only Android timesheet that records actual worked time by date and immediately shows an expected monthly total.

It is a modern salary calendar, not a project tracker, shift planner, timer, HR system or payroll suite.

## Primary flow

1. Open the required month.
2. Tap a current-month date.
3. Enter duration and hourly rate.
4. Optionally add a bonus and/or penalty.
5. Review the calculation and save.
6. Read the compact month summary or tap/drag up the detailed report.

## MVP requirements

### Calendar

- Monday-first 6 x 7 layout with fixed geometry.
- Previous/next month navigation updates the title and date grid immediately without crossfade or old/new month flashing.
- Adjacent-month dates remain visible but faint and inactive.
- Filled cells show a centered compact duration and a rounded whole-number daily amount with no fractional digits.
- Bonus and penalty have distinct centered markers.
- Application orientation remains locked to portrait.

### Day editor

- One duration field accepting `H`, `HH`, `H:MM` or `HH:MM`.
- A new day may display duration `0` before focus; focusing the field removes that zero before entry, and leading zeroes are normalized defensively.
- Hourly rate beside duration on the same row.
- Bonus is always above penalty when expanded.
- Numeric fields use state-based Compose text input and focus moves directly between visible fields without intentionally closing/reopening the IME.
- Invalid numeric input is indicated by the Material error outline only; no validation helper text is shown.
- Live calculation with `At hourly rate`, optional adjustments and total.
- Save/edit/delete with delete confirmation and recoverable write errors shown without resizing the sheet.
- No notes, quick presets or currency controls.

### Monthly information

The fixed card always shows:

- work days;
- hours worked;
- monthly income.

The draggable report shows:

- work days;
- hours worked;
- bonus only when non-zero;
- penalty only when non-zero;
- total.

The report opens by tap or drag. Holding its handle must not show Material's drag-handle tooltip.

### Settings

- default hourly rate;
- system, light or dark theme;
- outline-only validation for an invalid rate;
- persistence errors shown without changing sheet geometry.

## Business rules

- One aggregate record per date.
- Duration range is `0..1440` minutes; `24:00` is valid, `24:01` is not.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only records are valid and do not increase work-day count.
- Historical records retain their saved hourly rate.
- Numeric amounts are neutral values; there is no currency setting or exchange-rate behavior.
- User-entered amounts accept at most two fractional digits.
- Normal amount displays use at most two fractional digits and omit a zero fractional part; compact calendar day cells intentionally show only a rounded whole number.
- Domain/data calculations store integer micros (six decimal places of internal precision) and never use persisted binary floating point.

## Non-goals

- registration or cloud sync;
- time clock and background timer;
- projects, clients or invoices;
- scheduled shifts or overtime rules;
- taxes, exchange rates or multi-currency accounting;
- notes or quick-duration templates;
- validation helper text for numeric fields;
- landscape layout support;
- report export in the current release.

## Release criteria

- All local verification commands pass on the current head.
- Core create/edit/delete/relaunch flows pass on supported physical hardware.
- Report tap/drag/long-press and editor IME transitions pass the focused device checklist.
- Calendar month switching and compact whole-number daily totals pass the focused device checklist.
- Calendar, fixed summary and report sheet do not clip at supported portrait font scales.
- No known data-loss or calculation defect remains.
