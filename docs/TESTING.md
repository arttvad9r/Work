# Testing strategy

## Verification layers

### Static audit

```bash
python3 scripts/static_audit.py
```

Checks XML/resources, EN/RU key parity, privacy controls, portrait/IME manifest controls, forbidden binary floating point in domain/data, destructive Room fallback, pinned CI actions, wrapper checksum, release signing and supported Nix platforms.

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

Device tests do not replace the manual interaction checklist in `ANDROID_QA.md`, especially for IME visibility, haptics, widget presentation and bottom-sheet gestures.

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

## Current evidence

The feature merge candidate passed the full automated gate with 104 JVM tests, lint, debug APK assembly and instrumentation APK assembly on Gradle 9.7.1 / Java 17. A subsequent cold `main` run exposed a nondeterministic rollback test that relied on `advanceUntilIdle()` even though the operation runs in `viewModelScope`; repository cleanup replaces that scheduler assumption with explicit completion waits.

The application has previously been exercised on physical hardware, but exact device/build details were not recorded. Do not expand that into unsupported device-specific claims. The merged persistent-editor, haptic and widget changes require one fresh documented physical-device pass before release.
