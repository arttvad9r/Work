# Build and CI

## Toolchain

- Java 17
- Gradle Wrapper 9.7.1
- Android Gradle Plugin 9.3.2
- Kotlin 2.4.10
- compile/target SDK 37
- Compose BOM 2026.08.00

Compose libraries, including Material 3, are resolved from the stable BOM. Do not add a Material 3 alpha override solely to work around editor IME behavior; the attempted alpha override did not eliminate keyboard rebuilding on the tested physical device and has been removed.

## Release optimization

The `release` build uses the AGP 9.3 optimization DSL:

```kotlin
optimization {
    enable = true
}
```

This enables R8 code optimization and optimized resource shrinking together. Project-specific keep rules remain in `app/proguard-rules.pro` and should be added only for demonstrated runtime/reflection requirements.

Release signing is opt-in through `releaseStoreFile`, `releaseStorePassword`, `releaseKeyAlias` and `releaseKeyPassword` Gradle properties or their `RELEASE_*` environment-variable equivalents. Without all four values the project intentionally produces an unsigned release bundle for verification; it never falls back to debug signing.

The production upload-key procedure is documented in `docs/RELEASE_SIGNING.md`. Keep the keystore and passwords outside source control. The repository helper `scripts/build_release_candidate.sh` builds and verifies the exact signed candidate without writing signing secrets to the project.

## Local verification

Use a compatible Android SDK/JDK installation and the checked-in wrapper:

```bash
./scripts/verify.sh
```

Equivalent tasks:

```bash
python3 scripts/static_audit.py
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:lintRelease
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:bundleRelease
```

Outputs:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/bundle/release/app-release.aab
app/build/outputs/mapping/release/
```

The release AAB is unsigned unless production signing inputs are supplied. Do not distribute the unsigned CI artifact.

For an actual signed candidate, configure the `RELEASE_*` inputs and run:

```bash
./scripts/build_release_candidate.sh
```

That helper requires a clean Git tree, verifies the AAB signature and writes release metadata under `app/build/outputs/release-candidate/`.

Do not substitute an arbitrary system Gradle version when recording verification evidence; the wrapper defines the project Gradle version.

## Host environment

Primary development is on Arch Linux with a system JDK 17 and Android SDK. The project has no distribution-specific build layer: any compatible Linux/macOS/Windows Android development environment is valid when the Java/Android SDK requirements are met.

Use the checked-in Gradle Wrapper for project builds and keep one compatible `adb` installation active during physical-device testing.

## CI

`.github/workflows/android.yml` runs for pull requests and pushes to `main`. The `verify` job runs the static audit, JVM tests, debug/release lint, debug APK/test APK assembly and optimized release AAB build. It uploads verification reports, the debug APK, the unsigned release AAB and R8 mapping as short-lived artifacts.

After `verify` succeeds, two independent jobs run:

- `signing-smoke` creates a disposable CI-only keystore, builds the optimized release variant through the normal `RELEASE_*` signing inputs and verifies the AAB signature with `jarsigner`. The disposable signed AAB is not uploaded or distributed.
- `instrumented-tests` executes the instrumentation suite on a Pixel 2 API 30 AOSP ATD Gradle Managed Device. KVM access and Android SDK licenses are configured explicitly on the hosted Linux runner.

The real production upload key is intentionally absent from GitHub Actions.

Third-party actions are pinned to immutable commit SHAs and use Node 24-native releases.

A red CI run must be classified from its actual logs rather than assumed to be infrastructure. Test synchronization should wait for observable operation completion instead of relying on `advanceUntilIdle()` when production work runs in `viewModelScope` outside the coroutine-test scheduler.

## Device verification

Automated verification does not replace physical-device QA for the interaction paths that depend on OEM behavior. Recheck at minimum:

- persistent numeric-editor/IME transitions;
- haptic feedback on the intentionally limited interaction set;
- modal-sheet tap/drag/insets;
- calendar gestures and contextual `Fill today`;
- system document picker import/export;
- home-screen widget refresh/tap-through and compact layout;
- launcher icon presentation after install/update;
- install/update and smoke-test of the exact signed, optimized release candidate.

Record exact device model, Android version and tested commit for a release candidate.
