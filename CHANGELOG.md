# Changelog

All notable changes are documented here.

## [Unreleased] - 2026-08-22

### Interaction stability

- Removed Material 3's tooltip-wrapped monthly-report handle slot and replaced it with a stable in-content handle that keeps tap and drag behavior without showing `Drag handle` / `Маркер перемещения` on long press.
- Kept the full report composed/measured while collapsed so sheet height and swipe anchors remain stable during repeated tap/drag cycles.
- Replaced the day editor's duplicated `String` + `TextFieldValue` ownership with Material 3 state-based `TextFieldState` and synchronous `InputTransformation`.
- New/zero-valued day-editor numeric fields start as empty editor text instead of mutating from `0` during focus. Duration sanitization removes leading zeroes defensively, so typing `12` into a new day cannot produce `01:2`.
- Restored normal `ModalBottomSheet` inset handling after the zero-inset experiment left the editor underneath the software keyboard.
- All day-editor numeric fields now share one identical decimal `KeyboardOptions`/`ImeAction.Next` configuration so switching fields does not change the OEM IME action-key layout.
- Removed the temporary Material 3 alpha override after it did not eliminate the keyboard rebuild on the tested physical device; Material 3 again follows the stable Compose BOM.
- Reworked settings-rate focus so an initial `0` is selected instead of temporarily replaced with an empty value.
- Removed the extra settings `imePadding` layer.
- Moved save/delete/settings persistence errors to transient Snackbar overlays so failures do not reflow modal content.
- Month navigation publishes the requested month immediately instead of waiting for the Room month flow; the month title and day-cell colors no longer crossfade, eliminating old/new month flashes.

### Product and domain consistency

- Numeric validation remains intentionally outline-only: invalid fields turn red without helper text.
- Removed obsolete validation-helper strings from EN/RU resources.
- Enforced the documented domain rule that worked time requires a positive hourly rate while preserving zero-rate bonus/penalty-only entries.
- Calendar day cells display rounded whole daily amounts without fractional digits so values fit the compact cell width.
- Calendar card is edge-to-edge horizontally, with 0.5 dp horizontal cell gutters.
- Day-cell layout now follows the compact reference geometry: bold date at top-right, daily income at bottom-left, larger worked-duration text in the center, and adjustment markers in the free top-left corner.
- Kept full fractional precision in calculations and up to two fractional digits in non-calendar amount displays.
- Confirmed portrait-only orientation as a product constraint and aligned QA documentation accordingly.

### Build and maintenance

- `scripts/verify.sh` and GitHub Actions use the checked-in Gradle Wrapper rather than a system Gradle binary.
- Extended the static audit with portrait/IME manifest checks and guards against tooltip, value-based editor input, focus-time text mutation, changing per-field IME configurations, delayed month animation and fractional calendar-amount regressions.
- Updated README, product/UX, testing, build, device QA, release and static-audit documentation to match the current application behavior and verification process.
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

Some intermediate 21 August and 22 August attempts to suppress the report-handle tooltip or stabilize IME focus/insets were later reverted or superseded. The current branch implementation above is authoritative.
