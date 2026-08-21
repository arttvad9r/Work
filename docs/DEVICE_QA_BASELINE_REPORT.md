# Device QA Baseline

## Current State

Device QA has been executed against Android API 26, API 35 and API 37 emulators. The core user flow passed on API 35: first launch, entry creation, exact salary calculation, persistence after process restart and deletion/recalculation. Instrumentation passed on API 26 and API 35.

API 37 is online and the application launches, but the Compose smoke test is blocked by an AndroidX Test/Espresso reflection failure. No product behavior bug was confirmed.

## Changes Made

- Preserved the existing QA report and aligned it with the required `Automated Tests`, `Manual QA` and `Bugs` sections.
- Added `scripts/adb.sh` as an infrastructure-only NixOS ADB wrapper.
- Added device-test setup and execution documentation.
- Added Android Test compatibility documentation without changing dependency versions.
- Added this baseline summary.

No business logic, UI behavior, architecture or application dependencies were changed.

## Files Added

- `scripts/adb.sh`
- `docs/ANDROID_DEVICE_TESTING.md`
- `docs/TEST_COMPATIBILITY.md`
- `docs/DEVICE_QA_BASELINE_REPORT.md`

## Files Modified

- `docs/DEVICE_QA_REPORT.md`

## Verification

- `./scripts/static_audit.py`: passed inside `nix develop`.
- `./scripts/adb.sh devices`: verified against the NixOS environment; the wrapper executes the available ADB command through `steam-run` when present.
- `./gradlew :app:testDebugUnitTest`: passed.
- `./gradlew :app:lintDebug`: passed.
- `./gradlew :app:assembleDebug`: passed.
- `./gradlew :app:assembleDebugAndroidTest`: passed.
- `./gradlew connectedDebugAndroidTest`: passed on API 26 and API 35; API 37 reached the device but failed in Espresso with BUG-001.

## Known Limitations

- API 37 Compose instrumentation remains blocked by `InputManager.getInstance` reflection incompatibility.
- TalkBack speech verification was unavailable because no accessibility service was enabled in the emulator.
- Generic downloaded SDK `adb` still requires FHS/`steam-run`; the wrapper documents and standardizes the workaround but does not alter SDK installation.
- Manual edge-case flows and full visual accessibility review remain separate follow-up work.

## Next Steps

1. Investigate a compatible AndroidX Test/Espresso version for API 37 without changing product code.
2. Keep API 26 and API 35 as required CI/device baselines.
3. Complete the remaining manual edge-case and TalkBack checks on an appropriate device image.
4. Re-run the full matrix after the test-framework compatibility decision.
