# Roadmap

## Current — release candidate hardening

Implemented:

- fixed 6 × 7 salary calendar with month arrows, true horizontal swipe navigation and month picker;
- compact daily editing with one persistent numeric input session across duration/rate/bonus/penalty;
- restrained motion and haptics without decorative looping or bounce;
- default-rate initialization rules and per-entry rate snapshots;
- bonus/penalty adjustments and deterministic integer-micros calculations;
- compact monthly summary plus detailed draggable month report;
- full-screen yearly summary;
- bulk rate change for current/custom periods with confirmation and Undo;
- light/dark Material 3 themes and a shared UI token/component system;
- JSON backup/import with backward-compatible hidden preference state plus CSV export;
- optional home-screen month-summary widget with app-theme integration and direct today-entry action;
- portrait-only layout with focused OEM IME/sheet stabilization work;
- current Gradle/JUnit/CI toolchain and shared wrapper verification.

The current priority is release verification, not feature expansion. Automated build/test/lint belongs to every merge gate; remaining release confidence depends on physical-device interaction checks and release packaging.

## Next — release verification

- install the exact `main` candidate on the target portrait phone and record device model, Android version and commit;
- complete focused physical-phone QA for persistent IME behavior, haptics, sheets, calendar gestures, import/export and widget tap-through/layout;
- verify supported large-font/narrow-screen accessibility behavior and TalkBack semantics;
- verify process death/relaunch persistence paths;
- prepare signed internal/public release artifacts, screenshots, store metadata and privacy declarations.

## Later, only after explicit product approval

- multiple jobs/work profiles;
- overtime or pay-period configuration.

Currency selection, notes, quick-duration presets, live timers, projects, landscape support and cloud accounts are not planned by default.
