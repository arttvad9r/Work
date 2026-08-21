# Release checklist

## Build

- [ ] Static audit passes on current head.
- [ ] JVM tests pass on current head.
- [ ] Android lint passes on current head.
- [ ] Debug APK and instrumentation APK assemble.
- [ ] Release signing/configuration is prepared outside source control.

## Functional QA

- [ ] Create, edit, delete and relaunch are verified on a physical phone.
- [ ] Monthly calculation is manually reconciled.
- [ ] Historical rate snapshots are verified.
- [ ] Empty, populated, bonus and penalty months are verified.
- [ ] Report sheet opens by both tap and drag.
- [ ] Persistence failure path retains the draft.

## UI/accessibility

- [ ] Calendar never scrolls or jumps.
- [ ] Russian labels fit, including `Системная` and `Отработано часов`.
- [ ] Keyboard does not hide Save after dismissal.
- [ ] Narrow-screen and 200% font-scale passes complete.
- [ ] Light/dark contrast and TalkBack checks complete.
- [ ] No currency text/symbol appears.
- [ ] Final launcher/store assets approved.

## Privacy/release

- [ ] No unnecessary permission, analytics or ad SDK.
- [ ] Backup/transfer behavior matches privacy docs.
- [ ] Final privacy policy and Play Data Safety form reviewed.
- [ ] Version code/name and release notes finalized.
- [ ] Internal testing and pre-launch report completed.

The current draft PR is not release-ready while build/device evidence remains missing.
