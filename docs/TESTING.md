# Testing strategy

## Verification layers

### Static audit

```bash
python3 scripts/static_audit.py
```

Checks XML/resources, EN/RU key parity, privacy controls, portrait/IME manifest controls, forbidden binary floating point in domain/data, destructive Room fallback and known interaction regressions around the report handle and persistent numeric editor.

### JVM tests

```bash
./gradlew :app:testDebugUnitTest
```

Coverage includes work-entry invariants, salary rounding/aggregation, month grids, entity mapping, preferences, neutral/compact amount formatting, compact duration formatting and digit-to-duration input formatting.

### Android build and lint

```bash
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

`./scripts/verify.sh` runs the static audit and all commands above through the repository Gradle Wrapper.

### Device tests

```bash
./gradlew connectedDebugAndroidTest
```

Device tests do not replace the manual interaction checklist in `ANDROID_QA.md`, especially for IME visibility and bottom-sheet gestures.

## Required regression cases

- `0`, `15` and `15:30` duration formatting.
- `530 -> 5:30` and `1530 -> 15:30` input behavior.
- worked time with zero hourly rate is rejected by the domain model.
- adjustment-only entry with zero hourly rate remains valid.
- rate snapshot survives settings change.
- bonus/penalty-only entry does not increment work days.
- amount fractions display only when non-zero.
- compact calendar amounts use the same rounding rules without grouping separators.
- no currency string/symbol in current UI resources.
- numeric validation uses red outline only; obsolete helper-text resources stay removed.
- adjacent-month dates cannot open the current-month editor.
- monthly report opens by tap and drag and contains no duplicate total.
- long-pressing the monthly report handle does not show Material's drag-handle tooltip.
- the report content remains measured while collapsed so sheet anchors stay stable.
- all numeric labels (`Время`, rate, bonus, penalty) remain minimized on the outline even when empty/inactive; the duration `00:00` hint remains a separate centered placeholder.
- the first tap on any visible numeric logical field activates/focuses the persistent editor without requiring a second tap.
- repeated `Время -> Ставка` switching keeps Gboard continuously visible and does not move the sheet.
- after expanding adjustments, repeated `Время -> Ставка -> Премия -> Штраф -> Время` switching uses the same persistent editor/input session and does not cause an IME hide/restart/show cycle.
- values remain attached to their logical fields across repeated switches and survive save/reopen.
- persistence failures keep the relevant sheet open and surface transient Snackbar feedback without layout reflow.
- portrait orientation remains enforced.

## Current evidence

The `main` baseline has been exercised on physical hardware by the project owner. Exact device/build details were not previously recorded in the repository, so that result should not be expanded into unsupported device-specific claims.

Physical-device ADB diagnostics on the interaction-stability branch identified the previous keyboard jump as a client-side IME hide/restart/show sequence during focus transfer between separate Compose TextFields. The persistent editor architecture has already eliminated that jump for `Время <-> Ставка`; the current branch extends the same mechanism to bonus and penalty and therefore requires one fresh four-field hardware pass before merge.

GitHub Actions may fail to start while the account's Actions usage limit is exhausted. A run with no executed steps is an infrastructure/account limitation, not a test result. Local wrapper verification and recorded device QA remain valid evidence while CI runners are unavailable.
