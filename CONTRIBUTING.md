# Contributing

## Scope

Keep WorkTime a small calendar-first personal timesheet. Do not add notes, quick-duration presets, currency, accounts, projects, timers or payroll complexity without an explicit product decision.

## Before opening or updating a pull request

1. Keep domain/data calculations in integer micros.
2. Preserve Room data and historical hourly-rate snapshots.
3. Update EN and RU resources together.
4. Update documentation when a UI or product contract changes.
5. Run:

```bash
./scripts/verify.sh
```

6. Execute the relevant device checklist in `docs/ANDROID_QA.md` for UI changes.

Do not report a build, test or device result as passed unless the command actually ran to completion. Infrastructure failures must be recorded separately from code failures.

## UI expectations

- The calendar geometry is fixed and must not depend on monthly data.
- Normal day entry should fit without scrolling when the keyboard is closed.
- Required actions remain reachable with the keyboard open.
- Labels and values must survive Russian text and increased font scale.
- Color is never the only carrier of meaning.
