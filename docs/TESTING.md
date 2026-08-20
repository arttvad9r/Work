# Testing strategy

## Risk order

1. money correctness;
2. data persistence/history correctness;
3. month/date state correctness;
4. editor validation/error recovery;
5. accessibility/layout;
6. visual motion/polish.

## Non-Android automated coverage

### Salary/domain

Current JVM cases cover:

- whole hours;
- minutes;
- bonus;
- penalty;
- combined adjustments;
- adjustment-only record;
- very small rates;
- one-minute conversion;
- half-micro rounding;
- month aggregation and shift-count rule;
- invalid >24h;
- negative/unsupported money;
- empty records;
- note length;
- preference money bounds.

### Calendar

Current tests cover:

- all seven possible month-start weekdays with Monday-first layout;
- fixed 42-cell geometry;
- leap February;
- custom Sunday-first calculation path.

### Formatting

Current tests cover:

- six fractional digits;
- half-up decimal-to-micros rounding;
- exponent-notation rejection;
- sanitizer normalization;
- exact `BigDecimal` currency formatting;
- invalid currency rejection.

### Repository/entity

- domain/entity round-trip;
- save/observe/delete using a fake DAO;
- month-range boundary behavior.

## Static audit script

```bash
python3 scripts/static_audit.py
```

Checks:

- XML parseability for checked resources/manifest;
- base/Russian string-key parity;
- `allowBackup=false`;
- absence of Internet/location/contact/microphone/camera permissions;
- no `Float`/`Double` use in domain/data Kotlin files;
- no destructive Room fallback.

## Android instrumentation source

`WorkTimeDatabaseTest` creates an in-memory Room database and exercises upsert/observe/delete. It must still be executed on an emulator/device.

Before public beta add Compose UI tests for:

1. empty date → 8h → save → cell/summary update;
2. bonus/penalty → total update;
3. edit existing day → stored rate retained;
4. delete → cell/summary update;
5. navigate month → correct month data;
6. settings save/restore;
7. validation blocks invalid duration/rate.

## CI verification command

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
:app:assembleDebugAndroidTest
```

CI compiles instrumentation targets; it does not claim connected/emulator execution.

## Release gates

Before public beta:

- static audit green;
- JVM test suite green;
- Android build green;
- lint reviewed;
- Room v1 schema committed;
- instrumentation executed;
- critical Compose flows green;
- golden salary sheet manually reconciled;
- TalkBack and 200% font smoke tests;
- process-death/relaunch data checks;
- API/device matrix in `ANDROID_QA.md` completed.
