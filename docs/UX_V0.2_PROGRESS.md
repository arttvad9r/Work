# UX v0.2 Progress

## Changed

- Extended the existing monthly summary card with explicit Material 3 KPI labels:
  - monthly income;
  - worked time;
  - work days;
  - base pay;
  - bonuses;
  - penalties.
- Kept the total income as the primary visual value.
- Added a formula-oriented breakdown below the total: base pay, bonuses and penalties.
- Added an empty-month card with the message:
  - `Нет записей за этот месяц.`
  - `Выберите день, чтобы добавить рабочее время.`
- Kept the direct hourly-rate settings action for a zero default rate.
- Preserved calendar cell behavior: empty cells show the date, filled cells show duration, and bonus/penalty markers remain separate visual markers.
- Preserved and extended semantic calendar descriptions containing the localized full date, today/selected state, duration and adjustment markers.

## Files changed

- `app/src/main/java/com/worktime/app/ui/calendar/CalendarScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ru/strings.xml`
- `docs/UX_V0.2_PROGRESS.md`

No domain, Room, repository, ViewModel, navigation or offline architecture changes were made.

## Verification

The following checks were run after the UX changes:

- `./scripts/static_audit.py` — passed inside `nix develop`.
- `./gradlew :app:testDebugUnitTest` — passed.
- `./gradlew :app:lintDebug` — passed.
- `./gradlew :app:assembleDebug` — passed.

## Known limitations

- The separate statistics screen described as a possible v0.2 direction was not added; the existing monthly KPI card remains the only summary surface.
- TalkBack speech output requires a device/emulator with TalkBack enabled and was not changed by this implementation.
- Detailed visual QA at 200% font scale and on narrow devices remains a device-level verification task.
