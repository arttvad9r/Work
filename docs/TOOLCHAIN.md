# Android Toolchain Baseline

## Current versions

| Component | Version |
| --- | --- |
| JDK | 17 |
| Gradle Wrapper | 9.5.0 |
| Android Gradle Plugin | 9.3.1 |
| Kotlin Compose plugin | 2.4.10 |
| KSP | 2.3.10 |
| Compose BOM | 2026.08.00 |
| Room | 2.8.4 |
| compile/target SDK | 37 |

The project uses AGP 9 built-in Kotlin support and intentionally does not apply `org.jetbrains.kotlin.android`.

## Compatibility assessment

The baseline build, unit tests, lint and debug APK assembly succeeded with the versions above. No version was changed automatically. The current NixOS setup also uses an `aapt2` override through `steam-run`; Gradle reports this option as experimental.

The primary operational risk is environment drift: the wrapper pins Gradle 9.5.0, while a generic Nix `gradle_9` package may move independently. The Android SDK build tools are explicitly pinned to 37.0.0 in the project and development shell.

## Update policy

1. Update one toolchain family at a time.
2. Prefer the Gradle Wrapper over a system Gradle binary.
3. Review AGP, Gradle, Kotlin, KSP and Compose compatibility together.
4. Run static audit, JVM tests, lint, debug build and instrumentation APK compilation before merging.
5. Treat SDK/build-tools upgrades as separate changes from dependency upgrades.
6. Do not upgrade versions solely to remove warnings without a verified compatibility need.
