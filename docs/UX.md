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

The compact phone layout is the primary interaction target, but the application is not orientation-locked. Layout responds to the available app window: day editing, rate changing and export-format selection use modal sheets; Settings and Year Summary are full-screen surfaces; the calendar can expose the monthly report as a supporting pane when enough horizontal space is available.

## Calendar

- Fixed Monday-first 6 × 7 grid; its geometry does not change based on entries.
- Header contains previous month, tappable localized month/year, next month and Settings.
- Tapping month/year opens the month picker.
- Horizontal month navigation uses a real `HorizontalPager`: the neighbouring calendar follows the finger during the drag and snaps with Compose pager physics instead of changing only after release.
- Previous/next arrows drive the same interruptible pager animation programmatically, so gesture and button navigation share one spatial model and repeated taps preserve/re-target the current spring velocity.
- The visible month plus both immediate neighbours are observed together from the repository so a drag does not expose an empty page while Room switches queries.
- Vertical-dominant interaction remains available to the calendar content and does not intentionally navigate months.
- Adjacent-month dates stay visible but muted and inactive.
- Populated cells use a neutral `surfaceContainerHigh`-style treatment. Date is secondary, duration is primary content and amount is a restrained `primary` accent; negative amount uses `error`.
- Selected day uses `primaryContainer`; today uses its own primary outline/date treatment. Selection, today and data emphasis must remain distinguishable.
- Day-cell state colors interpolate over a short transition instead of hard-swapping. Enabled day cells also retain bounded press/ripple feedback so touch-down is acknowledged before the editor sheet appears.
- Grid/divider lines are deliberately quiet so data reads before table chrome.
- In the current month only, when today has no entry, a lightweight `Fill today` TextButton appears below the calendar.
- Compact-height windows may scroll the primary pane to preserve reachability instead of clipping controls.
- Wider windows keep the calendar as the primary pane and show the month report in a supporting pane rather than stretching the compact footer across the whole width.
- Rotation, split-screen and freeform resizing are treated as window-size changes, not as separate device-specific layouts.

## Monthly summary and report

The fixed footer is one compact summary strip containing shifts, worked hours and monthly income. It acts as the entry point to the detailed month report and must remain visually subordinate to the calendar.

- the strip has a very small pressed compression/tonal response while retaining normal Material indication;
- during an upward drag it follows gesture progress with a restrained direct lift/scale response and springs back if the gesture is cancelled or remains below the activation distance;
- its up/down chevron rotates with the report state;
- crossing the actionable upward-drag threshold emits one threshold haptic;
- geometry remains fixed.

The detailed report sheet contains:

- month label;
- primary monthly income;
- shift count and hours;
- pay by hourly rate;
- bonus and penalty when non-zero;
- average shift duration;
- average income per shift;
- a normal navigation row to `Year summary`.

The report is view-only. The handle remains tooltip-free; when it is tappable, touch-down gets a restrained visual tone response. Sheet opening/closing keeps Material drag physics. On a wide supporting-pane layout the same report content is persistent and does not open another sheet.

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
- modal content remains scrollable above the IME when required;
- the persistent editor moves between fixed rows with a non-bouncy spring while preserving the same focusable node and IME session.

### Bonus and penalty semantics

Collapsed Bonus/Penalty rows show an Add affordance, not a chevron: tapping reveals an inline field rather than navigating elsewhere. If a revealed adjustment is still empty when focus moves away, it collapses back to the Add row.

Successful Save gets a confirmation haptic only after repository persistence succeeds. A failed persistence operation gets semantic reject feedback rather than a success-like tap vibration.

## Settings

`SettingsScreen` uses three sections:

- `Calculation`: Default rate, Change rate for period;
- `Appearance`: system/light/dark segmented control;
- `Data`: Export data, Import data.

All interactive rows follow the shared 48 dp row contract; segmented controls and inline fields use 44 dp height; primary sheet actions are at least 52 dp. Normal Material press feedback is retained except where a custom shape-aware press state replaces it.

