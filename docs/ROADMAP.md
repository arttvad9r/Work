# Roadmap

## Current — polished local product

Implemented:

- fixed 6 × 7 salary calendar with month arrows, swipe navigation and month picker;
- compact daily editing with stable persistent numeric input;
- default-rate initialization rules and per-entry rate snapshots;
- bonus/penalty adjustments and deterministic micros calculations;
- compact monthly summary plus detailed draggable month report;
- full-screen yearly summary;
- bulk rate change for current/custom periods with confirmation and Undo;
- light/dark Material 3 themes and a shared UI token/component system;
- JSON backup/import and CSV export;
- optional home-screen month-summary widget;
- portrait-only layout with focused physical-device IME/sheet stabilization work;
- shared Gradle-wrapper verification and documented QA/release process.

The current priority is no longer feature expansion. It is release verification and removal of regressions discovered on real hardware.

## Next — release hardening

- complete clean wrapper build/lint/test on the release candidate when dependency/runner access is available;
- complete focused physical-phone QA for editor IME, sheets, calendar, import/export, widget and year summary;
- verify supported large-font/narrow-screen accessibility behavior;
- verify process death/relaunch persistence paths;
- prepare signed internal/public release assets and final store metadata.

## Later, only after explicit product approval

- multiple jobs/work profiles;
- overtime or pay-period configuration.

Currency selection, notes, quick-duration presets, live timers, projects, landscape support and cloud accounts are not planned by default.
