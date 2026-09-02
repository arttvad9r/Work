# Changelog

All notable product changes are documented here. Detailed intermediate implementation history remains available in Git.

## [Unreleased] — 2026-09-02

### Motion and interaction

- Calendar day cells now use one restrained custom touch-down tone only. Framework ripple and the persistent selected-cell fill were removed because their overlap with editor opening produced a visible flash; the closed calendar keeps selection as business/accessibility state rather than another broad color layer.
- Segmented controls keep the single moving capsule but now add a restrained rounded press state to the touched segment; the custom state replaces the deliberately disabled rectangular framework indication without leaving the control visually silent before release.
- Theme selection is now optimistic: the visible Material palette changes immediately on selection while the serialized DataStore write completes afterward. Failed persistence rolls the palette back to repository state and surfaces error feedback instead of making the user wait on storage or another app mutation.
- Calendar and Year Summary paging now share a softer pager spring and the same positional threshold. Year arrow navigation uses the same interruptible, velocity-preserving retargeting model as month arrows. Paging haptics were removed entirely after physical-device QA showed that feedback emitted from settled-page state arrives after the direct gesture and feels delayed/detached.
- Year Summary keeps its vertical hierarchy but reduces entry/exit travel to one-sixteenth of the viewport plus a short fade, so the screen reads as appearing from the monthly report rather than sliding a large distance.
- The compact monthly summary follows an upward drag with only a small direct vertical lift and springs back when the gesture is cancelled/insufficient. Normal Material click indication remains, while the previous extra scale and custom pressed-color layers were removed; the threshold haptic remains because it occurs at the actual finger-crossing event.
- Full-screen side navigation now uses short symmetric enter/exit travel instead of entering a small distance and leaving by a full screen width.
- Tappable sheet drag handles acknowledge touch-down with a restrained tone change instead of remaining visually silent.
- Added semantic `Reject` haptics for explicit calendar, settings-persistence and backup failures; ordinary navigation/day/field taps remain silent and successful persistence keeps the existing `Confirm` feedback.
- Reworked the motion system around a shared direct-feedback vocabulary and refresh-rate-independent timing: gesture motion follows the platform frame clock, compact positional controls use critically damped springs, and non-spatial feedback uses short real-time tiers rather than frame counts.
- The persistent day-editor numeric node still preserves one IME session but now relocates between fixed slots without visually travelling up and down the form; Bonus/Penalty presentation changes are atomic so their labels and values do not cross-fade through an empty frame.
- Calendar navigation uses a real horizontal pager so adjacent months follow the finger; video QA removed the extra page alpha/scale treatment and header/summary dimming that read as flicker in device recordings. Programmatic navigation remains interruptible instead of stacking repeated targets.
- The calendar title, `Fill today` action and compact monthly summary are derived from the same displayed pager state, so month labels, entries and totals do not expose stale-content crossfades.
- Day/month content does not cross-fade stale values between months, so the new page settles without old entries or summary text blinking through it.
- `Change rate for period` keeps one fixed two-row period geometry in both current-month and custom modes, so moving the segmented capsule no longer resizes the sheet body and shifts the action button.
- Modal sheets share one zero-tonal-elevation surface and restrained scrim so editor/settings sheets read as the same visual layer.
- AndroidX SplashScreen provides the launch transition without an artificial delay.

### Data integrity and reports

- Backup JSON v2 preserves the hidden `defaultRateInitialized` preference while remaining compatible with v1 backups.
- Import rollback restores both Room data and preference initialization state after partial replacement failures.
- Detailed month/year reports preserve non-zero fractional amounts while dense calendar cells and the compact summary retain whole-ruble presentation.
- Empty bulk-rate changes now report a no-op instead of closing silently.

### Widget and platform integration

- Widget Light/Dark follows the app theme preference while System remains resource-driven.
- Widget observation only stays active while widgets are installed; date/time/time-zone changes perform a cold refresh correctly after process death.
- Widget body opens the app and `+` opens today's editor without recreating an already-running activity or leaving the editor hidden behind another surface.
- The 4×1 widget now mirrors the app's lower monthly-summary strip: the same secondary-container family, 16 dp corner language, centered month, one compact `shifts · hours · income` line with whole-ruble formatting, and a small dedicated add action; the picker preview mirrors the real layout.
- Launcher resources and API-specific monochrome/adaptive icon variants were consolidated and lint intent is documented explicitly.

