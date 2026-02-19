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
