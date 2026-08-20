# Static audit — 20 August 2026

## Scope

A full non-device pass was performed across:

- Gradle/AGP configuration;
- domain models and money calculation;
- Room entity/DAO/repository boundaries;
- DataStore preferences;
- ViewModel state/error handling;
- Compose calendar/editor/settings code;
- localization/resources/manifest;
- tests;
- CI configuration;
- repository documentation.

This audit deliberately does **not** claim an Android build, emulator run or physical-device result.

## Dependency/toolchain verification

The current version line was checked against official upstream documentation:

- AGP 9.3.1 / Gradle 9.5.0: Android Gradle Plugin release notes;
- compileSdk 37: current Android/Compose requirements;
- Compose BOM `2026.08.00`: Android Compose BOM documentation;
- Room 2.8.4: AndroidX Room documentation;
- AndroidX Test core/runner 1.7.0 and ext.junit 1.3.0: AndroidX Test release notes;
- AGP 9 built-in Kotlin behavior: Android built-in Kotlin migration documentation.

## Findings fixed

### Money and domain integrity

- Added an explicit upper bound for user-entered money components to keep checked `Long` calculations away from overflow.
- Added domain note-length enforcement.
- Empty records are rejected by the domain model.
- Worked-time records require a positive rate in the editor.
- Direct decimal parsing rejects exponent notation and malformed decimal syntax.
- Invalid ISO currency codes no longer silently fall back to the device locale currency.
- Currency formatting now uses the selected currency's fraction-digit convention.
- Added tests for half-micro rounding, month aggregation, money limits and formatting edge cases.

### State and persistence UX

- Added a readiness state so the day editor cannot initialize from placeholder preferences before DataStore emits the saved settings.
- Save/delete/settings write failures are caught without logging financially sensitive values.
- Failed writes keep the relevant sheet/draft open and show an inline error.
- Day editor and settings sheet are made mutually exclusive.
- Month + entries remain an atomic snapshot when switching months.

### UI/accessibility

- Settings now scroll vertically on small screens/large font scales.
- Validation errors are visible instead of only disabling Save.
- Bonus and penalty inputs are stacked vertically to reduce narrow-screen pressure.
- Calendar selected state is explicit.
- Calendar day semantics include full date, today/selected state, duration and adjustment markers.
- Compact calendar texts are constrained to one line.
- Added a minimal launcher icon placeholder.

### Build/test hygiene

- Added explicit `androidx.test:runner` dependency for `AndroidJUnitRunner`.
- Release ProGuard configuration now includes the optimized Android default rules.
- Removed unused generated `BuildConfig` enablement.
- Expanded month-grid tests to all possible starting weekdays plus leap-year/custom-first-day cases.
- Expanded domain and money-format tests.
- CI now has timeout/concurrency controls and uploads verification reports even when verification fails.

## Important product clarification

Currency is currently a **global accounting/display unit**, not an exchange-rate subsystem. Changing `EUR` to `USD`, for example, relabels saved numeric amounts; it does not convert them. The settings UI now states this explicitly. A future multi-currency design would require a schema/product decision rather than silent FX behavior.

## Remaining items that require Android tooling or a device

- resolve any AGP/KSP/Compose compile errors found by a real Android build;
- execute Android lint and inspect its report;
- generate and commit the Room v1 schema JSON;
- execute the Room instrumentation test on an emulator/device;
- Compose UI tests for critical flows;
- TalkBack pass;
- 200% font-scale/small-screen visual pass;
- light/dark/dynamic-color visual pass;
- rotation/process-death/relaunch checks;
- startup/performance/ANR profiling;
- verify launcher icon rendering across launchers/API levels.

## Known repository/tooling limitation

A complete Gradle Wrapper cannot be committed from the current environment because the required wrapper binary cannot be generated or safely retrieved here. `gradle-wrapper.properties` pins 9.5.0 and CI installs that version directly. `BUILD.md` documents the trusted local bootstrap procedure.

## Static-audit conclusion

No additional high-confidence correctness bug was found in the reviewed non-Android code after the fixes above. The project should now move to **real Android build verification**, not additional speculative refactoring.
