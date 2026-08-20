# Roadmap

Status legend: `DONE`, `IN PROGRESS`, `NEXT`, `LATER`.

## Phase 0 — specification freeze — DONE

- MVP boundaries defined.
- Salary formula and historical-rate rule defined.
- Calendar-first UX selected.
- Technical direction and data model documented.

## Phase 1 — project foundation — IN PROGRESS

Exit criteria: app shell builds, domain tests are green, basic calendar and editor render.

- [x] Repository bootstrap and documentation.
- [x] Gradle/Compose project skeleton.
- [x] Material 3 theme.
- [x] Salary calculator + golden tests.
- [x] Fixed 6×7 month grid + unit test.
- [x] Calendar summary prototype.
- [x] Day editor prototype with quick durations, rate, bonus, penalty and note.
- [ ] Add Gradle wrapper binary and verify CI on GitHub Actions.
- [ ] Replace hard-coded presentation strings with resources.
- [ ] Accessibility pass for prototype.

## Phase 2 — data & settings — NEXT

Exit criteria: entries and settings survive process/app restarts and are source-of-truth driven.

- [ ] Room database with schema export.
- [ ] `WorkEntryEntity`, DAO and mappings.
- [ ] Repository contract + Room implementation.
- [ ] DataStore settings: default rate, currency, theme.
- [ ] ViewModel backed by repository Flow.
- [ ] Database/repository tests.
- [ ] Backup policy decision.

## Phase 3 — calendar MVP — NEXT

Exit criteria: production-quality monthly browsing and state rendering.

- [ ] Previous/next month motion and swipe behavior.
- [ ] Today/selected/filled states final design.
- [ ] Localized weekday and month labels.
- [ ] Empty/loading/error states where applicable.
- [ ] Month summary with base/bonus/penalty breakdown.
- [ ] Responsive behavior and 200% font-scale checks.

## Phase 4 — day editor MVP — NEXT

Exit criteria: full create/edit/delete flow on persistent data.

- [ ] Inline validation for hours/minutes/rate/adjustments.
- [ ] Locale-aware money parser/formatter.
- [ ] Rate snapshot semantics on create vs edit.
- [ ] Delete confirmation or undo.
- [ ] Process-death/state-restoration checks.
- [ ] Compose UI tests for critical flows.

## Phase 5 — settings & polish — LATER

- [ ] Settings screen.
- [ ] Theme: system/light/dark.
- [ ] Currency selection.
- [ ] First day of week if beta feedback warrants it.
- [ ] Motion, haptics and visual polish.
- [ ] TalkBack semantics audit.

## Phase 6 — QA & hardening — LATER

- [ ] Unit/UI test matrix green.
- [ ] Database migration tests.
- [ ] Performance and startup profiling.
- [ ] Crash/ANR review.
- [ ] Release build optimization.
- [ ] Golden salary cases manually reconciled.

## Phase 7 — beta & Play release — LATER

- [ ] Internal test release.
- [ ] Closed beta with real shift workers.
- [ ] Play listing, icon, screenshots.
- [ ] Privacy policy and Data Safety form.
- [ ] Pre-launch report clear of critical issues.

## Post-MVP candidates

Only after v1 retention and beta feedback:

- CSV/PDF export;
- manual backup/restore;
- multiple jobs and rates;
- planned monthly hours;
- richer analytics;
- cloud sync only if ongoing server cost is justified.

## Scope guardrail

Do not add timer/clock-in, shift planning, multiple jobs, advanced analytics, cloud sync, custom themes, or overtime engines before every P0 item for v1 is complete and tested.
