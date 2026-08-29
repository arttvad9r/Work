# Release signing

WorkTime is distributed as a directly installable APK through GitHub Releases. There is no store-managed app-signing layer: the private key used here is the **actual app-signing key** trusted by Android for every installed release.

That key is a long-lived release identity. Every future APK intended to update an existing WorkTime installation must be signed with the same certificate. Losing the private key means existing installations cannot receive a normally installable update signed by a replacement key.

The keystore and passwords must never be committed to this repository or pasted into issue/PR logs. The project reads signing inputs from Gradle properties or `RELEASE_*` environment variables and never falls back to debug signing.

## One-time app-signing key creation

Create the keystore locally on a trusted machine. The command prompts for secrets instead of putting passwords in shell history:

```bash
mkdir -p "$HOME/.android/keys"
keytool -genkeypair -v \
  -keystore "$HOME/.android/keys/worktime-release.jks" \
  -storetype JKS \
  -alias worktime-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Use a unique strong password and keep it in a password manager. Keep at least two encrypted backups of `worktime-release.jks` in separate locations. Do not keep the only copy on the development machine or only in GitHub Actions.

Export and archive the public certificate and its SHA-256 fingerprint. The certificate is safe to publish; the private key is not:

```bash
keytool -export -rfc \
  -keystore "$HOME/.android/keys/worktime-release.jks" \
  -alias worktime-release \
  -file "$HOME/.android/keys/worktime-release-certificate.pem"

keytool -list -v \
  -keystore "$HOME/.android/keys/worktime-release.jks" \
  -alias worktime-release
```

Record the certificate SHA-256 fingerprint in the private release records. Compare it with the `signerSha256` emitted for every candidate.

## Build a signed release candidate locally

Use the exact clean commit that passed CI. Set the path and alias normally; enter passwords interactively:

```bash
export RELEASE_STORE_FILE="$HOME/.android/keys/worktime-release.jks"
export RELEASE_KEY_ALIAS="worktime-release"

read -rsp "Keystore password: " RELEASE_STORE_PASSWORD; export RELEASE_STORE_PASSWORD; echo
read -rsp "Key password: " RELEASE_KEY_PASSWORD; export RELEASE_KEY_PASSWORD; echo

./scripts/build_release_candidate.sh

unset RELEASE_STORE_PASSWORD RELEASE_KEY_PASSWORD
```

The script refuses a dirty working tree, runs the static audit and release lint, builds the optimized release APK, verifies it with Android `apksigner`, and records:

- exact Git commit;
- `versionCode` / `versionName`;
- APK SHA-256;
- signer certificate SHA-256;
- matching R8 mapping.

Distribution outputs are copied to:

```text
app/build/outputs/release-candidate/WorkTime-<version>.apk
app/build/outputs/release-candidate/SHA256SUMS.txt
app/build/outputs/release-candidate/metadata.txt
app/build/outputs/release-candidate/WorkTime-<version>-mapping.txt
```

The keystore and passwords are never part of the release output.

## GitHub Actions secrets

The tag release workflow can build the same signed APK in GitHub Actions. Configure these repository Actions secrets before creating the first release tag:

- `RELEASE_KEYSTORE_BASE64` — base64-encoded contents of `worktime-release.jks`;
- `RELEASE_STORE_PASSWORD`;
- `RELEASE_KEY_ALIAS`;
- `RELEASE_KEY_PASSWORD`.

Example local encoding on Linux:

```bash
base64 -w 0 "$HOME/.android/keys/worktime-release.jks"
```

On systems without `-w`, remove line breaks from the base64 output before storing it as the secret.

GitHub Actions is a convenience copy of the signing material, not the backup strategy. Keep independent encrypted backups outside GitHub.

## Release tags and GitHub Releases

A release tag must be named `v<versionName>`, for example `v0.1.0`, and must point to a commit contained in `main`.

Pushing such a tag runs `.github/workflows/release.yml`. The workflow:

1. verifies the tag matches `versionName` and points into `main`;
2. restores the signing keystore only inside the runner temporary directory;
3. runs `scripts/build_release_candidate.sh`;
4. creates a **draft GitHub Release** with the signed APK, SHA-256 file, release metadata and R8 mapping.

The release remains draft intentionally. Download that exact APK, install/update it on the target phone, complete the physical-device checklist, verify the checksum and signer fingerprint, then publish the draft release. Do not rebuild a different APK after QA.

## Update continuity

For every later release:

- increment `versionCode`;
- set the intended `versionName`;
- sign with the same WorkTime app-signing certificate;
- verify installation over the previous public APK without uninstalling;
- confirm Room/DataStore data and the home-screen widget survive the update.

Changing package name or signing certificate creates a different Android installation identity and is not a normal update path.

## CI signing smoke test

Normal PR/main CI intentionally uses a disposable signing key in `signing-smoke`. It exercises the same release signing configuration, `assembleRelease`, `apksigner`, metadata and checksum path without using the real WorkTime key. The disposable APK is never uploaded or distributed.
