# Roadmap

## Current — repository modernization complete, release/device QA remains

Implemented:

- fixed 6 × 7 salary calendar with month arrows, true horizontal swipe navigation and month picker;
- compact daily editing with one persistent numeric input session across duration/rate/bonus/penalty;
- restrained motion and haptics without decorative looping or bounce;
- default-rate initialization rules and per-entry rate snapshots;
- bonus/penalty adjustments and deterministic integer-micros calculations;
- compact monthly summary plus detailed draggable month report;
- full-screen yearly summary with horizontal year paging;
- bulk rate change for current/custom periods with confirmation and Undo;
- light/dark Material 3 themes and a shared UI token/component system;
- JSON backup/import with backward-compatible hidden preference state plus CSV export;
- optional home-screen month-summary widget with app-theme integration and direct today-entry action;
- phone-first adaptive layout: compact portrait remains the primary visual target, but rotation, resizable windows and wide-window supporting-pane layouts are part of the supported contract;
- current Gradle/JUnit/CI toolchain and shared wrapper verification;
- optimized release APK build, R8, distinct Baseline/Startup Profiles, disposable signing smoke and managed-device instrumentation in CI;
- API 30 full instrumentation matrix plus API 37 target-platform smoke;
- in-app privacy/data disclosure and repository security/static checks.

Repository modernization/refactoring is complete at the application/code baseline from PR #95 (`49d5bfa71e3d49a713377548c5bcf0378796d9ca`). The remaining work is release/device QA rather than another architecture or UI migration. See [`MODERNIZATION_STATUS.md`](MODERNIZATION_STATUS.md) for the audit boundary.

## Next — physical release verification

- build/sign the exact release candidate with the permanent WorkTime signing key and verify the pinned certificate fingerprint;
- create the matching tag and draft GitHub Release without rebuilding the candidate;
- download the exact draft APK and verify fresh install plus update over the previous public APK while preserving data;
- complete physical TalkBack checks for key flows, focus order and state descriptions;
- exercise the persistent numeric editor with Gboard/OEM IME through repeated duration/rate/bonus/penalty focus changes;
- verify the documented sparse haptic set on hardware;
- verify rotation, split/freeform resizing and wider-window behavior while preserving valid UI state and drafts;
- verify supported large-font/narrow-screen behavior, light/dark transitions, locales, import/export and widget interactions;
- run Macrobenchmark on a representative physical device against the exact release candidate and retain startup/frame P95/P99 evidence;
- publish the existing draft GitHub Release only after the exact downloaded APK passes `RELEASE_CHECKLIST.md`.

## Later, only after explicit product approval

- multiple jobs/work profiles;
- overtime or pay-period configuration.

Currency selection, notes, quick-duration presets, live timers, projects and cloud accounts are not planned by default. A separate landscape-only product mode is also not planned; adaptive rotation/window resizing is already part of the Android quality contract.
