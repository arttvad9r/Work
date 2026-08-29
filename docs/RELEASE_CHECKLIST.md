# Release checklist

Run this checklist against the exact commit that will be distributed. Prior verification from another commit is supporting evidence, not a substitute for the final candidate gate.

## Automated build gate

- [ ] `python3 scripts/static_audit.py` passes on the exact release candidate.
- [ ] `:app:testDebugUnitTest` passes with no failed or skipped regression tests.
- [ ] `:app:lintDebug` and `:app:lintRelease` pass; any remaining hints are reviewed and understood.
- [ ] `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and `:app:bundleRelease` pass through the checked-in Gradle Wrapper.
- [ ] Release optimization remains enabled through AGP `optimization { enable = true }`.
- [ ] The matching GitHub Actions run is green, including `signing-smoke` and the managed-device instrumentation job.
- [ ] CI retains the unsigned release AAB and R8 mapping for inspection; neither is treated as a production artifact.
- [ ] CI signing uses only the disposable runner keystore; no production upload key or password is present in GitHub Actions.
- [ ] Release signing/configuration is prepared outside source control and never falls back to debug signing.

## Upload key

- [ ] A dedicated WorkTime upload keystore exists outside the repository.
- [ ] Keystore and key passwords are stored in a password manager, not in source files or shell scripts.
- [ ] At least two encrypted backups of the upload keystore exist in separate locations.
- [ ] The public upload certificate has been exported and its SHA-256 fingerprint recorded.
- [ ] Play App Signing is configured so the upload key and Play app-signing key are distinct unless a documented cross-store requirement says otherwise.

## Release artifact

- [ ] Final `versionCode` and `versionName` are set before building the candidate.
- [ ] The working tree is clean and the exact candidate commit matches the green CI run.
- [ ] The candidate is built with `./scripts/build_release_candidate.sh` using the production upload key.
- [ ] The script reports a verified AAB signature and records the signer SHA-256 fingerprint.
- [ ] The signer fingerprint matches the upload certificate registered in Play Console.
- [ ] The signed AAB is archived together with its commit SHA, release-candidate metadata and matching R8 mapping.
- [ ] The candidate is accepted by Play Internal Testing and installed from Play-generated APKs on a target phone.
- [ ] No code, resources, signing inputs or version metadata change after the final QA pass without producing a new candidate.

## Functional QA

- [ ] Create, edit, delete and relaunch are verified on a physical phone.
- [ ] Process death/relaunch preserves Room/DataStore state and returns to a valid UI state.
- [ ] Monthly calculation is manually reconciled.
- [ ] Historical rate snapshots are verified.
- [ ] Empty, populated, bonus and penalty months are verified.
- [ ] Worked time with zero rate is rejected; adjustment-only zero-rate entries remain valid.
- [ ] Report sheet opens by both tap and drag.
- [ ] Repeated report open/collapse cycles keep the same peek height and anchors.
- [ ] Holding the report handle never shows Material's drag-handle tooltip.
- [ ] Persistence failure path retains the draft/settings surface and shows transient feedback.
- [ ] JSON export/import is verified through the system document picker on the release build.
- [ ] JSON import rollback preserves the previous Room/DataStore state when preference restore is forced to fail.
- [ ] Install/update over the previous build preserves local data and widget behavior.

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
- [ ] Home-screen widget theme, layout, body tap, `+` action and refresh after data changes are verified on the target launcher.
- [ ] Widget refresh is verified across date/time/time-zone changes and process restart.
- [ ] Final launcher/store assets approved.

## Privacy/release

- [ ] Final release dependency graph contains no unnecessary permission, analytics or ad SDK.
- [ ] Backup/transfer behavior matches privacy docs on a release device.
- [ ] Final privacy policy and Play Data Safety form reviewed against the signed release candidate.
- [ ] Ads, target-audience and content-rating declarations are finalized in Play Console.
- [ ] Store description, screenshots and release notes are finalized.
- [ ] Internal testing and Play pre-launch report completed and reviewed.

The merged feature set has automated coverage, but the release is not considered device-verified until a fresh run records the exact phone, Android version, signed release-candidate commit and Play-delivered build.
