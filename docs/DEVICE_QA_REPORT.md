# Device QA Report

## Environment

### Application

- Application ID: `com.worktime.app`
- Target SDK: 37
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Test date: 21 August 2026

### Devices

| Emulator | Android API | Result |
|---|---:|---|
| `WorkTimeApi26` | 26 / Android 8.0 | Connected, instrumentation passed |
| `WorkTimeApi35QA` | 35 / Android 15 | Connected, instrumentation passed; manual QA performed |
| `WorkTimeApi37QA` | 37 / Android 17 | Connected, instrumentation reached device but Compose smoke test failed in test infrastructure |

`adb devices -l` showed a connected emulator during device testing. The NixOS SDK required running the downloaded emulator through `steam-run`. A separate SDK installation was used for system images because the project Nix shell intentionally does not include system images.

## Automated Tests

### Instrumentation

```text
./gradlew connectedDebugAndroidTest --stacktrace
```

Results:

- API 26: 2 tests started, 2 finished successfully.
- API 35: 2 tests started, 2 finished successfully.
- API 37: 2 tests started, 1 test failed and the task failed.

The API 37 failure was:

```text
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []
at androidx.test.espresso.base.InputManagerEventInjectionStrategy.initialize
at androidx.test.espresso.Espresso.onIdle
```

This occurs inside Espresso/AndroidX Test before the Compose assertion and is a test infrastructure/API compatibility issue. It is not an application crash.

An initial API 37 run with the downloaded SDK's generic `adb` also failed with:

```text
Could not create ADB Bridge
ADB location: /home/artt/.android/sdk/platform-tools/adb
Could not determine adb version.
```

Rerunning with the Nix-provided dynamically linked `adb` removed this environment issue and exposed the Espresso failure above.

## Manual QA

### First-run and core flow

Executed on API 35:

1. Installed the debug APK.
2. Started `MainActivity`.
3. Confirmed no startup crash.
4. Opened the current month and selected the current day.
5. Created an entry using:
   - 8 hours / 480 minutes;
   - hourly rate `1000`;
   - bonus `500`;
   - penalty `100`.
6. Confirmed the calculated result:
   - base: `$8,000.00`;
   - bonus: `+$500.00`;
   - penalty: `-$100.00`;
   - total: `$8,400.00`;
   - hours: `8 h`;
   - shifts: `1`.
7. Confirmed the calendar cell showed `8 h`, bonus and penalty markers.
8. Force-stopped the process and relaunched the app.
9. Confirmed the entry and monthly summary were preserved.
10. Opened the existing entry and confirmed its stored values were loaded.
11. Deleted the entry and confirmed the delete confirmation dialog, then confirmed:
    - total reset;
    - hours reset to `0 h`;
    - shifts reset to `0`;
    - the calendar entry was cleared.

### UI and configuration checks

- Current month rendered on first launch.
- Calendar day nodes exposed full-date content descriptions.
- Previous/next month and Settings controls exposed content descriptions.
- Empty state rendered without crash and showed `Set hourly rate`.
- Dark mode was enabled and the app remained running.
- Font scale was set to 200%; the app remained running and key calendar/navigation nodes remained present in the accessibility hierarchy.
- Rotation to landscape was performed; the Compose hierarchy remained available and the app did not crash.
- `enabled_accessibility_services` returned `null`; TalkBack was not installed/enabled in the emulator, so real TalkBack speech verification was unavailable.

## Passed

- APK installation on API 35 and API 37.
- First launch without crash on API 35 and API 37.
- Instrumentation tests on API 26 and API 35.
- Room instrumentation path on API 26/API 35.
- Compose startup smoke test on API 26/API 35.
- Current-month calendar rendering.
- Day selection and editor opening.
- Create work entry.
- Exact salary calculation for the requested scenario.
- Calendar summary and cell update after save.
- Persistence after force-stop/relaunch.
- Existing entry values loaded in the editor.
- Delete confirmation and recalculation.
- Empty state.
- Basic dark-mode, 200% font-scale and rotation smoke checks without crash.
- Calendar accessibility content descriptions were present for date and navigation controls.

## Failed

### API 37 instrumentation

The `WorkTimeSmokeTest` failed before its assertion because Espresso attempted to reflectively call a removed/hidden API:

```text
NoSuchMethodException: android.hardware.input.InputManager.getInstance []
```

The Room test completed as part of the same run; the Compose smoke test was the failing test.

### Not completed manually

The following cases were not completed as manual device flows in this session:

- 0 minutes plus bonus;
- exactly 1440 minutes;
- negative input values;
- maximum/very large money values;
- currency change and relabeling warning;
- invalid currency input;
- fully verified edit-and-save value change;
- TalkBack speech output;
- visual clipping assessment at 200% font scale across all editor fields.

Existing JVM tests cover several domain and validation edge cases, but that does not replace device-level verification.

## Bugs

### BUG-001 — Compose instrumentation incompatible with API 37

- **Description:** The Compose startup smoke test cannot reach its assertion on API 37.
- **Steps to reproduce:**
  1. Start an API 37 emulator.
  2. Connect it through Nix-provided `adb`.
  3. Run `./gradlew connectedDebugAndroidTest --stacktrace`.
  4. Observe `WorkTimeSmokeTest.calendarShowsSettingsActionAfterStartup` failure.
- **Severity:** High for API 37 automated QA; not a confirmed production runtime failure.
- **Likely cause:** Current AndroidX Test/Espresso event-injection implementation reflects `android.hardware.input.InputManager.getInstance`, which is unavailable on API 37.
- **Recommended next step:** Review AndroidX Test/Compose UI test compatibility with API 37 and update only the test dependency or test configuration after confirming the supported version matrix. Do not change application logic as a response to this failure.

### BUG-002 — Generic SDK `adb` cannot run directly on NixOS

- **Description:** The downloaded SDK's `platform-tools/adb` is a dynamically linked generic Linux binary and cannot run directly on this NixOS host.
- **Steps to reproduce:**
  1. Set `ANDROID_HOME=/home/artt/.android/sdk`.
  2. Run `/home/artt/.android/sdk/platform-tools/adb devices` outside an FHS runner.
  3. Observe the NixOS dynamic-linker error.
- **Severity:** Medium, environment/tooling only.
- **Likely cause:** Generic Android SDK binaries require FHS compatibility on NixOS.
- **Recommended next step:** Document or standardize the FHS/`steam-run` wrapper for downloaded SDK tools, or use the Nix-provided SDK tools consistently.

No reproducible product behavior bug was confirmed during the executed core flow.

## Recommended Fixes

1. Treat BUG-001 as the immediate QA blocker for API 37 and investigate AndroidX Test/Espresso compatibility.
2. Keep application code unchanged until BUG-001 is isolated from production runtime behavior.
3. Standardize which SDK/ADB path is used by Gradle and emulator tooling on NixOS.
4. Complete the unexecuted edge-case and accessibility flows on a real or fully configured emulator.
5. Repeat the edit-and-save scenario with a value change after the API 37 test infrastructure issue is resolved.

No source files were modified as part of this QA run. This report was created without a commit, as requested.
