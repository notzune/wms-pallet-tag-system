# WMS Pallet Tag System

A production-grade Java system for generating and printing Zebra ZPL pallet/shipping labels from WMS (Oracle) data. Built with a modular Maven architecture to support current CLI operations and future extensions as background workers and web GUI.

## Features

- **WMS Data Integration**: Connects to Oracle WMS databases (QA and Prod) to retrieve shipment, LPN, and line item data
- **Data Validation**: Comprehensive validation including required fields, length constraints, and barcode payload validation
- **ZPL Label Generation**: Template-driven label generation with deterministic output
- **Smart Printer Routing**: Location-based printer routing with configuration-driven rules
- **Snapshot & Replay**: Capture normalized WMS data as JSON snapshots for replay and debugging without DB access
- **Comprehensive Logging**: Structured logging with correlation IDs and operational traceability
- **Dry-Run Mode**: Full execution without network printing for validation and verification
- **Flexible CLI**: Multiple execution modes (DB-driven, snapshot replay, manual labels, blank templates)

## Quick Start

### Prerequisites

- Java 17 or higher (Java 21 recommended)
- Maven 3.8.1 or higher
- Windows 10 or later (primary target)
- Oracle WMS database connectivity (QA and/or Prod)
- Network access to Zebra printers on port 9100

### Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-org/wms-pallet-tag-system.git
   cd wms-pallet-tag-system
   ```

2. **Install dependencies and build**:
   ```bash
   mvn clean install
   ```

3. **Configure environment**:
   - Copy `.env.example` to `.env` (local only, ignored by git):
     ```bash
     cp .env.example .env
     ```
   - Edit `.env` with your site-specific Oracle credentials and host information:
     ```dotenv
     WMS_ENV=QA
     ACTIVE_SITE=TBG3002
     ORACLE_USERNAME=<your_username>
     ORACLE_PASSWORD=<your_password>
     SITE_TBG3002_QA_HOST=<qa_host_or_ip>
     SITE_TBG3002_PROD_HOST=<prod_host_or_ip>
     ```

4. **Test configuration**:
   ```bash
   java -jar cli/target/cli-*.jar config
   ```

5. **Verify database connectivity** (Sprint 1):
   ```bash
   java -jar cli/target/cli-*.jar db-test
   ```

## Usage

### Display Effective Configuration

```bash
java -jar cli/target/cli-*.jar config
```

Shows all active configuration values with sensitive data (passwords) redacted. Useful for troubleshooting configuration precedence.

### Verify Database Connectivity

```bash
java -jar cli/target/cli-*.jar db-test
```

Validates connection to the Oracle WMS database. Reports:
- Site and environment configuration
- Database host, port, service name
- Connection pool size and timeouts
- Actual connection test results (duration, pool stats, DB version)
- Actionable error messages if connectivity fails (e.g., "Check DB_HOST and VPN", "Service not found")

**Exit codes**:
- `0` – Database is reachable and responding
- `2` – Configuration error (missing env vars, invalid settings)
- `3` – Database connectivity error (host unreachable, port closed, auth failed, service not found)
- `10` – Unexpected internal error

Example output (success):
```
=== Database Connectivity Test ===

Configuration:
  Active Site:     TBG3002 (Jersey City)
  WMS Environment: QA
  Database Host:   10.19.68.100
  Database Port:   1521
  Service Name:    WMSP
  Username:        RPTADM
  JDBC URL:        jdbc:oracle:thin:@//10.19.68.100:1521/WMSP

Pool Configuration:
  Max Size:        5
  Connect Timeout: 3000 ms
  Validation Timeout: 2000 ms

Attempting to create connection pool...
Testing connectivity...

✓ Database connectivity verified!

Test Results:
  Duration:        250 ms
  Database Version: Oracle Database 19c
  Pool Statistics: 1 active, 4 idle

The database is reachable and responding. You may proceed with label operations.
```

### Generate and Print Labels (WMS-driven)

```bash
# Dry-run: validate without printing
java -jar cli/target/cli-*.jar run <shipment_id> --dry-run --write-snapshot --label-plan --out out/

