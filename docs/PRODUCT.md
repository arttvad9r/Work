# Product specification

## Product statement

WorkTime is a personal Android calendar for recording actual worked time and immediately understanding expected monthly salary.

The user should not have to think in terms of projects, clients, invoices, payroll periods, or time-management methodology. The core mental model is **date → worked time → money**.

## Primary user

A worker with hourly pay and a non-fixed or shift-based schedule who wants to independently track hours and reconcile expected salary.

## Jobs to be done

1. After a shift, record worked hours before they are forgotten.
2. Mid-month, see accumulated earnings and hours immediately.
3. Attach a bonus or penalty to a specific date and see the effect on the total.
4. Change the default rate without rewriting historical salary.
5. Correct a previous entry in a few seconds.

## Product principles

- **Calendar first.** Date selection is the entry point.
- **One-tap entry.** Typical shifts should be recordable in under 10 seconds.
- **Monthly clarity.** Earnings, hours, and shifts stay visible.
- **Progressive disclosure.** Rare fields do not dominate the editor.
- **Local first.** Core use does not depend on internet or an account.
- **Historical correctness.** Every entry stores the effective hourly rate.
- **No dark patterns.** No interstitial ads or paywalls in the core flow.
- **Small surface area.** New features must justify their UI and cognitive cost.

## MVP requirements

### Calendar

- Month view with Monday as default first day of week.
- Previous/next month navigation and swipe later if useful.
- Today, selected day, and filled day states.
- Filled cells show duration; bonus/penalty may be indicated with small markers.

### Day editor

- Hours and minutes.
- Quick duration chips: 4h, 6h, 8h, 10h, 12h.
- Default rate inherited from settings and stored as a snapshot.
- Bonus and penalty monetary adjustments.
- Optional note.
- Live total preview.
- Save, edit, and delete.

### Month summary

- Total salary.
- Total worked duration.
- Shift count where workedMinutes > 0.
- Base pay, bonuses, and penalties available for the detailed summary screen in a later slice.

### Settings

- Default hourly rate.
- Currency.
- Theme: system/light/dark.
- First day of week can be added before public release if needed.

## Business rules

```text
basePay = round(workedMinutes × hourlyRateMicros / 60)
entryPay = basePay + bonusMicros - penaltyMicros
monthPay = Σ entryPay
shiftCount = count(entries where workedMinutes > 0)
```

Money is stored as integer micros (1 currency unit = 1,000,000 micros). Floating point is not used for persisted or domain money.

An entry with zero worked minutes but a bonus remains valid and contributes money while not increasing shift count.

## Non-goals for v1.0

- clock-in timer;
- GPS/geofencing;
- projects, clients, tasks and tags;
- invoices and tax calculation;
- team/employer features;
- shift schedule generation;
- advanced overtime rules;
- cloud account/sync.

## Success criteria

- A normal shift can be saved in under 10 seconds in usability testing.
- No known data-loss bug reaches beta.
- Golden salary cases are deterministic and covered by automated tests.
- Historical entries do not change after the default rate changes.
