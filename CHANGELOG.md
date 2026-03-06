# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- None.

### Changed

- Performance tuning pass across rail/sku/gui services to reduce repeated allocations and repeated scans.
- Reduced rail planner and CSV helper overhead by removing redundant record scans and regex-based header normalization.
- Reduced DB SKU candidate and description lookup overhead with deduplicated candidate generation and cached PRTMST column discovery.
- Reduced GUI queue parser allocation churn via single-pass newline parsing.
- Enforced SRP in GUI print workflow by extracting info-tag ZPL rendering into a dedicated `InfoTagZplBuilder`.
- Enforced SRP in GUI print workflow by extracting checkpoint persistence/scanning into a dedicated `JobCheckpointStore`.
- Enforced SRP in core configuration loading by extracting dotenv/env-style parsing into `EnvStyleConfigParser`.
- Enforced SRP in core configuration loading by extracting config file discovery/validation into `ConfigFileLocator`.
- Enforced SRP in DB description resolution by extracting SKU candidate generation into `SkuCandidateBuilder`.
- Enforced SRP in DB description resolution by extracting human-readable text heuristics into `DescriptionTextHeuristics`.
- Enforced SRP in DB schema-probing logic by extracting PRTMST description-column resolution/caching into `PrtmstDescriptionColumnResolver`.
- Reduced per-label SKU lookup overhead in `LabelDataBuilder` by selecting representative line items and Walmart mappings in a single pass.
- Reduced repeated literal usage in GUI preview formatting by extracting separator/summary constants in `LabelPreviewFormatter`.
- Hardened DB pool candidate probing in `DbConnectionPool` with try-with-resources validation borrows to avoid transient connection-handle leaks.
- Simplified right-click clipboard cooldown fallback logic in `TextFieldClipboardController` for cleaner null-default resolution.
- Expanded Javadocs/package docs for helper classes (`DescriptionTextHeuristics`, `PrtmstDescriptionColumnResolver`, `RailFootprintResolver`) to capture creation rationale, ownership boundaries, and necessity.
- Fixed PRTMST description-column resolution fallback so direct column probes still run when Oracle dictionary visibility is restricted.
- Reduced rail family-share percentage allocation overhead by replacing BigDecimal-based rounding with `Math.round` in `RailLabelPlanner`.
- Refreshed all package-level documentation (`package-info.java`) across CLI/core/db/gui and updated README documentation coverage guidance.
- Refined barcode CLI/GUI generation paths by removing mutable option-field reassignment in `BarcodeCommand` and extracting slug-length constants for shared consistency.
- Refined `RunCommand` print-to-file execution path to avoid mutating parsed option fields and use explicit effective runtime values.
- Reduced remaining magic-value usage in CLI/core flows by extracting job-id/default-port constants and simplified PRTMST direct-probe execution to avoid warning-prone unused assignments.
- Reduced `shipmentExists` database overhead by switching from `COUNT(*)` to an existence probe (`ROWNUM = 1`) in `OracleDbQueryRepository`.
- Reduced rail-footprint lookup query overhead by de-duplicating normalized short codes before batched SQL execution.
- Enforced SRP for runtime output-directory resolution by introducing shared `RuntimePathResolver` and reusing it across CLI/GUI print-to-file flows.
- Simplified `NormalizationService.normalizeToUppercase` by removing a redundant conditional branch.
- Reduced small but repeated allocation/read overhead in label generation loops by caching per-job LPN counts and reusing a single `LocalDate.now()` value for virtual LPN date stamping.
- Reduced rail workflow allocation overhead by introducing single-record planning (`RailLabelPlanner.planOne`) and removing per-card singleton list construction in `RailWorkflowService`.
- Refined rail family-share percentage allocation using deterministic largest-remainder rounding so family percentages remain stable and sum correctly.
- Improved `LabelDataBuilder` SRP by extracting product-field population/defaulting helpers and removing repeated field-mapping blocks.
- Reduced duplicated rail family-bucket logic by introducing shared `RailFamilyClassifier` and reusing it in resolver/calculator paths.
- Refreshed rail documentation snippets in README/instructions to reflect explicit `CAN`/`DOM`/`KEV` bucket reporting.
- Reduced rail short-code collection overhead in `RailWorkflowService` by using insertion-order deduplication and deferring sorting to result/report layers.
- Reduced rail aggregation key allocation overhead by replacing string-concatenated grouping keys with a typed `RailcarGroupKey`.
- Hardened shared rail CSV parsing for null/empty lines so helper callers get safe deterministic output instead of runtime failures.
- Added strict pallet-index validation in `LabelDataBuilder` to prevent invalid sequence fields from negative or out-of-range inputs.
- Added shipment-state validation in `LabelDataBuilder` so label builds fail fast when shipment payload has zero pallets.
- Hardened `WmsRailDbRepository` adapter preconditions (null-guarded train/short-code inputs) and added explicit delegation tests.
- Refactored `OracleDbQueryRepository` input precondition normalization into shared helpers and replaced concatenated rail-stop grouping keys with a typed key object.
- Expanded DB repository tests to cover carrier-move input preconditions and train-ID normalization in rail stop lookups.
- Renamed the VBA formatting macro entrypoint to `ApplyTrainDetailFormatting` and replaced hard `End` termination with `Exit Sub` in `RunReports` to reduce ambiguous-name collisions and abrupt macro termination.
- Reduced redundant Oracle `ResultSet` date/timestamp column reads by centralizing nullable temporal extraction helpers in `OracleDbQueryRepository`.
- Moved VBA macro helper modules to `vba/` and documented their purpose/ownership in README.
- Renamed VBA `Delete` helper macro to `CleanupRailTempSheets` and removed hard `End` termination from `RefreshData` to avoid ambiguous references and abrupt macro shutdown.
- Hardened `AppConfig` numeric settings parsing with clear key/value validation errors for invalid integer/long config values.
- Hardened printer-routing YAML parsing to skip empty/null condition entries safely instead of failing on malformed rule condition arrays.
- Improved dotenv-style parser handling to strip unquoted inline comments while preserving quoted `#` characters in values.
- Hardened GUI queue input parsing with explicit `defaultType` null checks and positive `maxItems` validation.
- Hardened carrier-move print routing selection in CLI/GUI by resolving the first available shipment across all stop groups instead of assuming index-zero stop/job entries exist.
- Fixed location-matrix CSV parsing to handle quoted commas safely by switching to shared RFC-style CSV tokenization.
- Hardened rail pallet ceiling math against integer-overflow edge cases by using long intermediates and explicit overflow signaling.
- Hardened rail merge CSV export directory handling by guarding parent-directory creation when the output path has no parent component.
- Hardened GUI barcode ZPL export directory handling by guarding parent-directory creation when the output path has no parent component.
- Hardened carrier-move stop grouping in GUI workflow to safely ignore null stop refs and null shipment IDs during deterministic shipment ordering.
- Hardened rail footprint resolution to reject short codes with conflicting cases-per-pallet candidates instead of selecting a potentially incorrect value.
- Hardened DB connectivity error classification by using locale-stable lowercase normalization for Oracle message matching.
- Standardized rail planner missing-footprint diagnostics to be deduplicated and sorted for stable reconciliation output.
- Improved DB remediation hint matching to be case-insensitive for common Oracle/JDBC connectivity error phrases.
- Hardened carrier-move checkpoint filename slugging to fall back safely when source IDs contain only non-alphanumeric characters.
- Hardened rail model code normalization to use `Locale.ROOT` for uppercase conversion, preventing locale-dependent family-code drift.
- Updated `rail-helper` train filtering to be case-insensitive so user-provided train IDs match normalized rail data consistently.
- Expanded terminal-style right-click clipboard behavior wiring to Rail Labels GUI text-entry fields (`Train ID`, output directory), keeping input UX consistent across shipment, queue, barcode, settings, and rail dialogs.

