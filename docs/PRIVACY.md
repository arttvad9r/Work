# Privacy and data handling

WorkTime is local-first. It does not require an account or internet connection for its core functions.

This document is the repository privacy audit and the source of truth for the in-app `Privacy & data` disclosure. WorkTime is distributed through GitHub Releases, so store-specific Data Safety, advertising, target-audience and privacy-policy submission forms are not part of the release process.

## Stored data

Room stores per-date worked minutes, hourly-rate snapshot, bonus and penalty. The schema also contains a legacy note column; the current UI does not expose notes.

DataStore stores the default hourly rate and selected theme. Currency is not collected or stored by the current model; an unused legacy preference key may remain on an upgraded installation.

These values are processed on-device and are not automatically sent to the developer or a third party.

## Network and permissions

- The application manifest does not request `INTERNET`, location, contacts, microphone or camera permission.
- No analytics or advertising SDK is part of the current dependency graph.
- The app does not automatically transmit work, earnings or settings data.
- Financial/work values must not be written to logs or error messages.

Recheck these statements against the exact final release dependency graph and merged manifest before every public GitHub Release. Adding networking, analytics, crash reporting, advertising or another SDK can change the privacy model even if the visible application flow is unchanged.

## Backup, export and import

Android cloud backup and device-to-device transfer are disabled/excluded for app data by the manifest and extraction/backup rules. This behavior must still be checked on release devices because manufacturer behavior can differ.

JSON/CSV export is explicit and user-directed. The app writes to the destination selected by the user through the Android system document picker. If the user chooses another app or a cloud-backed provider, that transfer occurs because of the user's explicit destination choice; WorkTime does not automatically upload the file itself.

Import reads only the file explicitly selected by the user through the system picker.

## Deletion and retention

Users can delete individual entries in the app. Uninstalling the app removes its local app data subject to Android platform behavior. Exported files are outside the app sandbox and must be removed separately from the destination selected by the user.

WorkTime does not create or host user accounts and does not keep a developer-side copy of work/earnings data.

## Release privacy matrix

Treat these statements as release-time assertions, not permanent guarantees:

| Area | Current WorkTime behavior |
| --- | --- |
| Account/login | None |
| Automatic network transmission | None |
| Analytics | None |
| Advertising | None |
| Location/contacts/camera/microphone | Not requested |
| Work/earnings/settings data | Stored and processed locally |
| Cloud backup/device transfer | Disabled by app configuration |
| Export | Explicit user-directed JSON/CSV through system picker |
| Import | Explicit user-selected file through system picker |
| Local deletion | Individual entries can be deleted; uninstall removes app sandbox subject to Android behavior |

If any future feature or dependency automatically sends data off-device, update this document and the in-app disclosure before distributing that build.

## Release verification

Before every public GitHub Release:

1. Inspect the exact signed APK dependency graph and merged manifest.
2. Confirm there is no unexpected network, analytics, advertising or crash-reporting data path.
3. Recheck backup and device-transfer rules.
4. Re-run managed-device and physical-device release QA.
5. Compare the in-app Privacy & data disclosure with this document.
6. Confirm release assets and workflow logs contain no user data, signing secrets or other sensitive information.
