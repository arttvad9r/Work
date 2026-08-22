# Device QA report

## Status

The current `main` baseline has been exercised on physical hardware by the project owner and is considered functionally working. Earlier repository notes that claimed the compact interface had not been tested on real hardware are obsolete.

Exact device model, Android version and build/commit details for that owner verification were not recorded in the repository, so this document does not invent them.

Historical environment notes remain relevant:

- API 26 and API 35 instrumentation paths passed in an earlier environment.
- API 35 manual create/save/relaunch/edit/delete flow passed for the earlier baseline UI.
- API 37 Compose instrumentation was previously blocked inside Espresso by `InputManager.getInstance` compatibility behavior; this was not a confirmed production crash.
- NixOS may require compatible/FHS Android SDK tooling.

## Interaction-stability branch

Branch: `fix/interaction-stability-cleanup`

This branch changes two device-sensitive interaction paths:

- monthly-report handle composition/tap handling to remove Material's long-press drag-handle tooltip while preserving stable sheet measurement and swipe anchors;
- day-editor/settings focus and IME behavior to avoid an intermediate keyboard close/reopen and sheet jump.

It also moves persistence failures to transient Snackbar overlays, removes validation helper-text reflow, enforces the positive-rate domain invariant and cleans build/documentation drift.

## Required focused retest before merge

- hold the monthly report handle: no `Маркер перемещения`, `Drag handle` or empty tooltip surface appears;
- open/collapse the report repeatedly by tap and drag: peek height and anchors remain stable;
- move focus duration -> rate and back while the numeric keyboard is open: keyboard remains continuously visible;
- expand bonus and penalty from an already focused numeric field: focus transfers directly and the sheet does not jump;
- use IME Next through visible fields: no close/reopen flash;
- focus settings rate when it contains `0`: value is selected, sheet height does not change;
- invalid numeric input shows red outline only and no helper text;
- failed save/delete/settings persistence shows a transient localized Snackbar without resizing the sheet;
- create/edit/delete/relaunch persistence still works;
- Russian, dark theme, narrow portrait screen and increased font scale remain usable.

Use `ANDROID_QA.md` for the executable checklist and append exact device/Android/commit/results after the focused run.
