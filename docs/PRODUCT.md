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
- Horizontal swipes across the calendar also switch months; vertical drags never do.
- Adjacent-month dates remain visible but faint and inactive.
- The calendar uses a minimal horizontal safety margin so the seven columns have as much usable width as practical on a portrait phone.
- Filled cells show a centered compact duration and a rounded whole-number daily amount with no fractional digits.
- Bonus and penalty have distinct centered markers.
- An empty month offers a prompt that opens today's editor directly.
- Application orientation remains locked to portrait.

### Day editor

- One duration field accepting `H`, `HH`, `H:MM` or `HH:MM`.
- A new day starts with an empty duration editor value and the `00:00` hint; leading zeroes are normalized defensively so typing `12` cannot become `01:2`.
- Hourly rate beside duration on the same row.
- Bonus is always above penalty when expanded.
- Numeric fields use state-based Compose text input; focus changes do not mutate field text.
- Numeric focus moves directly between visible fields without intentionally closing/reopening the IME, and transient IME insets do not reposition the whole editor sheet.
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

The sheet is grouped into `Calculation`, `Appearance` and `Data and operations`:

- default hourly rate (Calculation);
- system, light or dark theme (Appearance);
- change rate for period (Data and operations);
- outline-only validation for an invalid rate;
- persistence errors shown without changing sheet geometry.

`Change rate for period` rewrites the hourly rate of every entry inside an inclusive date range. The range is the visible month or a custom start/end period picked with native date dialogs, the new value is confirmed in a dialog before anything is written, and only each record's hourly rate changes — durations, bonuses and penalties and the default rate stay untouched. Success shows a Snackbar with Undo; Undo restores every original per-record rate from before the operation.

## Business rules

- One aggregate record per date.
- Duration range is `0..1440` minutes; `24:00` is valid, `24:01` is not.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only records are valid and do not increase work-day count.
- Historical records retain their saved hourly rate.
- A bulk rate change affects every record in the inclusive period regardless of each record's current rate, and updates only the stored hourly rate; the default rate is never modified.
- Entry deletion and bulk rate changes can be undone through the success Snackbar. Undo covers only the most recent such operation, lives in memory for the process lifetime and does not survive process death.
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
- landscape layout support.

## Release criteria

- All local verification commands pass on the current head.
- Core create/edit/delete/relaunch flows pass on supported physical hardware.
- Report tap/drag/long-press and editor IME transitions pass the focused device checklist.
- Calendar month switching, wider grid geometry and compact whole-number daily totals pass the focused device checklist.
- Rate-change-for-period with confirmation, Snackbar Undo restore and horizontal month swipe pass the focused device checklist.
- Calendar, fixed summary and report sheet do not clip at supported portrait font scales.
- No known data-loss or calculation defect remains.
