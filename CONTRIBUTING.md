# Contributing

This repository is currently a private product project. Keep changes small, reviewable and tied to the roadmap.

## Branching

- `main` is release-oriented and should remain buildable.
- Use `feat/<scope>`, `fix/<scope>`, `chore/<scope>` branches.
- Avoid unrelated refactors inside feature work.

## Commit style

- `feat:` product behavior;
- `fix:` bug/correctness fix;
- `test:` test-only changes;
- `docs:` documentation;
- `refactor:` behavior-preserving restructuring;
- `chore:` build/tooling/maintenance.

## Before review

Without Android tooling:

```bash
python3 scripts/static_audit.py
```

With JDK 17 + SDK 37 + Gradle 9.5.0:

```bash
./scripts/verify.sh
```

or Windows PowerShell:

```powershell
./scripts/verify.ps1
```

Until the complete Gradle Wrapper is bootstrapped, do not document `./gradlew` as if it worked.

Also verify:

- no hard-coded user-facing strings when a resource is appropriate;
- business logic is outside composables;
- domain/data money remains integer micros;
- default-rate changes cannot mutate historical records;
- currency changes do not imply silent FX conversion;
- persistence failures do not discard an open draft;
- no destructive Room fallback;
- database-version changes include migration tests;
- docs/ADR are updated when a contract changes.

## Pull requests

State:

1. problem;
2. solution;
3. explicit non-scope;
4. test/build evidence actually executed;
5. screenshots/recordings for material UI changes;
6. persistence/migration/privacy risks.

Never call Android CI/device testing “green” without real evidence from that environment.
