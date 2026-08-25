# Nix Development Environment

## Entering the environment

The preferred command is:

```bash
nix develop
```

`flake.nix` is the only supported source of the development shell. `.envrc` continues to enter it for existing direnv-based setups.

## Provided tools

The default dev shell provides:

- JDK 17;
- Gradle 9 series;
- Kotlin;
- Android SDK platforms 35 and 37;
- Android build tools 37.0.0;
- Android emulator tooling without system images;
- Git;
- Python 3 for repository scripts;
- zip/unzip;
- `steam-run` and an FHS-compatible `aapt2` wrapper required by the current NixOS build setup.

`ANDROID_HOME`, `ANDROID_SDK_ROOT`, `JAVA_HOME` and the Android build-tools path are configured by the shell.

## Updating the lock file

Update inputs from the repository root with:

```bash
nix flake lock --update-input nixpkgs
```

Review the resulting `flake.lock` and run:

```bash
nix flake check
nix develop --command bash
```

Do not update the lock file as part of an unrelated application change.
