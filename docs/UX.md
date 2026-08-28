# UX specification

## Screen hierarchy

```text
Calendar
|- tap day / Fill today -> Day editor sheet
|- tap settings -> full-screen SettingsScreen
|  |- change rate for period -> ChangeRateSheet
|  |- export data -> ExportFormatSheet -> system document picker
|  `- import data -> system document picker -> confirmation
`- tap/drag monthly summary -> Monthly report sheet
   `- Year summary -> full-screen YearSummaryScreen
```

The application is portrait-only. Day editing, rate changing and export-format selection use modal sheets. Settings and year summary are full-screen surfaces.

## Calendar

- Fixed Monday-first 6 × 7 grid; no vertical scrolling and no geometry changes based on entries.
- Header contains previous month, tappable localized month/year, next month and Settings.
- Tapping month/year opens the month picker. Horizontal swipe switches months; vertical-dominant drags do not.
- Adjacent-month dates stay visible but muted and inactive.
- Populated cells use a neutral `surfaceContainerHigh`-style treatment. Date is secondary, duration is primary content and amount is a restrained `primary` accent; negative amount uses `error`.
- Selected day uses `primaryContainer`; today uses its own primary outline/date treatment. Selection, today and data emphasis must remain distinguishable.
- Grid/divider lines are deliberately quiet so data reads before table chrome.
- In the current month only, when today has no entry, a lightweight `Fill today` TextButton appears below the calendar. It disappears after today's entry is saved and never appears for other months.

## Monthly summary and report

The fixed footer is one compact summary strip containing shifts, worked hours and monthly income. It acts as the entry point to the detailed month report and must remain visually subordinate to the calendar.

The detailed report sheet contains:

- month label;
- primary monthly income;
- shift count and hours;
- pay by hourly rate;
- bonus and penalty when non-zero;
- average shift duration;
- average income per shift;
- a normal navigation row to `Year summary`.

The report is view-only. The handle remains tooltip-free and tap/drag behavior must keep stable anchors.

## Day editor

The editor uses one compact modal sheet:

1. localized date title;
2. Duration row;
3. Hourly rate row;
4. optional Bonus row;
5. optional Penalty row;
6. calculation block;
7. primary Save action;
8. destructive Delete action for existing entries.

### Numeric editing

All logical numeric fields share one persistent state-based editable node/input session. The editor is repositioned between fixed trailing slots instead of creating a new TextField for each logical value. This is intentional: physical-device testing showed that normal focus handoff between separate fields could rebuild the OEM numeric IME.

- compact editable slot: shared 120 × 44 dp contract;
- active text uses the same `bodyLarge`/Medium hierarchy as other compact fields;
- new duration starts empty with `00:00` hint;
- focus changes do not rewrite logical values;
- invalid values use error outline only;
- modal content remains scrollable above the IME when required.

### Bonus and penalty semantics

Collapsed Bonus/Penalty rows show an Add affordance, not a chevron: tapping reveals an inline field rather than navigating elsewhere. If a revealed adjustment is still empty when focus moves away, it collapses back to the Add row.

## Settings

`SettingsScreen` uses three sections:

- `Calculation`: Default rate, Change rate for period;
- `Appearance`: system/light/dark segmented control;
- `Data`: Export data, Import data.

All interactive rows follow the shared 48 dp row contract; segmented controls and inline fields use 44 dp height; primary sheet actions are at least 52 dp. Normal Material press feedback is retained.

Default rate edits inline and autosaves valid values. It is semantically separate from an individual entry rate. The first saved worked entry may initialize an uninitialized default; later per-day rates do not overwrite an initialized default.

The settings screen uses IME-aware scrolling so lower actions remain reachable while the default rate is being edited.

## Change rate for period

- Opens as a modal sheet from Settings.
- Period segmented control offers Current month and Custom period.
- Current month shows the month without empty date rows.
- Custom period uses compact Start date and End date navigation rows. Empty date values show no placeholder dash; the chevron already communicates selection.
- Start/end rows are grouped tightly without unnecessary vertical gaps.
- End dates before the selected start are disabled through Material 3 `SelectableDates`.
- If a new start would make the existing end invalid, the end is cleared.
- A valid rate and complete period are required before the primary action enables.
- Confirmation explicitly states that entries in the period change and the Default rate remains unchanged.
- Success closes the sheet and shows `Period rate changed`; Undo restores original per-entry rates.

## Year summary

The year summary is a full-screen, view-only screen opened from the monthly report.

- compact year pager with previous/next arrows;
- yearly income, work days, hours, average working-month income, average shift and optional bonus/penalty totals;
- month breakdown header is one line: `By month` at the left, `shifts · h` and `income` aligned to their data columns;
- populated months are normal emphasis; missing months are muted with dashes;
- an entirely empty year uses compact rows instead of stretching empty content across available height.

## Export and import

- Export data opens a compact format sheet: JSON backup or CSV spreadsheet.
- JSON contains entries plus relevant settings and can be imported back.
- CSV is export-only.
- Import validates before showing replacement confirmation and writes nothing when validation fails.
- Success/failure feedback uses Snackbar/error surfaces without inserting temporary layout rows.

## Home-screen widget

The optional 3 × 2 widget mirrors the current month summary and opens WorkTime on tap. It follows the app's light/dark presentation and refreshes from entry changes plus system date/time/timezone update paths.

## Visual system

The canonical tokens and component semantics live in [`UI_SYSTEM.md`](UI_SYSTEM.md). In particular:

- navigation uses chevrons only when an action actually navigates or chooses;
- inline reveal uses Add rather than navigation affordance;
- flat rows are preferred over decorative cards;
- primary/secondary/error colors communicate hierarchy and state, not decoration;
- dark theme is a readable counterpart of the light theme, while the light theme remains the primary visual target;
- motion is limited to short feedback/platform sheet motion; data layout and row geometry do not animate.
