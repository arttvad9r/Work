# Changelog

All notable product changes will be documented here.

## [Unreleased]

### Verification — 2026-08-21

- Bootstrapped and verified the Gradle 9.5.0 wrapper.
- Passed static audit, JVM tests, lint, debug APK and instrumentation APK assembly.
- Added a Compose startup smoke test source; connected execution remains pending because the available emulator runtime did not expose package/activity services.

### Added

- Calendar-first Compose MVP.
- Room persistence and repository boundary.
- DataStore rate/currency/theme settings.
- Historical hourly-rate snapshots.
- Bonus, penalty and note persistence.
- Monthly salary/hours/shift summary.
- Deterministic integer-micros salary calculator.
- English and Russian resources.
- Light/dark/system themes and dynamic color path.
- Instrumented Room test source plus expanded JVM test coverage.
- Minimal launcher icon placeholder.
- Repository static-audit script and build verification scripts.
- Build, static-audit, Android-QA and release-checklist documentation.

### Changed

- Editor now exposes explicit inline validation rather than silently disabling Save.
- Worked-time entries require a positive effective hourly rate in the editor.
- User-entered money components are bounded defensively against checked-arithmetic overflow.
- Settings are vertically scrollable for small screens/large fonts.
- Currency settings explicitly state that no exchange-rate conversion occurs.
- Calendar cold start waits for persistence/preferences readiness before entry editing.
- Save/delete/settings persistence failures keep their draft/sheet open with generic inline error text.
- Calendar selected state and TalkBack descriptions were hardened.
- CI now includes static audit, timeout/concurrency controls and verification-report artifacts.

### Fixed

- Prevented month-title/data mismatch during rapid month switching.
- Prevented placeholder default preferences from being snapshotted into a new entry during cold start.
- Corrupted Preferences DataStore files now recover to safe defaults through `ReplaceFileCorruptionHandler`.
- Removed silent fallback to locale currency for invalid stored/display currency codes.
- Added explicit AndroidX test runner dependency for `AndroidJUnitRunner`.
- Release ProGuard configuration now includes the standard optimized Android rule set.

### Verification status after the QA baseline

- Gradle Wrapper 9.5.0 is committed and used by the verification scripts.
- Room v1 schema JSON is committed.
- Static audit, JVM tests, lint, debug APK and instrumentation APK assembly pass.
- API 26 and API 35 Room/Compose instrumentation pass.
- API 35 manual core flow passes, including persistence and delete/recalculation.
- API 37 Compose smoke remains blocked inside AndroidX Test/Espresso before the assertion; this is tracked as QA infrastructure issue BUG-001.
- Manual edge-case, full accessibility and production signing gates remain open.

### Fixed in audit follow-up — 2026-08-21

- Restored quick-hour entry chips and note editing removed from the QA branch.
- Disabled month navigation while a day/settings modal is open.
- Made shell and PowerShell verification scripts invoke the committed Gradle Wrapper.
- Added consolidated [docs/CODE_AUDIT.md](docs/CODE_AUDIT.md).
