# UX specification

## Screen hierarchy

```text
Calendar
|- tap day -> Day editor sheet
|- tap settings -> Settings sheet
`- drag bottom handle -> Monthly report sheet
```

Only one modal editor/settings surface may be open at a time. The monthly report belongs to the calendar scaffold and does not replace the fixed summary card.

## Calendar screen

- Header: previous month, localized month/year, next month, settings.
- The calendar uses a fixed compact height above the fixed summary and report handle; no additional bottom padding may compete with the scaffold peek area.
- It never scrolls vertically and its position does not depend on entries or report content.
- The grid always has six rows; adjacent-month dates are faint and inactive.
- Current-month cells remain large enough for date, duration and amount.
- Duration and amount are horizontally centered.
- Bonus/penalty icons are centered in equal circular markers.

## Fixed monthly summary

- Constant height and position.
- Three aligned rows with one typography hierarchy:
  - Work days;
  - Hours worked / `Отработано часов`;
  - Monthly income.
- Values use regular weight; the card must not compete visually with the calendar.
- A single 48 x 5 dp handle is located below this card as the raised peek of the report sheet and remains above system navigation.

## Monthly report sheet

- In the collapsed state only the handle is visible; the report surface and text remain transparent until expansion starts.
- Opens by tapping the handle or dragging from it upward.
- Collapses by downward drag or the standard sheet action.
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

The duration field is labeled `Время` and shows a faint `00:00` format hint only while focused and empty. Duration and rate values stay centered in focused and unfocused states. Initial zero clears on focus. The hint and the entered value use the same typography and centered alignment. Digits `530` resolve to `5:30`; `1530` resolves to `15:30`. Switching between duration and amount fields keeps the same numeric keyboard mode and does not apply a second IME inset, so the sheet remains stationary while the keyboard stays open.

Bonus is always the first adjustment slot and penalty the second. With neither expanded, both buttons share a row. Expanding one replaces only its own slot and pushes the remaining control below in stable order. The newly expanded amount field receives focus immediately.

The calculation card uses `At hourly rate` / `По ставке`, then optional bonus/penalty rows, then total. Before any value is entered it shows only the `Total` / `Итого` label; zero adjustment rows are omitted.

## Settings

- Compact one-screen sheet.
- Hourly-rate label and a centered 120 dp input share a row.
- Theme choices fit in one row, including `Системная`.
- Selecting light or dark immediately previews the theme.
- Dismissing settings without saving restores the persisted theme; the selected theme is persisted only after pressing Save.
- Save remains reachable without scrolling when the keyboard is closed.

## Visual system

- Controlled calm blue-neutral palettes in light and dark modes.
- 24 dp major-card radius, 16-20 dp compact-card radius, 8-12 dp cell/input radius.
- Regular body weight for comparable labels/values; medium/semi-bold only for titles, selected dates and totals.
- Error red is reserved for errors, delete and penalty semantics.
- No information relies on color alone; labels and accessibility descriptions remain present.
