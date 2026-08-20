# Privacy and data handling

## MVP privacy posture

WorkTime is designed as a local-first personal utility.

The MVP should require:

- no account;
- no contacts permission;
- no location permission;
- no microphone/camera permission;
- no advertising identifier;
- no background network access for core functionality.

## Stored data

Local data may include:

- work date;
- worked duration;
- effective hourly rate;
- bonus;
- penalty;
- optional user note;
- app preferences such as currency and theme.

These fields can be financially sensitive to the user even though they are not payment credentials. They should not be uploaded or logged by default.

## Logging

Production logs must not print full work entries, notes, rates or salary totals. Crash reporting, if added later, needs an explicit privacy review and a documented data-retention policy.

## Backup

Android backup behavior must be explicitly decided before production release. The choice affects the promise that data is local-only versus recoverable after device migration.

## Play Store readiness

Before public release:

- publish a privacy policy consistent with actual SDKs and permissions;
- complete the Google Play Data Safety form from the final dependency graph;
- verify no analytics/ad SDK was introduced indirectly;
- document backup/restore behavior for users.
