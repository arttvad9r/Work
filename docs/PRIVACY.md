# Privacy and data handling

## MVP posture

WorkTime is designed as a local-first personal financial/work utility.

The MVP requires:

- no account;
- no contacts permission;
- no location permission;
- no microphone/camera permission;
- no advertising identifier;
- no Internet/background network access for core functionality.

The current manifest declares no Internet or dangerous permissions. `scripts/static_audit.py` guards these assumptions.

## Stored local data

Room may contain:

- work date;
- worked duration;
- hourly-rate snapshot;
- bonus;
- penalty;
- optional note.

DataStore contains:

- default hourly rate;
- global ISO currency code;
- theme.

These values can be financially sensitive even though they are not payment credentials.

## Logging/error handling

Production code must not log work entries, notes, rates or salary totals. Current write-error handling exposes only generic operation state to UI, not exception content or user financial values.

Crash/analytics tooling, if ever added, requires a separate privacy review before inclusion.

## Backup and device transfer

`android:allowBackup="false"` remains set, but that flag alone is not sufficient to describe Android 12+ device-to-device behavior on every manufacturer.

The app therefore also defines explicit backup rules:

- `@xml/backup_rules` excludes all supported app-data domains for Android 11 and lower backup rules;
- `@xml/data_extraction_rules` excludes all supported app-data domains from cloud backup and Android device-to-device transfer on Android 12+.

This is the strongest configuration-level local-data posture available without introducing a custom transfer/backup subsystem. It must still be verified on the release target/device matrix because Android/OEM transfer behavior can evolve.

Cross-platform transfer to iOS is not configured because WorkTime has no corresponding iOS bundle/team mapping. If cross-platform support is ever introduced, it requires a separate privacy/data-model review.

Consequence of the intended v1 behavior: uninstalling the app removes the user's accessible local data. Manual export/backup is a later explicit-user-action candidate.

## Network/dependencies

No application Internet permission is required. Development-time Gradle dependency downloads and GitHub CI are not runtime app data transfers.

## Before Play release

- verify final manifest and backup rules on target Android versions;
- verify final dependency graph contains no analytics/ad/transitive data-collection SDK;
- publish a privacy policy matching actual behavior;
- complete Google Play Data Safety from the release artifact;
- document local storage/uninstall/backup behavior accurately.
