# Testing strategy

## Priorities

The highest-risk behavior is not animation or navigation; it is **money correctness and persistence correctness**. Tests are ordered accordingly.

## Test pyramid

### 1. Domain unit tests — mandatory

Cover:

- hourly rate × whole hours;
- minutes;
- bonuses;
- penalties;
- bonus + penalty together;
- adjustment-only entries;
- very small rates;
- one-minute conversion;
- month aggregation;
- shift-count rule;
- checked-overflow boundaries where practical.

Golden examples from the source specification are implemented in `SalaryCalculatorTest`.

### 2. Calendar/date unit tests

Cover:

- every possible weekday for month start;
- leap February;
- 28/29/30/31-day months;
- first-day-of-week configuration when added.

### 3. Repository tests — implemented / expanding

Cover:

- insert/update/delete;
- uniqueness by date in one-job MVP;
- observing month ranges;
- process restart / database reopen;
- migration tests for every schema change.

### 4. Compose UI tests — next hardening slice

Critical scenarios:

1. tap empty date → enter 8h → save → cell and summary update;
2. add bonus and penalty → total updates;
3. edit existing day → historical rate remains explicit;
4. delete → entry disappears and month totals recalculate;
5. navigate months → summary matches visible month.

## Release gates

Before public beta:

- all domain tests green;
- no ignored database migration tests;
- critical Compose flows green;
- manual TalkBack smoke test;
- 200% font-scale smoke test;
- light and dark theme smoke test;
- process-death/relaunch data check;
- golden salary sheet reconciled manually.

## CI target

Current CI compiles the production and instrumentation targets and runs JVM verification:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

The repository includes an in-memory Room instrumented test. Executing connected Android tests requires an emulator/device job and remains a release-gate item.
