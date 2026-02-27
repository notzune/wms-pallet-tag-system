# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- None.

### Changed
- None.

### Deprecated
- None.

### Removed
- None.

### Fixed
- None.

### Security
- None.

## [1.3.1] - 2026-02-27
### Added
- Configurable GUI right-click clipboard cooldown via `RIGHT_CLICK_COOLDOWN_MS` (with legacy env var fallback support).

### Changed
- Project/version metadata updated to `1.3.1` across Maven modules, CLI version, README, and setup/bundle scripts.

### Fixed
- Restored full per-shipment detail rendering inside Carrier Move stop expanders, including shipment header, label plan, and SKU-level pallet math.
- Prevented accidental double right-click copy/paste actions by debouncing repeated clipboard actions within a short cooldown window.

## [1.3.0] - 2026-02-26
### Added
- Carrier Move ID workflow (default GUI mode) with stop-ordered multi-shipment preview and printing.
- Per-stop and final 4x6 INFO TAG generation for carrier move jobs.
- Bulk queue printing for mixed Shipment (`S:`) and Carrier Move (`C:`) inputs.
- Print checkpoint persistence and resume support for incomplete jobs.
- Phase-0 carrier move stop mapping specification under `analysis/docs`.

### Changed
- GUI input now supports toggling between Carrier Move ID and Shipment ID workflows.
- Shared stop-mapping query contract added to DB repository (`findCarrierMoveStops`).
- Package-level documentation refreshed across modules (`package-info.java`).
- Project/version metadata updated to `1.3.0` across Maven modules, CLI version, docs, and setup scripts.

### Improved
- Reduced repeated asset and lookup overhead in preview preparation by caching template/SKU/site metadata.
- Reduced redundant DB session churn in carrier move preparation by reusing one repository context for mapped shipment loads.

### Fixed
- None.

## [1.2.3] - 2026-02-26
### Added
- Terminal-style right-click clipboard behavior in GUI text fields: right-click copies selected text, or pastes clipboard at cursor when no text is selected.

### Changed
- Project/version metadata updated to `1.2.3` across Maven modules, CLI version output, documentation, and setup/bundle script default JAR paths.

### Fixed
- None.

## [1.2.2] - 2026-02-19
### Added
- Persistent GUI settings entry for default print-to-file output directory.
- Canonical `run.bat` launcher for bundled CLI execution.

### Changed
- Shipment toolbar layout now preserves practical input width while giving more space back to printer selection.
- Barcode generator print target now uses the shared printer dropdown model (including print-to-file) for consistency.
- Portable bundle and setup scripts now default to `cli-1.2.2.jar` and ship `run.bat`.

### Fixed
- YAML printer routing parsing now tolerates top-level `version` metadata.
- Barcode generator output directory field is now disabled unless print-to-file is selected.

## [1.2.1] - 2026-02-19
### Added
- GUI Tools menu entry for the barcode generator dialog.
- Printer dropdown option for print-to-file in the GUI (aligned with CLI `--print-to-file`).
- Ctrl+F shortcut in the GUI to trigger preview.

### Changed
- GUI print-to-file output now saves under the JAR-relative `out/` directory.
- Barcode orientation documentation updated to use `^PON` + field rotation instead of printer rotation.
- Setup and bundle scripts now default to `cli-1.2.1.jar`.
- Barcode landscape rendering now uses field rotation (`^PON` + `^FWR` + rotated `^BC`) for printer-safe output.
- YAML routing and snapshot parsing paths were refactored to typed models for maintainability and safer validation.
- String normalization paths now use locale-stable uppercase conversion (`Locale.ROOT`).

### Fixed
- Javadoc warnings from unescaped characters and ambiguous tag markers.
- Development documentation now explicitly requires a JDK for Maven builds and documents the `No compiler is provided in this environment` remediation.
- Removed unchecked/raw-type deserialization in snapshot loading and reduced avoidable allocations in CLI/GUI label loops.
- Repository hygiene: `wms-tags.log` files are now ignored globally and no longer tracked; `.ai/` is ignored.

## [1.2.0] - 2026-02-19
### Added
- GitHub Actions workflow to build aggregated Javadoc and publish to GitHub Pages.
- Standalone barcode ZPL builder and CLI command for barcode generation/printing.
- New consolidated `INSTRUCTIONS.md` with updated requirements and roadmap.
- `barcode --print-to-file` option to export ZPL under `/out` next to the JAR.
- `run --print-to-file` option and GUI toggle to export ZPL under `/out` next to the JAR.
- Configurable ship-from address keys for site-specific labels.

### Changed
- Barcode landscape output now switches printer orientation using `^POL` in ZPL.
- Updated setup script default jar path to `cli-1.2.0.jar`.

### Deprecated
- None.

### Removed
- None.

### Fixed
- None.

### Security
- None.

## [1.1.0-BETA] - 2026-02-18
### Added
- None.

### Changed
- Consolidated release and installation documentation into `README.md`.
- Simplified documentation set to reduce duplication.

### Deprecated
- None.

### Removed
- Removed standalone release and installation guides now covered by `README.md`.

### Fixed
- Corrected pallet math for full and partial pallet calculation so GUI preview and label counts match per-pallet planning rules.

### Security
- None.

## [1.0.0-BETA] - 2026-02-18
### Added
- Oracle WMS read-only integration with connection pooling and diagnostics.
- ZPL label generation with template-driven field substitution.
- Printer routing with YAML-driven rules and TCP 9100 printing.
- Walmart SKU matrix matching for the Walmart item field.
- CLI commands: `config`, `db-test`, `run`, and `gui`.

### Changed
- None.

### Deprecated
- None.

### Removed
- None.

### Fixed
- None.

### Security
- Read-only DB sessions and redacted configuration output.

## [0.1.0] - 2026-02-10
### Added
- Multi-module Maven structure (core, db, cli).
- Configuration loading from `.env` and environment variables.
- Initial CLI scaffolding and config display.
- Unit test coverage for core configuration logic.

### Changed
- None.

### Deprecated
- None.

### Removed
- None.

### Fixed
- None.

### Security
- None.
