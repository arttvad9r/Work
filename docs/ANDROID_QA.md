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
- [ ] Calendar card keeps only the intended ~1 dp horizontal safety margin.
- [ ] Neighboring day cells have the intended very small ~0.5 dp visual gap.
- [ ] Every month shows six rows and faint adjacent-month dates.
- [ ] Adjacent-month dates are inactive.
- [ ] Day number is bold and anchored near the top-right corner with the intended small right inset.
- [ ] Worked duration is geometrically centered and visibly larger than the previous `labelMedium` presentation.
- [ ] Daily income is anchored near the bottom-left corner with the intended small left inset and is displayed as a whole rounded number only.
- [ ] Bonus/penalty markers occupy the free top-left corner and do not overlap the date.
- [ ] Previous/next arrows switch the month title and date grid immediately; there is no crossfade, delayed old month, or old/new date flash.
- [ ] Rows from the previous Room month are never shown under the newly requested month.

## Fixed summary and monthly report

- [ ] Fixed card always shows work days, `Отработано часов`, monthly income.
- [ ] Card height/position does not change with data.
- [ ] Exactly one handle is visible below the fixed card.
- [ ] Handle opens the report by tap.
- [ ] Handle/sheet opens by upward drag and collapses by downward drag.
- [ ] Repeated tap/drag cycles do not change the sheet anchors or break the peek height.
- [ ] Long-pressing/holding the handle never shows `Маркер перемещения`, `Drag handle` or another tooltip.
- [ ] TalkBack exposes one monthly-report action for the custom handle without duplicate drag-handle speech.
- [ ] Report has one title and one total; no duplicated income row/handle.
- [ ] Bonus and penalty rows appear only when non-zero.

## Day editor

- [ ] Closed-keyboard form fits without a required scroll in the normal state.
- [ ] Duration and rate share one row and remain centered.
- [ ] Labels for `Время`, `Ставка за час`, `Премия` and `Штраф` always stay minimized on the field outline; an empty unfocused field never moves its label into the input area.
- [ ] A new empty day starts with an empty duration field and shows the centered `00:00` hint rather than a literal `0`.
- [ ] Typing `12` into a new duration field produces `12`, never `01:2` or another leading-zero variant.
- [ ] Defensive leading-zero normalization also makes pasted/typed `012` resolve to `12`.
- [ ] `0`, `15`, `530`, `1530`, `24:00` behave as specified.
- [ ] `24:01`, invalid minutes and zero rate with worked time mark the affected field with a red outline only; no validation helper text appears.
- [ ] Opening the numeric keyboard lifts the modal editor above the keyboard; the fields/actions are not left underneath the IME.
- [ ] First tap on any visible numeric slot focuses/activates it and opens the numeric keyboard immediately when the editor was previously unfocused; a second tap is never required.
- [ ] The active logical field shows exactly one label/value/placeholder; no overlapping duplicate text is visible.
- [ ] Tapping an inactive logical field does not show a rectangular gray ripple/pressed overlay.
- [ ] Tap `Время -> Ставка -> Время -> Ставка` at least 10 times with the IME open: Gboard remains continuously visible and the sheet does not jump.
- [ ] Expand `Премия` and `Штраф`, then repeatedly cycle `Время -> Ставка -> Премия -> Штраф -> Время` at least 10 times: Gboard remains continuously visible, there is no hide/reopen cycle, and the sheet does not jump after the intentional one-time control expansion.
- [ ] Repeated field switches preserve all four logical values; values never swap, reset or leak into another field.
- [ ] IME Next advances through the visible logical fields without a visible IME rebuild or sheet movement.
- [ ] Save and reopen after several four-field switches preserves duration, rate, bonus and penalty exactly.
- [ ] Duration, rate, bonus and penalty use the same numeric keyboard family/action-key configuration.
- [ ] Bonus always occupies the first adjustment slot and penalty the second after expansion.
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
- [ ] No currency label/symbol appears anywhere.
