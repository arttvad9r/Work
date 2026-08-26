# WorkTime Reliability Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the listed data-loss, concurrency, backup, widget, UI, tooling, dead-code, and documentation defects while preserving the existing WorkTime architecture and backup compatibility.

**Architecture:** Keep `CalendarViewModel` as the operation coordinator, add a single mutation mutex and key-level preference writes, and use explicit snapshot/restore for the unavoidable Room+DataStore import boundary. Keep backup codec validation pure and bounded, and make widget month selection a small time-injected flow. Avoid new frameworks, DI, schema migrations, or new database tables.

**Tech Stack:** Kotlin, coroutines/Flow, Room, Preferences DataStore, Compose Material 3, JUnit 5, Compose UI tests, Gradle/Nix/Python audit script.

**Spec:** `docs/superpowers/specs/2026-08-25-worktime-reliability-pass.md`

## Global Constraints

- Preserve Room, DataStore, Compose, and the current ViewModel/repository architecture.
- Do not use destructive Room migration and do not change JSON backup version 1 incompatibly.
- Mutations must serialize the actual write, not only suppress stale UI events.
- File streams are owned and closed by the layer that opens them.
- Device verification is reported only when actually run; emulator is used for checks unless phone installation is explicitly requested.

---

### Task 1: Key-level preference updates

**Files:**
- Modify: `app/src/main/java/com/worktime/app/domain/repository/UserPreferencesRepository.kt`
- Modify: `app/src/main/java/com/worktime/app/data/preferences/DataStoreUserPreferencesRepository.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
- Test: `app/src/test/java/com/worktime/app/ui/calendar/CalendarViewModelTest.kt`

- [ ] Add deterministic tests for concurrent rate updates and concurrent theme/rate updates using deferred barriers; assert the final value of each independent key is preserved.
- [ ] Run `./gradlew testDebugUnitTest --tests '*CalendarViewModelTest'` and observe the current read-modify-write race fail.
- [ ] Add `updateThemeMode(themeMode)` and `updateDefaultHourlyRate(rateMicros)` to the repository; each uses one `DataStore.edit` touching only its own key.
- [ ] Migrate ViewModel callers away from `preferences.first()` plus whole-pair `update`.
- [ ] Run focused preference/ViewModel tests and commit `fix: make preference updates field-atomic`.

### Task 2: Serialize mutations and preserve Undo

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
- Test: `app/src/test/java/com/worktime/app/ui/calendar/CalendarViewModelTest.kt`

- [ ] Add a same-date race test: first mutation waits, second completes, first resumes; assert final repository state is the second mutation and writes are serialized.
- [ ] Run it red against the current generation-only implementation.
- [ ] Guard actual mutation bodies with a private coroutine `Mutex`; retain generation only for stale event/result suppression.
- [ ] Classify operations so read/export/parse-only import do not clear Undo, while entry/bulk/delete/confirmed import/undo do.
- [ ] Add delete → export → undo regression coverage and run all ViewModel tests.

### Task 3: Save/adoption and recoverable import

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/worktime/app/domain/repository/WorkEntryRepository.kt`
- Modify: `app/src/main/java/com/worktime/app/data/repository/RoomWorkEntryRepository.kt`
- Modify: `app/src/main/java/com/worktime/app/data/db/WorkEntryDao.kt`
- Test: `app/src/test/java/com/worktime/app/ui/calendar/CalendarViewModelTest.kt`

- [ ] Add a fake preference failure test proving Room save remains successful and is not retried as a duplicate.
- [ ] Separate default-rate adoption from save success and report adoption failure independently.
- [ ] Add repository snapshot/restore primitives that preserve entries and preferences without a destructive migration.
- [ ] Add a DataStore-failure-after-Room-replace test; assert Room restores the snapshot, pending import remains retryable, and rollback failure is reported distinctly.
- [ ] Keep malformed/import-parse failures mutation-free and run the complete data-operation test group.

### Task 4: Backup validation, I/O, and CSV

**Files:**
- Modify: `app/src/main/java/com/worktime/app/data/backup/BackupCodec.kt`
- Modify: `app/src/main/java/com/worktime/app/data/backup/WorkEntryCsv.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/WorkTimeApp.kt`
- Tests: `app/src/test/java/com/worktime/app/data/backup/BackupCodecTest.kt`, `app/src/test/java/com/worktime/app/data/backup/WorkEntryCsvTest.kt`

- [ ] Add tests for empty/oversized backup, duplicate dates, invalid preferences/dates/values, and HALF_UP CSV rounding (`6159.125` → `6159.13`).
- [ ] Introduce a bounded backup byte limit and validate UTF-8 input before JSON parsing; map malformed runtime/JSON/domain failures to the existing invalid-backup flow.
- [ ] Move stream read/write and serialization to `Dispatchers.IO`; make the layer opening a ContentResolver stream own and close it.
- [ ] Catch `SecurityException`, `IOException`, `FileNotFoundException`, null streams, and invalid payloads in `WorkTimeApp` and route them to the existing snackbar error flow.
- [ ] Run backup tests and verify no stream is closed by both caller and ViewModel.

