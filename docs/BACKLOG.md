# Prioritized backlog

This backlog is implementation-oriented. `P0` means required for a credible v1 MVP.

| Priority | ID | Item | Status |
| --- | --- | --- | --- |
| P0 | FND-01 | Android/Compose project foundation | In progress |
| P0 | FND-02 | CI: unit tests + lint | In progress |
| P0 | CAL-01 | Fixed 6×7 month calendar | Prototype done |
| P0 | CAL-02 | Today / selected / filled states | Prototype |
| P0 | CAL-03 | Previous / next month navigation | Prototype |
| P0 | DAY-01 | Create/edit work entry | Prototype |
| P0 | DAY-02 | Duration input + quick chips | Prototype |
| P0 | DAY-03 | Historical rate snapshot | Domain ready; persistence pending |
| P0 | DAY-04 | Bonus | Prototype |
| P0 | DAY-05 | Penalty | Prototype |
| P0 | DAY-06 | Delete entry | Prototype |
| P0 | SUM-01 | Month salary | Prototype |
| P0 | SUM-02 | Month worked duration | Prototype |
| P0 | SUM-03 | Shift count | Prototype |
| P0 | DATA-01 | Room database | Not started |
| P0 | DATA-02 | WorkEntry DAO/repository | Not started |
| P0 | SET-01 | Default hourly rate in DataStore | Not started |
| P0 | SET-02 | Currency setting | Not started |
| P0 | SET-03 | Theme setting | Not started |
| P0 | TEST-01 | Golden salary unit tests | Done |
| P0 | TEST-02 | Repository/database tests | Not started |
| P0 | UX-01 | Localize all user-facing strings | Not started |
| P0 | A11Y-01 | TalkBack + font scale pass | Not started |
| P1 | DAY-07 | Optional note persistence | UI prototype |
| P1 | CAL-04 | Month picker | Later |
| P1 | UX-02 | Undo delete | Later |
| P2 | EXP-01 | CSV/PDF export | Post-MVP |
| P2 | BACK-01 | Manual backup/restore | Post-MVP |
| P2 | JOB-01 | Multiple jobs | Post-MVP |
| P2 | NORM-01 | Planned monthly hours | Post-MVP |

## Next execution order

1. Make CI authoritative and add the Gradle wrapper.
2. Add Room persistence and repository Flow.
3. Add DataStore settings.
4. Replace prototype strings/formatters with locale-aware resources.
5. Harden Calendar and Day Editor states.
6. Add Compose UI tests.
7. Perform accessibility and process-death checks.

## Scope lock

Do not begin timer/clock-in, shift planning, multiple jobs, analytics charts, cloud sync, custom themes, or complex overtime rules until all P0 rows are complete and beta-stable.
