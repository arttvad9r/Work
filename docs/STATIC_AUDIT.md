# Static audit - interaction stability cleanup

Audit date: 22 August 2026  
Scope: `fix/interaction-stability-cleanup`  
Base: `main` at `009d3b18cd8502788a6821b3cec76918cf19ef71`

## Findings addressed

### Monthly report handle

- Material 3 wraps every non-null `BottomSheetScaffold.sheetDragHandle` in its internal tooltip anchor, so clearing semantics inside a custom handle cannot remove the long-press tooltip.
- The Material handle slot is now `null`; the visual handle is rendered inside the stable sheet content and owns tap-to-toggle behavior itself.
- Unlike the earlier failed attempt, the full report remains composed and measured while collapsed. Only alpha/accessibility visibility changes, so the measured sheet height and swipe anchors are not rebuilt during expansion/collapse.
- A static audit guard prevents reintroducing `sheetDragHandle = { PlainDragHandle() }`.

### Day editor and IME

- Removed frame-delayed focus transfer (`withFrameNanos`) from bonus/penalty expansion.
- Duration, rate, bonus and penalty now use explicit `FocusRequester`/`ImeAction` transitions.
- Bonus/penalty expansion buttons cannot take keyboard focus, allowing direct text-field-to-text-field transfer.
- Numeric field state continues to preserve `TextFieldValue` selection so recomposition does not intentionally recreate the editing session.
- Persistence failures are shown with an overlay Snackbar rather than inserting/removing a layout row.

### Settings

- Removed the extra `imePadding` layer from the settings sheet.
- Focusing an initial zero selects the value instead of replacing it with an empty string.
- Validation uses the intended red outline only; helper text is not inserted and cannot resize the sheet.
- Persistence errors use the same transient Snackbar pattern as the day editor.

### Domain/data consistency

- `WorkEntry` now enforces the documented invariant that worked time requires a positive hourly rate.
- Adjustment-only entries may still use a zero hourly rate.
- Unit coverage was added for both cases.

### Cleanup

- Removed obsolete EN/RU validation-helper strings.
- Centralized compact calendar amount formatting instead of duplicating a `NumberFormat` implementation in `CalendarScreen`.
- Local verification and CI now use the checked-in Gradle Wrapper rather than an arbitrary system Gradle installation.
- Static audit now checks portrait-only/IME manifest controls and known interaction-regression patterns.
- QA/UX/build/release documentation was aligned with the actual portrait-only product, red-outline validation policy and hardware-verification status.

## Existing strengths retained

- Currency remains absent from current preferences/UI contracts.
- Room preserves one entry per date and historical rate snapshots.
- Money calculation continues to use checked integer micros rather than persisted binary floating point.
- Backup/privacy exclusions and no-network/no-sensitive-permission constraints remain unchanged.
- EN/RU resource-key parity remains required.

## Verification status

The `main` baseline has been exercised on physical hardware by the project owner. Exact device/build metadata for that run was not recorded in the repository.

This cleanup branch modifies two device-sensitive paths (monthly-report gestures/long-press and IME focus transitions), so it still requires a focused physical-device pass before merge. The executable cases are listed in `ANDROID_QA.md` and `DEVICE_QA_REPORT.md`.

GitHub Actions may currently be unable to start because of account usage limits. A workflow run with no executed steps is an infrastructure/account condition and must not be treated as either success or code failure. Local `./scripts/verify.sh` output plus recorded device QA is the fallback evidence while runners are unavailable.