### Deprecated

- None.

### Removed

- None.

### Fixed

- Resolved Maven parent-version drift that caused `javadoc:aggregate` failures in CI when resolving module parents.
- Fixed rail family normalization precedence in DB footprint lookup so `UC_PARS_FLG=1` is applied as a CAN override before other family-code checks.
- Fixed `LabelDataBuilder` null-safety for non-empty but unusable pallet line-item lists (for example null/missing-SKU entries) to avoid runtime failures and emit safe defaults.
- Fixed GUI version-tag resolution so the footer/title can still display the Maven project version when package `Implementation-Version` metadata is unavailable at runtime.

### Security

- None.

## [1.6.0] - 2026-03-04

### Added

- New `rail-print` CLI command for WMS-first rail processing with preview, confirmation, direct PDF rendering, and
  optional print dispatch.
- New core rail workflow services:
  - `RailDbRepository`
  - `RailAggregationService`
  - `RailPalletCalculator`
  - `RailFootprintResolver`
  - `RailWorkflowService`
  - `RailCardRenderer`
  - `RailPrintService`
- New rail workflow tests for aggregation, per-railcar pallet math, footprint resolution, workflow integration, and PDF
  rendering.
- New rail data-source analysis document at `docs/rail-db-analysis.md`.

### Changed

