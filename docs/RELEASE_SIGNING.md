# Release signing

WorkTime uses a developer-controlled Android app-signing key. The private key is the **actual update identity** trusted by Android for directly distributed APKs.

## Signing reset for 0.2.0+

The private key that signed the public GitHub release `0.1.0` has been lost. That key cannot be reconstructed from the APK, certificate, fingerprint or repository history.

As a result:

- APKs signed with a replacement key cannot update an installed `0.1.0` APK in place;
- the first RuStore release has not yet established a RuStore signing identity, so it can safely start with a newly generated key;
- WorkTime `0.2.0` and every later RuStore/GitHub build should use the **same new key** so cross-store update continuity is preserved from this point forward;
- anyone who installed `0.1.0` directly from GitHub must export a JSON backup before migrating, uninstall the old app, install the new-signed build, then import the JSON backup.

The legacy public certificate fingerprint is still present in `release/production-signing-cert-sha256.txt` until the replacement key is generated. Do not build or publish the `0.2.0` production candidate until that file has been replaced with the new public SHA-256 fingerprint and committed.

RuStore also documents that different signing certificates prevent normal Android updates. Because WorkTime has not been published in RuStore yet, no RuStore support-side certificate reset is needed for this first publication.

## Generate the replacement key once

Generate the private key **locally on a trusted machine**, not in ChatGPT, GitHub Actions, a public CI runner or the repository. The repository includes a helper that deliberately leaves the private key outside Git:

```bash
sh scripts/init_production_signing_key.sh
```

By default it creates:

```text
$HOME/.android/keys/worktime-release-v2.jks
$HOME/.android/keys/worktime-release-v2-certificate.pem
```

and updates only this public repository file:

```text
release/production-signing-cert-sha256.txt
```

The helper uses RSA 4096 and a long-lived certificate, prompts for secrets interactively, refuses to overwrite an existing keystore, exports the public certificate, and pins its SHA-256 fingerprint. The fingerprint is public information; the `.jks` file and passwords are not.

Immediately after generation:

1. Save the keystore password and key password in a password manager.
2. Create at least two encrypted backups of `worktime-release-v2.jks` in separate locations.
3. Verify one backup can be opened with `keytool -list -v`.
4. Commit only the changed `release/production-signing-cert-sha256.txt` file.
5. Never upload the private keystore or passwords to GitHub, RuStore descriptions, issue/PR comments or chat.

## Production signing identity

The public SHA-256 fingerprint of the active production signing certificate is pinned in:

```text
release/production-signing-cert-sha256.txt
```

`build_release_candidate.sh` compares every normal release candidate against that fingerprint and refuses an APK signed by another certificate. `create_github_release.sh` also verifies the candidate metadata against the pinned signer.

CI uses a disposable certificate only in the explicitly isolated `WORKTIME_SIGNING_SMOKE=1` path. That bypass is accepted only when `CI=true`, and its APK is never a production release artifact.

## Build a signed release candidate locally

After the replacement fingerprint is committed, build from the exact clean commit that passed CI:

```bash
export RELEASE_STORE_FILE="$HOME/.android/keys/worktime-release-v2.jks"
export RELEASE_KEY_ALIAS="worktime-release"

read -rsp "Keystore password: " RELEASE_STORE_PASSWORD; export RELEASE_STORE_PASSWORD; echo
read -rsp "Key password: " RELEASE_KEY_PASSWORD; export RELEASE_KEY_PASSWORD; echo

./scripts/build_release_candidate.sh

unset RELEASE_STORE_PASSWORD RELEASE_KEY_PASSWORD
```

The script refuses a dirty working tree, runs the static audit and release lint, builds the optimized release APK, verifies it with Android `apksigner`, checks the signer against the pinned production certificate, and records:

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

## RuStore first publication

For the first RuStore publication, upload the exact APK produced by the release-candidate process above. From `0.2.0` onward, keep the same new signing certificate for every RuStore update and for any direct GitHub APK release.

Using one new certificate across both channels means users who first install `0.2.0+` can move between newer RuStore and direct APK builds without a signature mismatch, provided package name and version ordering also remain compatible.

## Existing 0.1.0 direct installs

The lost legacy private key means there is no cryptographic path to an in-place update of the already signed `0.1.0` APK with the new certificate.

The supported migration is:

1. Open `0.1.0` and export a JSON backup.
2. Store the JSON file outside the app sandbox.
3. Uninstall WorkTime `0.1.0`.
4. Install WorkTime `0.2.0+` from RuStore or the new direct release.
5. Import the JSON backup.
6. Verify entries and settings before deleting the backup file.

This migration must be called out in any direct-release notes intended for existing `0.1.0` users.

## Why the permanent key stays out of GitHub

The real WorkTime key is more sensitive than an ordinary repository secret because Android update continuity depends permanently on it. Keeping it offline reduces the number of systems that can expose the release identity.

GitHub may contain the final signed APK, the public certificate/fingerprint and verification metadata. None of those reveal the private key.

## Update continuity from 0.2.0 onward

For every later release:

- increment `versionCode`;
- set the intended `versionName`;
- sign with the same replacement WorkTime certificate;
- verify installation over the previous `0.2.0+` public APK without uninstalling;
- confirm Room/DataStore data and the home-screen widget survive the update.

Changing package name or signing certificate again creates another Android installation identity and must not be done as a routine release step.
