# UX specification

## Primary navigation model

WorkTime is calendar-first. There is no dashboard and no bottom navigation in MVP.

```text
Calendar month
   ├─ tap day → Day Editor bottom sheet
   └─ settings icon → Settings screen
```

The calendar must remain the user's spatial context while editing a day.

## Calendar screen

### Header

- Previous month action.
- Localized month + year title.
- Next month action.
- Settings action.

### Monthly summary

Always visible above the grid:

- total expected earnings;
- total worked duration;
- shift count.

The summary should read as one compact block, not three oversized dashboard cards.

### Week grid

- Seven columns.
- Monday is the MVP default first day.
- Six fixed rows keep vertical geometry stable while browsing months.
- Empty leading/trailing cells are visually quiet.

### Day states

**Empty:** day number only.

**Today:** subtle tonal container or outline; do not use a loud filled primary color.

**Filled:** worked duration plus optional compact bonus/penalty markers.

**Selected:** selected state may temporarily override the filled/today treatment while the editor is open.

Calendar cells must not show the full salary, note, rate, bonus amount and penalty amount simultaneously. Detail belongs in the editor.

## Day Editor

Use a Material 3 modal bottom sheet so the month remains mentally visible behind the editing context.

### Information hierarchy

1. Date.
2. Worked duration.
3. Quick duration shortcuts.
4. Hourly rate.
5. Bonus / penalty.
6. Optional note.
7. Live total.
8. Save.
9. Delete for existing entries.

### Quick duration chips

Provide 4h, 6h, 8h, 10h and 12h. The selected shortcut only updates duration; it must not save automatically.

### Validation

- hours and minutes must resolve to 0..1440 total minutes;
- minutes field must not silently accept invalid values that push the total beyond 24h;
- rate, bonus and penalty are non-negative;
- invalid monetary input disables save and shows inline feedback before beta;
- zero hours with a positive bonus remains valid.

### Rate snapshot behavior

For a new entry the editor starts with the current default rate. Once saved, that value belongs to the entry. Editing an old day displays its stored rate, not today's settings rate.

## Settings screen

MVP settings:

- default hourly rate;
- currency;
- theme: system/light/dark.

Potential pre-release addition:

- first day of week.

Settings changes affect future defaults, not historical saved entries.

## Motion

- Month changes should be spatial and restrained.
- Opening the editor uses standard Material sheet motion.
- Avoid decorative animations that delay entry.
- Haptics may be added for save/delete after accessibility testing.

## Typography and spacing

Use Material 3 typography and an 8dp spacing system with 4dp sub-grid where needed.

Recommended hierarchy:

- monthly earnings: `headlineMedium`;
- month title / editor date: `titleLarge` or `titleMedium` depending on width;
- day number: `labelLarge`;
- worked duration in cell: `labelMedium`.

## Color

Default to Material 3 dynamic color on Android 12+ and standard light/dark schemes otherwise.

Color must communicate state without becoming the only state indicator. Bonus and penalty markers need semantic text/description for TalkBack.

## Accessibility

Before beta:

- TalkBack reads date, worked duration and adjustment markers as a coherent cell description;
- icon buttons have localized content descriptions;
- touch targets meet Android guidance;
- 200% font scale does not make Save/Delete unreachable;
- color contrast is checked in both themes;
- motion does not carry essential information.

## Core usability benchmark

A user who already configured a default rate should be able to:

`open app → tap today → tap 8h → save`

in under 10 seconds without reading help text.
