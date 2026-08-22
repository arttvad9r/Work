# Changelog

All notable changes are documented here.

## [Unreleased] - 2026-08-22

### Interaction stability

- Removed Material 3's tooltip-wrapped monthly-report handle slot and replaced it with a stable in-content handle that keeps tap and drag behavior without showing `Drag handle` / `Маркер перемещения` on long press.
- Kept the full report composed/measured while collapsed so sheet height and swipe anchors remain stable during repeated tap/drag cycles.
- Replaced the day editor's duplicated `String` + `TextFieldValue` ownership with Material 3 state-based `TextFieldState` and synchronous `InputTransformation`, so the active IME editing session is no longer recreated by callback/state synchronization on every edit.
- Numeric IME Next now relies on Compose's default focus traversal between stable text-field nodes; bonus/penalty expansion buttons cannot take keyboard focus before the newly created numeric field receives focus.
- A zero-valued numeric field clears when focused in the day editor. Duration sanitization also removes leading zeroes defensively, so typing `12` into a new day cannot produce `01:2`.
- Reworked settings-rate focus so an initial `0` is selected instead of temporarily replaced with an empty value.
- Removed the extra settings `imePadding` layer.
- Moved save/delete/settings persistence errors to transient Snackbar overlays so failures do not reflow modal content.
- Month navigation now publishes the requested month immediately instead of waiting for the Room month flow; the month title and day-cell colors no longer crossfade, eliminating old/new month flashes.

### Product and domain consistency

- Numeric validation remains intentionally outline-only: invalid fields turn red without helper text.
- Removed obsolete validation-helper strings from EN/RU resources.
- Enforced the documented domain rule that worked time requires a positive hourly rate while preserving zero-rate bonus/penalty-only entries.
- Calendar day cells now display rounded whole daily amounts without fractional digits so values fit the compact cell width.
- Kept full fractional precision in calculations and up to two fractional digits in non-calendar amount displays.
- Confirmed portrait-only orientation as a product constraint and aligned QA documentation accordingly.

### Build and maintenance

- `scripts/verify.sh` and GitHub Actions now use the checked-in Gradle Wrapper rather than a system Gradle binary.
- Extended the static audit with portrait/IME manifest checks and guards against tooltip, value-based editor input, delayed month animation and fractional calendar-amount regressions.
- Updated README, UX, testing, build, device QA, release and static-audit documentation to match the current application behavior and verification process.
- Documented that the `main` baseline has been exercised on physical hardware; GitHub Actions runner failures caused by account usage limits are tracked separately from application correctness.

## [Unreleased baseline] - 2026-08-21

### Interface

- Rebuilt the main screen around a fixed, non-scrolling 6 x 7 calendar.
- Added faint previous/next-month dates without allowing them to open the current-month editor.
- Centered duration and amount inside filled calendar cells.
- Replaced text markers with centered bonus/penalty markers.
- Added a fixed three-row monthly summary and a separate draggable report sheet.
- Made the day editor a compact one-screen form: duration and hourly rate share one row.
- Kept bonus above penalty in every expanded/collapsed state.
- Reduced the settings rate input to a compact field and made all theme labels fit.
- Added controlled light and dark palettes; dynamic device colors are disabled by default.

### Product simplification

- Removed currency from preferences, UI contracts, formatting, settings and reports.
- Removed redundant decimal zeroes from displayed amounts.
- Standardized duration output to `0`, whole hours, or `hours:minutes`.
- Kept notes and quick-duration presets intentionally absent.
- Removed the requirement message demanding hours, bonus or penalty; Save simply remains unavailable for an empty draft.

### Earlier fixes

- Updated tests after `formatMoneyMicros` was replaced by neutral `formatAmountMicros`.
- Restored the missing Compose `clickable` import in the calendar.
- Fixed three-digit duration entry so `530` becomes `5:30`.
- Replaced the calculation label `Base` / `База` with `At hourly rate` / `По ставке`.
- Bottom-anchored the compact summary above the report handle without changing calendar geometry.
- Added short transitions for month titles and calendar-cell states while removing height animation from editor controls.
- Matched numeric-field line height to the input typography so the caret no longer towers over text.
- Anchored calendar date, duration and amount independently for equal top/bottom spacing and true center alignment.
- Standardized current-month date weight and separated the expanded monthly report with a distinct surface color.

Some intermediate 21 August attempts to suppress the report-handle tooltip or stabilize IME focus were later reverted or superseded. The 22 August interaction-stability work above is the current implementation.
