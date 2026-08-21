# Code audit — 21 August 2026

## Scope

This audit covers the latest implementation branch, chore/device-qa-baseline, including source inherited from feat/foundation-mvp, build configuration, verification scripts, tests, CI and repository documentation.

The audit combines source review with the repository's recorded verification evidence. A test-infrastructure failure is not treated as an application crash.

## Executive result

The core flow has no confirmed product crash or data-loss defect in the recorded API 35 manual run. The implementation is a coherent single-module offline MVP, with exact integer-micros salary arithmetic, Room persistence, DataStore preferences, localized resources and explicit backup rules.

Two implementation/documentation mismatches were found and fixed in this branch:

1. DayEditorSheet had lost the documented quick-hour chips and note input on the QA branch, although README/backlog still listed both as implemented. Quick entry and note editing were restored.
2. scripts/verify.sh and scripts/verify.ps1 required a system Gradle installation even though the repository now commits Gradle Wrapper 9.5.0. Both scripts now invoke the wrapper.

A third UI correctness issue was fixed:

3. Month navigation was available while a day/settings modal was open. That could leave an editor for one date over a different visible month. Previous/next month actions are now disabled while a modal is open.

## Findings that remain open

### P1 — API 37 Compose instrumentation blocker

WorkTimeSmokeTest fails on API 37 inside Espresso before the assertion with NoSuchMethodException: android.hardware.input.InputManager.getInstance. The same Room and Compose instrumentation tests pass on API 26 and API 35. The evidence points to AndroidX Test/Espresso compatibility, not a confirmed production-code failure.

Owner/action: isolate this in a dependency-only change and verify an API 37-compatible AndroidX Test/Compose test stack. Do not change product logic to work around the reflection failure.

### P2 — Manual coverage is incomplete

The recorded device run did not complete all edge cases: bonus-only entries, exact 24-hour entry, invalid/maximum money values, currency relabeling, full edit-and-save value change, TalkBack speech output and comprehensive 200% font-scale clipping review.

Owner/action: complete docs/ANDROID_QA.md on API 26/API 35 and a real device before beta.

### P2 — NixOS SDK tooling caveat

Generic downloaded adb/emulator binaries require an FHS runner on NixOS. The repository documents and wraps this with steam-run; this is environment tooling, not application behavior.

## Verification matrix

| Area | Result |
| --- | --- |
| Static audit | Passed |
| JVM unit tests | Passed |
| Android lint | Passed |
| Debug APK | Built |
| Instrumentation APK | Built |
| Room instrumentation | Passed on API 26/API 35 |
| Compose startup smoke | Passed on API 26/API 35 |
| API 37 Compose smoke | Blocked by AndroidX Test/Espresso reflection |
| API 35 manual core flow | Passed |
| Production release signing | Not configured |

## Audit conclusion

The code is suitable for continued MVP work and beta hardening, but not for public release until the remaining device/accessibility checks and the API 37 test-stack decision are documented and resolved. The release checklist remains the final authority.
