# Static audit - compact interface follow-up

Audit date: 21 August 2026  
Scope: draft PR `feat/compact-modern-interface`  
Current code fix commit reviewed: `7625a1af076b3aba800c0f61959de1f5cb4a04d1`

## Findings fixed

### Build correctness

- Restored a missing `androidx.compose.foundation.clickable` import used by calendar day cells.
- Updated formatter tests from removed `formatMoneyMicros` to `formatAmountMicros`.
- Added pure tests for compact duration formatting and three/four-digit duration input.

### Product consistency

- Removed currency from preferences, repository contracts, UI state, settings, editor, calculations and localized copy.
- Kept old DataStore currency data ignored rather than introducing a destructive migration.
- Confirmed notes and quick-duration controls remain intentionally absent.
- Updated calculation terminology from `Base` / `База` to `At hourly rate` / `По ставке`.

### Layout and interaction

- Calendar is no longer nested in a vertical scroll container.
- The grid uses a fixed six-row geometry and the available stable viewport space.
- Fixed summary has a constant 120 dp height.
- Detailed report uses the standard Material 3 bottom-sheet scaffold with native drag/tap behavior and a single handle.
- Settings rate input is constrained to 120 dp instead of filling the sheet.
- Day-editor values are centered and zero-prefilled amount fields clear on focus.

### Formatting

- Duration output omits redundant minutes.
- Neutral amount output omits a zero fractional part and preserves non-zero precision.
- Calendar cell values no longer include currency symbols or hour suffixes.

## Checks completed in this workspace

- EN and RU XML parse successfully.
- Both locales contain the same 46 string keys.
- Every `R.string` reference in the changed UI files resolves to a localized key.
- No current changed source/resource file contains currency, `RUB`, `formatMoneyMicros` or `База` references.
- Review found and corrected the missing calendar import before handoff.

## Verification limitation

No Android SDK/Gradle runtime was available in this workspace. The observed GitHub Actions job ended before any configured step started and returned no logs. Consequently, this audit does not claim a successful compile, test, lint, APK build, installation or physical-device run.

## Remaining release risks

1. Compile and test the exact current head.
2. Verify report-sheet dragging on the target phone.
3. Recheck small-screen/keyboard behavior with both adjustment fields expanded.
4. Run light, dark, Russian, increased-font, rotation, relaunch and persistence QA.
5. Do not merge the draft PR until those checks are recorded.
