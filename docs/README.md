# WorkTime documentation

Repository documentation is the implementation-oriented source of truth.

## Product

- [`PRODUCT.md`](PRODUCT.md) — scope, business rules and success criteria.
- [`RESEARCH.md`](RESEARCH.md) — competitor research and product conclusions.
- [`UX.md`](UX.md) — calendar/day-editor/settings interaction specification.

## Engineering

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — layers, data flow, persistence and invariants.
- [`DECISIONS.md`](DECISIONS.md) — lightweight ADR index.
- [`BUILD.md`](BUILD.md) — toolchain, local verification and CI.
- [`TESTING.md`](TESTING.md) — automated-test strategy and release gates.
- [`STATIC_AUDIT.md`](STATIC_AUDIT.md) — static repository invariants and dependency review.
- [`CODE_AUDIT.md`](CODE_AUDIT.md) — consolidated code findings and verification matrix.

## Delivery

- [`ROADMAP.md`](ROADMAP.md) — phased delivery plan.
- [`BACKLOG.md`](BACKLOG.md) — implementation priorities.
- [`ANDROID_QA.md`](ANDROID_QA.md) — emulator/device-only checks.
- [`ANDROID_DEVICE_TESTING.md`](ANDROID_DEVICE_TESTING.md) — setup and connected-test commands.
- [`TEST_COMPATIBILITY.md`](TEST_COMPATIBILITY.md) — AndroidX Test/API compatibility notes.
- [`DEVICE_QA_REPORT.md`](DEVICE_QA_REPORT.md) — recorded device run and known blockers.
- [`DEVICE_QA_BASELINE_REPORT.md`](DEVICE_QA_BASELINE_REPORT.md) — QA baseline summary.
- [`BASELINE_STATUS.md`](BASELINE_STATUS.md) — toolchain and baseline status.
- [`NIX.md`](NIX.md) — reproducible Nix development environment.
- [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) — beta/release gate.
- [`PRIVACY.md`](PRIVACY.md) — local-data and Play Store privacy posture.

When implementation changes, update the relevant document in the same PR/commit whenever the change alters a documented contract or release status. The current release gate is device/accessibility verification plus the API 37 test-stack decision.
