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

Theme-mode changes update the visible Material palette immediately when the user selects a
mode. The preference write is optimistic and runs after the visual state has changed; a failed
write rolls the palette back to repository state and surfaces error feedback. The theme segmented
indicator and its labels snap to the selected option in the same frame as the palette: device-video
QA showed that letting the pill continue a spring after the palette changed exposed a contradictory
one-frame state. Geometry, typography and content order stay fixed while the theme changes.

### Calendar hierarchy

Calendar color is semantic rather than decorative. The three data levels must remain
visually distinct without turning the grid into a heatmap:

- date → `onSurfaceVariant` (today uses `primary` plus a primary outline);
- worked duration → `onSurface`, the strongest text inside a populated cell;
- amount → restrained `primary`; negative amounts → `error`;
- populated cells get only a very light neutral container tint;
- tapping a day opens the editor immediately, so the closed grid has no persistent selected fill;
  the selected date remains business/accessibility state, not another broad color layer;
- out-of-month dates are reduced by alpha.

## Shapes

Use `MaterialTheme.shapes` (8/12/16/24/28). Sheets use `AppSheetShape` (28 top
corners). No ad-hoc `RoundedCornerShape(13.dp)`-style values.

## Motion

Motion communicates continuity and feedback; it is never decorative. The interface
should feel responsive rather than animated for its own sake.

- Micro state/color feedback: roughly 75–150 ms.
- Local content/position changes: roughly 150–220 ms or a non-bouncy spring.
- Full-screen hierarchy changes use restrained, symmetric travel; entering and leaving the same
  hierarchy must not use dramatically different distances.
- Settings enters from the side with about one-sixth of the viewport width and returns by the same
  distance. Year Summary rises about one-tenth of the viewport from below with a short fade and
  returns downward. Device-video QA showed that the previous one-eighth/one-sixteenth travel read
  almost as a hard cut, while full-screen travel remained unnecessarily heavy.
- Calendar month navigation uses `HorizontalPager`: content follows the finger during a drag and
  arrows animate the same pager programmatically. Adjacent month data stays warm.
- The fixed calendar header, `Fill today` affordance and monthly summary do not switch when
  `PagerState.currentPage` flips in the middle of a drag. They keep the last committed month until
  paging settles, preventing a one-frame title/summary jump while the page is still moving.
- Year-summary paging uses the same pager stiffness, snap threshold and interruptible arrow
  retargeting as the calendar. Its fixed year label follows the same committed-state rule instead
  of changing halfway through a swipe.
- Pager springs are deliberately softer than compact control springs; direct manipulation must
  settle smoothly rather than snap sharply after the finger is released.
- The persistent numeric editor may move between its fixed rows with a non-bouncy spring;
  it must remain one focusable node so the OEM IME session is preserved.
- Bonus/Penalty Add/value states use a short fade-through while that persistent editor
  moves into or out of the fixed adjustment slot; row geometry stays unchanged.
- Selected segmented state normally uses one moving pill rather than unrelated hard-swapped fills.
  Theme mode is the explicit exception: because the whole palette changes at the same time, its pill
  and labels snap atomically with the new palette instead of visually lagging behind it.
- Conditional contextual content such as `Fill today` may fade/expand in and collapse out.
- A calendar day uses exactly one custom press signal: an immediate very light tone on touch-down
  that fades out quickly after release. It deliberately has no framework ripple and no persistent
  selected background, because stacking those layers caused a visible flash before the editor.
- The monthly summary strip is the actual persistent peek/header of `BottomSheetScaffold`.
  Vertical drag progress therefore moves the Material sheet itself under the finger; do not add a
  second custom drag detector, activation distance, decorative lift/scale, or release-triggered
  animation on top. Normal taps keep the Material click indication and the chevron follows the
  report open/closed state.
- The Settings default-rate value/editor swaps atomically inside the fixed value slot. Device-video
  QA showed that a crossfade drew the old value underneath the focused field for a few frames.
- Month/year numeric summary values may use short fade-through transitions rather than
  replacing text in one frame when no interactive control occupies the same pixels.
- Year-to-year report changes follow the pager laterally while screen entry/exit remains vertical.
- Modal sheets keep the Material platform motion and drag physics they already provide.
- App launch uses the Android SplashScreen API and a short exit fade into real content.
- Never add looping motion, bounce/overshoot for routine controls, artificial action delays,
  animated data-layout reflow, or multiple simultaneous feedback effects for one routine action.

Press/ripple feedback from Material components remains enabled unless a custom control provides
an equivalent shape-aware press state. Motion must remain interruptible where practical and must
not change business state timing.

### Haptics

Haptics are sparse and semantic, not a vibration on every tap. They use Compose/platform
feedback types so device and system settings remain authoritative.

- `SegmentTick` — when a segmented option actually changes and as the light pager detent.
- A direct month/year swipe emits one `SegmentTick` when horizontal finger travel crosses the same
  35% positional threshold used by pager snapping. The observer never consumes the gesture. If the
  finger returns close to the origin, the detent re-arms so a later deliberate crossing can tick.
- Month/year arrow navigation emits the same light tick immediately when a valid arrow action is
  accepted, at animation start rather than after animation completion.
- `settledPage` is never a haptic source. It commits the new month/year only; attaching vibration
  there produced the delayed, detached feedback observed on physical devices.
- Vertical-dominant movement does not trigger the pager detent.
- Monthly-summary dragging is deliberately silent. There is no synthetic activation threshold now:
  Material sheet physics directly track the finger, so adding a separate threshold haptic would
  reintroduce a second feedback timeline for the same gesture.
- `Confirm` — only after persistence succeeds for saving/deleting an entry or applying a
  non-empty rate change.
- `Reject` — once for an explicit persistence/backup/calendar operation failure.
- Ordinary navigation taps, day taps, field focus and passive scrolling do not add extra haptics.

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
  pill normally moves between equal-width options, each segment provides a rounded press state,
  and one light tick is emitted on a real selection change. Theme mode disables pill/label motion
  so its visual selection changes atomically with the global palette.
- **`AppPrimaryButton`** — full-width ≥52 dp primary action (Save, Change rate).
- **`AppDestructiveAction`** — full-width error-colored text action (Delete entry).
- **Contextual quick action** — lightweight `TextButton` with a leading semantic icon,
  no card/fill, and a ≥48 dp touch target. Use only for a timely optional shortcut such
  as “Fill today”; hide it when the shortcut is no longer relevant.
- **`PlainDragHandle`** — tooltip-free drag handle shared by modal sheets. When tappable it
  provides a restrained tone change on touch-down rather than remaining visually silent. The
  calendar monthly report does not use it: its persistent SummaryStrip is the sheet peek/handle.

Row decision guide: opens another screen/sheet → `AppNavigationRow`; shows a computed
or stored value → `LabelValueRow`; edits a number in place → value-slot contract;
chooses one of 2–4 mutually exclusive options → `AppSegmentedControl`.

## Explicit exceptions

- **Calendar** — data-dense grid with fixed macro geometry (6×7, 64 dp week rows,
  28 dp weekday/date areas, 48 dp header). It keeps its own compact typography and the
  calendar hierarchy above instead of adopting settings-row styling. In the current
  month, a missing entry for today may expose the lightweight “Fill today” contextual
  action below the grid; it disappears as soon as today's entry exists.
- **Year summary** — dense yearly report showing all twelve months when height permits and
  becoming vertically scrollable on short windows. Populated years use the available height;
  an entirely empty year keeps compact month rows so missing data does not look stretched.
- **Month picker dialog** — grid of month chips inside an AlertDialog; a deliberate
  picker pattern, not a segmented control.
