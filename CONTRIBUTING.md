# Contributing

This repository is currently a private product project. Keep changes small, reviewable, and tied to the roadmap.

## Branching

- `main` is release-oriented and should remain buildable.
- Use `feat/<scope>`, `fix/<scope>`, `chore/<scope>` branches.
- Avoid unrelated refactors inside feature changes.

## Commit style

Use concise conventional prefixes:

- `feat:` product behavior;
- `fix:` bug fix;
- `test:` tests only;
- `docs:` documentation;
- `refactor:` behavior-preserving restructuring;
- `chore:` tooling/build/maintenance.

## Before review

Run at minimum:

```bash
gradle testDebugUnitTest lintDebug
```

Also check:

- no new hard-coded user-facing strings;
- business logic is not embedded in composables;
- money uses integer micros in domain/data;
- default rate changes cannot mutate historical entries;
- new database versions include migration tests;
- screenshots or recordings accompany material UI changes.

## Pull requests

A PR description should state:

1. problem;
2. solution;
3. scope explicitly not included;
4. test evidence;
5. screenshots for UI changes;
6. risk/migration notes if persistence changed.
