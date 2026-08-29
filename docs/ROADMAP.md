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
- current Gradle/JUnit/CI toolchain and shared wrapper verification;
- optimized release build, signing smoke and real managed-device instrumentation in CI;
- in-app privacy/data disclosure.

The current priority is release verification, not feature expansion. Automated build/test/lint belongs to every merge gate; remaining release confidence depends on physical-device interaction checks, permanent APK signing and GitHub Release packaging.

## Next — GitHub release verification

- create and securely back up the permanent WorkTime app-signing key;
- configure the four GitHub Actions release signing secrets;
- set final `versionCode` / `versionName` and tag the tested `main` commit as `v<versionName>`;
- let the tag workflow create a draft GitHub Release with signed optimized APK, checksum, metadata and R8 mapping;
- download that exact draft APK and verify fresh install plus update over the previous public APK;
- complete focused physical-phone QA for persistent IME behavior, haptics, sheets, calendar gestures, import/export and widget tap-through/layout;
- verify supported large-font/narrow-screen accessibility behavior and TalkBack semantics;
- verify process death/relaunch persistence paths;
- publish the existing draft GitHub Release only after the exact downloaded APK passes QA.

## Later, only after explicit product approval

- multiple jobs/work profiles;
- overtime or pay-period configuration.

Currency selection, notes, quick-duration presets, live timers, projects, landscape support and cloud accounts are not planned by default.
