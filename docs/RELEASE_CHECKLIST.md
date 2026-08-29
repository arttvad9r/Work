# Release checklist

Run this checklist against the exact commit that will be distributed. Prior verification from another commit is supporting evidence, not a substitute for the final candidate gate.

## Automated build gate

- [ ] `python3 scripts/static_audit.py` passes on the exact release candidate.
- [ ] `:app:testDebugUnitTest` passes with no failed or skipped regression tests.
- [ ] `:app:lintDebug` passes; any remaining hints are reviewed and understood.
- [ ] `:app:assembleDebug` and `:app:assembleDebugAndroidTest` pass through the checked-in Gradle Wrapper.
- [ ] The matching GitHub Actions run is green and retains verification-report/debug-APK artifacts.
- [ ] Release signing/configuration is prepared outside source control.

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
- [ ] JSON import rollback preserves the previous Room/DataStore state when preference restore is forced to fail.

## UI/accessibility

- [ ] App remains portrait-only.
- [ ] Calendar never vertically scrolls or jumps; horizontal month swipes track the finger and settle on the expected month.
- [ ] Russian labels fit, including `Системная` and `Отработано часов`.
- [ ] Invalid numeric input uses red outline only; no validation helper text appears.
- [ ] Numeric IME stays visible while moving between duration/rate/bonus/penalty.
- [ ] Expanding bonus/penalty does not close/reopen the keyboard or move the modal sheet.
- [ ] Intended haptics occur only on the documented interaction set; ordinary navigation remains silent.
- [ ] Settings initial `0` is selected on focus without changing sheet height.
- [ ] Save is reachable after keyboard dismissal.
- [ ] Narrow portrait screen and 200% font-scale passes complete.
- [ ] Light/dark contrast and TalkBack checks complete.
- [ ] Fixed `₽` and `₽/h` labels are readable in the supported locales.
- [ ] Home-screen widget theme, layout, body tap and `+` action are verified on the target launcher.
- [ ] Final launcher/store assets approved.

## Privacy/release

- [ ] No unnecessary permission, analytics or ad SDK.
- [ ] Backup/transfer behavior matches privacy docs.
- [ ] Final privacy policy and Play Data Safety form reviewed.
- [ ] Version code/name and release notes finalized.
- [ ] Internal testing and pre-launch report completed.

The merged feature set has automated coverage, but the release is not considered device-verified until a fresh run records the exact phone, Android version and release-candidate commit.