### Task 5: Widget rollover and year semantics

**Files:**
- Modify: `app/src/main/java/com/worktime/app/widget/WorkTimeWidgetProvider.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarUiState.kt`
- Modify: `app/src/main/java/com/worktime/app/domain/model/MonthSummary.kt` if an explicit `hasData`/entry count is the smallest stable representation.
- Tests: `app/src/test/java/com/worktime/app/widget/WorkTimeWidgetProviderTest.kt`, `app/src/test/java/com/worktime/app/ui/calendar/CalendarViewModelTest.kt`

- [ ] Add a small injected `nowYearMonth` flow/time provider and test that month observation switches after rollover without a busy loop.
- [ ] Implement `flatMapLatest`/boundary refresh so old-month emissions cannot overwrite the new month.
- [ ] Add `bonus == penalty != 0` year-summary coverage and assert `monthsWithData`, dimmed rows, and averages use entry presence rather than net total.
- [ ] Set `yearSummaryYear` from `visibleMonth.year` in `openYearSummary()` and test opening from a historical month.

### Task 6: Change-rate UI interaction

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/settings/ChangeRateSheet.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/settings/SettingsScreen.kt`
- Tests: change-rate flow tests.

- [ ] Expose the existing ChangeRateSheet directly from Settings; do not add a rate-history model or database table.
- [ ] Keep current-month/custom-period selection and existing-entry-only bulk updates.
- [ ] Convert `Settings` rate to the same compact editor interaction as day-editor rate/bonus/penalty and remove blue-only value styling.

### Task 7: Monthly sheet and font-scale/runtime UI safety

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarScreen.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/settings/YearSummaryScreen.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/settings/SettingsScreen.kt`
- Add/modify: Compose UI tests for monthly sheet anchors and large text fallback.

- [ ] Keep the collapsed SummaryStrip stationary above navigation insets; the monthly report sheet moves independently over it with one handle and tap/upward/downward drag anchors.
- [ ] Remove duplicate collapsed surfaces and verify no custom handle tooltip appears on long press.
- [ ] Replace fixed-height/clipped important text with minimum-safe sizes and fallback layout at large font scale; preserve 6×7 calendar at normal scale.
- [ ] Add Compose tests for tap/open and collapsed/expanded semantics where feasible; document physical drag as emulator/device QA.

### Task 8: CompactMoneyField, release, supply chain, and Nix

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/components/CompactMoneyField.kt`
- Test: Compose/unit test for initial zero selection.
- Modify: `app/build.gradle.kts`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `.github/workflows/*` only if workflows exist.
- Modify/delete: `shell.nix`, `flake.nix`, `docs/NIX.md`.

- [ ] Select the sole `0` on first focus, preserve normal cursor behavior for other values, and test `0` → `3` without producing `03`.
- [ ] Configure release signing from local properties/environment; fail or remain clearly unsigned when production signing is absent, while leaving debug builds unchanged.
- [ ] Add the Gradle 9.5.0 distribution SHA256 and pin any existing third-party Actions by commit SHA with version comments.
- [ ] Remove `shell.nix` as duplicate source of truth or make it a thin flake wrapper; only advertise supported Linux/NixOS systems.

### Task 9: Dead code and static audit

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/dayeditor/DayEditorSheet.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarScreen.kt`
- Modify/delete: obsolete resources after repository-wide reference search.
- Modify: `scripts/static_audit.py`

- [ ] Prove and remove unused `WorkTimeColors`/composition local, obsolete empty-month resources, unused `YearNavRow.year`, redundant section branches, imports, and stale comments; keep Room `note`.
- [ ] Replace brittle source-text checks with stable localization parity, manifest/privacy, destructive-migration, release-signing, wrapper-SHA, and structural checks.
- [ ] Run `python3 scripts/static_audit.py` before and after cleanup.

### Task 10: Documentation and final verification

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `docs/PRODUCT.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `docs/BUILD.md`, `docs/ANDROID_QA.md`, `docs/RELEASE_CHECKLIST.md`, and relevant Nix docs.

- [ ] Update documentation to describe actual JSON/CSV currency symbols, non-atomic recoverable import, current editor/report geometry, supported build/signing flow, and only evidence-backed QA.
- [ ] Remove claims of atomic import, successful CI/device QA, or obsolete UI behavior that are not true on HEAD.
- [ ] Run `python3 scripts/static_audit.py`.
- [ ] Run `./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace`.
- [ ] Run `./gradlew connectedDebugAndroidTest` only on the emulator or an explicitly selected emulator device.
- [ ] Inspect `git diff`, `git status`, generated artifacts, secrets, TODO/FIXME additions, and compiler warnings; report any unverified item instead of claiming completion.
- [ ] Commit coherent changes and report exact commands/results.
