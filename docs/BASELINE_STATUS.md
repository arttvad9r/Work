# WorkTime Baseline Status

## Toolchain

| Component | Version | Source |
| --- | --- | --- |
| Gradle | 9.5.0 | Gradle Wrapper |
| Android Gradle Plugin | 9.3.1 | `build.gradle.kts` |
| Kotlin/Compose plugin | 2.4.10 | version catalog |
| Compose | BOM `2026.08.00` | version catalog |
| Room | 2.8.4 | version catalog |
| compile/target SDK | 37 | `app/build.gradle.kts` |
| min SDK | 26 | `app/build.gradle.kts` |
| JDK | 17 | local baseline and CI |

## Initial verification

The following commands were run before production-baseline changes:

- `git status --short --branch`: passed; initial branch was `feat/foundation-mvp` with pre-existing local changes in `.gitignore`, `app/build.gradle.kts`, `.envrc`, and `shell.nix`.
- `git branch`: passed.
- `./gradlew --version`: passed; Gradle 9.5.0, JDK 17.
- `./gradlew tasks`: passed.
- `./gradlew :app:testDebugUnitTest`: passed.
- `./gradlew :app:lintDebug`: passed.
- `./gradlew :app:assembleDebug`: passed.

Gradle reported the existing experimental `android.aapt2FromMavenOverride` option.

## Known limitations

- Connected Android tests require an available device/emulator.
- Release signing and store publishing are not configured.
- The application is a single offline Android module; no backend or web frontend exists.
- Nix currently uses `shell.nix`; the reproducible flake is introduced by this baseline change.

## Current architecture

```text
Compose UI
    ↓
CalendarViewModel
    ↓
Domain models, calculations and repository interfaces
    ↓
Room/SQLite work entries + DataStore preferences
```

The project remains single-module. Domain code is separated from UI and data implementations; business behavior and UI behavior are unchanged by this baseline work.
