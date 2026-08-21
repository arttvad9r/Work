# Release Baseline

## Build result

`./gradlew :app:assembleRelease` was run during the production-baseline audit. The release variant is configured with `isMinifyEnabled = false` and the standard optimized Android ProGuard file plus `app/proguard-rules.pro`.

## Signing

No production keystore or release signing credentials are committed. The project does not currently define a production signing configuration, so a publishable signed artifact still requires release signing setup outside the repository.

## Pre-publication steps

Before publication:

1. establish the protected release signing/keystore process;
2. build and verify the signed release artifact;
3. execute connected instrumentation and device QA;
4. complete the accessibility, persistence and salary reconciliation checks;
5. define the distribution and rollback process.

No product behavior was changed as part of this baseline.
