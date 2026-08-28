# Prioritized backlog

## P0 — release verification

1. Run `./scripts/verify.sh` on the release candidate when local/CI dependency access is available.
2. Build and install the exact `main` candidate on the target portrait phone.
3. Create/edit/delete entries, relaunch the app and verify Room/DataStore persistence.
4. Move through duration/rate/bonus/penalty with the numeric IME open; confirm no keyboard close/reopen flash or sheet jump.
5. Repeatedly open/collapse the monthly report by tap and drag; confirm stable anchors and no drag-handle tooltip.
6. Verify `Fill today` appears only in the current month while today has no entry and disappears after saving.
7. Verify calendar selected/today/populated states in light and dark themes and confirm grid text remains readable.
8. Run `Change rate for period` for current month and a custom range; verify invalid end dates are blocked, confirmation copy is correct, the default rate stays unchanged and Undo restores original per-entry rates.
9. Confirm first-entry default-rate adoption on clean app data, then confirm a different rate on a later day does not overwrite the initialized default.
10. Export JSON and CSV; import the JSON backup and confirm entries/settings restore correctly. Confirm malformed import writes nothing.
11. Open Year summary from the monthly report, switch years and verify totals plus month-column alignment; verify an empty year remains compact.
12. Add the home-screen widget, change an entry and confirm refresh/tap-through behavior.
13. Check Russian and English locales, narrow portrait width and increased supported font scale.
14. Complete the remaining items in `ANDROID_QA.md` and `RELEASE_CHECKLIST.md`.

## P1 — release hardening

- Add reliable Compose instrumentation for report gestures and persistent-editor focus transitions where the test environment supports them.
- Verify process death around the latest in-memory Undo state and document the expected loss of Undo after process death.
- Capture final release screenshots.
- Record exact device model, Android version and tested commit for the release candidate.
- Review signing, launcher/store assets and Play pre-launch results before public distribution.

## P2 — future decisions

- multiple work profiles/jobs;
- overtime/pay-period configuration.

Do not reintroduce currency selection, notes, quick-duration presets, validation helper text, landscape support, cloud accounts or timers without an explicit product decision.
