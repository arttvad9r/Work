# Release checklist

## Build

- [ ] Static audit passes on current head.
- [ ] JVM tests pass on current head.
- [ ] Android lint passes on current head.
- [ ] Debug APK and instrumentation APK assemble through the checked-in Gradle Wrapper.
- [ ] Release signing/configuration is prepared outside source control.
- [ ] If GitHub Actions cannot start because of account usage limits, record that separately and retain local wrapper output instead of treating the empty run as a build result.

## Functional QA

- [ ] Create, edit, delete and relaunch are verified on a physical phone.
- [ ] Monthly calculation is manually reconciled.
- [ ] Historical rate snapshots are verified.
- [ ] Empty, populated, bonus and penalty months are verified.
- [ ] Worked time with zero rate is rejected; adjustment-only zero-rate entries remain valid.
- [ ] Report sheet opens by both tap and drag.
- [ ] Repeated report open/collapse cycles keep the same peek height and anchors.
- [ ] Holding the report handle never shows Material's drag-handle tooltip.
- [ ] Persistence failure path retains the draft/settings surface and shows transient feedback.

## UI/accessibility

- [ ] App remains portrait-only.
- [ ] Calendar never scrolls or jumps.
- [ ] Russian labels fit, including `Системная` and `Отработано часов`.
- [ ] Invalid numeric input uses red outline only; no validation helper text appears.
- [ ] Numeric IME stays visible while moving between editor fields.
- [ ] Expanding bonus/penalty does not close/reopen the keyboard or move the modal sheet.
- [ ] Settings initial `0` is selected on focus without changing sheet height.
- [ ] Save is reachable after keyboard dismissal.
- [ ] Narrow portrait screen and 200% font-scale passes complete.
- [ ] Light/dark contrast and TalkBack checks complete.
- [ ] No currency text/symbol appears.
- [ ] Final launcher/store assets approved.

## Privacy/release

- [ ] No unnecessary permission, analytics or ad SDK.
- [ ] Backup/transfer behavior matches privacy docs.
- [ ] Final privacy policy and Play Data Safety form reviewed.
- [ ] Version code/name and release notes finalized.
- [ ] Internal testing and pre-launch report completed.

The `main` baseline has already been exercised on physical hardware. Any branch that changes IME, insets or sheet gestures still requires the focused device checks above before merge/release.
