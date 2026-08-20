# Prioritized backlog

This backlog is implementation-oriented. `P0` means required for a credible v1 MVP.

| Priority | ID | Item | Status |
| --- | --- | --- | --- |
| P0 | FND-01 | Android/Compose project foundation | Implemented |
| P0 | FND-02 | CI: tests + lint + debug APK | Implemented; branch run verification pending |
| P0 | CAL-01 | Fixed 6×7 month calendar | Implemented |
| P0 | CAL-02 | Today / filled states | Implemented; visual polish pending |
| P0 | CAL-03 | Previous / next month navigation | Implemented |
| P0 | DAY-01 | Create/edit work entry | Implemented with Room persistence |
| P0 | DAY-02 | Duration input + quick chips | Implemented |
| P0 | DAY-03 | Historical rate snapshot | Implemented |
| P0 | DAY-04 | Bonus | Implemented |
| P0 | DAY-05 | Penalty | Implemented |
| P0 | DAY-06 | Delete entry | Implemented with confirmation |
| P0 | SUM-01 | Month salary | Implemented |
| P0 | SUM-02 | Month worked duration | Implemented |
| P0 | SUM-03 | Shift count | Implemented |
| P0 | DATA-01 | Room database | Implemented |
| P0 | DATA-02 | WorkEntry DAO/repository | Implemented |
| P0 | SET-01 | Default hourly rate in DataStore | Implemented |
| P0 | SET-02 | Currency setting | Implemented |
| P0 | SET-03 | Theme setting | Implemented |
| P0 | TEST-01 | Golden salary unit tests | Done |
| P0 | TEST-02 | Repository/database tests | Added; Android execution still requires CI/device |
| P0 | UX-01 | Localize all user-facing strings | Implemented: EN/RU |
| P0 | A11Y-01 | TalkBack + font scale pass | Partial: day semantics + overflow hardening; device pass next |
| P1 | DAY-07 | Optional note persistence | Implemented |
| P1 | CAL-04 | Month picker | Later |
| P1 | UX-02 | Undo delete | Later; confirmation exists |
| P2 | EXP-01 | CSV/PDF export | Post-MVP |
| P2 | BACK-01 | Manual backup/restore | Post-MVP |
| P2 | JOB-01 | Multiple jobs | Post-MVP |
| P2 | NORM-01 | Planned monthly hours | Post-MVP |

## Next execution order

1. Verify the published branch through Android CI and fix any build/lint failures.
2. Execute the Room instrumentation test on an emulator/device job.
3. Add Compose UI tests for create/edit/delete and settings.
4. Complete TalkBack, 200% font-scale and small-screen checks.
5. Add launcher icon, screenshots and release signing configuration guidance.
6. Run beta hardening before merging the foundation branch to `main`.

## Scope lock

Do not begin timer/clock-in, shift planning, multiple jobs, analytics charts, cloud sync, custom themes, or complex overtime rules until all P0 rows are complete and beta-stable.
