# Static audit - compact interface follow-up

Audit date: 21 August 2026  
Scope: draft PR `feat/compact-modern-interface`  
Current reviewed head: `105ab08d7461e04a556a6f9e87a2aa2c0a8a2ef3`

## Findings fixed

### Product consistency

- Currency is removed from preferences, repository contracts, UI state, settings, editor, calculations and localized copy.
- Old persisted currency data is ignored without a destructive migration.
- Notes and quick-duration controls remain intentionally absent.
- Calculation terminology uses `At hourly rate` / `По ставке`.
- Empty calculation no longer shows a dash.
- Monthly summary uses `Отработано часов`.

### Layout and interaction

- Calendar uses fixed six-row geometry and does not scroll vertically.
- Calendar position is independent of entries, keyboard state and report content.
- Adjacent-month dates are faint and inactive.
- Filled-cell duration and amount are centered; adjustment markers use centered equal markers.
- Fixed summary contains only work days, worked hours and monthly income.
- Monthly report is a separate draggable bottom sheet with one handle, one concise breakdown and no report/export actions.
- The report handle has a reserved bottom peek above system navigation.
- Day-editor duration and rate share one row and remain centered in focused/unfocused states.
- Duration input is labeled with the hours/minutes format and shows an example placeholder.
- Expanding bonus or penalty focuses the newly shown input immediately and preserves bonus-before-penalty order.
- Settings rate input remains compact and theme chips fit on one row.
- Light/dark/system theme selection previews immediately; persistence occurs only through Save and dismissal restores the saved theme.

### Formatting

- Duration output is `0`, whole hours, or `hours:minutes`; redundant `:00` is omitted.
- Neutral amounts omit a zero fractional part and preserve explicitly entered non-zero precision.
- Calendar and editor values contain no currency symbols.

## Checks completed

- EN and RU XML parse successfully.
- EN/RU resources contain matching keys.
- Changed UI `R.string` references resolve to localized keys.
- Current source/resources contain no currency, `RUB`, `formatMoneyMicros` or `База` references.
- Remote source inspection confirms the SettingsSheet callback and WorkTimeApp call site match.
- Remote source inspection confirms fixed calendar dimensions, report peek reservation, duration placeholder and adjustment focus requesters.

## CI status

GitHub Actions run `32503347603` for the reviewed head completed with failure, but its only job returned `steps: null` and no logs. It ended before any workflow step executed. This is not evidence of a compile, test, lint or APK failure, and it is not a successful build.

## Release limitations

No Android SDK/Gradle runtime or physical device was available for this audit. The following still require a real Android environment:

1. clean compile, unit tests, lint and APK assembly;
2. report-sheet tap/drag behavior above Android navigation;
3. keyboard and increased-font-size layout;
4. light/dark preview, cancel and persistence behavior on device;
5. rotation, relaunch and process-death persistence checks.

The draft PR must remain unmerged until those checks produce actual logs/results.
