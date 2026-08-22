# Android QA checklist

Run this checklist against a clean build from the branch/commit being evaluated. Record device model, Android version, app version/commit and result.

WorkTime is portrait-only by product decision. Landscape support is out of scope.

## Install and startup

- [ ] `assembleDebug` completes.
- [ ] APK installs cleanly.
- [ ] First launch does not crash or flash an editor before preferences load.
- [ ] Relaunch preserves entries and theme.
- [ ] App remains locked to portrait orientation.

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
- [ ] Repeated tap/drag cycles do not change the sheet anchors or break the peek height.
- [ ] Long-pressing/holding the handle never shows `Маркер перемещения`, `Drag handle` or another tooltip.
- [ ] Report has one title and one total; no duplicated income row/handle.
- [ ] Bonus and penalty rows appear only when non-zero.

## Day editor

- [ ] Closed-keyboard form fits without a required scroll in the normal state.
- [ ] Duration and rate share one row and remain centered.
- [ ] `0`, `15`, `530`, `1530`, `24:00` behave as specified.
- [ ] `24:01`, invalid minutes and zero rate with worked time mark the affected field with a red outline only; no validation helper text appears.
- [ ] Moving focus between duration and rate keeps the numeric IME continuously visible.
- [ ] Expanding bonus/penalty while another numeric field is focused transfers focus directly without closing/reopening the IME.
- [ ] Moving through rate -> bonus -> penalty using the IME Next action does not move the sheet vertically.
- [ ] Bonus always appears above penalty in every expansion sequence.
- [ ] Calculation uses `По ставке`; zero adjustment rows are hidden.
- [ ] Save, edit, delete and delete confirmation work.
- [ ] Failed save/delete keeps the draft open and shows a transient localized Snackbar without resizing the sheet.

## Settings

- [ ] Rate field is compact and centered.
- [ ] Focusing an initial `0` selects it instead of deleting it; the sheet height remains unchanged.
- [ ] Invalid rate marks only the field outline red; no validation helper text appears.
- [ ] Opening/closing the numeric keyboard does not produce a second inset jump.
- [ ] `Системная`, `Светлая`, `Тёмная` fit without clipping.
- [ ] Save is reachable with keyboard closed and after keyboard dismissal.
- [ ] Theme selection persists.
- [ ] Failed settings persistence keeps the sheet open and shows a transient localized Snackbar.

## Visual/accessibility

- [ ] Light and dark themes have readable contrast.
- [ ] 200% font scale does not hide essential actions.
- [ ] Narrow portrait phone does not produce overlapping text.
- [ ] TalkBack announces dates, selected/today state, duration and adjustments.
- [ ] The custom monthly-report handle does not expose duplicate tooltip/content-description speech.
- [ ] No currency label/symbol appears anywhere.
