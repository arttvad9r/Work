# Device QA report

## Status

The earlier baseline build was exercised on API 26, API 35 and API 37 environments. That evidence predates the compact-interface commits and does not validate the current UI.

Historical results:

- API 26 and API 35 instrumentation paths passed in the earlier environment.
- API 35 manual create/save/relaunch/edit/delete flow passed for the baseline UI.
- API 37 Compose instrumentation was blocked inside Espresso by `InputManager.getInstance` compatibility behavior; this was not a confirmed production crash.
- NixOS required compatible/FHS Android SDK tooling.

## Current branch

Branch: `feat/compact-modern-interface`

The current branch changes calendar geometry, summary/report sheets, editor formatting, settings and preference contracts. A fresh APK has not yet been built or installed from the newest head in this work session.

Required retest:

- fixed calendar position across empty and populated months;
- adjacent-month dates;
- report tap and drag gestures;
- duration entry (`0`, `15`, `530`, `1530`);
- bonus/penalty order and calculation labels;
- compact settings field and theme chips;
- absence of all currency UI;
- create/edit/delete/relaunch persistence;
- Russian, dark theme, narrow screen and increased font scale.

Use `ANDROID_QA.md` for the executable checklist and append exact device/commit/results here after the run.
