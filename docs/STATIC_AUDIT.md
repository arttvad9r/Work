# Static audit — 20 August 2026

## Scope

A full non-device pass was performed across build configuration, domain/data logic, persistence, ViewModel state, Compose UI, resources/localization, privacy configuration, tests, CI and repository documentation.

This audit deliberately does **not** claim an Android build, emulator run or physical-device result.

## Dependency/toolchain verification

The current version line was checked against official upstream documentation:

- AGP 9.3.1 / Gradle 9.5.0;
- AGP 9 built-in Kotlin model;
- Compose Compiler Gradle plugin usage;
- stable Compose BOM `2026.08.00` / Compose 1.12;
- compileSdk 37 requirement;
- Room 2.8.4;
- DataStore 1.2.1;
- AndroidX Test core/runner 1.7.0 and ext.junit 1.3.0;
- kotlinx.coroutines 1.11.0.

No dependency was changed merely for novelty after verification.

## Findings fixed

### Money/domain integrity

- Added a defensive upper bound for user-entered money components so checked `Long` calculations stay well below overflow even across a 31-day month.
- Added domain note-length enforcement.
- Empty records are rejected.
- Worked-time records require a positive rate in the editor.
- Decimal input rejects exponent notation/malformed syntax.
- Invalid ISO currency no longer silently falls back to locale currency.
- `UserPreferences` now validates a real uppercase ISO 4217 currency code.
- Currency formatting uses the selected currency's fraction-digit convention.
- Expanded rounding, aggregation, bounds and formatting tests.

### State/persistence UX

- Added readiness state so editor cannot snapshot placeholder preferences before DataStore emits.
- Save/delete/settings write failures are caught without logging financial content.
- Failed writes keep the sheet/draft open with a generic inline error.
- Day editor/settings are mutually exclusive.
- Month + entries remain one atomic snapshot during navigation.

### UI/accessibility

- Settings scroll vertically on small screens/large font scales.
- Invalid fields show localized supporting errors.
- Adjustment fields no longer depend on a narrow two-column layout.
- Calendar selected state and semantic descriptions were hardened.
- Added a minimal launcher icon placeholder.

### Privacy

A second privacy pass found that `android:allowBackup="false"` alone can be insufficient for device-to-device transfer behavior on Android 12+ on some manufacturers. The manifest now points to explicit legacy and Android 12+ rules that exclude all supported app-data domains from cloud backup and Android D2D transfer.

### Build/test hygiene

- Added explicit `androidx.test:runner` dependency.
- Release ProGuard includes the standard optimized Android rules.
- Removed unused BuildConfig generation.
- Expanded calendar/domain/formatting test coverage.
- CI has timeout/concurrency controls and always uploads verification reports when present.
- Added a non-Android static-audit script.

## Product clarification

Currency is a **global accounting/display unit**, not an exchange-rate subsystem. Changing the code relabels saved numeric amounts and does not convert them. Multi-currency/FX support requires an explicit future schema/product decision.

## Remaining Android-environment work

- real AGP/KSP/Compose build and lint;
- generate/commit Room v1 schema JSON;
- execute Room instrumentation test;
- Compose UI tests;
- TalkBack/200% font/small-screen/theme pass;
- rotation/process-death/relaunch checks;
- startup/performance/ANR profiling;
- launcher rendering and backup/D2D behavior verification on target devices.

## Known tooling limitation

A complete Gradle Wrapper cannot be committed from the current environment because its binary JAR cannot be safely generated/retrieved here. CI pins Gradle 9.5.0 directly; `BUILD.md` documents the trusted bootstrap procedure.

## Conclusion

After these fixes, no additional high-confidence non-Android correctness defect was identified in the reviewed code. The next useful evidence is a real Android build/device pass rather than speculative feature/refactor work.
