# WorkTime UI system

One page of rules so no future change invents a fifth way to show "label + value".
The implementation lives in `app/src/main/java/com/worktime/app/ui/components/`.

## Principles

Flat UI + typography + whitespace. Screens are built from plain rows on a clean
background, not from stacked cards. A container (fill/border) is justified only when
it carries state: an editable value, a selected segmented option, a primary action,
the summary strip, or a modal sheet. Navigation/value rows stay flat. Identical
entities must always look and behave identically, on every screen.

## Spacing (`AppDimens`)

| Token | Value | Use |
|---|---|---|
| `screenHorizontalPadding` | 16 dp | every full screen and modal sheet |
| `sectionSpacing` | 16 dp | above section headers / between sections |
| `rowGap` | 8 dp | vertical gap between rows and small blocks |
| `rowMinHeight` | 48 dp | compact accessible interactive row across screens and sheets |
| `rowWithSubtitleMinHeight` | 56 dp | navigation rows with explanatory copy |
| `compactControlHeight` | 44 dp | segmented controls and inline editors |
| `compactFieldWidth/Height` | 120×44 dp | inline numeric editor slot |
| `primaryButtonMinHeight` | 52 dp | primary actions |

## Typography

Only `MaterialTheme.typography` (all styles carry tabular numerals):

- Screen title → `AppTopBar`. Sheet title → `AppSheetTitle` (`titleLarge`, sentence case).
- Section label → `labelMedium` onSurfaceVariant, uppercase (section headers only,
  never sheet titles).
- Primary row label → `bodyLarge` onSurface; secondary/subtitle → `bodySmall` /
  `bodyMedium` onSurfaceVariant.
- Numeric values → `bodyLarge`; emphasized totals → `titleMedium` SemiBold.
- The monthly-report headline amount (20 sp) is the one sanctioned large total.
- Calendar day cells keep their own compact sizes: Calendar is a data-dense
  special case.

## Colors (semantic roles)

- `primary` — focus/today and compact data emphasis, never broad decorative fills.
- `onSurface` — primary content values and row labels.
- `onSurfaceVariant` — secondary labels, subtitles, chevrons, placeholders and dates.
- `outlineVariant` — dividers and field borders.
- `secondaryContainer` / `onSecondaryContainer` — selected segmented option,
  SummaryStrip, secondary CTA surfaces (e.g. year-summary navigation).
- `error` — invalid input outline, negative totals, destructive actions.

### Calendar hierarchy

Calendar color is semantic rather than decorative. The three data levels must remain
visually distinct without turning the grid into a heatmap:

- date → `onSurfaceVariant` (today uses `primary` plus a primary outline);
- worked duration → `onSurface`, the strongest text inside a populated cell;
- amount → restrained `primary`; negative amounts → `error`;
- populated cells get only a very light neutral container tint;
- selected state is carried by `primaryContainer` + cell geometry, not by recoloring
  every piece of data;
- out-of-month dates are reduced by alpha.

## Shapes

Use `MaterialTheme.shapes` (8/12/16/24/28). Sheets use `AppSheetShape` (28 top
corners). No ad-hoc `RoundedCornerShape(13.dp)`-style values.

## Motion

Motion is feedback, not decoration. Shared Material controls keep their normal press
feedback. Visual state changes may use the shared short
`AppDimens.feedbackAnimationMillis` (120 ms) transition for color only. Do not animate
row sizes, spacing, data layout, or add delays before an action. Modal sheets keep the
platform Material motion they already provide.

## Components

- **`AppTopBar`** — the only top bar for full-screen pages (Settings, Year summary).
- **`AppModalBottomSheet(title)`** — the only modal bottom sheet wrapper: fixed shape,
  drag handle, optional sentence-case title, nav-bar padding, horizontal padding.
- **`AppSheetTitle`** — sentence-case sheet title below the handle. Never uppercase.
- **`AppSectionHeader`** — uppercase section label (РАСЧЁТ / ВНЕШНИЙ ВИД / ДАННЫЕ).
- **`AppNavigationRow(label, value?, subtitle?)`** — flat tappable row with trailing
  chevron for "go somewhere / choose something" actions (Change rate, Export JSON/CSV,
  Import, date pickers in ChangeRate).
- **`LabelValueRow`** — read-only "label … value" line (reports, summaries,
  calculation blocks). Label onSurfaceVariant, value onSurface.
- **`AppFieldValueSlot` + `CompactInputChrome` (+ `CompactMoneyField`)** — the editable
  value contract: a fixed 120×44 slot at the row's trailing edge that shows either the
  read-only value or the compact bordered editor. Activating editing never swaps the
  row for a full-width form field and never changes the row height.
- **`AppSegmentedControl(options, selectedIndex)`** — the only segmented presentation
  for mutually exclusive options (theme mode, rate period). Selected option uses
  secondaryContainer.
- **`AppPrimaryButton`** — full-width ≥52 dp primary action (Save, Change rate).
- **`AppDestructiveAction`** — full-width error-colored text action (Delete entry).
- **Contextual quick action** — lightweight `TextButton` with a leading semantic icon,
  no card/fill, and a ≥48 dp touch target. Use only for a timely optional shortcut such
  as “Fill today”; hide it when the shortcut is no longer relevant.
- **`PlainDragHandle`** — tooltip-free drag handle shared by all sheets.

Row decision guide: opens another screen/sheet → `AppNavigationRow`; shows a computed
or stored value → `LabelValueRow`; edits a number in place → value-slot contract;
chooses one of 2–4 mutually exclusive options → `AppSegmentedControl`.

## Explicit exceptions

- **Calendar** — data-dense grid with fixed macro geometry (6×7, 64 dp week rows,
  28 dp weekday/date areas, 48 dp header). It keeps its own compact typography and the
  calendar hierarchy above instead of adopting settings-row styling.
- **Year summary** — non-scrolling screen showing all twelve months at once. Populated
  years use the available height; an entirely empty year uses compact fixed month rows
  so missing data does not look like stretched content.
- **Month picker dialog** — grid of month chips inside an AlertDialog; a deliberate
  picker pattern, not a segmented control.
