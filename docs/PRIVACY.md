# Privacy and data handling

## MVP privacy posture

WorkTime is designed as a local-first personal utility.

The MVP requires:

- no account;
- no contacts permission;
- no location permission;
- no microphone/camera permission;
- no advertising identifier;
- no background network access for core functionality.

The current manifest requests no dangerous permissions and declares no Internet permission.

## Stored data

Local data may include:

- work date;
- worked duration;
- effective hourly rate snapshot;
- bonus;
- penalty;
- optional user note;
- app preferences such as default rate, currency and theme.

These fields can be financially sensitive to the user even though they are not payment credentials. They are not uploaded or logged by the application.

## Logging

Production logs must not print full work entries, notes, rates or salary totals. Crash reporting, if added later, needs an explicit privacy review and a documented data-retention policy.

## Backup

Android cloud backup is disabled for v1 with `android:allowBackup="false"`. This keeps the implementation consistent with the local-only product promise and avoids silently copying financial work-history data to a cloud backup provider.

Manual export/backup can be added later as an explicit user action.

## Play Store readiness

Before public release:

- publish a privacy policy consistent with actual SDKs and permissions;
- complete the Google Play Data Safety form from the final dependency graph;
- verify no analytics/ad SDK was introduced indirectly;
- document the fact that uninstalling the app removes local data unless a future manual export feature is used.
