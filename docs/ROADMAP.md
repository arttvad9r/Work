# Roadmap

Status legend: `DONE`, `IN PROGRESS`, `NEXT`, `LATER`.

## Phase 0 — specification and research — DONE

- [x] MVP scope and non-goals.
- [x] Salary formula and historical-rate rule.
- [x] Calendar-first UX.
- [x] Technical and data direction.

## Phase 1 — foundation and toolchain — DONE

- [x] Android/Compose single-module foundation.
- [x] Static audit and dependency/version review.
- [x] CI for static audit, JVM tests, lint and APK compilation.
- [x] Gradle Wrapper 9.5.0.
- [x] Room v1 schema JSON.
- [x] Reproducible Nix development environment and version catalog.

## Phase 2 — data and settings — DONE IN SOURCE

- [x] Room database, DAO/entity/mappers and repository boundary.
- [x] DataStore rate/currency/theme preferences.
- [x] Historical hourly-rate snapshots.
- [x] Defensive money bounds and no-FX currency semantics.
- [x] Backup/transfer exclusion rules.
- [x] JVM and Room instrumentation test sources.
- [x] Room instrumentation executed on API 26/API 35.
- [ ] API 37 Room/Compose compatibility decision.

## Phase 3 — calendar and editor MVP — DONE IN SOURCE

- [x] Fixed 6×7 calendar and month navigation.
- [x] Atomic month/data switching and readiness state.
- [x] Create/edit/delete entries with confirmation.
- [x] Quick-hour chips, notes, bonus and penalty fields.
- [x] Salary/hours/shift summary and calculation breakdown.
- [x] English/Russian resources and system/light/dark themes.
- [x] Loading, empty, validation and recoverable-error states.
- [ ] Full device visual/accessibility verification.

## Phase 4 — QA hardening — IN PROGRESS

- [x] Static audit.
- [x] JVM tests.
- [x] Android lint.
- [x] Debug and instrumentation APK assembly.
- [x] API 26/API 35 instrumentation.
- [x] API 35 manual core flow and persistence smoke.
- [ ] API 37 Compose smoke compatibility.
- [ ] Manual edge cases and full edit/save coverage.
- [ ] TalkBack speech verification.
- [ ] 200% font-scale/small-screen review.
- [ ] Process-death/rotation matrix completion.

## Phase 5 — beta and release — LATER

- [ ] Final adaptive launcher/store assets.
- [ ] Release signing and signed artifact verification.
- [ ] Privacy/Data Safety review from the release build.
- [ ] Internal test release.
- [ ] Closed beta and pre-launch report.

## Post-MVP candidates

Only after the release gates pass:

- CSV/PDF export;
- manual backup/restore;
- multiple jobs;
- planned monthly hours;
- richer analytics;
- cloud sync;
- per-entry/multi-currency support if validated.

## Scope guardrail

Do not add timer/clock-in, shift generation, teams, cloud sync or advanced payroll rules before the current MVP passes the remaining QA and release gates.
