# UX specification

## Navigation model

WorkTime is calendar-first. MVP has no dashboard or bottom navigation.

```text
Calendar month
├─ tap day → Day Editor bottom sheet
└─ settings icon → Settings bottom sheet
```

Only one modal sheet may be open at a time.

## Cold start

Until Room/DataStore have emitted the first real state, the calendar content shows a compact loading indicator and settings/day editing remain unavailable. This prevents placeholder preferences from being snapshotted into a new record.

## Calendar

### Header

- previous month;
- localized month/year;
- next month;
- settings.

### Monthly summary

One compact card contains:

- expected salary;
- worked duration;
- shift count;
- base/bonus/penalty breakdown when relevant;
- shortcut to configure rate when the default rate is zero.

### Grid

- seven columns;
- Monday-first MVP;
- fixed six rows;
- localized weekday names;
- visually quiet out-of-month cells.

### Day states

- **Empty:** day number.
- **Today:** subtle tonal state.
- **Filled:** duration + compact adjustment markers.
- **Selected:** stronger selected container while editor is open.

TalkBack description combines full localized date, today/selected state, worked duration and adjustment markers.

## Day editor

Material 3 modal bottom sheet preserves the month as context.

Order:

1. date;
2. worked duration;
3. quick durations;
4. hourly rate;
5. bonus;
6. penalty;
7. note;
8. live total;
9. persistence error if any;
10. Save;
11. Delete for an existing entry.

### Validation

- hours: 0..24;
- minutes: 0..59;
- 24h requires 0 additional minutes;
- rate/bonus/penalty: non-negative decimal input, at most six fractional digits;
- worked time requires rate > 0;
- user-entered money is capped by the defensive calculation limit;
- note: <=200 characters;
- at least worked time or an adjustment is required.

Invalid fields display localized supporting text. Save is disabled until the draft is valid. Persistence failure does not close the sheet or discard input.

### Rate snapshot

New entry uses current default rate. Existing entry always displays its stored rate snapshot. Settings changes do not rewrite it.

## Settings

- default hourly rate;
- global ISO currency code;
- system/light/dark theme.

The settings sheet scrolls vertically for small screens / large font scale. Theme chips scroll horizontally rather than wrapping unpredictably.

A valid currency displays an explicit warning:

> Changing currency relabels existing values; no exchange-rate conversion is performed.

Saving settings may use a zero default rate, but worked-time entries themselves require a positive effective rate.

## Error handling

Database/DataStore write errors are represented as generic localized UI messages. Financial values, notes and salary totals are never inserted into the error text/logging path. Failed operations keep the relevant modal open when possible.

## Responsive/accessibility requirements

Before beta, verify on device/emulator:

- TalkBack day semantics;
- localized icon descriptions;
- 200% font scale;
- small phone width;
- Save/Delete remain reachable;
- light/dark/dynamic-color contrast;
- no essential information depends on color alone.

## Core usability benchmark

After a default rate is configured:

```text
open app → tap today → tap 8h → save
```

Target: under 10 seconds without help text.
