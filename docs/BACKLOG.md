# Prioritized backlog

## P0 - before merge/release

1. Run `./scripts/verify.sh` on the current branch head.
2. Build and install the APK on the target phone.
3. Verify the monthly report opens by tap and upward drag.
4. Complete the calendar/editor/settings checklist in `ANDROID_QA.md`.
5. Confirm no clipping at Russian locale and increased font scale.

## P1 - release hardening

- Add Compose tests for fixed summary/report semantics and adjacent-month inactivity.
- Add a day-editor test for bonus/penalty expansion order.
- Capture final screenshots after device QA.
- Review launcher icon, signing and Play pre-launch report.

## P2 - future decisions

- export/backup;
- multiple work profiles;
- overtime/pay-period configuration.

Do not reintroduce currency, notes or quick-duration presets without a new product decision.
