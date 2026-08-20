# Architecture & product decisions

Material reversals should be recorded rather than silently changed.

## ADR-001 — Calendar-first home

**Status:** accepted

The month calendar is the home screen; a separate dashboard would duplicate the monthly context.

## ADR-002 — One aggregate entry per date in MVP

**Status:** accepted with migration path

The first release uses `dateEpochDay` as the Room primary key. Multiple jobs/entries later require a broader key/model and explicit migration.

## ADR-003 — Rate snapshot per entry

**Status:** accepted

The effective hourly rate is stored on save. Default-rate changes do not rewrite history.

## ADR-004 — Integer micros for money

**Status:** accepted

Domain/persistence uses `Long` micros, not binary floating point. Decimal parsing/formatting belongs at the presentation boundary.

## ADR-005 — Offline/local sources of truth

**Status:** implemented

Room is the work-entry source of truth and DataStore is the preference source of truth. MVP has no backend/account dependency.

## ADR-006 — Compose + Material 3

**Status:** accepted

Compose provides state-driven modern Android UI and Material 3 visual/accessibility primitives.

## ADR-007 — Single app module initially

**Status:** accepted

Current codebase/team size does not justify multi-module build overhead. Package boundaries preserve a future split path.

## ADR-008 — No interstitial ads in critical flow

**Status:** accepted

Shift entry/edit/save must not be interrupted by monetization UI.

## ADR-009 — No timer in MVP

**Status:** accepted

The initial job is factual personal time/accounting entry, not background live tracking.

## ADR-010 — Global currency is not FX conversion

**Status:** accepted

MVP stores one global ISO currency code in preferences and does not store currency per work entry. Changing it relabels numeric historical values; no exchange-rate conversion occurs. The UI must communicate this explicitly.

Adding real multi-currency/FX behavior requires a new ADR and data-model change.

## ADR-011 — Defensive money component limit

**Status:** accepted

User-entered rate/bonus/penalty components are bounded before calculation. The limit exists to keep checked `Long` arithmetic safely below overflow for maximum daily/monthly aggregation; it is not intended as a normal business restriction.

## ADR-012 — Cloud backup disabled for v1

**Status:** accepted

`android:allowBackup="false"` makes the local-only privacy promise explicit. Manual export/backup can be a later user-controlled feature.

## ADR-013 — No destructive Room fallback

**Status:** accepted

Work-history data must not be silently deleted because a migration is missing. Every future Room schema-version change requires a migration and migration test.
