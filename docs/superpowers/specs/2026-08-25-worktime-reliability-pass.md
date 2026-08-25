# WorkTime reliability and consistency pass

## Scope

Fix the listed concurrency, persistence, backup, widget, UI/runtime, build,
audit, dead-code, and documentation defects on the existing `main` HEAD.
Room, DataStore, Compose, and the ViewModel/repository boundaries remain in
place. No destructive migration or incompatible backup-format change is
introduced.

## Data and operation safety

- Serialize WorkEntry mutations in `CalendarViewModel`; generation checks may
  continue to suppress stale UI events but are not used as write protection.
- Expose key-level DataStore updates so theme and default rate edit only their
  own key atomically.
- Import validates the complete backup before mutation, snapshots current
  entries/preferences, applies the two stores, and restores the snapshot on a
  second-stage failure. A rollback failure is surfaced distinctly and pending
  import remains retryable.
- Entry save succeeds after Room succeeds; default-rate adoption is secondary
  and reports independently without converting a saved entry into a failed save.
- Export, parse-only import, and reads do not invalidate Undo; confirmed data
  replacement does.

## Runtime and validation

- File streams and serialization run off the main dispatcher, have ownership in
  the caller that opens them, and enforce a bounded backup size.
- Backup decoding rejects duplicate dates, unsupported versions, invalid
  dates/values/preferences, and malformed payloads with one invalid-backup
  error type.
- CSV uses HALF_UP monetary rounding.
- Widget observation follows the current `YearMonth` and switches at rollover.
- Year summaries use explicit data presence and open for the visible month year.

## UI and product consistency

- Preserve the current calendar/report visual direction while restoring monthly
  sheet drag anchors and avoiding duplicate handles.
- Keep essential values visible at normal scale and provide scrolling/fallback
  layout for large font scales rather than clipping important text.
- Remove confirmed dead code and stale resources only after repository-wide
  reference checks.

## Tooling and documentation

- Release signing is debug-safe but never silently presents a debug-signed
  artifact as production release; local/CI signing properties are optional.
- Pin the Gradle distribution checksum and immutable GitHub Actions revisions
  when workflows exist. Keep the flake as the sole Nix environment source.
- Static audit checks stable repository invariants only.
- README, product, architecture, testing, build, QA, release checklist, and
  changelog describe the resulting implementation and verification evidence.

## Verification

Run static audit, unit tests, lint, debug APK and androidTest assembly, plus
connected tests when an emulator is available. Device QA is reported only when
actually run.
