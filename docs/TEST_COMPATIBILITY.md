# Android Test Compatibility

## Current versions

The Android test dependencies are centralized in `gradle/libs.versions.toml`:

| Dependency | Current version | Usage |
|---|---:|---|
| `androidx.test:core` | 1.7.0 | Android test support |
| `androidx.test:runner` | 1.7.0 | `AndroidJUnitRunner` |
| `androidx.test:rules` | Not declared | No direct rule dependency in the current test sources |
| `androidx.test.ext:junit` | 1.3.0 | JUnit4 Android integration |
| Compose UI test | Compose BOM `2026.08.00` | Compose startup smoke test |
| `androidx.test.espresso:espresso-core` | 3.5.0, transitive | Compose/Espresso synchronization and event injection |
| Room testing | Room 2.8.4 | In-memory Room instrumentation test |

No Android Test dependency was upgraded during this QA baseline.

## Observed API compatibility

| API | Result |
|---:|---|
| 26 | Room and Compose instrumentation tests passed |
| 35 | Room and Compose instrumentation tests passed |
| 37 | Room test completed; Compose smoke test failed before assertion |

## BUG-001

On API 37, `WorkTimeSmokeTest` fails inside Espresso:

```text
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []
at androidx.test.espresso.base.InputManagerEventInjectionStrategy.initialize
at androidx.test.espresso.Espresso.onIdle
```

The failure is in the AndroidX Test/Espresso compatibility layer and is not a confirmed production application crash. The same test passes on API 26 and API 35.

## Possible resolution paths

1. Check AndroidX Test and Compose UI test release notes for an API 37-compatible version.
2. Evaluate a targeted AndroidX Test/Espresso update in an isolated dependency-only change.
3. If no compatible stable version exists, mark API 37 Compose instrumentation as blocked while retaining API 26/API 35 coverage.
4. Avoid changing application code or UI behavior to work around a test-framework reflection failure.

Dependency versions must not be updated automatically as part of this baseline.
