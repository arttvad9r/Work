# Compact day-editor UX progress

## Implemented

- Duration and hourly rate share one row and remain centered.
- Duration displays no redundant zero minutes.
- Three/four typed digits map to `H:MM` / `HH:MM`.
- Bonus always precedes penalty and each control expands in its own stable slot.
- Calculation uses `At hourly rate` / `По ставке`; zero adjustment rows are hidden.
- Initial zero amount values clear on focus.
- Currency, notes and quick-duration presets remain absent.
- Settings rate field is compact and theme labels fit one row.

## Verification

Pure formatting/input tests were added. Full Compose/device verification is still required.
