# Production Baseline Report

## Current State

WorkTime is a working single-module offline Android MVP. The baseline uses JDK 17, Gradle Wrapper 9.5.0, AGP 9.3.1, Kotlin Compose plugin 2.4.10, Compose BOM 2026.08.00, Room 2.8.4 and Android SDK 37.

The application architecture and product behavior remain unchanged: Compose UI feeds a ViewModel, domain rules and repository interfaces, then Room/SQLite and DataStore implementations.

## Changes Made

- Created a pinned Nix flake and generated `flake.lock`.
- Added `devShells.default` with JDK, Gradle, Kotlin, Android SDK/build tools, Git and Python.
- Switched `.envrc` to `use flake`; retained `shell.nix` for migration compatibility.
- Centralized existing Gradle/plugin/dependency versions in `gradle/libs.versions.toml`.
- Updated Gradle build files to consume the version catalog without changing dependency coordinates or application behavior.
- Updated the static audit to recognize the centralized Android test runner dependency.
- Documented baseline, Nix usage, toolchain policy and release readiness.
- Did not change UI, UX, domain rules or persistence behavior.

## Files Added

- `flake.nix`
- `flake.lock`
- `gradle/libs.versions.toml`
- `docs/BASELINE_STATUS.md`
- `docs/NIX.md`
- `docs/TOOLCHAIN.md`
- `docs/RELEASE_BASELINE.md`
- `docs/PRODUCTION_BASELINE_REPORT.md`

## Files Modified

- `.envrc`
- `.gitignore`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `scripts/static_audit.py`
- Existing migration files `shell.nix` and `.envrc` were retained rather than removed.

## Verification Results

### Initial baseline

- `git status`: passed; initial branch was `feat/foundation-mvp` with pre-existing local environment/build changes.
- `git branch`: passed.
- `./gradlew --version`: passed, Gradle 9.5.0 and JDK 17.
- `./gradlew tasks`: passed.
- `./gradlew :app:testDebugUnitTest`: passed.
- `./gradlew :app:lintDebug`: passed.
- `./gradlew :app:assembleDebug`: passed.

### Nix and dependency stages

- `nix flake lock`: passed; `nixpkgs` pinned in `flake.lock`.
- `nix flake check`: passed for the supported Linux flake output.
- `nix develop --command bash` with Java, Gradle and Python version checks: passed.
- `./gradlew dependencies`: passed.
- `./gradlew :app:dependencies`: passed.
- `./gradlew :app:assembleRelease`: passed.

### Final verification

- `./scripts/static_audit.py` inside `nix develop`: passed.
- `./gradlew :app:testDebugUnitTest`: passed.
- `./gradlew :app:lintDebug`: passed.
- `./gradlew :app:assembleDebug`: passed.
- `./gradlew :app:assembleDebugAndroidTest`: passed.

The build still reports the pre-existing experimental `aapt2FromMavenOverride` warning and packages two native libraries without stripping them. These are warnings, not build failures.

Secret/credential review found no tracked keystore, credential, private-key or typical API-key files. Production signing is not configured.

## Remaining Risks

- Connected Android tests still require an available device/emulator.
- Release output is unsigned for production distribution.
- `gradle_9` in the Nix shell is a moving package while the Wrapper pins Gradle 9.5.0; the Wrapper remains authoritative.
- `nix flake check` evaluates the supported host output; incompatible Darwin/ARM outputs are not validated on this host.
- No Play Store publishing or release credential management is configured.
- The existing `shell.nix` remains as a migration fallback and may drift from `flake.nix`.

## Next Recommended Steps

1. Run connected instrumentation and device QA on the supported Android matrix.
2. Establish protected release signing and verify a signed release artifact.
3. Decide when the transitional `shell.nix` can be retired.
4. Keep toolchain and dependency updates isolated, reviewable and one family at a time.
