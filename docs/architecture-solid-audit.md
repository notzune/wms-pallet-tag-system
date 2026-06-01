# Architecture And SOLID Audit

Last updated: 2026-06-01

## Current Baseline

The project is a Java 17 Maven multi-module desktop and CLI application:

- `core` owns domain services, label planning, rail calculations, template rendering, printer routing, update policy, and runtime configuration.
- `db` owns Oracle connection pooling and WMS query/hydration support.
- `cli` owns Picocli command parsing and command-specific output formatting.
- `gui` owns Swing workflows, operator dialogs, preview state, update UI, analyzers, and GUI-only workflow orchestration.
- `scripts` owns packaging, installer, smoke-test, and VM validation automation.
- `vba` preserves Excel macro helpers used by legacy rail workflows.

The latest local verification baseline for this audit used:

```powershell
.\mvnw.cmd -q -pl core,db,gui,cli -am test
java -jar cli\target\cli-1.7.6.jar --help
```

The Maven test command exited `0`. The CLI help smoke printed the expected command list.

## Open Work

Open GitHub issues at audit time:

- `#42` Rail label generation improvements: larger/readable labels, documented physical label geometry, multi-train input, combined PDF generation, explicit printable-row selection, and SRP/SOLID preservation.
- `#43` Contextual GUI help: shared help buttons and per-view operator guidance without duplicating dialog rendering.

Recent merged repository context:

- PR `#41` added the Daily Operations analyzer dashboard and package hardening.
- Local `main` was ahead of `origin/main` with rail and Oracle ODBC planning commits.

## SOLID Boundary Rules

Use these rules when adding features or refactoring:

- Single Responsibility: parsing, data access, orchestration, rendering, UI state, persistence, and external process execution should each have one owner.
- Open/Closed: add new analyzers, rail render variants, and command formatting through focused implementations rather than condition-heavy changes in existing coordinators.
- Liskov Substitution: analyzer providers and section loaders should keep shared contracts predictable, including failure snapshots and load timing behavior.
- Interface Segregation: do not force UI classes to depend on DB, printer, update, or renderer details they do not use directly.
- Dependency Inversion: UI and command layers should depend on services or small support classes, with direct environment, file-system, network, and database access pushed behind injectable boundaries.

## Current Hotspots

These files are not automatically wrong, but they carry enough responsibility that new work should reduce pressure rather than add more:

- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java` - broad Swing coordinator for startup, menus, dialogs, workflow wiring, and status behavior.
- `gui/src/main/java/com/tbg/wms/cli/gui/BarcodeDialogFactory.java` - sizable dialog construction and behavior wiring.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java` - complex workflow orchestration that should stay clear of UI rendering and persistence details.
- `scripts/run-smoke-tests.ps1` - valuable release harness, but broad enough that future changes should split manifest parsing, command execution, and reporting carefully.
- `vba/m_Count_Product.bas` - legacy macro logic; preserve workbook behavior and add comments/tests or fixture evidence before further extraction.

## Managed Branches

Use `.worktrees/` for isolated branches. Current branch purposes:

- `docs/repo-documentation-solid-audit`
  - Documentation refresh, changelog notes, release checklist architecture gate, and this audit.
- `refactor/srp-label-gui-frame`
  - Extract low-risk responsibilities from `LabelGuiFrame` behind existing tested support classes.
  - Candidate first steps: menu action registration, status footer updates, and dialog launch adapters.
- `refactor/srp-analyzer-loading`
  - Normalize analyzer loading state and asynchronous section loading after current dirty analyzer work is reviewed.
  - Candidate first steps: keep data-provider concurrency in a coordinator and keep dashboard views passive.
- `perf/code-optimization-baseline`
  - Low-risk cleanup branch for repeated formatting, collection, parsing, and caching improvements.
  - No behavior change should land without tests that already pass before the refactor.

## Refactor Criteria

Start a refactor only when at least one of these is true:

- a file has more than one clear reason to change during the current feature
- a collaborator cannot understand public behavior without reading UI or infrastructure internals
- tests require excessive setup because dependencies are too broad
- a release bug touched the same coordinator more than once
- performance cleanup can be proven by simpler code or a measurable smoke/test result

Avoid refactors that only rename, reshuffle, or split code without improving testability, responsibility boundaries, or reviewability.

## Verification Expectations

Before merging any refactor branch:

```powershell
.\mvnw.cmd -q -pl core,db,gui,cli -am test
.\mvnw.cmd -q -pl cli -am package -DskipTests
java -jar cli\target\cli-1.7.6.jar --help
```

For release branches, also run the smoke commands in `docs/release-checklist.md`.
