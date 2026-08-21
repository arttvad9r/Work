# Android QA checklist

Run this checklist against a clean build from the current feature-branch head. Record device model, Android version, app version/commit and result.

## Install and startup

- [ ] `assembleDebug` completes.
- [ ] APK installs cleanly.
- [ ] First launch does not crash or flash an editor before preferences load.
- [ ] Relaunch preserves entries and theme.

## Calendar

- [ ] Calendar does not scroll vertically.
- [ ] Grid position and size stay identical in empty, partially filled and full months.
- [ ] Every month shows six rows and faint adjacent-month dates.
- [ ] Adjacent-month dates are inactive.
- [ ] Duration and daily amount fit and stay centered.
- [ ] Bonus/penalty markers are centered and distinguishable.
- [ ] Month navigation is fast and never mixes title/data.

## Fixed summary and monthly report

- [ ] Fixed card always shows work days, `Отработано часов`, monthly income.
- [ ] Card height/position does not change with data.
- [ ] Exactly one handle is visible below the fixed card.
- [ ] Handle opens the report by tap.
- [ ] Handle/sheet opens by upward drag and collapses by downward drag.
- [ ] Report has one title and one total; no duplicated income row/handle.
- [ ] Bonus and penalty rows appear only when non-zero.

## Day editor

- [ ] Closed-keyboard form fits without a required scroll in the normal state.
- [ ] Duration and rate share one row and remain centered.
- [ ] `0`, `15`, `530`, `1530`, `24:00` behave as specified.
- [ ] `24:01`, invalid minutes and zero rate with worked time show useful errors.
- [ ] Bonus always appears above penalty in every expansion sequence.
- [ ] Calculation uses `По ставке`; zero adjustment rows are hidden.
- [ ] Save, edit, delete and delete confirmation work.
- [ ] Failed persistence keeps the draft open.

## Settings

- [ ] Rate field is compact and centered.
- [ ] `Системная`, `Светлая`, `Тёмная` fit without clipping.
- [ ] Save is reachable with keyboard closed and after keyboard dismissal.
- [ ] Theme selection persists.

## Visual/accessibility

- [ ] Light and dark themes have readable contrast.
- [ ] 200% font scale does not hide essential actions.
- [ ] Narrow phone and landscape do not produce overlapping text.
- [ ] TalkBack announces dates, selected/today state, duration and adjustments.
- [ ] No currency label/symbol appears anywhere.
