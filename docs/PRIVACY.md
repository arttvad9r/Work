# Privacy and data handling

WorkTime is local-first. It does not require an account or internet connection for its core functions.

This document is both the repository privacy audit and the source-of-truth checklist for the Google Play Data Safety form. It is not itself the final public privacy-policy page: the public page still needs the approved developer identity/privacy contact and a stable public URL before release.

## Stored data

Room stores per-date worked minutes, hourly-rate snapshot, bonus and penalty. The schema also contains a legacy note column; the current UI does not expose notes.

DataStore stores the default hourly rate and selected theme. Currency is not collected or stored by the current model; an unused legacy preference key may remain on an upgraded installation.

These values are processed on-device. Work and earnings values can fall under Google Play user-data categories such as financial information when transmitted, but local-only access/processing is not treated as data collection for the Data Safety form.

## Network and permissions

- The application manifest does not request `INTERNET`, location, contacts, microphone or camera permission.
- No analytics or advertising SDK is part of the current dependency graph.
- The app does not automatically transmit work, earnings or settings data to the developer or a third party.
- Financial/work values must not be written to logs or error messages.

This must be rechecked from the exact final signed release dependency graph and merged manifest before every Play release. Adding a network/analytics/crash-reporting SDK can change the Data Safety declaration even if application code is unchanged.

## Backup, export and import

Android cloud backup and device-to-device transfer are disabled/excluded for app data by the manifest and extraction/backup rules. This behavior must still be checked on release devices because manufacturer behavior can differ.

JSON/CSV export is explicit and user-directed. The app writes to the destination selected by the user through the Android system document picker. If the user chooses another app or a cloud-backed provider, that transfer is initiated by the user and is not an automatic developer-side collection path. Google Play's Data Safety guidance exempts a third-party transfer from the "sharing" declaration when it is based on a specific user-initiated action and the user reasonably expects the transfer.

Import reads only the file explicitly selected by the user through the system picker.

## Deletion and retention

Users can delete individual entries in the app. Uninstalling the app removes local app data subject to Android platform behavior. Exported files are outside the app sandbox and must be removed separately from the destination selected by the user.

WorkTime does not create or host user accounts and does not keep a developer-side copy of work/earnings data, so the Google Play account-deletion workflow does not apply to the current product. If account creation or server-side storage is ever added, this section and the Play deletion declarations must be redesigned before release.

## Google Play Data Safety release matrix

The expected declaration for the current build is below. Treat it as a release candidate, not a permanent assertion: verify it against the exact signed artifact and all bundled SDKs before submitting or updating the Play form.

| Play question / area | Current WorkTime answer | Evidence / rationale |
| --- | --- | --- |
| Does the app collect user data? | No | User/work/earnings/settings data is processed locally and is not transmitted off-device by the app or bundled SDKs. Google Play defines collection as transmission off-device and excludes local-only processing. |
| Does the app share user data with third parties? | No | There is no automatic third-party transfer. JSON/CSV export is a specific user-initiated action through the system picker, which is covered by the user-initiated sharing exception when the user expects the transfer. |
| Account creation | No | WorkTime has no account/login model. |
| Account deletion requirement | Not applicable | There is no WorkTime account or developer-held account data. |
| Local data deletion | Available | Individual entries can be deleted; uninstall removes the app sandbox subject to Android behavior. |
| Cloud backup / device transfer | Disabled | `allowBackup=false` plus extraction/backup exclusions cover app-data domains. |
| Analytics / advertising | None | No analytics or advertising SDK is in the current dependency graph. |
| Automatic network transmission | None | No `INTERNET` permission is declared and no current SDK provides a collection path. |
| Encryption in transit | Not applicable to automatic collection | The current app does not automatically transmit user data. A user-chosen export destination is outside the app's automatic collection path. |

### Data-type sanity check

The app locally handles information that could map to Play data types if it were ever transmitted, especially:

- other financial information: earnings/rate/bonus/penalty values;
- app/user-generated content or activity: work-entry dates and durations, depending on the exact Play form taxonomy in effect at submission time.

Do **not** mark those types as collected solely because they exist on-device. If any future feature or SDK sends them off-device, the Data Safety form must be updated before that build is distributed.

## Public privacy-policy release gate

Google Play requires every app to provide a comprehensive privacy policy in Play Console and a privacy-policy link or text inside the app. Before production release, publish one governing policy page that:

- identifies the developer/entity shown on the store listing;
- provides an approved privacy contact or inquiry mechanism;
- explains what user/device data the app accesses, collects, uses and shares, including the parties involved;
- explains secure data handling where applicable;
- explains retention and deletion behavior;
- describes user-directed export/import accurately;
- is readable in a normal browser at an active public URL and is not a PDF-only document;
- is linked from the Play store listing and remains accessible from inside WorkTime.

PR #40 provides the in-app privacy/data disclosure without inventing the public URL or developer contact. Those two release values must be supplied and approved before the public policy can be finalized.

## Release verification

Before every Play submission:

1. Inspect the exact signed AAB dependency graph and merged manifest.
2. Confirm there is still no unexpected network/analytics/advertising/crash SDK collection.
3. Recheck backup and device-transfer rules.
4. Re-run the managed-device and physical-device release QA gates.
5. Compare the public privacy policy, in-app disclosure and Play Data Safety answers for consistency.
6. If any data leaves the device automatically, stop and redo the Data Safety matrix before upload.
