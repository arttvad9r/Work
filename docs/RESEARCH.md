# Product research

Research snapshot: **20 August 2026**.

The research focused on Android products that overlap with WorkTime on calendar entry, worked hours, hourly pay, bonuses/deductions, and monthly salary visibility.

## Primary reference — «Табель учета рабочего времени» (RavenDEV)

Strong pattern:

- calendar is the primary interaction model;
- direct day-level entry;
- salary/hour tracking is easy to understand;
- reports exist without turning the app into project-management software.

Weak pattern:

- dated visual language;
- dense utility-style UI;
- recent user feedback highlights intrusive advertising as friction in the core flow;
- users also request multiple jobs/rates.

**Decision for WorkTime:** preserve calendar-first simplicity; reject interruptive monetization and legacy visual density.

## Timesheet: Рабочий график смен

Useful capabilities include hourly rates, overtime, bonuses, deductions, periods, backup and exports.

**Lesson:** calculation flexibility is valuable, but exposing every payroll rule in the daily form creates cognitive load. MVP keeps only rate, bonus and penalty.

## Work Log — Shift Tracker

Useful patterns include quick shifts, pay-period summaries and multiple jobs.

**Lesson:** fast repeated entry matters. Quick duration shortcuts belong in MVP; multiple jobs do not.

## Timesheet — Work Hours Tracker

A broad tracker with timer, calendar, rates, projects, reports and invoicing.

**Lesson:** this is the upper bound of scope, not the product target. WorkTime deliberately avoids clients/projects/invoices in v1.

## WorkingHours

A modern implementation with careful state-driven UX, manual/time-tracked entries, rates and exports.

**Lesson:** use modern Material/state patterns without inheriting a project/task mental model.

## Shift calendar + salary products

These apps often combine planned schedule generation with actual worked-time accounting.

**Lesson:** planning and factual accounting are different jobs. Mixing them on the same calendar too early makes states ambiguous. WorkTime MVP records actual work only.

## Cross-product conclusions

1. Calendar-first interaction is already validated by the market.
2. Users value salary visibility more than analytics charts in this problem.
3. Bonuses/deductions are common, but a simple explicit model is sufficient initially.
4. A changing default rate requires an immutable historical snapshot per entry.
5. Export is useful but not necessary to prove the first value proposition.
6. Mandatory registration reduces time-to-value for a local personal tool.
7. Interstitial ads directly conflict with the promise of fast shift entry.
8. Multiple jobs should influence the data-model migration path even though they stay out of MVP.

## Product positioning

WorkTime should feel like a **modern salary calendar**, not a time-management suite.

The product wins if a user can record a shift in seconds and trust the monthly number. It does not win by having the largest settings menu.
