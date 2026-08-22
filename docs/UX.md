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
- If Room has not emitted the requested month's rows yet, rows from the previous month must never be displayed under the new title/grid.

## Fixed monthly summary

- Constant height and bottom-anchored position immediately above the report handle.
- Three aligned rows with one typography hierarchy:
  - Work days;
  - Hours worked / `Отработано часов`;
  - Monthly income.
- Values use regular weight; the card must not compete visually with the calendar.
- A single compact handle is located below this card as the raised peek of the report sheet and remains above system navigation.

## Monthly report sheet

- In the collapsed state only the handle is visible; the report surface, text and shadow remain hidden.
- The complete report remains composed/measured in both states so the sheet height and swipe anchors do not change during a gesture.
- Opens by tapping the handle or dragging upward.
- Collapses by downward drag or a second handle tap. When month navigation, day editing or settings is opened, the destination appears immediately while the report collapses behind it without delaying the next surface.
- The visual handle is rendered inside sheet content rather than through Material 3's `sheetDragHandle` slot because that slot adds a long-press tooltip. Holding the handle must never show `Drag handle` / `Маркер перемещения`.
- Handle tap feedback is intentionally invisible; no full-width flash or tooltip is shown.
- Contains one heading with a trailing colon, work days, hours worked, optional bonus, optional penalty, divider and total.
- Does not duplicate the heading as a second total and does not contain report/export buttons.

## Day editor

Normal closed-keyboard state should fit as one compact sheet.

1. Localized date.
2. One row: duration and hourly rate.
3. Bonus/penalty controls.
4. Calculation card.
5. Save.
6. Delete for an existing entry.

The duration field is labeled `Время` and shows a faint `00:00` format hint while empty. A new day starts with an empty duration field rather than a literal `0`, so typing `12` yields `12` immediately. Duration sanitization also removes accidental leading zeroes defensively, so input such as `012` cannot become `01:2`. Sequential input preserves valid two-digit hours: `1` -> `12` -> `12:0` -> `12:00`. Compact input remains supported: `530` resolves to `5:30`, and `1530` resolves to `15:30`.

Zero-valued numeric editor fields are represented as empty editor text and parsed as zero. Focus changes must not clear or rewrite field text. Numeric validation is intentionally minimal: invalid duration/rate/bonus/penalty values are indicated by the field's red error outline only. Validation helper text is not shown and must not change sheet height.

The primary pair `Время` / `Ставка за час` uses one persistent state-based editable `OutlinedTextField` and one platform text-input session. The same editable node moves between the left and right visual slots when the logical active field changes. The inactive slot is a read-only Material field shell. Switching between the two primary fields must therefore not blur/dispose one editable TextField and focus/start another. This architecture is required because physical-device ADB traces showed that a normal two-TextField focus handoff caused client-side IME hide, a temporary empty `EditorInfo`, `restartInput`, and a visible Gboard hide/show cycle.

The active primary slot is rendered exactly once. Its label, placeholder and value must never be duplicated by an underlying passive field. The passive slot has an invisible click indication: tapping it must not show a rectangular ripple/pressed overlay. If the persistent editor is not currently focused, the same first tap both activates the requested logical field and focuses/shows the numeric keyboard; a second tap must never be required.

Primary input sanitization remains synchronous through `InputTransformation`, and the duration/rate logical values are copied to and from the persistent editor without replacing its focus node. Both primary fields share the same decimal `KeyboardOptions`/`ImeAction.Next`; IME Next from duration activates rate without an editable-field focus handoff.

Bonus and penalty currently remain separate state-based editable fields with the same decimal keyboard configuration. Expansion buttons do not take keyboard focus. If device testing shows the same IME session-handoff defect when moving to/from adjustment fields, the persistent-session pattern should be extended rather than reintroducing inset/focus timing workarounds.

The modal editor uses normal `ModalBottomSheet` window-inset handling so the sheet lifts above the software keyboard instead of remaining underneath it. The content stays vertically scrollable when required. Repeatedly switching `Время ↔ Ставка` must keep the keyboard continuously presented and the sheet stationary after its initial IME lift.

Bonus is always the first adjustment slot and penalty the second. With neither expanded, both buttons share a row. Expanding one replaces only its own slot and pushes the remaining control below in stable order.

The calculation card uses `At hourly rate` / `По ставке`, then optional bonus/penalty rows, then total. Before any value is entered it shows only the `Total` / `Итого` label; zero adjustment rows are omitted.

Save/delete persistence failures keep the draft open and are shown as transient localized Snackbar feedback layered over the sheet. Error feedback must not insert/remove layout rows or resize the sheet.

## Amount formatting

- Input accepts at most two fractional digits.
- Normal amount display rounds to at most two fractional digits and omits a zero fractional part.
- Calendar-cell daily totals are a deliberate exception: they are rounded to a whole number and omit grouping separators to fit compact cells.
- Calculations keep deterministic micros precision internally; the UI precision limit applies only to entry and presentation.

## Settings

- Compact one-screen sheet.
- Hourly-rate label and a centered 120 dp input share a row.
- Initial zero is selected on focus instead of being replaced by an empty value.
- Invalid input uses red outline only; no helper text is inserted below the field.
- The sheet relies on the modal window/inset handling without an additional `imePadding` layer.
- Theme choices fit in one row, including `Системная`.
- Selecting light or dark immediately previews the theme.
- Dismissing settings without saving restores the persisted theme; the selected theme is persisted only after pressing Save.
- Save remains reachable without scrolling when the keyboard is closed.
- Persistence failure is shown with an overlay Snackbar and does not resize the sheet.

## Visual system

- Controlled calm blue-neutral palettes in light and dark modes. The expanded monthly report uses a distinct elevated surface color so its edge remains visible over the calendar.
- 24 dp major-card radius, 16-20 dp compact-card radius, 8-12 dp cell/input radius.
- Regular body weight for comparable labels/values; medium/semi-bold for titles/totals and bold for calendar day numbers.
- Error red is reserved for invalid input, persistence feedback, delete and penalty semantics.
- Report expansion may use short Material easing. Month title/date-grid changes are immediate and must not crossfade; editor controls never animate sheet height or move it while primary input changes logical field.
- Numeric input line height matches its text size so the caret does not visually exceed the entered value.
- No information essential to calendar interpretation relies on color alone; labels and accessibility descriptions remain present where appropriate.
