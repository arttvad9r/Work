# Changelog

All notable changes are documented here.

## [Unreleased] - 2026-08-22

### Year summary

- Settings gained a `Statistics` group with `Year summary`: a view-only sheet showing yearly income, work days, hours worked, average monthly income and average shift (averages cover only months that carry data), plus bonus/penalty totals when non-zero.
- A fixed twelve-month breakdown lists each month's day count, compact hours and income; empty months render dimmed. Years switch through arrows and default to the current year.

### Calendar readability and navigation

- Worked days now fill with `primaryContainer` and render their content in `onPrimaryContainer`, so a fully booked month keeps a clear figure-ground split against white free days; the selected day steps up to the full `primary` surface.
- Day-cell duration is bold on-container instead of low-contrast blue-on-pale-blue; daily amounts use `labelMedium` at full opacity; cell borders and the weekday divider gained contrast; the entry glyph keys off `onPrimaryContainer`.
- Tapping the month/year title opens a month picker dialog (year arrows plus a 3x4 month grid) that jumps directly to any month without repeated swiping.

### Home screen widget

- Added an optional 3x2 home-screen widget mirroring the fixed monthly summary (`Work days`, `Hours worked`, `Monthly income` as label-colon-value rows).
- The widget follows the app's light/dark primary-container palette and opens the main screen on tap.
- While the app process is alive the widget updates on every entry change; with the process dead the 30-minute system update tick keeps it current.

### Data export and import

- Settings `Data and operations` gained `Export data` and `Import data`: a versioned JSON file covering every entry (date, duration, hourly rate, bonus, penalty, note) plus the settings (default rate, theme).
- `Export data` asks for the format first — JSON backup or a spreadsheet-friendly CSV (`date,duration,hourly_rate,bonus,penalty,total`, dot-decimal amounts); export-only, import stays JSON.
- Import parses and validates the file first, then replaces all entries and settings atomically after an explicit confirmation dialog; malformed or unsupported files show a localized error without writing anything.
- Export/import streams are owned by the view model so a slow write cannot race stream close and fail the operation.

### Settings layout

- All settings rows share one visual recipe (~52 dp height, label left, value/control right); the oversized Material text fields were replaced by a compact 120x40 dp pill money input shared by the settings and change-rate sheets.
- `Change rate for period` moved into the `Calculation` group next to the default rate; the theme chips fill their section directly without a redundant inner caption.

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
- Saving the very first entry adopts its hourly rate as the settings default when no default exists; an existing default is never overwritten.
- Summary label-value rows render their labels with a trailing colon, matching the home-screen widget typography.

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
