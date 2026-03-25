# Daily Operations Analyzer Design

Date: 2026-03-25

## Summary

Add a new default analyzer named `Daily Operations` that renders a composite operational dashboard inside the existing `Analyzers...` dialog. Extend the analyzer framework so analyzers can render either as the current table presentation or as a new dashboard presentation. Keep existing analyzers such as `Unpicked Partials`, `Open Loads`, and `All Dock Doors` as table analyzers for drill-down workflows.

## Goals

- Make `Daily Operations` the default analyzer when the dialog opens.
- Support a composite dashboard made of multiple independently loaded sections.
- Preserve the existing analyzer dialog entry point, refresh controls, and analyzer selector.
- Keep table analyzers working without behavior changes beyond framework integration.
- Ensure one failing dashboard section does not blank the entire dashboard.

## Non-Goals

- Replacing the analyzer dialog with a separate window.
- Converting all existing analyzers into dashboards.
- Introducing side-by-side compare workflows or custom tab sets.
- Parallelizing dashboard section loading in the first implementation.

## User Experience

Operators open `Analyzers...` from the existing tools menu and land on `Daily Operations` by default. The dialog keeps the current selector and refresh controls. The center content switches based on analyzer presentation type:

- Table analyzers render the existing table view.
- Dashboard analyzers render a scrollable stacked dashboard with titled sections.

The `Daily Operations` dashboard should emphasize scanability over visual density. Sections should present the most immediately useful data first and use compact tables or KPI-style cards only where the data is naturally scalar.

## Dashboard Sections

The first `Daily Operations` dashboard will contain these sections in order:

1. `Case Pick Summary`
   - 7-day daily totals
   - domestic/canadian totals
   - domestic/canadian remaining
2. `Case Pick Shift Throughput`
   - third A / first / second / third B by day
3. `Outbound / Inbound Appointments`
   - trucks
   - outbounds completed and remaining
   - inbounds completed and remaining
4. `Unload and Load Activity`
   - unloads
   - rail unloads
   - rail loads
   - truck loads
5. `Production Snapshot`
   - recent produced pallets by work order and item
6. `Storage / Capacity Snapshot`
   - locked racks
   - warehouse capacity
   - building utilization

Future analyzers such as `Open Loads` and `All Dock Doors` remain separate analyzer definitions rather than subsections of the dashboard.

## Architecture

### Presentation Types

Add a small presentation abstraction above the existing analyzer contract:

- `table` presentation for current analyzers
- `dashboard` presentation for composite dashboards

The analyzer dialog will switch the center component based on the selected analyzer's presentation type. This preserves one analyzer registry and one dialog while allowing multiple rendering strategies.

### Dashboard Model

Introduce a dashboard snapshot model with:

- dashboard metadata
- an ordered list of section snapshots
- per-section success or failure state

Each section snapshot should carry only the data required for rendering that section. Section models should be intentionally narrow so each section remains independently testable and maintainable.

### Section Loading

`Daily Operations` will own a coordinator that loads all dashboard sections during one analyzer refresh cycle. The first implementation should load sections sequentially in a background load and then publish a complete dashboard snapshot to the UI once all section results are collected.

This keeps the refresh behavior deterministic and minimizes repaint churn. If runtime proves slow, the section loader abstraction should allow later parallelization without changing the dialog contract.

## Data Flow

1. User opens `Analyzers...`.
2. Registry selects `Daily Operations` as the default analyzer.
3. Dialog starts a refresh for the selected analyzer.
4. `Daily Operations` creates its section loaders and executes each query-backed section.
5. Each section returns either a rendered-data snapshot or an error snapshot.
6. The analyzer assembles a complete dashboard snapshot.
7. The dialog swaps the center content to the new dashboard snapshot and updates refresh timestamps/status text.

For existing table analyzers, the current load flow remains unchanged except for using the new presentation dispatch path.

## Error Handling

- Section-level query or mapping failures render an inline error state in that section only.
- Other successfully loaded sections still render normally.
- Analyzer-level failures before section execution can use the existing dialog-level status error path.
- Null and sparse database results should normalize into zero or empty-state values where the SQL semantics clearly imply absence rather than failure.

## Testing Strategy

Follow TDD during implementation.

Required test areas:

1. Framework tests
   - analyzer presentation contract
   - dialog switching between table and dashboard content
   - registry defaulting to `Daily Operations`
2. Dashboard assembly tests
   - successful multi-section snapshot rendering
   - one-section failure does not break other sections
   - refresh updates dashboard snapshot and status
3. Section tests
   - query result mapping
   - null and zero normalization
   - date bucketing
   - shift split calculations
4. Regression tests
   - existing table analyzer behavior remains intact

## Implementation Notes

- Start by extending the analyzer framework with presentation-specific rendering.
- Keep `Daily Operations` section components modest and reusable.
- Avoid over-generalizing section widgets before at least two sections need the same abstraction.
- Register `Daily Operations` first in the default registry so it becomes the selected default analyzer.
- Add `Open Loads` and `All Dock Doors` as follow-on table analyzers using the existing table-oriented pattern.

## Open Decisions Resolved

- Use the existing analyzer dialog rather than a separate operational dashboard window.
- Use a composite dashboard as the default view.
- Keep other operational views as separate analyzers instead of tabs or subviews.
- Implement presentation-type support in the analyzer framework rather than forcing dashboard data into the table model.
