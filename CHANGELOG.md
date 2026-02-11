# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added (Test-Analysis Module - Database Discovery)
- test-analysis Maven module for WMS schema discovery and validation
- WmsSchemaAnalyzer utility for read-only database introspection
- Schema discovery methods: tables, columns, data types
- Pattern-based table/column search
- Query validation and result mapping
- Column candidate finding (shipment ID, LPN, staging location)
- Comprehensive test coverage for analyzer (15+ test methods)
- Package documentation for analysis package

### Added (Sprint 3 - ZPL Label Generation)
- LabelTemplate class for ZPL template representation
- Placeholder extraction and validation at template construction
- Placeholder name validation (alphanumeric + underscore only)
- ZplTemplateEngine for template-driven label generation
- Template field substitution with validation
- Special character escaping for ZPL safety (^, ~, {, })
- Field length validation (maximum 255 characters per ZPL spec)
- ZPL output validation (checks for ^XA start and ^XZ end markers)
- Deterministic label generation (same input → same output)
- Comprehensive template and engine test coverage (40+ test methods)
- Package documentation (package-info.java) for template package

### Design Highlights (Sprint 3)
- Immutable template design prevents accidental modification
- Stateless engine methods for thread-safe operation
- Type-safe validation at template construction time
- Comprehensive error messages for invalid templates/fields
- Zero external dependencies (Java standard library only)

### Planned for Sprint 4
- ZPL label template engine with placeholder mapping and substitution
- Template-driven generation supporting static elements and dynamic fields
- Barcode generation (Code128 or QR codes as specified)
- Label plan JSON output mapping fields to final printable values
- Validation of field lengths and barcode payloads per template requirements
- Deterministic output verification tests ensuring same snapshot produces identical ZPL

### Planned for Sprint 4
- Printer routing module with YAML rule evaluation
- Network printing service with TCP 9100 connection to Zebra printers
- Retry logic with exponential backoff and connection timeout handling
- Integration of routing engine with run command
- Dry-run mode that skips network printing but validates routing and generates artifacts
- Printer override flag (--printer) for testing and manual routing override
- Integration tests with mock TCP server for print failure scenarios

### Planned for Sprint 5
- Blank template label generation for calibration and training
- Manual label mode accepting operator-supplied fields via --field parameters
- Template command for generating blank labels without database
- Print-template command for printing blank templates to specified printer
- Manual command with field validation (required fields, barcode constraints, length limits)
- Support for manual mode via JSON input file

### Planned for Sprint 6+
- Snapshot replay mode for regenerating labels without database access
- Performance optimization for large LPN batches (100+ labels per shipment)
- Streaming/incremental processing to keep memory bounded
- Comprehensive retry policies with metrics and instrumentation
- Daemon mode preparation (connection pool lifecycle, per-job isolation)
- Integration with centralized logging (optional Splunk/ELK support)

## [Sprint 1] - 2026-02-10

### Database Connectivity and Structured Logging

Sprint 1 completed the database layer foundation and production-grade logging infrastructure, establishing core patterns for error handling and observability that subsequent sprints will build upon.

### Added
- New `db` module providing database access layer abstraction
- DbConnectionPool service wrapping HikariCP with lifecycle management and error diagnostics
- DbConnectivityDiagnostics record capturing connection test results (duration, pool stats, DB version)
- SQL error mapping to specific error types with remediation hints (DNS failures, port issues, auth problems, service not found)
- Typed exception hierarchy: WmsException base class with exit codes and remediation hints
- WmsConfigException for configuration errors (exit code 2) with user guidance
- WmsDbConnectivityException for connectivity errors (exit code 3) with troubleshooting steps
- SLF4J + Logback integration with console and rolling file appenders
- Logback configuration (logback.xml) with 10MB rolling files, 7-day retention, async appending for performance
- MDC (Mapped Diagnostic Context) support for correlation IDs (jobId, site, env) in all logging
- Comprehensive DbTestCommand rewrite with config validation, pool creation, connectivity testing
- DbTestCommand output with formatted configuration display, pool statistics, and actionable error messages
- Unit tests for exception hierarchy covering constructors, exit codes, and remediation hints
- Unit tests for DbConnectionPool diagnostics record structure and pool creation error paths