### Engineering and maintenance

- Strengthened EN/RU string/plural parity and repository static-audit coverage.
- Updated the build/test baseline to AGP 9.3.2, Gradle Wrapper 9.7.1, JUnit 6.1.3 and test-only `org.json` 20260814.
- Updated pinned GitHub Actions to Node 24-native releases while retaining immutable commit-SHA pinning.
- The full merge-candidate gate covers 104 JVM tests, lint, debug APK assembly and instrumentation APK assembly.
- Stabilized import-rollback tests by waiting for observable `viewModelScope` operation completion rather than assuming coroutine-test scheduler drain completes production work.
- Removed stale toolchain/product snapshot documents and redundant repository placeholder files; current behavior is documented from `main` and historical snapshots remain in Git history.
- Grouped routine Dependabot updates by ecosystem to avoid accumulating parallel one-package maintenance pull requests.

### Earlier UI polish and consistency

- Standardized compact UI rhythm around 48 dp interactive rows, 44 dp inline fields/segmented controls and primary actions of at least 52 dp.
- Unified typography, spacing, sheet titles, navigation/value rows and Material press feedback through the shared UI system.
- Calendar hierarchy prioritizes worked duration, uses a restrained accent for income, neutral populated-cell surfaces and quieter grid lines; today remains distinct while tapping a day transitions directly into the editor without leaving a persistent selected fill on the closed grid.
- Added the contextual `Fill today` action for the current month when today's entry is missing.
- Day-editor Bonus/Penalty collapsed rows use Add affordances instead of navigation chevrons; empty optional fields collapse again when focus leaves.
- Compact field borders, DatePicker styling and sheet spacing were refined without changing calculation/domain behavior.
- Dark theme received a readability pass with higher secondary-text/border contrast and clearer surface separation.

### Settings and rates

- Settings copy explicitly distinguishes `Default rate` from a per-entry rate.
- Preserved first-entry default-rate adoption: the first saved worked entry can initialize an uninitialized default; later per-day rates do not overwrite an initialized default.
- `Change rate for period` uses compact current/custom period controls, prevents end-before-start ranges through Material `SelectableDates`, and clears an end date made invalid by a later start date.
- Empty custom start/end values no longer render decorative dash placeholders.
- Confirmation and success copy explicitly describe a period rate change and state that the default rate remains unchanged.

### Reports

- Monthly report hierarchy was simplified around month income, shifts/hours, rate pay, adjustments and averages.
- `Year summary` is a normal navigation row from the monthly report.
- Year summary uses a compact year pager and clearer totals/month hierarchy.
- `By month`, `shifts · h` and `income` share one header line aligned with their data columns.
- Empty-year presentation stays compact instead of stretching meaningless empty content.

## Development baseline — 2026-08-21 to 2026-08-26

### Core product

- Rebuilt the app around a fixed portrait 6 × 7 salary calendar with adjacent-month context.
- Added compact day editing, monthly summary/report, full-screen settings and year summary.
- Added deterministic integer-micros income calculations with per-entry hourly-rate snapshots.
- Added first-entry default-rate adoption and bulk rate changes with Undo.
- Added JSON backup/import, CSV export and an optional home-screen month-summary widget.
- Removed currency selection, notes, quick-duration presets and validation helper text by product decision.

### Interaction stability

- Reworked numeric editing around one persistent state-based editable node/input session shared across duration/rate/bonus/penalty to avoid OEM keyboard rebuilds observed on physical hardware.
- Removed the framework drag-handle tooltip path while keeping stable report sheet measurement and anchors.
- Made persistence feedback layout-neutral through Snackbar/error overlays.
- Made month navigation spatial and consistent without stale-month flashes.

### Engineering

- Consolidated duplicated UI and operation helpers, introduced the shared `UI_SYSTEM.md` contract and standardized common rows/controls.
- Hardened import rollback and preference serialization.
- Standardized local/CI verification on the checked-in Gradle Wrapper.
- Added product, architecture, testing, Android QA, release and build documentation around the current local-first application.
