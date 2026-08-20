# Privacy and data handling

## MVP posture

WorkTime is a local-first personal financial/work utility.

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

## Backup

`android:allowBackup="false"` is intentional for v1. It prevents silently putting work-history data into platform cloud backup and matches the local-only promise.

Consequence: uninstalling the app removes local data. Manual export/backup is a later explicit-user-action candidate.

## Network/dependencies

No application Internet permission is required. Development-time Gradle dependency downloads and GitHub CI are not runtime app data transfers.

## Before Play release

- verify final manifest/dependency graph;
- verify no analytics/ad/transitive data-collection SDK was added;
- publish a privacy policy matching actual behavior;
- complete Google Play Data Safety from the release artifact;
- document local-only/uninstall behavior to users.
