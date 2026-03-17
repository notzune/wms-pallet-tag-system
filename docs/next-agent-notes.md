# Next Agent Notes

- Add per-label printing support, likely as a selection flow from the preview screen so operators can print a subset of labels instead of the full job.
- Clean up the GUI menu/tool options to reduce clutter and tighten the operator-facing workflow.
- Printer workflow scoping now uses `capabilities` metadata in `config/<site>/printers.yaml` (`ZPL` for pallet labels, `RAIL` for rail cards). Keep future menu work capability-driven instead of ID-driven.
- Project baseline is now Java 17 for local builds, CI, Javadoc, and bundled runtime packaging. Keep future dependency/runtime updates aligned to the current LTS target.
