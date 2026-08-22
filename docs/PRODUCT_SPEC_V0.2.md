# WorkTime product specification - current compact iteration

This document supersedes the earlier broad v0.2 draft. The canonical short specification is [`PRODUCT.md`](PRODUCT.md); this file records the frozen acceptance contract for the compact interface.

## Acceptance contract

| Area | Required behavior |
|---|---|
| Orientation | Portrait-only application layout |
| Calendar | Fixed 6 x 7 grid, Monday first, no vertical scrolling or data-dependent jumping |
| Adjacent dates | Previous/next-month dates visible in a faint inactive state |
| Filled day | Centered compact duration, centered neutral amount, optional adjustment markers |
| Duration display | `0`, `15`, or `15:30`; no redundant suffix or zero minutes |
| Fixed summary | Work days, hours worked, monthly income; constant size and position |
| Detailed report | Separate draggable bottom sheet; tap/drag handle; optional bonus/penalty rows and one total; no long-press drag-handle tooltip |
| Day editor | Duration and rate in one row; bonus above penalty; stable numeric IME focus chain; live calculation |
| Validation | Invalid numeric fields use red outline only; no helper-text rows |
| Operational errors | Save/delete/settings failures remain recoverable and are shown without resizing the active sheet |
| Settings | Compact hourly-rate field plus system/light/dark theme; focusing initial zero selects it instead of clearing it |
| Amount input | At most two fractional digits |
| Amount display | No currency labels; at most two fractional digits; zero fractional part omitted |
| Internal money | Integer micros for deterministic six-decimal internal precision; no persisted `Float`/`Double` calculations |
| Removed scope | Currency, notes, quick-duration presets, validation helper text and landscape support stay absent |

## Data and calculations

Room remains the work-entry source of truth. DataStore contains only the default hourly rate and theme. Existing Room `note` storage remains for schema compatibility but the UI writes back the existing value and exposes no note field.

Worked time requires a positive hourly rate. Bonus/penalty-only records remain valid with a zero hourly rate and do not increment the work-day count.

```text
ratePayMicros = roundHalfUp(workedMinutes x hourlyRateMicros / 60)
totalMicros = ratePayMicros + bonusMicros - penaltyMicros
```

## Required verification

- static audit;
- JVM tests;
- Android lint;
- debug APK and debug instrumentation APK assembly through the checked-in Gradle Wrapper;
- physical-device create/edit/delete/relaunch pass;
- monthly-report tap/drag/long-press behavior;
- continuous IME visibility while moving between numeric editor fields and expanding adjustments;
- outline-only validation and layout-neutral persistence-error feedback;
- narrow portrait screen, dark theme and increased-font checks.

The `main` baseline has been exercised on physical hardware. Any branch changing sheet/IME behavior requires a focused device rerun before merge. GitHub Actions account/usage-limit failures are infrastructure status, not application test results.