# Live print with default routing
java -jar cli/target/cli-*.jar run <shipment_id> --out out/

# Force specific printer
java -jar cli/target/cli-*.jar run <shipment_id> --printer DISPATCH --out out/
```

### Print Blank Templates

```bash
# Generate blank label files
java -jar cli/target/cli-*.jar template --count 5 --out out/templates

# Print blank templates directly
java -jar cli/target/cli-*.jar print-template --count 2 --printer OFFICE
```

### Manual Label Mode

```bash
java -jar cli/target/cli-*.jar manual \
  --field lpn=LP123456 \
  --field shipToName="ACME Corp" \
  --field shipToCity="Jersey City" \
  --format both \
  --dry-run \
  --out out/manual
```

### Replay from Snapshot

```bash
# Re-generate and print from a previous snapshot without DB access
java -jar cli/target/cli-*.jar run \
  --replay out/2026-02-10T103045Z_TBG3002_SHIP123_snapshot.json \
  --dry-run \
  --out out/
```

## Configuration Reference

### Environment Variables

All configuration can be set via environment variables or `.env` file. Environment variables take precedence.

#### Core Settings

| Variable | Default | Description |
|----------|---------|-------------|
| `ACTIVE_SITE` | (required) | Site code, e.g., `TBG3002` |
| `WMS_ENV` | `QA` | WMS environment: `QA` or `PROD` |

#### Oracle Database

| Variable | Default | Description |
|----------|---------|-------------|
| `ORACLE_USERNAME` | (required) | Database user |
| `ORACLE_PASSWORD` | (required) | Database password |
| `ORACLE_PORT` | `1521` | Database port |
| `ORACLE_SERVICE` | `WMSP` | Service name or SID |
| `SITE_<CODE>_<ENV>_HOST` | (required) | Database host, e.g., `SITE_TBG3002_QA_HOST` |
| `SITE_<CODE>_<ENV>_NAME` | (required) | Display name, e.g., `SITE_TBG3002_NAME=Jersey City` |

#### Connection Pooling

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_POOL_MAX_SIZE` | `5` | Maximum concurrent connections |
| `DB_POOL_CONN_TIMEOUT_MS` | `3000` | Connection timeout in milliseconds |
| `DB_POOL_VALIDATION_TIMEOUT_MS` | `2000` | Validation query timeout |

#### Printing

| Variable | Default | Description |
|----------|---------|-------------|
| `PRINTER_ROUTING_FILE` | `config/printer-routing.yaml` | Path to routing rules YAML |
| `PRINTER_DEFAULT_ID` | `DISPATCH` | Default printer if no rule matches |
| `PRINTER_FORCE_ID` | (optional) | Force all prints to this printer (testing only) |

#### Output

| Variable | Default | Description |
|----------|---------|-------------|
| `OUTPUT_DIR` | `./out` | Output directory for artifacts |
| `SNAPSHOT_DIR` | `./snapshots` | Directory for snapshot files |

### Printer Routing YAML

Edit `config/<SITE>/printer-routing.yaml` to define location-based printing rules:

```yaml
printers:
  DISPATCH:
    id: DISPATCH
    host: 10.19.64.53
    port: 9100
    connect_timeout_ms: 2000
    write_timeout_ms: 5000
  
  OFFICE:
    id: OFFICE
    host: 10.19.64.106
    port: 9100
    connect_timeout_ms: 2000
    write_timeout_ms: 5000

rules:
  # Rule 1: Canadian orders in ROSSI location go to Dispatch
  - name: "Canadian ROSSI → Dispatch"
    staging_location: "ROSSI"
    printer_id: "DISPATCH"
  
  # Rule 2: All other locations go to Office
  - name: "Default → Office"
    staging_location: "*"  # wildcard
    printer_id: "OFFICE"

default_printer_id: "DISPATCH"
```

## CLI Commands

### Global Flags

| Flag | Description |
|------|-------------|
| `--help` | Show help message and exit |
| `--version` | Show version and exit |
| `-v, --verbose` | Increase console logging verbosity |

