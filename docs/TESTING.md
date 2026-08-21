# Testing strategy

## Verification layers

### Static audit

```bash
python3 scripts/static_audit.py
```

Checks XML/resources, EN/RU key parity, privacy controls, forbidden binary floating point in domain/data and destructive Room fallback.

### JVM tests

```bash
./gradlew :app:testDebugUnitTest
```

Coverage includes work-entry invariants, salary rounding/aggregation, month grids, entity mapping, preferences, neutral amount formatting, compact duration formatting and digit-to-duration input formatting.

### Android build and lint

```bash
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

### Device tests

```bash
./gradlew connectedDebugAndroidTest
```

Device tests do not replace the manual interaction checklist in `ANDROID_QA.md`.

## Required regression cases

- `0`, `15` and `15:30` duration formatting.
- `530 -> 5:30` and `1530 -> 15:30` input behavior.
- rate snapshot survives settings change.
- bonus/penalty-only entry does not increment work days.
- amount fractions display only when non-zero.
- no currency string/symbol in current UI resources.
- adjacent-month dates cannot open the current-month editor.
- monthly report opens by tap and drag and contains no duplicate total.

## Current evidence

Targeted source checks after the compact-interface changes found matching EN/RU resources and no remaining current currency references. The associated GitHub Actions run ended before any job step began (`steps` was empty), so it supplies no compilation, test, lint or APK evidence.

A clean wrapper verification and physical-device pass are required before release status can be upgraded.
