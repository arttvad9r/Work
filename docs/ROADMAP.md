# Roadmap

## Current - interaction stability and cleanup

- [x] Fixed calendar geometry and adjacent-month context.
- [x] Compact fixed summary.
- [x] Separate draggable monthly report.
- [x] One-row duration/rate editor.
- [x] Stable bonus-above-penalty ordering.
- [x] Remove currency UI/model and redundant fractions.
- [x] Compact settings and controlled color palette.
- [x] Keep the product portrait-only.
- [x] Remove Material drag-handle tooltip path while preserving stable report measurement.
- [x] Replace frame-delayed editor focus with an explicit numeric IME focus chain.
- [x] Keep validation outline-only and persistence errors layout-neutral.
- [x] Enforce positive hourly rate for worked time in the domain model.
- [x] Use the checked-in Gradle Wrapper for local/CI verification.
- [x] Align implementation and QA documentation.
- [ ] Clean wrapper build/lint/test on current head.
- [ ] Focused physical-phone pass for report long-press/drag and IME transitions.

## Next - release hardening

- Complete accessibility and large-font QA in narrow portrait layouts.
- Verify process death, relaunch and Room/DataStore persistence.
- Add reliable Compose UI regression coverage for sheet gestures and editor focus where supported by the test environment.
- Resolve any supported-API AndroidX instrumentation incompatibility.
- Prepare signed internal-test build and final launcher/store assets.

## Later, only after explicit product approval

- export/backup;
- multiple jobs/rates;
- configurable week start;
- overtime/pay-period rules.

Notes, quick-duration presets, currency, validation helper text, live timers, projects, landscape support and cloud accounts are not planned by default.
