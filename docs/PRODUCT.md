# Product specification

## Product statement

WorkTime is a personal, phone-first Android timesheet that records actual worked time by date and immediately shows expected income for the selected month.

It is a salary calendar, not a project tracker, shift planner, timer, HR system or payroll suite. The compact phone layout is the primary product surface, but the application remains resizable and adapts to the available app window instead of locking orientation or assuming one device resolution.

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
- Previous/next arrows, horizontal pager gesture and a month-picker dialog provide navigation.
- The calendar follows the finger while paging; arrows animate the same pager and adjacent month data is preloaded from the repository window.
- Adjacent-month dates remain visible but faint and inactive.
- Filled cells show date, worked duration and daily income. Duration is the strongest datum; income is a restrained accent.
- Filled cells use a neutral elevated surface rather than a second decorative primary fill.
- Selected day and today have distinct states; their visual state changes interpolate briefly rather than flashing.
- New or changed saved data gets a restrained content transition in the affected cell.
- Grid geometry does not depend on entries or report state.
- In the current month, `Fill today` appears only while today's entry is missing and uses a short enter/exit transition.
- Layout is derived from the available app window. Compact windows keep the summary/report below the calendar; wider windows can expose the month report as a supporting pane. Rotation, split-screen and resizing must not require an orientation lock.

## Day editor

- Modal bottom sheet with one persistent numeric editor/input session shared by duration, rate, bonus and penalty logical fields.
- The persistent editor moves between its fixed rows with a non-bouncy spring while keeping the same focusable node and IME session.
- Duration accepts compact hour/minute input and starts empty with a `00:00` hint for a new day.
- Hourly rate is a separate row.
- Hidden bonus and penalty rows use an Add affordance; Add/value states fade through rather than hard-swapping.
- Leaving an empty optional adjustment field collapses it back to the Add row.
- Calculation shows pay by rate, optional adjustments and total.
- Save is the primary action; existing entries can be deleted.
- Invalid numeric values use outline-only error treatment; helper text is intentionally absent.
- Persistence errors do not resize the sheet.

## Monthly information

The calendar footer is one compact summary strip containing shifts, worked hours and monthly income. Tapping/dragging it opens the detailed month report. Summary values fade through briefly when they change, the strip has restrained pressed feedback, and its chevron follows the open/closed report state without changing geometry.

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

Year summary enters upward from the bottom report and exits downward. Moving between years uses a restrained lateral transition in the direction of time while the previous year remains visible until the next dataset is ready. Settings remains a horizontal hierarchy transition.

## Settings

Settings are grouped into `Calculation`, `Appearance` and `Data`:

- default hourly rate, edited inline;
- `Change rate for period`;
- system/light/dark theme segmented control;
- JSON backup export, CSV spreadsheet export and JSON import.

The shared segmented control uses one animated selected pill and emits one light tactile tick only when selection actually changes. Theme-mode changes interpolate visible Material color roles rather than flashing between complete palettes. The label is explicitly `Default rate`: it is not the rate of the currently selected day.

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
- Backup version 2 preserves both visible preferences and the hidden default-rate initialization state, so export/import is a behavioral round trip even when the stored default rate is zero.
- Version 1 JSON backups remain import-compatible; legacy initialization state is inferred from the stored default and worked entries because the old format did not contain the flag explicitly.
- CSV is spreadsheet-oriented and export-only.
- Import validates the complete file before replacing current entries/settings and uses compensation snapshots if the multi-store replacement fails.
- Core operation remains fully local; export/import uses the system document picker.

## Home-screen widget

An optional 4 × 1 widget shows the current month, shift count, worked hours and income. Tapping the body opens WorkTime; the compact `+` action opens today's day editor directly. Explicit Light/Dark choices follow the app preference, while System mode remains resource-driven and follows the device configuration. Live Room/DataStore observation is kept only while at least one widget is installed; system update/date invalidation paths remain as fallback refresh mechanisms. The widget supports horizontal resizing.

## Launch behavior

The app uses the AndroidX SplashScreen compatibility API. The splash surface follows the current light/dark base surface and exits quickly into real content; there is no artificial branding delay.

## Haptic behavior

Haptics are deliberately sparse: a user-driven month pager snap and actual segmented selection get a light tick; the summary upward gesture gets one threshold cue; Save/Delete/non-empty bulk rate changes get confirmation only after persistence succeeds. Ordinary navigation taps, arrows, day selection and text-field focus do not add vibration.

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
- a separate device-model-specific or landscape-only product mode;
- looping/decorative animation or motion that delays user actions.

## Release criteria

- `./scripts/verify.sh` completes on the release candidate when runner/toolchain access is available.
- Core create/edit/delete/relaunch, bulk-rate, import/export and widget paths pass on physical hardware.
- Editor IME transitions and modal-sheet gestures remain stable.
- Calendar, reports, settings and year summary remain usable without clipping across the supported compact/expanded window states, supported font scales and locales.
- Rotation/window resize does not reset persisted feature state or expose an unsupported fixed-resolution layout.
- Motion and haptics remain smooth/restrained on supported devices and do not change interaction timing or data semantics.
- No known data-loss or calculation defect remains.
