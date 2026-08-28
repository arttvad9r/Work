# WorkTime documentation

## Current product and design

- [`PRODUCT.md`](PRODUCT.md) — current product scope and business rules.
- [`UX.md`](UX.md) — current calendar, editor, reports and settings behavior.
- [`UI_SYSTEM.md`](UI_SYSTEM.md) — canonical component, spacing, typography and color contract.
- [`PRODUCT_SPEC_V0.2.md`](PRODUCT_SPEC_V0.2.md) — frozen historical compact-product specification; use `PRODUCT.md` for current behavior.

## Engineering

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — layers, state and persistence.
- [`DECISIONS.md`](DECISIONS.md) — product and architecture decisions.
- [`BUILD.md`](BUILD.md) — toolchain, wrapper and CI behavior.
- [`TESTING.md`](TESTING.md) — automated and device verification strategy.
- [`NIX.md`](NIX.md) — optional Nix/FHS development environment; the project itself builds with the checked-in Gradle Wrapper on a normal compatible Android toolchain.

## Delivery

- [`ROADMAP.md`](ROADMAP.md) — current release state and future direction.
- [`BACKLOG.md`](BACKLOG.md) — remaining verification and hardening work.
- [`ANDROID_QA.md`](ANDROID_QA.md) — physical-device checks.
- [`ANDROID_DEVICE_TESTING.md`](ANDROID_DEVICE_TESTING.md) — device-test setup and commands.
- [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) — release gate.
- [`PRIVACY.md`](PRIVACY.md) — local data and privacy posture.
- [`../CHANGELOG.md`](../CHANGELOG.md) — canonical changelog.

`main` is the source of truth after a feature branch is merged. Documentation describing transient experiments should be removed or clearly marked historical rather than kept as a second current specification.
