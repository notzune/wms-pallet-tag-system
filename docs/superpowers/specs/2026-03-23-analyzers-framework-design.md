# Analyzers Framework Design

Date: 2026-03-23
Target Version: 1.7.7
Status: Draft for review

## Summary

Add a reusable analyzer subsystem to the Swing GUI that:

- exposes a new `Analyzers...` entry from the tools menu
- opens a single reusable analyzer dialog
- supports multiple analyzer views through a dropdown
- auto-loads on open and when the selected analyzer changes
- supports manual refresh plus configurable auto-refresh
- keeps query execution, table presentation, and row styling separated
- provides the first analyzer implementation for `Unpicked Partials`

The design is intentionally heavier than a one-off dialog so future analyzer screens can be added by registering focused analyzer modules instead of extending shared GUI classes with analyzer-specific conditionals.

## Goals

- Add a generic analyzer window that can host many future analyzers.
- Keep the analyzer shell independent from any single analyzer's query or display rules.
- Follow SRP, DRY, and SOLID with clear boundaries between:
  - menu wiring
  - dialog lifecycle
  - analyzer registration
  - data loading
  - row mapping
  - table column configuration
  - row styling
  - refresh scheduling
- Use the current active site from `AppConfig` for analyzer execution in the first version.
- Implement `Unpicked Partials` as the first analyzer using the order-level query shape.
- Match the existing operator expectation of a sortable data table with customer-specific row colors.
- Keep the UI stable during refreshes by retaining the last successful dataset while reloads run.

## Non-Goals

- analyzer-specific export or copy actions in the first release
- inline editing or write-back workflows
- site override controls in the analyzer toolbar
- date-range controls in the first analyzer version
- embedding customer rule logic into SQL
- converting the existing GUI into a full multi-document dashboard shell

## Architecture

## Entry Point

The existing tools popup gets a new `Analyzers...` item.

The frame remains thin:

- open the shared analyzer dialog
- keep only wiring concerns in the main frame and tools-menu support classes
- avoid placing analyzer query or formatting behavior inside `LabelGuiFrame`

## Shared Analyzer Shell

Add a reusable `AnalyzerDialog` that owns:

- analyzer dropdown
- `Refresh` button
- auto-refresh enabled toggle
- refresh interval selector
- status line
- last-updated indicator
- table container and sorter

The dialog must not know query details for specific analyzers. It should only coordinate the currently selected analyzer definition and render the generic result surface.

## Analyzer Plugin Contract

Future analyzers should be added by implementing a stable contract instead of modifying generic dialog logic.

Suggested core types:

- `AnalyzerDefinition<R>`
- `AnalyzerRegistry`
- `AnalyzerContext`
- `AnalyzerDataProvider<R>`
- `AnalyzerResult<R>`
- `AnalyzerColumnSet<R>`
- `AnalyzerRowStyler<R>`
- `AnalyzerRefreshScheduler`

Responsibilities:

- `AnalyzerDefinition<R>`
  - stable id
  - display name
  - default refresh interval
  - default sort definition
  - factory access to provider, columns, and styler
- `AnalyzerRegistry`
  - returns all available analyzer definitions
  - resolves the default/initial analyzer
- `AnalyzerContext`
  - current active site/config
  - clock/time source
  - DB/query dependencies
  - executor or worker dependencies if needed
- `AnalyzerDataProvider<R>`
  - loads typed rows for one analyzer
- `AnalyzerResult<R>`
  - rows
  - fetched timestamp
  - optional metadata such as row count or warnings
- `AnalyzerColumnSet<R>`
  - column names
  - value accessors
  - alignment
  - widths
  - optional formatter/display helpers
- `AnalyzerRowStyler<R>`
  - resolves full-row or cell appearance from row data
- `AnalyzerRefreshScheduler`
  - owns timer lifecycle
  - restarts timing after manual refresh
  - supports enable/disable behavior

This structure is meant to be repeatable for each future analyzer. A new analyzer should only need to provide a definition, a provider, a row type, column metadata, and optional styling policy.

## Dialog Flow

On open:

- instantiate the dialog with the registry and shared analyzer dependencies
- populate the dropdown from the registry
- select the default analyzer
- trigger an immediate load

On analyzer change:

- cancel or invalidate any in-flight load
- preserve the last rendered table until the new load completes
- trigger immediate load for the newly selected analyzer
- reset the auto-refresh timer baseline

On manual refresh:

- reload the currently selected analyzer immediately
- reset the auto-refresh timer baseline

On auto-refresh tick:

- reload the selected analyzer if auto-refresh is enabled
- leave the current table visible while refresh is in progress

## Loading and Concurrency

Loads should run off the Event Dispatch Thread.

Recommended behavior:

- use a dialog-scoped coordinator or controller to trigger asynchronous loads
- associate each load with a request token or sequence
- ignore stale results if the selected analyzer changes before the load completes
- update Swing components only on the EDT

The analyzer shell should never block the UI while querying WMS.

