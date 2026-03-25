# Analyzer Performance And Usability Design

Date: 2026-03-25
Branch: main

## Problem Statement

The new analyzer dashboard has two immediate operational problems:

1. `Daily Operations` is not user-friendly enough for warehouse operators. It renders raw dense tables with weak visual separation, no meaningful loading treatment, poor empty/error states, and limited scanability.
2. Analyzer switching is too slow. View selection currently blocks on live database work, and the `Daily Operations` dashboard amplifies that cost by loading each section sequentially and creating repeated data source infrastructure during a single refresh cycle.

There is also a confirmed functional defect:

- `Outbound / Inbound Appointments` throws `ORA-17004: Invalid column type` because the section loader maps the same result column as both a date and an integer.

## Goals

- Preserve live data on first load of an analyzer.
- Make analyzer switching feel responsive after the first successful load.
- Eliminate the `ORA-17004` appointments failure.
- Improve `Daily Operations` readability without replacing the existing analyzer framework.
- Keep explicit refresh and auto-refresh behavior predictable.

## Non-Goals

- No persistent on-disk caching.
- No broad dashboard redesign outside the analyzer dialog.
- No query cancellation or streaming partial repaint in the first pass.
- No rewrite of the existing table analyzers beyond integrating the new load strategy.

## Recommended Approach

Extend the analyzer runtime so the first time an analyzer is opened it always performs a live load in the background, but subsequent revisits can show the last successful in-memory snapshot immediately while a background refresh runs when needed. Keep the `Daily Operations` dashboard as the default analyzer, but make it materially more legible through compact card-like section presentation, visible headers, formatted numeric cells, and clear loading/empty/error states.

For performance, introduce asynchronous analyzer loading in the dialog and a dashboard-specific coordinator that performs section queries with shared infrastructure and bounded parallelism instead of sequential blocking work on the Swing UI thread.

## Architecture

### 1. Analyzer Load Lifecycle

Each analyzer will have a small runtime state object containing:

- whether it has ever loaded successfully in this dialog session
- the last successful result snapshot
- the last fetched timestamp
- whether a refresh is currently in progress

Behavior:

- First selection of an analyzer always performs a live load.
- That live load runs on a background worker.
- If an analyzer already has a successful snapshot, the dialog can render it immediately when reselected.
- A live refresh can then run in the background based on user refresh, auto-refresh, or freshness policy.
- If refresh fails, keep the last successful snapshot visible and surface the failure in status text or section-local error panels.

### 2. Analyzer Dialog Execution Model

`AnalyzerDialog` should no longer execute provider loads synchronously on the Swing event thread.

Instead it should:

- render an analyzer-level loading state immediately
- dispatch the selected analyzer load on a background executor
- swap in the returned snapshot on completion
- ignore stale completions when the user has already switched to another analyzer

This preserves responsiveness when switching views or refreshing.

### 3. Dashboard Loading Strategy

`Daily Operations` should use a dedicated dashboard load coordinator.

Responsibilities:

- construct shared database access infrastructure once for the dashboard load
- execute independent section loaders in parallel using a small fixed concurrency cap
- collect each section result independently
- return a full dashboard snapshot with success/failure per section

This keeps the dashboard deterministic while removing the current sequential bottleneck.

### 4. Database Access

The current per-section data source creation pattern is too expensive for dashboard loads.

The dashboard load coordinator should:

- create one data source for the dashboard refresh cycle
- pass it to section query services or loaders
- close it once all section work finishes

This reduces repeated connection-pool startup cost during one dashboard render.

## UX And Presentation

### Dashboard Presentation

Retain the vertical stacked dashboard, but improve readability:

- each section renders inside a compact bordered panel
- section titles remain visible and distinct
- tables always show column headers
- numeric fields are right-aligned
- percentages and counts are formatted consistently
- empty result sets display a short `No data` message
- failures display an inline warning panel with concise error text

### Loading Behavior

The dashboard should show clear loading feedback:

- analyzer-level loading state on first load
- cached snapshot immediately on revisit when available
- background refresh status in the footer or header
- per-section failure states rather than collapsing the entire dashboard

### Data Freshness Model

The freshness model should be:

- first open: always live
- revisit after successful load: show last successful snapshot immediately
- explicit `Refresh`: always live
- auto-refresh: always live on interval

This balances operator trust with practical responsiveness.

## Functional Fix

The `Appointments` section bug must be fixed at the query mapping boundary.

Current defect:

- the SQL aliases the appointment day column as `trucks`
- the loader then reads `trucks` as both a date and an integer

The fix should:

- use distinct and correct column aliases
- map date columns as dates only
- map numeric columns as numeric only
- add a regression test that reproduces the invalid column-type failure mode

## Implementation Scope

First implementation pass includes:

- asynchronous analyzer loading in the dialog
- first-load-live plus in-memory snapshot reuse
- bounded-parallel `Daily Operations` section loading
- shared data source lifecycle for the dashboard load
- appointments section mapper/query correction
- dashboard usability improvements for headers, formatting, loading, empty, and error states

Excluded from this pass:

- persistent cache storage
- cancellation of in-flight query work
- broader redesign of drill-down analyzers
- advanced dashboard theming beyond operational readability

## Testing Strategy

Add and update tests for:

- appointments section regression coverage for the invalid column-type bug
- analyzer dialog async load behavior
- stale result suppression when switching analyzers quickly
- cached snapshot reuse on revisit
- refresh replacement of cached results
- dashboard section partial-failure rendering
- dashboard empty/loading presentation logic where testable

Verification after implementation:

- `.\mvnw.cmd -q -pl gui -am test`
- targeted analyzer GUI tests if split out separately
- packaged smoke on the rebuilt app image

## Risks And Tradeoffs

### Benefits

- materially faster perceived analyzer switching
- better actual dashboard throughput
- cleaner operator experience
- safer failure handling with preserved last-known-good results

### Tradeoffs

- more runtime state inside the analyzer dialog
- background execution introduces stale-result coordination complexity
- shared dashboard infrastructure requires clearer loader boundaries

These tradeoffs are acceptable because the current synchronous model already fails user expectations for both correctness and responsiveness.
