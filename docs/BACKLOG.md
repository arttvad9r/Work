# Prioritized backlog

## P0 - before merge/release

1. Run `./scripts/verify.sh` on the current branch head.
2. Build and install the APK on the target portrait phone.
3. Hold the monthly-report handle and confirm no `Маркер перемещения` / `Drag handle` tooltip appears.
4. Exercise repeated report tap/drag open-collapse cycles and confirm stable peek height/anchors.
5. Move through duration/rate/bonus/penalty with the numeric IME open and confirm there is no close/reopen flash or sheet jump.
6. Verify outline-only validation and persistence-error Snackbars in editor/settings.
7. Complete the remaining calendar/editor/settings checklist in `ANDROID_QA.md`.
8. Confirm no clipping in Russian locale, narrow portrait layout and increased font scale.

## P1 - release hardening

- Add Compose instrumentation coverage for report-handle tap/drag/semantics where the test environment can reliably exercise current Material 3.
- Add editor UI coverage for focus transfer and bonus/penalty expansion order.
- Capture final screenshots after focused device QA.
- Record exact device model, Android version and tested commit in `DEVICE_QA_REPORT.md`.
- Review launcher icon, signing and Play pre-launch report.

## P2 - future decisions

- export/backup;
- multiple work profiles;
- overtime/pay-period configuration.

Do not reintroduce currency, notes, quick-duration presets or validation helper text without a new product decision. WorkTime remains portrait-only unless that product constraint is explicitly changed.