### Command: `config`

Display effective configuration with secrets redacted.

```bash
java -jar cli/target/cli-*.jar config
```

**Output** (sample):
```
=== WMS Pallet Tag System Configuration ===
Active Site:     TBG3002
WMS Environment: QA
Database Host:   10.19.68.100
Database Port:   1521
Database Service: WMSP
Pool Max Size:   5
Printer Routing: config/TBG3002/printer-routing.yaml
Printer Default: DISPATCH
[Passwords and tokens are redacted]
```

### Command: `db-test` (Sprint 1)

Verify database connectivity.

```bash
java -jar cli/target/cli-*.jar db-test
```

### Command: `run` (Sprint 2-4)

Generate and optionally print labels from WMS data.

```bash
java -jar cli/target/cli-*.jar run <ID> [OPTIONS]
```

**Options**:
- `--id-type {order|shipment|load|auto}` – Input identifier type (default: `auto`)
- `--env {QA|PROD}` – Override WMS environment
- `--site <CODE>` – Override site code
- `--dry-run` – Execute without printing
- `--out <DIR>` – Output directory
- `--format {zpl|json|both}` – Artifact format (default: `both`)
- `--printer <ID>` – Override routing and force printer
- `--write-snapshot` – Save normalized data snapshot
- `--label-plan` – Output label field mapping JSON
- `--replay <FILE>` – Replay from snapshot (no DB)
- `--limit <N>` – Limit number of labels (for testing)
- `--fail-fast` / `--continue-on-error` – Error handling mode

### Command: `template` (Sprint 5)

Generate blank label templates (layout only).

```bash
java -jar cli/target/cli-*.jar template [OPTIONS]
```

**Options**:
- `--count <N>` – Number of labels (default: `1`)
- `--out <DIR>` – Output directory
- `--format {zpl|png}` – Format (default: `zpl`)

### Command: `print-template` (Sprint 5)

Print blank label templates.

```bash
java -jar cli/target/cli-*.jar print-template [OPTIONS]
```

**Options**:
- `--count <N>` – Number of labels (default: `1`)
- `--printer <ID>` – Printer ID (default: routing default)
- `--dry-run` – Validate without printing

### Command: `manual` (Sprint 5)

Print manual labels without WMS data.

```bash
java -jar cli/target/cli-*.jar manual [OPTIONS]
```

**Options**:
- `--field <KEY>=<VALUE>` – Field (repeatable)
- `--input <FILE>` – JSON input file
- `--out <DIR>` – Output directory
- `--printer <ID>` – Printer ID
- `--dry-run` – Validate without printing
- `--format {zpl|json|both}` – Artifact format

## Troubleshooting

### Configuration Issues

**Problem**: `java.lang.IllegalArgumentException: Required configuration key missing: ORACLE_PASSWORD`

**Solution**:
- Ensure `.env` file exists in the working directory, or
- Set environment variables: `export ORACLE_PASSWORD=...`, or
- Use Windows: `setx ORACLE_PASSWORD <value>` (requires restart)
- Verify with: `java -jar cli/target/cli-*.jar config`

**Problem**: `SITE_TBG3002_QA_HOST` not found

**Solution**:
- Check `.env` file has the correct site code in `ACTIVE_SITE`, e.g., `TBG3002`
- Add corresponding host: `SITE_TBG3002_QA_HOST=<ip_or_hostname>`
- For environment-specific hosts, use: `SITE_<CODE>_<ENV>_HOST`

### Database Connectivity Issues

**Problem**: `java.sql.SQLException: Listener refused the connection`

**Solution** (when Sprint 1 `db-test` is available):
1. Run: `java -jar cli/target/cli-*.jar db-test`
2. Verify:
   - Database host is reachable: `ping <DB_HOST>`
   - Database port is open: check firewall / VPN
   - Database service name is correct: `ORACLE_SERVICE=WMSP`
   - Credentials are valid: test in SQL*Plus or similar
3. Check `.env` for typos: `SITE_TBG3002_QA_HOST`, `ORACLE_PORT`, etc.

### Printer Issues

