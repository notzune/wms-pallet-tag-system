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
