# Android QA checklist

Run this only after the project builds successfully.

## Minimum device matrix

At minimum verify:

- API 26 emulator/device (minSdk boundary);
- API 31+ device/emulator (dynamic color path);
- API 37 emulator/device (target/compile baseline);
- one narrow/small phone profile;
- one current real device if available.

## Installation/startup

- [ ] Clean install launches without crash.
- [ ] App shows loading state briefly rather than placeholder financial settings.
- [ ] No network/account permission is requested.
- [ ] Launcher icon renders correctly.
- [ ] Relaunch preserves entries/preferences.

## Core flow

- [ ] Configure hourly rate and currency.
- [ ] Tap today → 8h quick chip → save.
- [ ] Calendar cell shows 8h.
- [ ] Monthly hours increase by 8h.
- [ ] Shift count increases by one.
- [ ] Salary matches exact manual calculation.
- [ ] Edit the same day and verify historical stored rate is shown.
- [ ] Add bonus and penalty and verify total/breakdown.
- [ ] Delete and verify summary recalculates.

## Edge cases

- [ ] 0h + bonus saves and does not increase shift count.
- [ ] Worked time + 0 rate cannot be saved and explains why.
- [ ] 24h + 0m is accepted; 24h + 1m is rejected.
- [ ] 60 minutes is rejected.
- [ ] Invalid currency code is rejected.
- [ ] Changing global currency shows no-FX warning and only relabels amounts.
- [ ] Very large money input is rejected without crash.
- [ ] 200-character note saves; input cannot exceed the defined limit.

## Month navigation

- [ ] Previous/next month data never flashes under the wrong month title.
- [ ] February leap year renders correctly.
- [ ] Months starting on every weekday retain the fixed 6×7 geometry.
- [ ] Current day state is visible but not confused with a filled day.

## Persistence/error recovery

- [ ] Force-stop/relaunch retains data.
- [ ] Rotate/recreate while editing; draft survives configuration recreation.
- [ ] Process-death scenario is tested separately; document actual behavior.
- [ ] Simulate/force a database write failure if practical; draft remains visible.
- [ ] Database reopen produces the same entries.

## Accessibility

- [ ] TalkBack reads each filled day as full date + duration + bonus/penalty markers.
- [ ] Previous/next/settings buttons have useful labels.
- [ ] Save/Delete remain reachable at 200% font scale.
- [ ] Settings remain scrollable at 200% font scale.
- [ ] Touch targets are comfortable and no essential state is communicated by color alone.

## Theme/layout

- [ ] System/light/dark themes render legibly.
- [ ] Dynamic color on Android 12+ preserves contrast.
- [ ] Narrow screen has no clipped text/fields.
- [ ] Russian and English layouts fit.

## Instrumentation

- [ ] Run `WorkTimeDatabaseTest` on emulator/device.
- [ ] Add/run Compose UI tests for create/edit/delete/settings before public beta.

Record device/API, app commit SHA, result, screenshots for failures and any workaround. Do not silently waive failed checklist items.
