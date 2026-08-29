# Testing strategy

## Verification layers

### Static audit

```bash
python3 scripts/static_audit.py
```

Checks XML/resources, EN/RU key parity, privacy controls, portrait/IME manifest controls, forbidden binary floating point in domain/data, destructive Room fallback, pinned CI actions, wrapper checksum, release signing safety, release optimization and required release-build CI tasks.

### JVM tests

```bash
./gradlew :app:testDebugUnitTest
```

Coverage includes work-entry invariants, salary rounding/aggregation, month grids, entity mapping, preferences, neutral/compact amount formatting, compact duration formatting, digit-to-duration input formatting and LTR/RTL full-screen navigation direction.

### Android build and lint

```bash
./gradlew \
  :app:lintDebug \
  :app:lintRelease \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:bundleRelease
```

`./scripts/verify.sh` runs the static audit and all commands above through the repository Gradle Wrapper. `bundleRelease` exercises the same optimized release variant that will later be signed for distribution.

### Managed-device tests

CI executes the Android instrumentation suite after the build gate:

```bash
./gradlew :app:pixel2Api30DebugAndroidTest \
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

The managed device is a Pixel 2 API 30 AOSP ATD image. Current coverage includes database/repository integration, smoke/UI consistency, large-font behavior and full-screen motion regressions.

### Physical-device tests

A managed emulator does not replace the manual interaction checklist in `ANDROID_QA.md`, especially for IME visibility, haptics, widget presentation, launcher behavior, document picker flows and bottom-sheet gestures. The final pass must use the exact signed optimized release candidate.

## Required regression cases

- `0`, `15` and `15:30` duration formatting.
- `530 -> 5:30` and `1530 -> 15:30` input behavior.
- worked time with zero hourly rate is rejected by the domain model.
- adjustment-only entry with zero hourly rate remains valid.
- rate snapshot survives settings change.
- bonus/penalty-only entry does not increment work days.
- amount fractions display only when non-zero.
- compact calendar amounts use the same rounding rules without grouping separators.
- fixed `₽` and `₽/h` presentation strings remain consistent in EN/RU resources.
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
- import cancellation after Room replacement restores the previous Room/DataStore snapshot or reports `BACKUP_IMPORT_ROLLBACK`.
- rollback tests wait for observable operation completion; they do not treat coroutine-test scheduler drain as proof that `viewModelScope` work finished.
- initial default-rate adoption runs once; manually clearing the rate to zero does not re-enable it.
- sparse same-rate entries are labelled as recorded-entry groups, not continuous effective periods.
- concurrent theme and default-rate updates preserve both independent preference values.
- portrait orientation remains enforced.
- Settings and Year Summary exits travel beyond the old partial-width target before composition removes them.
- LTR/RTL full-screen navigation direction stays mirrored while predictive back follows the actual swipe edge.

## Current evidence

`main` CI #713 passed static audit, JVM tests, lint/debug compilation and the complete 17-test managed-device instrumentation suite after the predictive-back, motion-regression and RTL changes were merged.

The application has previously been exercised on physical hardware, but exact device/build details were not recorded. Do not expand that into unsupported device-specific claims. The final signed optimized release candidate still requires one fresh documented physical-device pass before release.