### Changed
- DbTestCommand now uses DbConnectionPool instead of standalone DataSourceFactory and DbHealthService
- DbTestCommand error handling uses typed exceptions with automatic exit code mapping
- DbTestCommand output includes structured logging with job tracing via unique jobId
- Error messages include specific remediation steps (5 troubleshooting actions per failure type)
- All database errors now map to SQL state codes for precise error categorization

### Deprecated
- DataSourceFactory in core module (use DbConnectionPool from db module instead)
- DbHealthService in core module (use DbConnectionPool.testConnectivity() instead)

### Fixed
- Fixed bug in AppConfig.siteHost() where variable name was incorrect in environment-scoped host lookup
- Fixed variable shadowing issue in siteHost() method using wmsEnvironment() correctly

### Security
- Sensitive configuration values (passwords, tokens) redacted in CLI output via ShowConfigCommand
- Passwords displayed as asterisks in logs without exposing actual values
- Structured logging with MDC does not log sensitive configuration in message bodies
- SQL error messages filtered to hide service names and internal details while preserving remediation value

### Testing Strategy
- Unit tests using JUnit 5 and Mockito for dependency isolation
- Exception tests verify exit codes and remediation hint propagation
- Connection pool tests verify diagnostics record contract and error handling
- No live database required for unit tests (all dependencies mocked)
- Ready for integration tests in Sprint 2 with test database or TestContainers

### Architectural Notes
- Separation of concerns: core (config/exceptions) vs db (pooling/queries) vs cli (commands)
- Typed exceptions eliminate generic catch-all blocks, enabling precise error handling
- Structured logging with MDC enables correlation across modules and log files
- DbConnectionPool provides boundary between Hikari implementation details and rest of system
- Error remediation hints embedded in exceptions enable user self-service without support

### Build and Deployment
- Maven multi-module build successfully compiles all modules
- All unit tests pass without failures
- CLI jar artifact builds successfully with shade plugin for fat jar execution
- No compiler warnings (except expected IDE hints)
- Ready for staging and review; no changes pushed upstream

## [0.1.0] - 2026-02-10

### Sprint 0: Project Scaffolding

Sprint 0 established the foundational project structure and configuration handling, creating the baseline for database and CLI work in subsequent sprints.

### Added
- Multi-module Maven project structure with core, cli, and db modules
- Parent pom.xml with dependency management and build plugin configuration
- AppConfig class loading configuration from .env file and environment variables with precedence rules
- ShowConfigCommand displaying effective configuration with passwords redacted
- DbTestCommand stub for database connectivity validation (completed in Sprint 1)
- RootCommand as Picocli root with subcommand registration
- CliMain entry point bootstrapping CLI with proper exit code handling
- README.md with quick start, configuration reference, CLI command documentation, troubleshooting
- CHANGELOG.md using Keep a Changelog format
- INSTRUCTIONS.md with comprehensive system requirements and architecture guidance
- Copyright headers on all Java files with Javadoc @author and @email tags
- Configuration examples: .env.example, printer-routing.yaml, printers.yaml
- Unit tests for AppConfig validating default values and required key detection
- All public classes and methods with comprehensive Javadoc documentation

### Documentation Standards
- All documentation uses professional prose without decorative elements
- Javadoc on public APIs follows Oracle conventions with @param, @return, @throws, @since tags
- Sprint tracking consolidated in CHANGELOG.md with dedicated sprint sections
- Architecture documentation in README.md and INSTRUCTIONS.md only
- No additional documentation files created without explicit approval
- Conventional Commits format enforced for all commits with clear scope and description

### Build and Deployment
- Maven 3.8.1+ builds entire multi-module project successfully
- Java 17+ required (Java 21 recommended)
- All unit tests pass without failures
- Configuration via .env file or environment variables with proper precedence
- CLI executes with proper exit codes for success and various error conditions




