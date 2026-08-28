# Product specification

## Product statement

WorkTime is a personal portrait-only Android timesheet that records actual worked time by date and immediately shows expected income for the selected month.

It is a salary calendar, not a project tracker, shift planner, timer, HR system or payroll suite.

## Primary flow

1. Open the required month.
2. Tap a current-month date, or use `Fill today` when it is relevant.
3. Enter duration and hourly rate.
4. Optionally add a bonus and/or penalty.
5. Review the calculation and save.
6. Read the compact monthly summary or open the detailed month report.
7. Open the yearly summary from the month report when a broader view is needed.

## Calendar

- Monday-first fixed 6 × 7 layout.
- Previous/next arrows, horizontal swipe and a month-picker dialog provide navigation.
- Adjacent-month dates remain visible but faint and inactive.
- Filled cells show date, worked duration and daily income. Duration is the strongest datum; income is a restrained accent.
- Filled cells use a neutral elevated surface rather than a second decorative primary fill.
- Selected day and today have distinct states.
- Grid geometry does not depend on entries or report state.
- In the current month, `Fill today` appears only while today's entry is missing.
- Application orientation remains portrait-only.

## Day editor

- Modal bottom sheet with one persistent numeric editor/input session shared by duration, rate, bonus and penalty logical fields.
- Duration accepts compact hour/minute input and starts empty with a `00:00` hint for a new day.
- Hourly rate is a separate row.
- Hidden bonus and penalty rows use an Add affordance; they do not use a navigation chevron because they expand inline.
- Leaving an empty optional adjustment field collapses it back to the Add row.
- Calculation shows pay by rate, optional adjustments and total.
- Save is the primary action; existing entries can be deleted.
- Invalid numeric values use outline-only error treatment; helper text is intentionally absent.
- Persistence errors do not resize the sheet.

## Monthly information

The calendar footer is one compact summary strip containing shifts, worked hours and monthly income. Tapping/dragging it opens the detailed month report.

The month report is view-only and shows:

- month and primary total income;
- shift count and worked hours;
- pay by hourly rate;
- bonus/penalty when non-zero;
- average shift duration;
- average income per shift;
- navigation to the yearly summary.

## Year summary

The yearly summary is a full-screen, view-only surface opened from the monthly report.

It shows total yearly income, work days, hours, averages and bonus/penalty totals when applicable. A compact month breakdown keeps the column labels (`shifts · h`, `income`) on the same header line as `By month`. Populated months are emphasized; missing months are muted. Empty years stay compact rather than stretching meaningless rows across the screen.

## Settings

Settings are grouped into `Calculation`, `Appearance` and `Data`:

- default hourly rate, edited inline;
- `Change rate for period`;
- system/light/dark theme segmented control;
- JSON backup export, CSV spreadsheet export and JSON import.

The label is explicitly `Default rate`: it is not the rate of the currently selected day.

### Default-rate behavior

- If the default rate has never been initialized, the hourly rate of the first saved worked entry can initialize it.
- Once initialized, entering a different rate for an individual day affects only that entry and does not overwrite the default.
- Manually changing the default in Settings marks it initialized.

### Change rate for period

- Applies a new hourly rate to every entry inside an inclusive current-month or custom date range.
- Custom end dates earlier than the selected start are disabled by the Material date picker; moving the start beyond an existing end clears the invalid end.
- Confirmation states that entries in the period change while the default rate stays unchanged.
- The operation does not alter durations, bonuses or penalties.
- Successful bulk changes can be undone from the root Snackbar.

## Data export and import

- JSON export is the complete backup format and can be imported back.
- CSV is spreadsheet-oriented and export-only.
- Import validates the complete file before replacing current entries/settings and uses compensation snapshots if the multi-store replacement fails.
- Core operation remains fully local; export/import uses the system document picker.

## Home-screen widget

An optional 3 × 2 widget mirrors the month summary and opens WorkTime on tap. It follows the current light/dark presentation and refreshes from entry changes plus system update/date invalidation paths.

## Business rules

- One aggregate record per date.
- Duration range is `0..1440` minutes; `24:00` is valid, `24:01` is not.
- Worked time requires a positive hourly rate.
- Bonus/penalty-only records are valid and do not increase work-day count.
- Historical records retain their saved hourly rate unless explicitly changed by the bulk-rate operation.
- Bulk rate change never modifies the default rate.
- Entry deletion and bulk rate changes can be undone through the most recent in-memory undo snapshot; undo does not survive process death.
- The UI uses fixed `₽` labels; there is no currency selector or exchange-rate behavior.
- User-entered amounts accept at most two fractional digits.
- Domain/data calculations store integer micros and do not persist binary floating-point values.

## Non-goals

- registration or cloud sync;
- time clock/background timer;
- projects, clients or invoices;
- scheduled shifts/overtime rules;
- taxes, exchange rates or multi-currency accounting;
- notes or quick-duration templates;
- validation helper text;
- landscape layout support.

## Release criteria

- `./scripts/verify.sh` completes on the release candidate when runner/toolchain access is available.
- Core create/edit/delete/relaunch, bulk-rate, import/export and widget paths pass on physical hardware.
- Editor IME transitions and modal-sheet gestures remain stable.
- Calendar, reports, settings and year summary do not clip in supported portrait font scales/locales.
- No known data-loss or calculation defect remains.
