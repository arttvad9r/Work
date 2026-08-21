# Release checklist

## Code/build

- [x] Complete Gradle Wrapper bootstrap is committed from a trusted environment.
- [x] `scripts/static_audit.py` passes.
- [x] JVM unit tests pass.
- [x] Android lint passes with reviewed output.
- [x] Debug APK builds.
- [x] Instrumentation APK builds.
- [x] Room v1 schema JSON is generated and committed.
- [x] No destructive Room migration fallback exists.
- [ ] Release build/signing configuration is prepared outside source control.

## Functional QA

- [ ] `ANDROID_QA.md` completed on required device/API matrix (API 26/API 35 automated/core flow pass; API 37 Compose blocker remains).
- [ ] Golden salary cases manually reconciled.
- [ ] Persistence/relaunch confirmed.
- [ ] Currency no-FX semantics confirmed in UI/product copy.
- [x] Error paths do not discard drafts (source review and recorded behavior).

## Accessibility/UI

- [ ] TalkBack audit complete.
- [ ] 200% font-scale pass complete.
- [ ] Small-screen pass complete.
- [ ] Light/dark/dynamic-color pass complete.
- [ ] Final adaptive launcher icon and store graphics approved.

## Privacy/security

- [ ] Manifest contains no unneeded permissions.
- [ ] No analytics/ad SDK is present unless separately reviewed.
- [ ] Financial entries/notes are not written to logs.
- [ ] Backup behavior matches privacy documentation.
- [ ] Privacy policy matches final dependency graph and behavior.
- [ ] Google Play Data Safety form completed from the release build.

## Play release

- [ ] Version code/name finalized.
- [ ] Release notes written.
- [ ] Screenshots captured from final UI.
- [ ] Internal testing release completed.
- [ ] Pre-launch report reviewed.
- [ ] Closed beta feedback triaged before production rollout.

## Current blockers

- API 37 Compose instrumentation is blocked by AndroidX Test/Espresso reflection incompatibility; see `docs/CODE_AUDIT.md`.
- Manual edge-case, TalkBack and complete 200% font-scale coverage is incomplete.
- Production signing and final store assets are not configured.

A release is not considered ready while any P0/release-gate item remains unchecked without an explicitly documented decision.
