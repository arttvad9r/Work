# WorkTime UX And Bulk Rate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add reversible deletion and bulk hourly-rate changes while improving calendar state visibility, summary emphasis, empty state, and month swipe navigation.

**Architecture:** Keep the existing single-screen Compose architecture and Room repository as the source of truth. Add one atomic repository operation for changing hourly-rate snapshots over an inclusive date range; the ViewModel owns a single in-memory undo action containing exact prior records. Recompose settings into grouped sections, with the bulk-rate flow in a separate modal sheet.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, coroutines, JUnit 5.

**Spec:** Approved conversation design on 2026-08-22.

## Global Constraints

- Core data remains local and offline.
- Bulk rate changes affect every record in the selected inclusive period, regardless of its current rate.
- Bulk rate changes modify only `hourlyRateMicros`; duration, bonus, penalty, date, and note remain unchanged.
- Default hourly rate is not changed by a bulk operation.
- Undo restores exact prior records and is available only while the process remains alive.
- Period choices are `Current month` and `Custom period`.
- No export/backup UI is added in this change; those are separate unfinished features.
- Existing portrait layout, immediate month switching, accessibility semantics, and stable IME behavior remain intact.

---

### Task 1: Add domain and repository support for bulk rate changes

**Files:**
- Modify: `app/src/main/java/com/worktime/app/domain/repository/WorkEntryRepository.kt`
- Modify: `app/src/main/java/com/worktime/app/data/repository/RoomWorkEntryRepository.kt`
- Modify: `app/src/main/java/com/worktime/app/data/db/WorkEntryDao.kt`
- Test: `app/src/test/java/com/worktime/app/data/repository/RoomWorkEntryRepositoryTest.kt`

**Interfaces:**
- Produce `suspend fun updateHourlyRate(startDate: LocalDate, endDate: LocalDate, hourlyRateMicros: Long): List<WorkEntry>` or an equivalent repository API that returns the exact records changed so the ViewModel can undo them.
- Preserve the existing `save` and `delete` APIs.

- [ ] **Step 1: Write a failing repository test**

Create a test with records both inside and outside an inclusive date range, including different old rates and bonus/penalty values. Assert that only in-range hourly rates change and all other fields remain identical; assert that the returned changed records contain their old values.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.worktime.app.data.repository.RoomWorkEntryRepositoryTest`

Expected: FAIL because the bulk-rate repository API does not exist.

- [ ] **Step 3: Implement the minimum Room/repository operation**

Add a DAO query for the date range and an update transaction that reads the matching entities, upserts copies with only the hourly rate changed, and returns the original domain records. Use an inclusive epoch-day range and reject an invalid range or non-positive rate before writing.

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.worktime.app.data.repository.RoomWorkEntryRepositoryTest`

Expected: PASS.

- [ ] **Step 5: Run existing domain/data tests**

Run: `./gradlew testDebugUnitTest`

Expected: PASS.

---

### Task 2: Add ViewModel undo state and bulk-rate workflow

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarUiState.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/WorkTimeApp.kt`
- Test: `app/src/test/java/com/worktime/app/ui/calendar/CalendarViewModelTest.kt`

**Interfaces:**
- Produce callbacks for `changeRateForPeriod`, `undoLastOperation`, and an operation result/message state consumed by the root UI.
- The bulk operation receives `startDate`, `endDate`, and `newRateMicros`.

- [ ] **Step 1: Write failing ViewModel tests**

Cover: deleting an existing entry stores the exact deleted entry for undo; undo restores it; bulk rate update stores all original records; undo restores mixed original rates; a failed operation does not expose an undo action.

- [ ] **Step 2: Run the focused tests and verify they fail**

Run: `./gradlew testDebugUnitTest --tests com.worktime.app.ui.calendar.CalendarViewModelTest`

Expected: FAIL because undo and bulk operation state do not exist.

- [ ] **Step 3: Implement minimal in-memory undo**

Use one sealed internal undo snapshot containing either one deleted record or a list of original records. On successful delete or bulk update, close the active sheet and expose a success event. On undo, save each original record, clear the undo snapshot, and expose a recoverable error if restoration fails.

- [ ] **Step 4: Add the bulk operation ViewModel method**

Clear stale operation errors before launching. Validate the positive new rate and inclusive date range, call the repository, store returned originals, and set a localized success event. Keep the current month flow reactive through Room.

- [ ] **Step 5: Run focused and full JVM tests**

Run: `./gradlew testDebugUnitTest --tests com.worktime.app.ui.calendar.CalendarViewModelTest` and `./gradlew testDebugUnitTest`

Expected: PASS.

---

### Task 3: Recompose settings and implement the rate-period sheet

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/settings/SettingsSheet.kt`
- Create: `app/src/main/java/com/worktime/app/ui/settings/ChangeRateSheet.kt`
- Modify: `app/src/main/java/com/worktime/app/ui/WorkTimeApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ru/strings.xml`

