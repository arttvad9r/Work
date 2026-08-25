# UX specification

## Screen hierarchy

```text
Calendar
|- tap day -> Day editor sheet
|- tap settings -> Settings sheet
`- drag/tap bottom handle -> Monthly report sheet
```

Only one modal editor/settings surface may be open at a time. The monthly report belongs to the calendar scaffold and does not replace the fixed summary card. The application is portrait-only by product decision.

## Calendar screen

- Header: previous month, localized month/year, next month, settings.
- The calendar uses a fixed compact height above the fixed summary and report handle; no additional bottom padding may compete with the scaffold peek area.
- The calendar card keeps only a 1 dp horizontal safety margin. Top-bar controls keep 2 dp and the fixed summary keeps 4 dp. Individual day cells use a 0.25 dp inset on each side, yielding an approximately 0.5 dp visual gap between neighbors.
- It never scrolls vertically and its position does not depend on entries or report content.
- The grid always has six rows; adjacent-month dates are faint and inactive.
- Current-month cells remain large enough for date, duration and amount.
- Day numbers use bold weight consistently, regardless of whether the day has an entry.
- Day-cell geometry follows the compact reference layout: date is anchored near the top-right corner with a small right inset, daily income near the bottom-left corner with a matching left inset, and worked duration is centered geometrically with a larger `titleMedium` treatment.
- Bonus/penalty markers stay in the free top-left corner so they never overlap the date.
- Daily amounts inside calendar cells are rounded to a whole number with no fractional digits or grouping separators; full calculation precision is retained internally and richer amount displays elsewhere may show fractions.
- Previous/next month navigation updates the requested month title and date grid immediately. The calendar does not crossfade the old and new month and does not animate day-cell colors across month boundaries.
- Tapping the month/year title opens a month picker dialog: a year row with previous/next arrows and a 3x4 grid of localized short month names. Selecting a month jumps straight to it and collapses the report behind the grid, like the arrows do.
- Worked days fill with `primaryContainer` and render all their content in `onPrimaryContainer`; free days stay on the white surface so a fully booked month keeps a clear figure-ground split. The selected day steps up to the full `primary` surface with `onPrimary` content, which stays distinct from the worked-day fill.
- If Room has not emitted the requested month's rows yet, rows from the previous month must never be displayed under the new title/grid.
- Dragging horizontally on the calendar card switches months once the horizontal drag passes a 48 dp threshold; vertical-dominant drags are ignored and never trigger a month switch.
- Days with an entry show a small circular check glyph in the bottom-right corner of the cell, opposite the bonus/penalty markers in the top-left.
- A month with no entries shows a compact prompt card between the grid and the summary (`No entries this month`) with an `Open today` action that opens today's day editor.

## Fixed monthly summary

- Constant height and bottom-anchored position immediately above the report handle.
- Three equal label-value rows: shifts, worked time, monthly income.
- The card must not compete visually with the calendar.
- A single compact handle is located below this card as the raised peek of the report sheet and remains above system navigation.

## Monthly report sheet

- In the collapsed state only the handle is visible; the report surface, text and shadow remain hidden.
- The complete report remains composed/measured in both states so the sheet height and swipe anchors do not change during a gesture.
- Opens by tapping the handle or dragging upward.
- Collapses by downward drag or a second handle tap. When month navigation, day editing or settings is opened, the destination appears immediately while the report collapses behind it without delaying the next surface.
- The visual handle is rendered inside sheet content rather than through Material 3's `sheetDragHandle` slot because that slot adds a long-press tooltip. Holding the handle must never show `Drag handle` / `Маркер перемещения`.
- Handle tap feedback is intentionally invisible; no full-width flash or tooltip is shown.
- Contains one heading with a trailing colon, work days, hours worked, optional bonus, optional penalty, divider and total.
- The total value is bold; error color is applied to it only when the total is negative and never otherwise.
- Does not duplicate the heading as a second total and does not contain report/export buttons.

## Day editor

Normal closed-keyboard state should fit as one compact sheet.

1. Localized date.
2. Duration row, followed by a separate hourly-rate row.
3. Bonus/penalty controls.
4. Calculation card.
5. Save.
6. Delete for an existing entry.

The duration field is labeled `Время` and shows a faint `00:00` format hint while empty. A new day starts with an empty duration field rather than a literal `0`, so typing `12` yields `12` immediately. Duration sanitization also removes accidental leading zeroes defensively, so input such as `012` cannot become `01:2`. Sequential input preserves valid two-digit hours: `1` -> `12` -> `12:0` -> `12:00`. Compact input remains supported: `530` resolves to `5:30`, and `1530` resolves to `15:30`.

Zero-valued numeric editor fields are represented as empty editor text and parsed as zero. Focus changes must not clear or rewrite field text. Numeric validation is intentionally minimal: invalid duration/rate/bonus/penalty values are indicated by the field's red error outline only. Validation helper text is not shown and must not change sheet height.

All four logical numeric fields — `Время`, `Ставка за час`, `Премия`, `Штраф` — share one persistent state-based editable `OutlinedTextField`, one `TextFieldState` and one platform text-input session. The same editable node is repositioned between the logical slots; inactive visible slots are read-only Material shells. This architecture is required because physical-device ADB traces showed that a normal focus handoff between separate editable TextFields caused client-side IME hide, a temporary empty `EditorInfo`, `restartInput`, and a visible Gboard hide/show cycle.

Every numeric Material field uses `TextFieldLabelPosition.Attached(alwaysMinimize = true)`. This is the Material3 1.4 API for forcing the attached label to remain minimized, so `Время`, `Ставка за час`, `Премия` and `Штраф` stay on the outline even when empty and unfocused. The duration placeholder `00:00` is a separate centered hint inside the field and must never replace or displace the `Время` label.

Only the active logical slot renders the persistent editable field. Every other visible numeric slot renders a passive read-only Material field with invisible click indication, so there is no duplicate label/value and no gray rectangular ripple. If the persistent editor is not currently focused, the first tap on any passive slot both activates that logical field and focuses/shows the numeric keyboard. If it is already focused, switching logical fields does not issue another focus request and does not replace the input session.

Input sanitization remains synchronous through `InputTransformation`. Logical duration/rate/bonus/penalty values are copied to and from the persistent editor without replacing its focus node. All logical fields use the same decimal `KeyboardOptions` and `ImeAction.Next`; IME Next advances among visible logical fields without an editable-field focus handoff.

Bonus is always the first adjustment slot and penalty the second. With neither expanded, both buttons share a row. Once either adjustment is expanded, bonus occupies the first full-width slot and penalty the second; an unexpanded adjustment remains a full-width button in its slot. Expanding bonus or penalty activates the same persistent editor instead of creating/focusing another TextField.

The modal editor uses normal `ModalBottomSheet` window-inset handling so the sheet lifts above the software keyboard instead of remaining underneath it. The content stays vertically scrollable when required. Repeated switching among all currently visible numeric fields must keep Gboard continuously presented and the sheet stationary apart from the intentional layout expansion when bonus/penalty controls are first revealed.

The calculation card uses `At hourly rate` / `По ставке`, then optional bonus/penalty rows, then total. Before any value is entered it shows only the `Total` / `Итого` label; zero adjustment rows are omitted.

Save/delete persistence failures keep the draft open and are shown as transient localized Snackbar feedback layered over the sheet. Error feedback must not insert/remove layout rows or resize the sheet.

## Amount formatting

- Input accepts at most two fractional digits.
- Normal amount display rounds to at most two fractional digits and omits a zero fractional part.
- Calendar-cell daily totals are a deliberate exception: they are rounded to a whole number and omit grouping separators to fit compact cells.
- Calculations keep deterministic micros precision internally; the UI precision limit applies only to entry and presentation.

## Settings

- Compact one-screen sheet organized into four titled groups: `Calculation`, `Statistics`, `Appearance`, `Data and operations`.
- All rows share one recipe: ~52 dp touch height, label on the left, value or control on the right.
- `Calculation`: hourly-rate row with a compact 120x40 dp pill input (`CompactMoneyField`), followed by the `Change rate for period` action row — every rate-related action lives in one group.
- Initial zero is selected on focus instead of being replaced by an empty value.
- Invalid input uses red outline only; no helper text is inserted below the field.
- `Appearance`: theme chips fill the section directly; the redundant inner `Theme` caption is not used.
- Selecting light or dark immediately previews the theme.
- Dismissing settings without saving restores the persisted theme; the selected theme is persisted only after pressing Save.
- `Data and operations`: contains `Export data` and `Import data` actions. `Export data` first asks for the format in a dialog — `JSON` (backup that can be imported back) or `CSV` (spreadsheet).
- The sheet relies on the modal window/inset handling without an additional `imePadding` layer and scrolls vertically when content or keyboard height requires it.
- Persistence failure is shown with an overlay Snackbar and does not resize the sheet.

## Change rate for period

- Opens from the settings `Calculation` group as its own modal sheet.
- Period choices: `Current month` pre-fills start/end from the visible month; `Custom period` exposes start/end date fields opened through native Material date picker dialogs.
- One rate input drives the operation; invalid values use the red outline only.
- `Change rate` asks for confirmation first: an alert dialog states that every entry in the selected period will be updated and that the default rate stays unchanged.
- On success the sheet closes and a root Snackbar confirms `Rate changed`; a period containing no entries is a silent no-op (the sheet still closes) with nothing to undo.
- Failure keeps the sheet open and shows localized error feedback without resizing it.

## Year summary

- Opens from the settings `Statistics` group as its own modal sheet; settings stay open behind it.
- A year switcher (`previous/next year` arrows around the centered year) defaults to the current year.
- The totals block shows yearly income as the primary bold value, then work days, hours worked, average monthly income and average shift (averages divide only by months that carry data), plus bonus/penalty rows when non-zero.
- The fixed twelve-month breakdown lists every month with its day count, compact hours and income; months without data render dimmed with an em-dash detail.
- Switching years reloads the breakdown immediately; the sheet is view-only and never mutates data.

## Data export and import

- `Export data` asks for the format, then opens the system save dialog — `worktime-backup-YYYY-MM-DD.json` for JSON (a versioned file containing every entry (date, duration, hourly rate, bonus, penalty, note) plus the settings (default rate, theme)) or `worktime-YYYY-MM-DD.csv` for CSV (a spreadsheet-friendly table: `date,duration,hourly_rate,bonus,penalty,total`, one row per entry, dot-decimal amounts, `H:MM` durations). CSV is export-only; import stays JSON.
- `Import data` opens the system file picker, parses and validates the file first, then asks for confirmation: the dialog states how many entries the file holds and that current entries and settings will be replaced.
- Confirming an import validates the full file first, then replaces Room/DataStore state with compensation snapshots. If the second store fails or the operation is cancelled after replacement, the previous state is restored where possible; rollback failure is reported separately. The parsed file remains available for retry until import succeeds or is cancelled. Imports have no undo — the confirmation dialog is the safety gate.
- A malformed or unsupported file shows a localized error in the settings sheet and writes nothing.
- Export/import success confirms through the root Snackbar; failures surface in the settings sheet like other operation errors.

## Home screen widget

- An optional 3x2 home-screen widget mirrors the fixed monthly summary: `Work days`, `Hours worked` and `Monthly income` as label-colon-value rows over the app's primary-container plaque in light and dark variants.
- While the app process is alive the widget updates on every entry change; with the process dead the system refresh tick (30 minutes) keeps it current.
- Tapping anywhere on the widget opens the main screen.

## Operation feedback and undo

- Successful entry deletion and successful bulk rate changes surface one root Snackbar anchored above system navigation with an `Undo` action (`Entry deleted` / `Rate changed`).
- Undo restores the exact previous records: a restored entry keeps its original duration, rate and adjustments; an undone bulk change restores every original per-record rate in the period, including records whose stored rate already equaled the new one.
- Only the most recent delete/bulk-rate operation can be undone. The undo snapshot exists only in memory: starting any new delete, bulk-rate or settings-save operation supersedes it, and it is lost when the process dies.
- Operation errors (save, delete, settings and bulk-rate failures) do not use the root Snackbar; they keep their existing localized error surfaces near the sheet that owns them. Undo failures are the exception: they surface as an action-less root Snackbar (`Could not undo the operation. Try again.`) because every sheet is already dismissed when undo runs.

## Visual system

- Controlled calm blue-neutral palettes in light and dark modes. The expanded monthly report uses a distinct elevated surface color so its edge remains visible over the calendar.
- 24 dp major-card radius, 16-20 dp compact-card radius, 8-12 dp cell/input radius.
- Regular body weight for comparable labels/values; medium/semi-bold for titles/totals and bold for calendar day numbers.
- Error red is reserved for invalid input, persistence feedback, delete and penalty semantics.
- Report expansion may use short Material easing. Month title/date-grid changes are immediate and must not crossfade; editor controls never animate sheet height or move it while the persistent numeric editor changes logical field.
- Numeric input line height matches its text size so the caret does not visually exceed the entered value.
- No information essential to calendar interpretation relies on color alone; labels and accessibility descriptions remain present where appropriate.