- GUI `Tools -> Rail Labels...` workflow now uses direct PDF card rendering with railcar table + card preview, replacing
  the previous merge-artifact-first flow.
- Rail train lookup now accepts either full train ID or 4-digit train number in `OracleDbQueryRepository`.
- Project metadata/version updated to `1.6.0` across Maven modules.

### Fixed

- Corrected module parent version drift in `core/pom.xml` that could break full-reactor builds.

## [1.5.2] - 2026-03-02

### Added

- Shared rail CSV helper utility (`RailCsvSupport`) to centralize header normalization and CSV parsing behavior.
- Unit tests for rail CSV helper, rail DB query methods, and rail artifact template-missing behavior.
- Build-time CLI version provider wired to filtered `version.txt` metadata instead of hardcoded command annotation text.

### Changed

- README rail documentation expanded with an explicit end-to-end GUI rail workflow (WMS-first with optional CSV
  override).
- GUI Rail Labels dialog now defaults CSV override to off and labels the control as an explicit WMS override.
- Portable build/setup scripts now resolve the latest built CLI jar automatically instead of hardcoded jar filenames.
- Portable build script Java resolution now prefers real Java homes (including `JAVA_HOME`) before Oracle `javapath`
  shims.
- Project metadata/version updated to `1.5.2` across Maven modules and release documentation.

### Fixed

- Git hygiene improved by ignoring rotated and temporary `wms-tags` log variants.

## [1.5.1] - 2026-03-02

### Added

- WMS-backed rail train lookup and short-code footprint lookup methods in the DB repository (`findRailStopsByTrainId`,
  `findRailFootprintsByShortCode`).
- New GUI rail workflow service and dialog under Tools (`Rail Labels...`) with train preview, merge-row preview, and
  detailed diagnostics.
- Word artifact automation service for merged DOCX/PDF/PRN generation from rail merge CSV output.

### Changed

- Rail merge output expanded from 6 to 13 item slots, with overflow diagnostics when rows exceed the slot limit.
- Rail helper/template field guidance updated to `ITEM_NBR_1..13` and `TOTAL_CS_ITM_1..13`.
- Rail footprint resolution now supports WMS-first lookup with optional CSV override layering.
- Project metadata/version updated to `1.5.1` across Maven modules, CLI version output, README, and bundle/setup script
  default JAR paths.

### Fixed

- GUI rail dialog serialization compile warning under strict `-Werror` builds by marking non-serializable state fields
  as `transient`.

## [1.5.0] - 2026-03-02

### Added

- New `rail-helper` CLI command to replace brittle Excel macro math with deterministic item-footprint planning.
- New rail planning/export core package (`com.tbg.wms.core.rail`) for reusable family-percentage calculations and
  `_TrainDetail.csv` output.
- CLI tests for rail-helper command output generation and missing-footprint reporting.
- GUI queue-input parser tests for mixed prefix handling and validation boundaries.

### Changed

- Refactored rail-helper CSV ingestion to stream rows instead of loading full files into memory.
- Split `LabelGuiFrame` preview/math text assembly and queue input parsing into dedicated helpers (
  `LabelPreviewFormatter`, `QueueInputParser`) to improve SRP and reduce frame complexity.
