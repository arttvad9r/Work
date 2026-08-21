# Product specification

## Product statement

WorkTime is a personal Android timesheet that records actual worked time by date and immediately shows an expected monthly total.

It is a modern salary calendar, not a project tracker, shift planner, timer, HR system or payroll suite.

## Primary flow

1. Open the required month.
2. Tap a current-month date.
3. Enter duration and hourly rate.
4. Optionally add a bonus and/or penalty.
5. Review the calculation and save.
6. Read the compact month summary or drag up the detailed report.

## MVP requirements

### Calendar

- Monday-first 6 x 7 layout with fixed geometry.
- Previous/next month navigation.
- Adjacent-month dates remain visible but faint and inactive.
- Filled cells show a centered compact duration and neutral daily amount.
- Bonus and penalty have distinct centered markers.

### Day editor

- One duration field accepting `H`, `HH`, `H:MM` or `HH:MM`.
- Hourly rate beside duration on the same row.
- Bonus is always above penalty when expanded.
- Live calculation with `At hourly rate`, optional adjustments and total.
- Save/edit/delete with delete confirmation and recoverable write errors.
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

### Settings

- default hourly rate;
- system, light or dark theme.

## Business rules

- One aggregate record per date.
- Duration range is `0..1440` minutes; `24:00` is valid, `24:01` is not.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only records are valid and do not increase work-day count.
- Historical records retain their saved hourly rate.
- Numeric amounts are neutral values; there is no currency setting or exchange-rate behavior.
- Optional fractions are preserved up to six decimal places and omitted when zero.

## Non-goals

- registration or cloud sync;
- time clock and background timer;
- projects, clients or invoices;
- scheduled shifts or overtime rules;
- taxes, exchange rates or multi-currency accounting;
- notes or quick-duration templates;
- report export in the current release.

## Release criteria

- All verification commands pass.
- Core create/edit/delete/relaunch flows pass on supported devices.
- Calendar, fixed summary and report sheet do not clip at supported font scales.
- No known data-loss or calculation defect remains.
