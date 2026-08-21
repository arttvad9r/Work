# Privacy and data handling

WorkTime is local-first. It does not require an account or internet connection for its core functions.

## Stored data

Room stores per-date worked minutes, hourly-rate snapshot, bonus and penalty. The schema also contains a legacy note column; the current UI does not expose notes.

DataStore stores the default hourly rate and selected theme. Currency is not collected or stored by the current model; an unused legacy preference key may remain on an upgraded installation.

## Network and permissions

- No analytics or advertising SDK is part of the current dependency graph.
- No internet, location, contacts, microphone or camera permission is required.
- Financial/work values must not be written to logs or error messages.

## Backup

The manifest and backup rules disable/exclude cloud backup and device-to-device transfer for app data. This behavior must be rechecked on release devices because manufacturer behavior can differ.

## Deletion

Users can delete individual entries in the app. Uninstalling the app removes local data subject to Android platform behavior.

The Play Data Safety declaration and public privacy policy must be reviewed again from the final signed dependency graph before release.