The shared segmented control uses one moving selected pill rather than unrelated hard-swapped fills. Each touched segment gets a restrained rounded press state. An actual selection change emits one light segment tick; tapping the already selected option does not.

Theme palette changes apply immediately when an option is selected. The segmented indicator may continue its short presentation spring, but it never delays the visible theme change. Persistence is optimistic: the DataStore write remains serialized with other app mutations after the palette changes; a failed write rolls the selection/palette back to repository state, emits reject feedback and surfaces the existing error message. Layout and typography do not animate or reflow during the switch.

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
- A non-empty successful bulk rate change gets one confirmation haptic after persistence succeeds.

## Year summary

The year summary is a full-screen, view-only screen opened from the monthly report.

- horizontal year pager with previous/next arrows;
- swipe and arrow navigation use the same spring stiffness and snap threshold as calendar month paging; arrow navigation is interruptible and preserves/re-targets current velocity just like the calendar;
- yearly income, shifts, hours, average working-month income, average shift and optional bonus/penalty totals;
- month breakdown header is one line: `By month` at the left, `shifts · h` and `income` aligned to their data columns;
- populated months are normal emphasis; missing months are muted with dashes;
- medium/tall windows keep the dense report in the fixed viewport; short windows allow vertical scrolling with compact minimum month-row heights so content remains reachable rather than clipped;
- changing year follows the finger horizontally and retains adjacent-year data during the gesture.

Settings enters from the right with a restrained spatial transition and reverses that direction on dismiss. Year summary appears from below with only a short one-sixteenth-viewport travel plus fade and returns downward, preserving its relationship to the bottom monthly report without reading as a full-height sliding page.

## Export and import

- Export data opens a compact format sheet: JSON backup or CSV spreadsheet.
- JSON contains entries, user-visible settings and the default-rate initialization state so a current-format export/import preserves behavior as well as values.
- Version 1 JSON remains readable; because it did not contain the initialization flag, compatibility import infers that legacy state from the stored default rate and worked entries.
- CSV is export-only.
- Import validates before showing replacement confirmation and writes nothing when validation fails.
- Success/failure feedback uses Snackbar/error surfaces without inserting temporary layout rows; explicit backup failures also use semantic reject haptics.

## Home-screen widget

The optional 4 × 1 widget shows the current month plus shift count, worked hours and income. Tapping the widget body opens WorkTime; tapping the compact `+` opens today's editor directly. Explicit Light/Dark choices follow the app theme preference; System mode keeps the normal `values`/`values-night` resource behavior. Live entry/theme observation runs only while a widget is installed, with system update/date/time/timezone paths retained as fallback refreshes. The widget can resize horizontally.

## Launch

WorkTime uses AndroidX SplashScreen compatibility so Android 12+ and older supported devices share one launch path. The splash background matches the app light/dark surface and exits with a short fade into the first real frame; it must never be held for branding delay.

## Haptics

Haptics are sparse and semantic. WorkTime does not vibrate on every tap.

- segmented selection: one light `SegmentTick` only when selection actually changes;
- direct month/year navigation: one light `SegmentTick` when a swipe or arrow request settles on a different page; restored/external pager synchronization stays silent;
- monthly-summary upward drag: one threshold activation when the gesture becomes actionable;
- Save/Delete/non-empty bulk rate update: `Confirm` only after the repository operation succeeds;
- explicit calendar/settings-persistence/backup failure: one `Reject`;
- ordinary navigation taps, day taps, text-field focus and passive scrolling add no extra vibration.

## Visual system

The canonical tokens and component semantics live in [`UI_SYSTEM.md`](UI_SYSTEM.md). In particular:

- navigation uses chevrons only when an action actually navigates or chooses;
- inline reveal uses Add rather than navigation affordance;
- flat rows are preferred over decorative cards;
- primary/secondary/error colors communicate hierarchy and state, not decoration;
- dark theme is a readable counterpart of the light theme, while the light theme remains the primary visual target;
- motion communicates continuity and feedback through short state transitions, non-bouncy springs, gesture-following pager motion and directional hierarchy changes; it must not become looping decoration, bounce-heavy feedback or delayed actions.