- [ ] **Step 1: Add localized strings and UI test selectors**

Add labels for `Calculation`, `Appearance`, `Data and operations`, `Change rate for period`, `Current month`, `Custom period`, date fields, change action, confirmation, success, undo, and restoration failure. Keep export/backup absent until implemented.

- [ ] **Step 2: Build grouped settings UI**

Keep the existing default-rate editor and theme chips, but place them under `Calculation` and `Appearance` sections. Add a single `Change rate for period` row under `Data and operations`; tapping it dismisses settings and opens the new sheet.

- [ ] **Step 3: Implement the rate-period sheet**

Use the native Material date picker for custom start/end dates, default the current-month option to the visible month, use the existing money sanitization/parser, and keep the action disabled until the rate and range are valid. Do not show a preview or estimate. Require a concise confirmation dialog before writing.

- [ ] **Step 4: Wire save, dismiss, and error behavior**

On success dismiss the sheet and let the root Snackbar show `Rate changed` with `Undo`. On failure keep the sheet open and show the existing layout-neutral Snackbar pattern.

- [ ] **Step 5: Run lint and JVM tests**

Run: `./gradlew testDebugUnitTest lintDebug`

Expected: PASS with no new lint errors.

---

### Task 4: Add deletion and bulk-operation Snackbars with undo

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/WorkTimeApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ru/strings.xml`

- [ ] **Step 1: Add root Snackbar host**

Place one Snackbar host above the calendar scaffold so it survives sheet dismissal and can display operation results without changing sheet geometry.

- [ ] **Step 2: Wire the undo action**

Map successful delete and bulk update events to localized messages with action label `Undo`; call `undoLastOperation` only from the Snackbar action. Clear the event after it is consumed.

- [ ] **Step 3: Verify behavior with tests**

Run: `./gradlew testDebugUnitTest` and `./gradlew lintDebug`

Expected: PASS.

---

### Task 5: Improve calendar state visibility and month navigation

**Files:**
- Modify: `app/src/main/java/com/worktime/app/ui/calendar/CalendarScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-ru/strings.xml`
- Test: `app/src/test/java/com/worktime/app/ui/calendar/CalendarAmountVisibilityTest.kt`

- [ ] **Step 1: Add focused calendar assertions**

Extend tests for distinct semantic descriptions and the negative-result display rule; preserve the existing whole-number amount behavior.

- [ ] **Step 2: Improve day-cell non-color signals**

Keep the existing today border and bonus/penalty markers. Add a small accessible filled-entry glyph/check indicator that does not overlap the date, duration, amount, or adjustment markers. Ensure bonus-only and penalty-only records remain distinguishable through their existing markers and descriptions.

- [ ] **Step 3: Make the monthly amount primary**

Change the collapsed summary to give monthly income a stronger typography hierarchy while keeping work days and hours secondary. In the expanded report, emphasize the total and use error semantic color only when the final total is negative.

- [ ] **Step 4: Add the empty-calendar affordance**

When the visible month has no entries, show a compact non-blocking prompt near the summary with an action that opens today’s editor. Keep today’s cell border as the primary visual anchor and avoid a full-screen onboarding state.

- [ ] **Step 5: Add horizontal month swipe**

Attach a horizontal drag gesture only to the calendar content area. Trigger previous/next month after a clear horizontal threshold, ignore predominantly vertical drags, and leave the report sheet gesture untouched. Month state changes must call the existing immediate `previousMonth`/`nextMonth` callbacks without transition animation.

- [ ] **Step 6: Run tests and build checks**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`

Expected: PASS.

---

### Task 6: Verify the complete change

**Files:**
- Review: `docs/UX.md`, `docs/PRODUCT.md`, `docs/BACKLOG.md`

- [ ] **Step 1: Run repository verification**

Run: `./scripts/verify.sh`

Expected: static audit, JVM tests, lint, debug APK, and instrumentation APK assembly pass.

- [ ] **Step 2: Perform focused device checks**

Verify delete -> Snackbar -> Undo, bulk current-month change, custom date range, mixed-rate restoration, settings scrolling, negative totals, empty month prompt, today/entry/bonus/penalty visual states, and horizontal month swipe while the report sheet is collapsed and expanded.

- [ ] **Step 3: Update documentation**

Document the new settings structure, bulk-rate semantics, process-lifetime undo limitation, and calendar gesture in the existing product/UX/backlog docs. Do not document export/backup as implemented.