**Problem**: `java.io.IOException: Connection refused on printer DISPATCH`

**Solution**:
1. Verify printer is powered on and reachable: `ping <PRINTER_IP>`
2. Check `printer-routing.yaml` has correct IP and port (usually 9100)
3. Try with `--dry-run` first to confirm routing: `java -jar ... run <ID> --dry-run`
4. Test printer directly: `java -jar ... test-print --printer DISPATCH`

## Project Structure

```
wms-pallet-tag-system/
├── core/                      # Domain models, interfaces, utilities, exceptions
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/tbg/wms/core/
│   │       ├── App.java
│   │       ├── AppConfig.java
│   │       └── exception/
│   │           ├── WmsException.java
│   │           ├── WmsConfigException.java
│   │           └── WmsDbConnectivityException.java
│   └── src/main/resources/
│       └── logback.xml        # Structured logging configuration
├── db/                        # Database access layer (Oracle, queries, repos)
│   ├── pom.xml
│   └── src/main/java/
│       └── com/tbg/wms/db/
│           └── DbConnectionPool.java
├── cli/                       # Picocli CLI application
│   ├── pom.xml
│   └── src/main/java/
│       └── com/tbg/wms/cli/
│           ├── CliMain.java
│           └── commands/
│               ├── RootCommand.java
│               ├── ShowConfigCommand.java
│               └── DbTestCommand.java
├── config/                    # Configuration files per site
│   └── TBG3002/
│       ├── printer-routing.yaml
│       └── printers.yaml
├── pom.xml                    # Parent POM (multi-module)
├── CHANGELOG.md               # Release notes (Keep a Changelog format)
├── README.md                  # This file
├── SPRINT_1_SUMMARY.md        # Sprint 1 detailed deliverables
├── .env.example               # Configuration template
└── .gitignore
```

### Module Dependencies

- **core**: Base module. No internal dependencies. Depends on: SLF4J, dotenv-java, HikariCP, Oracle JDBC, Logback
- **db**: Database layer. Depends on: core, SLF4J, HikariCP, Oracle JDBC
- **cli**: CLI application. Depends on: core, db, Picocli, SLF4J

Future modules (Sprint 2+): `routing`, `zpl`, `print`, `snapshot`, `validation`

## Development

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Build CLI Executable

```bash
mvn -pl cli clean package
java -jar cli/target/cli-*.jar config
```

### Code Standards

- **Language**: Java 17+
- **Build Tool**: Maven 3.8.1+
- **Logging**: SLF4J + Logback
- **Testing**: JUnit 5
- **Code Style**: Follow SOLID principles; all public APIs require Javadoc
- **Commits**: Use Conventional Commits format (`feat:`, `fix:`, `docs:`, etc.)
- **Changelog**: Update `CHANGELOG.md` in same commit as behavioral changes

### Versioning

This project follows [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking API changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

Version is defined in root `pom.xml` as `<version>1.0.0-SNAPSHOT</version>`.

## Contributing

1. Create feature branch from `dev`: `git checkout -b feature/my-feature`
2. Make changes and commit with Conventional Commits: `git commit -m "feat(cli): add new command"`
3. Update `CHANGELOG.md` with your changes
4. Ensure `mvn test` passes
5. Push and create pull request to `dev`
6. After review and merge, PRs to `main` trigger release process

## License

[Your License Here] – e.g., Proprietary, Apache 2.0, MIT

## Contact

For questions or support, contact the WMS team or open an issue in the repository.

Alternatively , email: zeyad.rashed@tropicana.com

## Sprint Roadmap

- **Sprint 0** (✅ Complete): Scaffolding, config command, changelog, copyright headers
- **Sprint 1** (✅ Complete): Database connectivity testing, typed exceptions, structured logging
- **Sprint 2**: Minimal WMS data pull and snapshot capture
- **Sprint 3**: ZPL generation from snapshot
- **Sprint 4**: Printer routing and network printing
- **Sprint 5**: Blank templates and manual labels
- **Sprint 6+**: Performance hardening, comprehensive retry policies, replay mode

