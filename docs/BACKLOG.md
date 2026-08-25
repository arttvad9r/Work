# Prioritized backlog

## P0 - before merge/release

1. Run `./scripts/verify.sh` on the current branch head.
2. Build and install the APK on the target portrait phone.
3. Hold the monthly-report handle and confirm no `Маркер перемещения` / `Drag handle` tooltip appears.
4. Exercise repeated report tap/drag open-collapse cycles and confirm stable peek height/anchors.
5. Move through duration/rate/bonus/penalty with the numeric IME open and confirm there is no close/reopen flash or sheet jump.
6. Verify outline-only validation and persistence-error Snackbars in editor/settings.
7. Delete an entry and confirm the root Snackbar offers Undo that fully restores the entry.
8. Run `Change rate for period` for the current month and a custom range, confirm the confirmation dialog, verify only hourly rates change, then Undo and confirm mixed original rates are restored.
9. Confirm the rate-change sheet scrolls its grouped sections (`Calculation`, `Appearance`, `Data and operations`) and the keyboard never covers the focused field.
10. Swipe horizontally across the calendar with the report collapsed and expanded; confirm month switch at a comfortable threshold and no switch on vertical drags.
11. Check calendar visual states: entry glyph in filled cells, three-row collapsed summary, bold report total with error color only when negative, empty-month prompt opening today's editor.
12. Export data to a JSON file, then import it back: confirm the replace dialog, verify entries and settings are restored, and confirm a malformed file shows an error without writing.
13. Add the widget to the home screen, change an entry and confirm the values refresh; then force-stop the app, wait past the 30-minute tick or use the launcher widget picker refresh and confirm light/dark variants and tap-through.
14. Complete the remaining calendar/editor/settings checklist in `ANDROID_QA.md`.
15. Open `Итоги за год` from settings, switch years, and confirm totals, averages and the twelve-month breakdown match the calendar months; confirm empty months render dimmed.
16. Confirm no clipping in Russian locale, narrow portrait layout and increased font scale.

## P1 - release hardening

- Add Compose instrumentation coverage for report-handle tap/drag/semantics where the test environment can reliably exercise current Material 3.
- Add editor UI coverage for focus transfer and bonus/penalty expansion order.
- Capture final screenshots after focused device QA.
- Record exact device model, Android version and tested commit in the release PR description.
- Review launcher icon, signing and Play pre-launch report.

## P2 - future decisions

- multiple work profiles;
- overtime/pay-period configuration.

Do not reintroduce currency, notes, quick-duration presets or validation helper text without a new product decision. WorkTime remains portrait-only unless that product constraint is explicitly changed.
