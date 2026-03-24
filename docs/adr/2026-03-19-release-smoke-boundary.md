# ADR: Release Smoke Boundary

## Status

Accepted

## Context

Recent `1.7.5` patch work exposed a release risk pattern:

- the application had working shared backend services for critical workflows
- packaged and runtime-specific failures still escaped into operator testing
- GUI functionality was being assumed to match CLI/runtime behavior without an explicit release coverage contract

The project needs a release smoke boundary that answers one operational question before tagging or shipping:

`Is the build functionally safe to distribute?`

That answer cannot rely on ad hoc manual spot checks.

## Decision

Release hardening will use one shared smoke specification with two required execution targets:

1. repo/JAR mode
2. packaged-app mode

GUI workflows are validated through existing shared backend services or CLI-equivalent paths first. Full Swing automation is not the first-line release gate for this codebase.

Printer smoke validation defaults to host/printer reachability and non-destructive backend-path checks. Live print-job submission is opt-in and not part of the default release smoke contract.

## Rationale

### Repo And Packaged Coverage Are Both Required

Repo-mode smoke catches regressions early during normal development and patch work.

Packaged-mode smoke catches issues that only appear after:

- app-image path relocation
- bundled config resolution
- installer/runtime layout changes
- packaged launcher behavior

Using only one of these modes leaves an avoidable blind spot.

### Shared Backend Paths Are The First GUI Safety Boundary

The GUI already delegates most critical work to shared services:

- shipment label preparation/printing
- carrier-move orchestration
- rail preview/render generation

Those shared paths are the stable behavior boundary. They can be tested deterministically without brittle UI automation. Where no backend-equivalent path exists, that gap must be documented explicitly rather than silently treated as covered.

### Printer Smoke Must Be Non-Destructive By Default

Operators need release confidence without accidental live printer output.

Default smoke behavior therefore validates:

- printer host reachability
- printer port reachability
- system-default print path behavior without shell-association dependence

This is enough to catch common regressions while avoiding unintended physical output during release verification.

## Consequences

### Positive

- Release decisions become evidence-based.
- Packaged/runtime regressions become visible before tagging.
- GUI coverage gaps are explicit and auditable.
- Printer validation becomes safer for routine smoke runs.

### Negative

- Some GUI-specific UX behavior remains outside the default smoke gate until dedicated automation is added.
- Smoke maintenance now becomes part of release engineering.

## Follow-Up

- Add a shared smoke manifest and runner
- Add a GUI coverage matrix
- Require smoke evidence in release checklist and release docs
