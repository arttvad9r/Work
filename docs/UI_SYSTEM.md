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

Motion communicates continuity and feedback; it is never decorative. The interface
should feel responsive rather than animated for its own sake.

- Micro state/color feedback: roughly 100–160 ms.
- Local content/position changes: roughly 160–220 ms or a non-bouncy spring.
- Full-screen hierarchy changes: roughly 220–300 ms with a short directional slide + fade.
- Calendar month changes use a directional transition matching previous/next navigation.
- The persistent numeric editor may move between its fixed rows with a non-bouncy spring;
  it must remain one focusable node so the OEM IME session is preserved.
- Selected segmented state is one moving pill rather than unrelated hard-swapped fills.
- Conditional contextual content such as `Fill today` may fade/expand in and collapse out.
- Calendar state colors (selected/today/populated) should interpolate rather than flash.
- Modal sheets keep the Material platform motion and drag physics they already provide.
- App launch uses the Android SplashScreen API and a short exit fade into real content.
- Never add looping motion, bounce/overshoot for routine controls, artificial action delays,
  animated data-layout reflow, or motion whose only purpose is decoration.

Press/ripple feedback from Material components remains enabled. Motion must remain
interruptible where practical and must not change business state timing.

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
  for mutually exclusive options (theme mode, rate period). A shared secondaryContainer
  pill moves between equal-width options.
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
  calendar hierarchy above instead of adopting settings-row styling. In the current
  month, a missing entry for today may expose the lightweight “Fill today” contextual
  action below the grid; it disappears as soon as today's entry exists.
- **Year summary** — non-scrolling screen showing all twelve months at once. Populated
  years use the available height; an entirely empty year uses compact fixed month rows
  so missing data does not look like stretched content.
- **Month picker dialog** — grid of month chips inside an AlertDialog; a deliberate
  picker pattern, not a segmented control.
