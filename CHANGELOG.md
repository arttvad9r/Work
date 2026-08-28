# Changelog

All notable product changes are documented here. Detailed intermediate implementation history remains available in Git.

## [Unreleased] — 2026-08-28

### UI polish and consistency

- Standardized compact UI rhythm around 48 dp interactive rows, 44 dp inline fields/segmented controls and primary actions of at least 52 dp.
- Unified typography, spacing, sheet titles, navigation/value rows and Material press feedback through the shared UI system.
- Calendar hierarchy now prioritizes worked duration, uses a restrained accent for income, neutral populated-cell surfaces, quieter grid lines and separate selected/today states.
- Added the contextual `Fill today` action for the current month when today's entry is missing.
- Day-editor Bonus/Penalty collapsed rows now use Add affordances instead of navigation chevrons; empty optional fields collapse again when focus leaves.
- Compact field borders, DatePicker styling and sheet spacing were refined without changing calculation/domain behavior.
- Dark theme received a single readability pass: higher secondary-text/border contrast and clearer surface separation while keeping the light theme as the primary visual target.

### Settings and rates

- Settings copy now explicitly distinguishes `Default rate` from a per-entry rate.
- Preserved first-entry default-rate adoption: the first saved worked entry can initialize an uninitialized default; later per-day rates do not overwrite an initialized default.
- `Change rate for period` uses compact current/custom period controls, prevents end-before-start ranges through Material `SelectableDates`, and clears an end date made invalid by a later start date.
- Empty custom start/end values no longer render decorative dash placeholders.
- Confirmation and success copy explicitly describe a period rate change and state that the default rate remains unchanged.

### Reports

- Monthly report hierarchy was simplified around month income, shifts/hours, rate pay, adjustments and averages.
- `Year summary` is a normal navigation row from the monthly report.
- Year summary uses a compact year pager and clearer totals/month hierarchy.
- `By month`, `shifts · h` and `income` now share one header line aligned with their data columns.
- Empty-year presentation stays compact instead of stretching meaningless empty content.

### Data and platform integration

- JSON backup/import and CSV export remain the supported data-transfer paths.
- Home-screen summary widget remains optional and local.
- Experimental replacement launcher artwork was reverted; the pre-existing adaptive launcher assets remain the current product state.

### Documentation and maintenance

- Refreshed README, product, UX, UI/documentation index, build notes, roadmap and backlog to describe the current application rather than superseded experiments.
- Build documentation is host-neutral: Nix/FHS support is optional, while the checked-in Gradle Wrapper is authoritative on any compatible Android toolchain.
- Removed the duplicate documentation changelog in favor of this root changelog as the single canonical history.

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
- Made month navigation immediate, avoiding old/new month crossfades and stale-month flashes.

### Engineering

- Consolidated duplicated UI and operation helpers, introduced the shared `UI_SYSTEM.md` contract and standardized common rows/controls.
- Hardened import rollback and preference serialization.
- Standardized local/CI verification on the checked-in Gradle Wrapper.
- Added product, architecture, testing, Android QA, release and build documentation around the current local-first application.
