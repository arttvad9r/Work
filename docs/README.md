# WorkTime documentation

## Current product and design

- [`PRODUCT.md`](PRODUCT.md) - product scope and business rules.
- [`PRODUCT_SPEC_V0.2.md`](PRODUCT_SPEC_V0.2.md) - frozen compact-product specification.
- [`UX.md`](UX.md) - current calendar, report, editor and settings behavior.
- [`RESEARCH.md`](RESEARCH.md) - reference-app conclusions and accepted/rejected patterns.

## Engineering

- [`ARCHITECTURE.md`](ARCHITECTURE.md) - layers, state and persistence.
- [`DECISIONS.md`](DECISIONS.md) - product and architecture decisions.
- [`BUILD.md`](BUILD.md) - toolchain, wrapper and CI limitations.
- [`TESTING.md`](TESTING.md) - automated and device verification strategy.
- [`STATIC_AUDIT.md`](STATIC_AUDIT.md) - current interaction-stability source audit.

## Delivery

- [`ROADMAP.md`](ROADMAP.md) - delivery phases.
- [`BACKLOG.md`](BACKLOG.md) - remaining prioritized work.
- [`ANDROID_QA.md`](ANDROID_QA.md) - required physical-device checks.
- [`DEVICE_QA_REPORT.md`](DEVICE_QA_REPORT.md) - recorded hardware status and focused retest scope.
- [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) - release gate.
- [`PRIVACY.md`](PRIVACY.md) - local data and privacy posture.

The `main` baseline has been exercised on physical hardware by the project owner. Branches that modify sheet gestures, IME focus or insets require their own focused device pass before merge. GitHub Actions runner/account limitations are documented separately from application verification.