- Updated README with rail-helper usage, options, and merge-output details.
- Updated `.gitignore` to exclude imported VBA macro and Word template artifacts used for analysis.
- Project metadata/version updated to `1.5.0` across Maven modules, CLI version, package docs, and setup/bundle script
  JAR defaults.

### Fixed

- None.

## [1.4.0] - 2026-03-02

### Added

- New dedicated `gui` Maven module for Swing-based workflows and dialogs.

### Changed

- Split GUI classes out of `cli` module into `gui` module while preserving runtime behavior and package contracts.
- Updated Maven reactor/module graph so `cli` depends on `gui` for desktop command launch and shared GUI workflows.
- Refreshed documentation, package-level Javadocs, and API comments to describe the module split and new GUI boundaries.
- Updated project metadata/versioning to `1.4.0`, including CLI version and build/setup script jar defaults.

### Fixed

- None.

## [1.3.2] - 2026-03-02

### Added

- Streamlined GUI barcode dialog with a new `Advanced Settings...` button that contains low-level layout/symbology
  controls.
- Scanner profile hint in barcode UI indicating Honeywell Granit 1980i / THOR VM1A optimized defaults.

### Changed

- Project/version metadata updated to `1.3.2` across Maven modules, CLI version, README, and setup/bundle scripts.
- Barcode generation defaults tuned for scanner reliability: `origin-x=60`, `origin-y=60`, `module-width=3`,
  `barcode-height=220`.
- Refactored oversized GUI class responsibilities into dedicated components (`BarcodeDialogFactory`,
  `TextFieldClipboardController`) to improve SRP and reduce UI coupling.
- Maven project metadata now explicitly declares developer identity: `Zeyad Rashed <zeyad.rashed@tropicana.com>`.
- Documentation/Javadocs refreshed for GUI package extraction, including a new `cli.gui` package overview with navigable
  `@link` references.

### Fixed

- None.

## [1.3.1] - 2026-02-27

### Added

- Configurable GUI right-click clipboard cooldown via `RIGHT_CLICK_COOLDOWN_MS` (with legacy env var fallback support).
- CLI `run` mode support for Carrier Move IDs via `--carrier-move-id`, with mutually-exclusive ID validation against
  `--shipment-id`.

### Changed

- Project/version metadata updated to `1.3.1` across Maven modules, CLI version, README, and setup/bundle scripts.
- CLI `run` workflow now uses the shared advanced print path for shipment and carrier modes (consistent summary, printer
  resolution, safety bounds, and print-to-file behavior).
- Documentation refresh across `README`, `CHANGELOG`, command/package Javadocs, and package-level metadata.
- Expanded package-level Javadocs with `@link` navigation sections for faster API discovery in generated docs.

### Fixed

- Restored full per-shipment detail rendering inside Carrier Move stop expanders, including shipment header, label plan,
  and SKU-level pallet math.
- Prevented accidental double right-click copy/paste actions by debouncing repeated clipboard actions within a short
  cooldown window.
- Resolved strict `-Werror` compile blockers in CI by adding `serialVersionUID` to WMS exception classes.
- Removed a warning-generating ignored auto-closeable probe in DB pool initialization to keep Javadoc pre-compile clean.
- Corrected stale `DbQueryRepository` Javadoc `@throws` references to non-existent exception types.

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
- Reduced redundant DB session churn in carrier move preparation by reusing one repository context for mapped shipment
  loads.

### Fixed

- None.

## [1.2.3] - 2026-02-26

### Added

- Terminal-style right-click clipboard behavior in GUI text fields: right-click copies selected text, or pastes
  clipboard at cursor when no text is selected.

### Changed

- Project/version metadata updated to `1.2.3` across Maven modules, CLI version output, documentation, and setup/bundle
  script default JAR paths.

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
- Development documentation now explicitly requires a JDK for Maven builds and documents the
  `No compiler is provided in this environment` remediation.
- Removed unchecked/raw-type deserialization in snapshot loading and reduced avoidable allocations in CLI/GUI label
  loops.
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

- Corrected pallet math for full and partial pallet calculation so GUI preview and label counts match per-pallet
  planning rules.

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
