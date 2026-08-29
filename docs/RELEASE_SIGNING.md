# Release signing

WorkTime uses Google Play App Signing. The local private key is an **upload key** used to sign the AAB before upload; Google Play keeps the separate app-signing key used for APKs delivered to users.

The upload keystore and its passwords must never be committed to this repository or pasted into issue/PR logs. The project already reads signing inputs from Gradle properties or `RELEASE_*` environment variables and never falls back to debug signing.

## One-time upload key creation

Create the keystore locally from a trusted machine. The command below prompts for the keystore/key password instead of putting it in shell history:

```bash
mkdir -p "$HOME/.android/keys"
keytool -genkeypair -v \
  -keystore "$HOME/.android/keys/worktime-upload.jks" \
  -storetype JKS \
  -alias worktime-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Use a unique strong password and store it in a password manager. Keep at least two encrypted backups of `worktime-upload.jks` in separate locations. Do not keep the only copy on the development machine.

The certificate validity above is longer than 25 years, which is the minimum lifetime recommended by Android documentation for long-lived app updates.

## Export the public upload certificate

The public certificate can be registered with Google Play and is safe to share. It does not contain the private key:

```bash
keytool -export -rfc \
  -keystore "$HOME/.android/keys/worktime-upload.jks" \
  -alias worktime-upload \
  -file "$HOME/.android/keys/worktime-upload-certificate.pem"
```

After Play App Signing is configured, record the SHA-256 fingerprints for both:

- the **upload certificate** — identifies the key used for future uploads;
- the **app-signing certificate** — identifies APKs actually delivered by Google Play.

These are intentionally different when Google manages the app-signing key.

## Build a signed release candidate

Use the exact clean commit that passed CI. Set the path and alias normally; enter passwords interactively so they are not written to shell history:

```bash
export RELEASE_STORE_FILE="$HOME/.android/keys/worktime-upload.jks"
export RELEASE_KEY_ALIAS="worktime-upload"

read -rsp "Keystore password: " RELEASE_STORE_PASSWORD; export RELEASE_STORE_PASSWORD; echo
read -rsp "Key password: " RELEASE_KEY_PASSWORD; export RELEASE_KEY_PASSWORD; echo

./scripts/build_release_candidate.sh

unset RELEASE_STORE_PASSWORD RELEASE_KEY_PASSWORD
```

The script refuses a dirty working tree, runs the static audit and release lint, builds the optimized release AAB, verifies that the bundle is signed, and records:

- exact Git commit;
- `versionCode` / `versionName`;
- AAB SHA-256;
- signer certificate SHA-256;
- matching R8 mapping path.

Outputs:

```text
app/build/outputs/bundle/release/app-release.aab
app/build/outputs/mapping/release/mapping.txt
app/build/outputs/release-candidate/metadata.txt
```

Archive the AAB, mapping and metadata together. The upload keystore and passwords are **not** part of the release archive.

## Play App Signing setup

For the first Google Play release:

1. Create/select the WorkTime application in Play Console.
2. Use Play App Signing and let Google generate/manage the app-signing key unless there is a specific cross-store requirement for controlling the app-signing key yourself.
3. Upload the AAB signed with the WorkTime upload key.
4. Confirm the upload-certificate SHA-256 fingerprint shown by Play matches the fingerprint recorded by `build_release_candidate.sh`.
5. Install the build from the Internal Testing track and run the physical-device release checklist on that Play-delivered build.

For later updates, keep signing uploads with the same upload key. If the upload key is lost or compromised, Play App Signing supports an upload-key reset without changing the app-signing key used for existing users.

## CI signing smoke test

CI intentionally does **not** have the production upload key. The `signing-smoke` job creates a disposable keystore inside the GitHub runner, builds the same optimized `release` variant with the normal `RELEASE_*` inputs and verifies the resulting AAB with `jarsigner`.

That job proves that the signing plumbing works while keeping the real upload key outside GitHub. Its disposable signed AAB is never uploaded as a workflow artifact and must never be distributed.
