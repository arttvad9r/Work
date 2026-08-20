# Roadmap

Status legend: `DONE`, `IN PROGRESS`, `NEXT`, `LATER`.

## Phase 0 — specification/research — DONE

- [x] MVP boundaries.
- [x] Competitor research.
- [x] Salary formula and historical-rate rule.
- [x] Calendar-first UX.
- [x] Technical/data direction.

## Phase 1 — repository/foundation — IN PROGRESS

Implementation is complete; Android build verification remains.

- [x] Repository/docs structure.
- [x] Compose/Material 3 project source.
- [x] Static code audit and dependency/version recheck.
- [x] CI definition for JVM tests/lint/APK target compilation.
- [x] Build/static-audit scripts.
- [x] Launcher icon placeholder.
- [ ] Complete trusted Gradle Wrapper bootstrap.
- [ ] Confirm real Android build/lint result.
- [ ] Generate/commit Room v1 schema JSON.

## Phase 2 — data/settings — DONE IN SOURCE

- [x] Room database, DAO/entity/mappers.
- [x] Repository boundary and month Flow.
- [x] DataStore rate/currency/theme.
- [x] Rate snapshot semantics.
- [x] Defensive money bounds.
- [x] Backup disabled.
- [x] Repository JVM test.
- [x] Instrumented Room test source.
- [ ] Execute instrumented test on Android emulator/device.

## Phase 3 — calendar MVP — DONE IN SOURCE

- [x] Fixed 6×7 grid.
- [x] Previous/next month navigation.
- [x] Month+entries atomic state.
- [x] Loading/readiness state.
- [x] Today/selected/filled states.
- [x] Localized month/weekday labels.
- [x] Summary with base/bonus/penalty breakdown.
- [x] Day TalkBack semantics in source.
- [ ] Device visual/accessibility verification.
- [ ] Swipe/month motion only if usability evidence justifies it.

## Phase 4 — day editor MVP — DONE IN SOURCE

- [x] Create/edit/delete persistent record.
- [x] Quick durations.
- [x] Inline duration/money validation.
- [x] Positive rate requirement for worked time.
- [x] Bonus/penalty/note.
- [x] Exact live total.
- [x] Delete confirmation.
- [x] Draft saveable across configuration recreation.
- [x] Generic persistence error state keeps draft open.
- [ ] Process-death/device verification.
- [ ] Compose UI instrumentation tests.

## Phase 5 — settings/polish — DONE IN SOURCE / QA PENDING

- [x] Default rate.
- [x] ISO currency validation.
- [x] Explicit no-FX currency warning.
- [x] System/light/dark theme.
- [x] Vertical/horizontal scroll hardening for large layouts.
- [ ] Full TalkBack/200% font/small-screen pass.
- [ ] Final adaptive launcher icon/store visual polish.

## Phase 6 — Android QA/hardening — NEXT

- [ ] Full Android build green.
- [ ] Lint report reviewed.
- [ ] Instrumented Room test executed.
- [ ] Compose critical-flow tests.
- [ ] API 26 / 31+ / 37 device matrix.
- [ ] Process-death/relaunch checks.
- [ ] Performance/startup/ANR check.
- [ ] `ANDROID_QA.md` completed.

## Phase 7 — beta/Play release — LATER

- [ ] Release signing/configuration.
- [ ] Internal test release.
- [ ] Closed beta.
- [ ] Store listing/screenshots/final icon.
- [ ] Privacy policy + Data Safety.
- [ ] Play pre-launch report.

## Post-MVP candidates

Only after beta evidence:

- CSV/PDF export;
- manual backup/restore;
- multiple jobs;
- planned monthly hours;
- richer analytics;
- cloud sync;
- per-entry/multi-currency support if users actually need it.

## Scope guardrail

Do not add timer/clock-in, schedule generation, teams, cloud sync or advanced payroll engines before current MVP passes Android build/device release gates.
