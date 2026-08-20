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

Cover every weekday for month start, leap February, and 28/29/30/31-day months.

### 3. Repository tests — Phase 2

Cover insert/update/delete, uniqueness by date in one-job MVP, observing month ranges, database reopen, and migrations.

### 4. Compose UI tests — Phase 4

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

```bash
gradle testDebugUnitTest lintDebug
```

Instrumentation tests can be added as a separate emulator job once the persistent MVP flow exists.
