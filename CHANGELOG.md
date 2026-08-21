# Changelog

All notable changes are documented here.

## [Unreleased] - 2026-08-21

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

### Fixed

- Updated tests after `formatMoneyMicros` was replaced by neutral `formatAmountMicros`.
- Restored the missing Compose `clickable` import in the calendar.
- Fixed three-digit duration entry so `530` becomes `5:30`.
- Initial zero values now clear on focus in duration and amount fields, preventing values such as `0150`.
- Replaced the calculation label `Base` / `База` with `At hourly rate` / `По ставке`.
- Made the monthly report content disappear immediately when collapse starts.
- The monthly report now closes before month navigation, day editing or settings opens.
- Confined report-handle press feedback to the handle and removed the full-width flash/tooltip behavior.
- Bottom-anchored the compact summary above the report handle without changing calendar geometry.
- Added short transitions for month titles, calendar-cell states and dynamic editor controls.
- Matched numeric-field line height to the input typography so the caret no longer towers over text.

### Verification status

- EN/RU resources parse and contain matching keys.
- Targeted source checks found no remaining currency UI/model references.
- GitHub Actions for the current iteration terminated before any workflow step started; it is not evidence of a compile or test result.
- A fresh APK build, installation and device QA pass are still required.
