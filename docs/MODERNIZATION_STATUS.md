# Android modernization status

This document records the repository-level modernization audit against the project Android 2026 standard.

## Status

- **Repository modernization:** complete.
- **Application/code baseline:** PR #95, `49d5bfa71e3d49a713377548c5bcf0378796d9ca`.
- **Post-merge Android CI:** run `33328647740`, completed successfully on that exact SHA.
- **Production release/device sign-off:** pending physical-device QA listed below.

The physical checks are release gates, not unfinished architecture/refactoring work. A later documentation-only commit may move `main`; the SHA above identifies the audited application/code baseline.

## Repository audit

| Area | Repository evidence | Status |
|---|---|---|
| Compose / state architecture | Compose UI, screen ViewModels/state holders, `StateFlow`, lifecycle-aware collection, repository/data boundaries | Complete |
| Data | Room for entries, DataStore for preferences, WorkManager only for persistent scheduling, manual constructor DI | Complete |
| Design system | Material 3 theme plus shared spacing/shape/component/motion contracts | Complete |
| Edge-to-edge / adaptive UI | Window-size-aware calendar layouts, compact-height handling, supporting pane, system-bar/IME inset handling; no orientation lock | Complete in implementation and automated coverage |
| Navigation / motion | Navigation 3 full-screen destinations, predictive-back integration, pager/gesture-driven month and year navigation, restrained shared motion rules | Complete in implementation and automated coverage |
| Accessibility | semantics, content descriptions, touch-target/static/UI coverage, large-font and accessibility test coverage | Automated portion complete; physical TalkBack pass pending |
| Tests | JVM/unit, Compose/device instrumentation, screenshot regression, lint/static audit, API 30 matrix, API 37 smoke | Complete for repository CI |
| Release optimization | optimized release build, R8, Baseline Profile and Startup Profile, signing smoke | Complete for repository CI |
| Performance pipeline | Baseline Profile generation/update workflow, Macrobenchmark module/workflow and thresholds | Pipeline complete; representative physical-device measurement pending |
| Security/privacy | no `INTERNET` permission, cleartext disabled, explicit exported surfaces, release-signing checks, dependency/security workflows, no production private key in Git | Complete for repository audit |

## Baseline Profile integrity

The release-consumed checked-in profiles are distinct:

- `baseline-prof.txt` blob: `87251a59d44b4d2488e9bb84b39fe9b6db16c7e8`
- `startup-prof.txt` blob: `4873b70ad8f3dcbf5dc82cf3871805b686db6bca`

The static modernization audit fails if the profiles become identical/stale or if required runtime calendar rules disappear from the baseline profile.

## Remaining physical release gates

These items cannot be established reliably by GitHub-hosted CI and emulators alone:

1. Walk the key flows with TalkBack on a physical device and verify focus/order/state announcements.
2. Verify Gboard/OEM numeric IME continuity during repeated duration/rate/bonus/penalty editing.
3. Verify the documented haptic set on hardware.
4. Exercise rotation, split/freeform resize and window-size changes on representative hardware while preserving drafts, selection and persisted state.
5. Install the exact signed release APK fresh and as an update over the previous public APK; verify data retention, checksum and signer.
6. Run Macrobenchmark against the exact release candidate on a representative physical device and retain startup/frame P95/P99 evidence.

Until those checks pass, the repository/refactoring work is complete but the application should not be described as fully device-verified or as having completed the complete release Definition of Done.

## Source-of-truth documents

- `ANDROID_QA.md` — manual interaction/accessibility checklist.
- `ANDROID_DEVICE_TESTING.md` — device and emulator setup/test matrix.
- `RELEASE_CHECKLIST.md` — exact release-candidate gate.
- `BACKLOG.md` — remaining physical release work.
- `ROADMAP.md` — current release state and future product work.