## Unpicked Partials Analyzer

## Query Shape

Use the order-level query shape, not the `prtnum` rollup.

Rationale:

- it matches the provided operator screenshot more closely
- each row is actionable without a second lookup
- it exposes the order and sold-to context operators need to resolve issues

Expected row fields:

- warehouse id
- appointment
- order number
- sold-to customer code
- ordered partial quantity
- allocated quantity
- unallocated quantity
- completed quantity
- remaining quantity
- warehouse/customer display names
- sold-to address fields
- current fetch timestamp if needed for parity with the legacy screen

The provider should parameterize warehouse/site concerns through app context instead of hardcoding environment-specific behavior into the dialog.

## Row Model

Create a dedicated immutable row type for this analyzer, for example `UnpickedPartialsRow`.

It should contain:

- the query fields needed for display
- any normalized customer key needed for classification
- derived display-rule value if helpful

Do not use generic `Map<String, Object>` row structures in the analyzer shell. Strong typing keeps the contract clearer and easier to test.

## Customer Rule Classification

Customer-specific handling should live in Java in a dedicated policy component, not in SQL and not in the dialog.

Suggested class:

- `UnpickedPartialsRuleClassifier`

Responsibilities:

- identify customer/rule buckets from sold-to name or related fields
- normalize aliases if needed
- return a stable rule enum or policy key

Initial rule buckets derived from the provided examples:

- `LOBLAWS`
- `CORE_MARK`
- `MR_DAIRY`
- `WALMART`
- `SOBEYS`
- `METRO`
- `DEFAULT`

The dialog and generic table shell should not contain customer-name conditionals.

## Row Styling

Full-row highlighting should be driven by the classifier output through an analyzer-specific styler.

Suggested class:

- `UnpickedPartialsRowStyler`

Initial visible color mapping based on the provided formatting screenshots:

- `CORE_MARK` = orange
- `METRO` = green
- `WALMART` or `WALMART CANADA` = blue
- `SOBEYS` = lavender
- `LOBLAWS` = yellow
- unmatched/default = standard table colors

`MR_DAIRY` should be modeled in the classifier and styler even if a final distinct color is deferred until confirmed.

Colors should be centralized in the styler/palette logic so future analyzers can reuse the same shared rendering pattern without copying Swing renderer code.

## Toolbar Behavior

The analyzer toolbar should include:

- analyzer dropdown
- `Refresh`
- auto-refresh toggle
- configurable interval control
- status text
- last updated text

Behavior:

- analyzer auto-loads on open
- analyzer auto-loads on dropdown change
- manual refresh triggers immediate reload
- manual refresh resets the timer countdown
- auto-refresh can be turned on or off
- interval can be changed without reopening the dialog

The first implementation may use a simple bounded interval selector such as a combo box. The exact values can be finalized in implementation planning.

## Error Handling

Required behavior:

- first load failure:
  - show empty table
  - show error in the status line
  - avoid a blocking modal popup
- refresh failure after a successful load:
  - keep the last successful rows visible
  - update the status line with the failure
- loading state:
  - show `Loading...` or equivalent in the status area
  - disable the refresh button while a refresh is in flight

The shell should favor operational stability over interruption. Operators should not lose the current table just because one refresh attempt failed.

## Testing

## Unit Tests

Add focused tests for:

- registry returns analyzers in the expected order
- analyzer dialog/controller triggers auto-load on open
- analyzer switch triggers reload
- manual refresh resets the scheduler baseline
- auto-refresh toggle and interval behavior
- stale async results are ignored after analyzer changes
- `UnpickedPartials` row mapping
- `UnpickedPartialsRuleClassifier`
- `UnpickedPartialsRowStyler`

## GUI-Focused Tests

Add focused GUI tests for:

- tools menu includes `Analyzers...`
- opening the analyzer dialog from the tools menu
- switching dropdown selection triggers the analyzer change path
- status line updates on loading and failure

GUI tests should stay shallow and avoid live DB dependency.

## Query/Provider Tests

Provider behavior should be testable behind interfaces or injectable query services.

Do not force the analyzer dialog tests to use Oracle. Keep data access behind analyzer provider boundaries so test fixtures can validate:

- row mapping
- classification behavior
- empty result handling
- error propagation

## Implementation Notes

- Preserve existing GUI patterns where practical, but keep analyzer abstractions out of unrelated workflow classes.
- Prefer new focused classes over expanding `LabelGuiFrame` with analyzer-specific state.
- Keep generic analyzer infrastructure reusable enough that the second analyzer can be added with minimal edits outside registration.
- If later analyzers need custom filters, consider extending the analyzer contract with an optional analyzer-specific toolbar fragment instead of hardcoding controls into the shared dialog.

## Open Items For Planning

- confirm default auto-refresh interval values
- confirm whether `MR_DAIRY` needs a distinct visual color in the first release
- choose whether analyzer registry is static/manual or service-loaded
- choose exact table column order and preferred default sort for `Unpicked Partials`
