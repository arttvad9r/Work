# WorkTime product specification - current compact iteration

This document supersedes the earlier broad v0.2 draft. The canonical short specification is [`PRODUCT.md`](PRODUCT.md); this file records the frozen acceptance contract for the compact-interface branch.

## Acceptance contract

| Area | Required behavior |
|---|---|
| Calendar | Fixed 6 x 7 grid, Monday first, no vertical scrolling or data-dependent jumping |
| Adjacent dates | Previous/next-month dates visible in a faint inactive state |
| Filled day | Centered compact duration, centered neutral amount, optional adjustment markers |
| Duration display | `0`, `15`, or `15:30`; no redundant suffix or zero minutes |
| Fixed summary | Work days, hours worked, monthly income; constant size and position |
| Detailed report | Separate draggable bottom sheet; optional bonus/penalty rows and one total |
| Day editor | Duration and rate in one row; bonus above penalty; live calculation |
| Settings | Compact hourly-rate field plus system/light/dark theme |
| Amounts | No currency labels; non-zero fractional precision only |
| Removed scope | Currency, notes and quick-duration presets stay absent |

## Data and calculations

Room remains the work-entry source of truth. DataStore contains only the default hourly rate and theme. Existing Room `note` storage remains for schema compatibility but the UI writes back the existing value and exposes no note field.

```text
ratePayMicros = roundHalfUp(workedMinutes x hourlyRateMicros / 60)
totalMicros = ratePayMicros + bonusMicros - penaltyMicros
```

## Required verification

- static audit;
- JVM tests;
- Android lint;
- debug APK and debug instrumentation APK assembly;
- physical-device create/edit/delete/relaunch pass;
- drag and tap behavior of the monthly report;
- narrow-screen, keyboard, dark-theme and increased-font checks.
