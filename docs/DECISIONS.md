# Architecture & product decisions

This file is the lightweight ADR index. Material reversals should be recorded rather than silently changed.

## ADR-001 — Calendar-first home

**Status:** accepted

The month calendar is the home screen. A separate dashboard would duplicate the same monthly information and add navigation cost.

## ADR-002 — One work entry per date in MVP

**Status:** accepted with migration path

The first release optimizes for one primary job and one aggregate record per date. Multiple jobs later will require a broader key such as `(jobId, date, entryId)`.

## ADR-003 — Rate snapshot per entry

**Status:** accepted

The default hourly rate is copied into an entry when the entry is saved. Settings changes do not rewrite history.

## ADR-004 — Integer micros for money

**Status:** accepted

Domain/persistence money uses `Long` micros instead of binary floating point. Formatting and decimal parsing are presentation-boundary concerns.

## ADR-005 — Offline/local first

**Status:** accepted

Room and DataStore are planned as on-device sources of truth. There is no mandatory account or backend in MVP.

## ADR-006 — Compose + Material 3

**Status:** accepted

The product requires a modern, minimal Android UI. Compose keeps state-driven UI concise and is the current Android-first UI toolkit.

## ADR-007 — Single app module initially

**Status:** accepted

The codebase is too small to justify build-logic and module-management overhead. Boundaries are enforced by packages until scaling makes a module split valuable.

## ADR-008 — No interstitial ads in critical flow

**Status:** accepted

Competitor research shows strong user friction from advertising that interrupts shift entry. Core entry/edit/save is protected from this pattern.

## ADR-009 — No timer in MVP

**Status:** accepted

The target problem is factual personal timesheet entry, not live productivity tracking. Timer semantics add background execution and more failure modes without improving the primary job-to-be-done.
