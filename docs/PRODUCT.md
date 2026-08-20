# Product specification

## Product statement

WorkTime is a personal Android calendar for recording actual worked time and immediately understanding expected monthly salary.

The mental model is **date → worked time → money**. It is not a project tracker, employer payroll system or live timer.

## Primary user

A worker with hourly pay and a non-fixed or shift-based schedule who wants to independently track hours and reconcile expected salary.

## Jobs to be done

1. After a shift, record worked hours before they are forgotten.
2. Mid-month, see accumulated earnings and hours immediately.
3. Attach a bonus or penalty to a specific date and see the effect on the total.
4. Change the default rate without rewriting historical salary.
5. Correct/delete a previous entry in a few seconds.

## Product principles

- **Calendar first.** Date selection is the entry point.
- **Fast entry.** A normal shift should be recordable in under 10 seconds.
- **Monthly clarity.** Earnings, hours and shift count stay visible.
- **Progressive disclosure.** Rare concepts do not dominate the core flow.
- **Local first.** Core use does not depend on internet or an account.
- **Historical correctness.** Every entry stores the effective hourly rate.
- **Explicit semantics.** Settings such as currency must not imply behavior the app does not implement.
- **No dark patterns.** No interstitial ads/paywalls inside core entry/edit/save.

## MVP requirements

### Calendar

- Fixed six-week / seven-day month geometry.
- Monday-first week for MVP.
- Previous/next month navigation.
- Empty, today, selected and filled states.
- Filled cells show duration plus compact bonus/penalty markers.

### Day editor

- Hours and minutes, bounded to 0..24h.
- Quick chips: 4h, 6h, 8h, 10h, 12h.
- Hourly rate; positive rate required for worked time.
- Bonus and penalty adjustments.
- Optional note up to 200 characters.
- Live calculated total.
- Inline validation with an explanation when Save is unavailable.
- Save/edit/delete with delete confirmation.
- Failed persistence must keep the draft open.

A record with no worked time and no adjustment is invalid. A zero-work record with bonus and/or penalty remains valid and does not count as a shift.

### Month summary

- Total expected salary.
- Total worked duration.
- Shift count (`workedMinutes > 0`).
- Base pay / bonus / penalty breakdown when adjustments exist.

### Settings

- Default hourly rate.
- Global ISO currency code.
- Theme: system/light/dark.

Changing currency does **not** query exchange rates or convert saved numeric amounts; it changes the global accounting/display unit. This warning is shown in settings.

## Business rules

```text
basePayMicros = roundHalfUp(workedMinutes × hourlyRateMicros / 60)
entryPayMicros = basePayMicros + bonusMicros - penaltyMicros
monthPayMicros = Σ entryPayMicros
shiftCount = count(entries where workedMinutes > 0)
```

Money uses integer micros. Binary floating point is not used for persisted/domain money.

User-entered money components have a defensive upper bound to protect checked `Long` arithmetic. The bound is an implementation safety guard, not a normal user-facing payroll constraint.

## Non-goals for v1.0

- clock-in timer;
- GPS/geofencing;
- projects/clients/tasks;
- invoices/tax calculation;
- teams/employer control;
- planned shift generation;
- advanced overtime engine;
- cloud account/sync;
- FX conversion or per-entry multi-currency accounting.

## Success/release criteria

- Normal shift can be saved in under 10 seconds in usability testing.
- No known data-loss bug reaches beta.
- Golden salary cases are deterministic and automated.
- Historical hourly rates do not change after settings updates.
- Static audit passes.
- Android build/lint and required device QA pass before release.
