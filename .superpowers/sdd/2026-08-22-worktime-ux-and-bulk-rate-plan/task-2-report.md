# Task 2 Report

## Status

Implemented and committed the Calendar ViewModel undo state and bulk hourly-rate workflow.

## Files changed

- `app/src/main/java/com/worktime/app/ui/calendar/CalendarUiState.kt`
  - Added operation success state, bulk/undo error kinds, and `canUndo`.
- `app/src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt`
  - Added `changeRateForPeriod`, `undoLastOperation`, and one in-memory sealed undo snapshot.
  - Successful deletes and bulk updates close the editor/store originals and expose success results.
  - Undo restores the exact deleted entry or every returned original record and reports recoverable failures.
  - Invalid bulk ranges and non-positive rates fail before repository calls.
- `app/src/main/java/com/worktime/app/ui/WorkTimeApp.kt`
  - Added root-localized bulk-rate and undo error messages.
- `app/src/main/res/values/strings.xml`
  - Added bulk-rate and undo failure strings.
- `app/src/test/java/com/worktime/app/ui/calendar/CalendarViewModelTest.kt`
  - Added ViewModel coverage for exact delete undo, mixed-rate bulk undo, and failed/invalid operations.

## Tests

- `nix develop --command ./gradlew testDebugUnitTest --tests com.worktime.app.ui.calendar.CalendarViewModelTest`
  - Initial red run failed at compilation because the requested undo/bulk APIs did not exist.
  - Final focused run: `BUILD SUCCESSFUL`; 4 tests passed.
- `nix develop --command ./gradlew testDebugUnitTest`
  - Final full JVM run: `BUILD SUCCESSFUL`; 49 tests passed.

## Concerns

- Operation success results are retained in state until another calendar action clears them; Task 4 owns root Snackbar/event consumption.
- Undo is intentionally one-shot and in-memory; process death clears it by design.

## Reviewer Fixes

Implemented the requested fix round in the uncommitted worktree after the original Task 2 commit:

- Replaced sticky `operationResult` state with a buffered `Channel` exposed as `operationEvents`; consumers receive each root-level success/error once without replay.
- Added `WorkEntryRepository.restore(entries)` and a Room `@Transaction` restore path for exact original records. ViewModel undo clears the snapshot only after restore succeeds, so failures retain a retryable snapshot and cannot partially restore through sequential writes.
- Invalidated pending undo on save, settings update, delete start, and bulk-operation start. Successful empty bulk updates emit `NO_OP` and do not expose undo.
- Added focused coverage for one-shot events, failed delete, empty bulk no-op, save supersession, and failed restore retry behavior while preserving `CancellationException` propagation.

## Reviewer-Fix Tests

- `nix develop --command ./gradlew testDebugUnitTest --tests com.worktime.app.ui.calendar.CalendarViewModelTest`
  - `BUILD SUCCESSFUL`; 9 tests passed.
- `nix develop --command ./gradlew testDebugUnitTest`
  - `BUILD SUCCESSFUL`; 54 tests passed.

## Reviewer-Fix Concerns

- `operationEvents` is a single buffered channel intended for the root UI collector; multiple collectors compete for events, so Task 4 should collect it once at the app root.
- Undo remains in-memory and is cleared on process death by design.

## Scoped Review Fixes

- Removed the test-only `CalendarOperationEvent` hierarchy; ViewModel tests now assert against the production event type from `CalendarUiState.kt`.
- Made undo operation-scoped with a generation guard. Completion and failure from an in-flight older undo/delete/bulk operation can no longer clear or overwrite a newer snapshot or emit stale operation state.
- Added deterministic coroutine-test coverage for a blocked restore followed by a newer bulk operation, plus root event/error assertions while `selectedDate` is null.
- Kept `operationEvents` as the production root-collection contract using a buffered `Channel`/`receiveAsFlow`, with no replay.

## Scoped Review Tests

- `nix develop --command ./gradlew testDebugUnitTest --tests com.worktime.app.ui.calendar.CalendarViewModelTest`
  - `BUILD SUCCESSFUL`; 12 tests passed.
- `nix develop --command ./gradlew testDebugUnitTest`
  - `BUILD SUCCESSFUL`; 57 tests passed.

## Scoped Review Concerns

- The repository fake does not inject a failure after a write inside `restore`; the atomicity guarantee remains covered by the Room `@Transaction` implementation and existing live Room test infrastructure. No artificial post-write failure was added.
- `operationEvents` is a single root-consumer channel; Task 4 should collect it once at the app root.
