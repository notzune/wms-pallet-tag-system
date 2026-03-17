# Next Agent Notes

- Add per-label printing support, likely as a selection flow from the preview screen so operators can print a subset of labels instead of the full job.
- Clean up the GUI menu/tool options to reduce clutter and tighten the operator-facing workflow.
- Printer workflow scoping now uses `capabilities` metadata in `config/<site>/printers.yaml` (`ZPL` for pallet labels, `RAIL` for rail cards). Keep future menu work capability-driven instead of ID-driven.
- Project baseline is now Java 17 for local builds, CI, Javadoc, and bundled runtime packaging. Keep future dependency/runtime updates aligned to the current LTS target.
- There is now a `scripts/build-jpackage-bundle.ps1` path for app-image / installer packaging. Keep the portable ZIP flow supported as the manual fallback for machines where the packaged executable is not viable. The app image works without WiX; optional `.exe` / `.msi` installer generation needs WiX Toolset on `PATH`. For release parity, build the app image with a Java 17 runtime image or under a Java 17 JDK.
- Installer helpers now exist for packaged installs: `install-wms-installer.ps1` / `.bat` for logged install and same-version replacement, plus `uninstall-wms-tags.ps1` / `.bat` for uninstall and clean-install prep.
- GUI now performs a GitHub Releases update check and shows an alert badge on `Tools` when a newer version is available. `Tools -> Settings...` also exposes manual update checks and packaged-install uninstall / clean-wipe launchers.
- Guided upgrade now prefers a published installer asset from the latest GitHub Release, downloads it into `updates/`, verifies it against a published `.sha256`, and launches the packaged installer helper. Keep the release workflow publishing both the `.exe` and `.sha256` assets, or the GUI will fall back to opening the release page.
