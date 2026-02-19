# WMS Pallet Tag System - Beta Release (v1.0.0-BETA)

**Release Date:** February 18, 2026  
**Status:** Beta - Fully Functional with Known Limitations

---

## Executive Summary

The WMS Pallet Tag System is a production-grade Java CLI application for generating and printing Zebra ZPL pallet labels from Oracle WMS data. This beta release is feature-complete for core operations and has been validated in testing environments.

### Release Classification
- **Production Ready:** Core label generation, printing, and Oracle integration
- **Beta Status:** GUI workflow, some edge cases
- **Known Limitations:** See section below

---

## What's Implemented

### Core Features (✓ Complete)

1. **Database Integration**
   - Oracle read-only access with HikariCP connection pooling
   - Configurable connection timeout and validation
   - Complete shipment/LPN/LineItem data mapping
   - Footprint-based pallet planning

2. **Label Generation**
   - ZPL template engine with placeholder substitution
   - Walmart Canada label template (4x6 @203 DPI)
   - Special character escaping and validation
   - SSCC-18 barcode generation (Code 128)
   - Pallet sequence numbering (X of Y)

3. **Printer Integration**
   - YAML-driven printer inventory and routing configuration
   - Network printing via TCP 9100 RAW socket
   - Rule-based routing (EQUALS, STARTS_WITH, CONTAINS)
   - Retry logic with exponential backoff
   - Manual printer override capability

4. **CLI Commands**
   - `config` - Display effective configuration with password redaction
   - `db-test` - Database connectivity diagnostics
   - `run` - Shipment label generation and printing
   - `gui` - Desktop workflow with preview and confirm-print

5. **Walmart Integration**
   - SKU matrix CSV lookup for Walmart item field
   - Intelligent field population (blank if no match)
   - Shipment footprint data for pallet planning
   - Virtual pallet generation for SKU-only shipments

6. **Operational Features**
   - Dry-run mode (no printing/file writing)
   - Configuration hot-loading from .env files
   - Structured logging with SLF4J/Logback
   - JSON snapshot persistence for replay
   - Safety by default (read-only DB, configurable printing)

### Commands Implemented
- ✓ `config` - Show effective configuration
- ✓ `db-test` - Test database connectivity
- ✓ `run` - Generate and print labels
- ✓ `gui` - Desktop GUI workflow

### Commands Not Yet Implemented
- ⬜ `template` - Generate blank label templates
- ⬜ `print-template` - Print template to network printer
- ⬜ `manual` - Manual label entry with GUI
- ⬜ `replay` - Replay shipment from snapshot JSON

---

## Known Limitations & Bugs

### Minor Known Issues

1. **GUI Window Focus**
   - On some systems, GUI window may not appear in foreground
   - **Workaround:** Click on taskbar icon to bring window to front

2. **Printer Timeout Edge Case**
   - If printer network is unstable, retry logic may hit timeout
   - **Status:** Documented, graceful error handling in place
   - **Planned Fix:** Configurable backoff strategy in v1.1

3. **SKU Matrix File Discovery**
   - File lookup checks 6 candidate paths before failing
   - **Improvement Planned:** Allow explicit path via config in v1.1

4. **Label Template Paths**
   - Template file must be in `config/templates/` directory
   - **Improvement Planned:** Configurable template path in v1.1

5. **Shipment Footprint Edge Case**
   - Some historical shipments may lack footprint data
   - **Workaround:** Virtual pallets generated from LPN count
   - **Status:** Documented in operational notes

### Tested Scenarios
- ✓ Walmart Canada orders (primary use case)
- ✓ Multi-pallet shipments
- ✓ Virtual pallet generation (SKU-only shipments)
- ✓ Printer routing with fallback
- ✓ Dry-run label generation
- ✓ Configuration hot-loading
- ✓ Database connectivity failures (graceful handling)

### Not Yet Tested
- Legacy Oracle schema variations (different PRTMST column names)
- Very large shipments (>100 pallets)
- Network printer failover scenarios

---

## Installation & Setup

### System Requirements
- **Java:** 11+ (21 recommended)
- **Platform:** Windows, Linux, macOS (with Java installed)
- **Network:** Oracle WMS access, Zebra printer network access (optional for dry-run)
- **Disk:** ~200MB for bundled distribution

### Option 1: Development Build
```bash
git clone https://github.com/notzune/wms-pallet-tag-system.git
cd wms-pallet-tag-system
copy .env.example .env
# Edit .env with credentials
mvnw.cmd test
mvnw.cmd -pl cli -am package
java -jar cli/target/cli-1.0.0-SNAPSHOT.jar config
```

### Option 2: Standalone JAR (Requires Java 11+)
```bash
# Download cli-1.0.0-SNAPSHOT.jar from releases
java -jar cli-1.0.0-SNAPSHOT.jar config
```

Place a `wms-tags.env` file in the same directory as the JAR:
```env
ACTIVE_ENV=PROD
WMS_ENV=PROD
ORACLE_USERNAME=your_user
ORACLE_PASSWORD=your_pass
ORACLE_SERVICE=WMSP
ORACLE_PORT=1521
...
```

### Option 3: Portable Bundle with JRE (Recommended)
```bash
# Download: wms-pallet-tag-system-v1.0.0-portable.zip (~150MB)
# Extract to C:\Program Files\wms-pallet-tag-system
# Run: wms-pallet-tag-system.bat
```

See **PORTABLE-INSTALLATION.md** for detailed setup.

---

## Quick Start

### 1. Verify Configuration
```bash
java -jar cli-*.jar config
```

### 2. Test Database Connection
```bash
java -jar cli-*.jar db-test
```

### 3. Generate Labels (Dry-Run)
```bash
java -jar cli-*.jar run --shipment-id 8000141715 --dry-run --output-dir out/
```

### 4. Print Labels
```bash
java -jar cli-*.jar run --shipment-id 8000141715 --output-dir out/
```

### 5. Launch GUI
```bash
java -jar cli-*.jar gui
# or simply:
java -jar cli-*.jar
```

---

## File Tree & Project Structure

```
wms-pallet-tag-system/
│
├── README.md                           # Main documentation
├── CHANGELOG.md                        # Version history
├── LICENSE                             # MIT-style license
├── pom.xml                             # Maven parent configuration
│
├── .env                                # Local config (git-ignored)
├── .env.example                        # Configuration template
├── .gitignore                          # Git exclusions
│
├── mvnw                                # Maven wrapper (Unix)
├── mvnw.cmd                            # Maven wrapper (Windows)
├── .mvn/                               # Maven wrapper config
│   └── wrapper/
│       ├── maven-wrapper.jar
│       ├── maven-wrapper.properties
│       └── MavenWrapperDownloader.java
│
├── config/                             # Site configuration and templates
│   ├── walmart-sku-matrix.csv          # SKU mapping for Walmart items
│   ├── wms-tags.env.example            # Packaged JAR env template
│   ├── TBG3002/                        # Site-specific (TBG3002 = Jersey City)
│   │   ├── printers.yaml               # Printer inventory
│   │   └── printer-routing.yaml        # Routing rules
│   └── templates/
│       ├── walmart-canada-label.zpl    # 4x6 label template
│       └── (future templates)
│
├── core/                               # Core domain & services module
│   ├── pom.xml
│   ├── src/main/java/com/tbg/wms/core/
│   │   ├── AppConfig.java              # Configuration management
│   │   ├── package-info.java           # Module documentation
│   │   ├── exception/
│   │   │   ├── WmsException.java       # Base exception
│   │   │   ├── WmsConfigException.java
│   │   │   └── WmsDbConnectivityException.java
│   │   ├── model/
│   │   │   ├── Shipment.java           # Top-level domain object
│   │   │   ├── Lpn.java                # Pallet/LPN
│   │   │   ├── LineItem.java           # SKU line item
│   │   │   ├── NormalizationService.java
│   │   │   ├── SnapshotService.java    # JSON persistence
│   │   │   ├── ShipmentSkuFootprint.java
│   │   │   ├── PalletPlanningService.java
│   │   │   └── package-info.java
│   │   ├── label/
│   │   │   ├── LabelDataBuilder.java   # Build label data from shipment
│   │   │   ├── LabelData.java          # Label field values
│   │   │   └── package-info.java
│   │   ├── template/
│   │   │   ├── LabelTemplate.java      # ZPL template model
│   │   │   ├── ZplTemplateEngine.java  # Placeholder substitution
│   │   │   └── package-info.java
│   │   ├── sku/
│   │   │   ├── SkuMappingService.java  # CSV-based SKU lookup
│   │   │   └── package-info.java
│   │   ├── print/
│   │   │   ├── PrinterConfig.java      # Printer endpoint config
│   │   │   ├── RoutingRule.java        # Routing rule definition
│   │   │   ├── PrinterRoutingService.java  # YAML-driven routing
│   │   │   ├── NetworkPrintService.java    # TCP 9100 printing
│   │   │   ├── WmsPrintException.java      # Print errors
│   │   │   └── package-info.java
│   │   └── db/
│   │       ├── DbConnectionPool.java   # HikariCP wrapper
│   │       └── (legacy components)
│   ├── src/main/resources/
│   │   ├── logback.xml                 # Logging configuration
│   │   └── wms-defaults.properties     # Default values
│   └── src/test/java/com/tbg/wms/core/
│       ├── AppConfigTest.java
│       ├── label/
│       │   └── LabelDataBuilderTest.java
│       ├── model/
│       │   ├── NormalizationServiceTest.java
│       │   └── PalletPlanningServiceTest.java
│       └── template/
│           └── TemplateEngineTest.java
│
├── db/                                 # Database access module
│   ├── pom.xml
│   ├── src/main/java/com/tbg/wms/db/
│   │   ├── DbConnectionPool.java       # Connection pooling
│   │   ├── DbHealthService.java        # Connection validation
│   │   ├── DbQueryRepository.java      # Repository interface
│   │   ├── OracleDbQueryRepository.java # Oracle implementation
│   │   ├── DataSourceFactory.java      # DataSource creation
│   │   ├── package-info.java           # Module documentation
│   │   └── (legacy components)
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/com/tbg/wms/db/
│       └── OracleDbQueryRepositoryTest.java
│
├── cli/                                # CLI & entry point module
│   ├── pom.xml
│   ├── src/main/java/com/tbg/wms/cli/
│   │   ├── CliMain.java                # Application entry point
│   │   ├── package-info.java           # Module documentation
│   │   ├── commands/
│   │   │   ├── RootCommand.java        # Root CLI command
│   │   │   ├── ShowConfigCommand.java  # config command
│   │   │   ├── DbTestCommand.java      # db-test command
│   │   │   ├── RunCommand.java         # run command (labels)
│   │   │   ├── GuiCommand.java         # gui command
│   │   │   ├── package-info.java
│   │   │   └── (future: TemplateCommand, ManualCommand, etc.)
│   │   └── gui/
│   │       ├── LabelGuiFrame.java      # Main GUI window
│   │       ├── GuiUtil.java            # GUI utilities
│   │       └── (future GUI components)
│   ├── src/main/resources/
│   │   └── logback.xml
│   └── src/test/java/com/tbg/wms/cli/
│       └── (command tests - future)
│
├── scripts/                            # Utility scripts
│   ├── build-portable-bundle.ps1       # Create portable distribution
│   ├── setup-wms-tags.ps1              # Windows setup script
│   ├── setup-wms-tags.bat              # Windows batch installer
│   ├── wms-tags-gui.bat                # GUI launcher
│   ├── wms-tags.bat                    # CLI launcher
│   ├── verify-wms-tags.ps1             # Verification script
│   └── verify-wms-tags.bat
│
├── analysis/                           # Analysis & documentation (not deployed)
│   ├── docs/
│   │   ├── ANALYSIS_STATUS.md
│   │   └── (analysis findings)
│   ├── db-dumps/                       # Database schema dumps
│   ├── python-tools/                   # Analysis tools
│   └── (exploratory - excluded from releases)
│
├── dist/                               # Distribution artifacts (generated)
│   ├── wms-pallet-tag-system-v1.0.0-portable.zip
│   └── (portable bundle with JRE)
│
├── logs/                               # Runtime logs
│   └── wms-tags.log
│
├── out/                                # Generated outputs
│   ├── labels/                         # Generated ZPL files
│   └── gui-*/                          # GUI session outputs
│
└── walmart_sku_matrix.csv              # Root-level SKU matrix copy

```

---

## Architecture Overview

### Module Dependencies
```
cli/
  └── depends on: core, db

core/
  └── depends on: (no internal deps, external: jackson, slf4j, dotenv, hikaricp)

db/
  └── depends on: core
```

### Runtime Flow (run command)
```
1. Load Configuration (AppConfig, .env)
2. Load SKU Matrix (CSV)
3. Load ZPL Template
4. Connect to WMS Database
5. Query Shipment + LPNs + LineItems
6. Load Shipment Footprint Data
7. Build Label Data (LabelDataBuilder)
8. Generate ZPL Labels (ZplTemplateEngine)
9. Load Printer Routing Config
10. Route Printers (PrinterRoutingService)
11. Print Labels (NetworkPrintService)
12. Save Artifacts & Snapshots
```

### Key Design Patterns
- **Repository Pattern:** OracleDbQueryRepository abstracts DB access
- **Template Method:** ZplTemplateEngine with placeholder substitution
- **Builder Pattern:** LabelDataBuilder constructs label data
- **Strategy Pattern:** Multiple routing rule types (EQUALS, STARTS_WITH, CONTAINS)
- **Immutable Models:** Domain objects (Shipment, Lpn, LineItem) are immutable

---

## Known Issues & Workarounds

| Issue | Severity | Workaround | Version |
|-------|----------|-----------|---------|
| GUI window not in foreground | Low | Click taskbar to bring window forward | v1.0.0-BETA |
| Printer timeout on unstable networks | Medium | Retry with longer timeout via env var | v1.0.0-BETA |
| SKU matrix file discovery complex | Low | Place file in `config/` directory | v1.0.0-BETA |
| Template path hard-coded | Low | Symbolic links to `config/templates/` | v1.0.0-BETA |
| Footprint data missing for old shipments | Low | Virtual pallets auto-generated | v1.0.0-BETA |

---

## Deployment Checklist

- [ ] Java 11+ installed and in PATH
- [ ] Oracle WMS network connectivity verified
- [ ] Zebra printer network connectivity verified (if printing)
- [ ] Configuration file (.env or wms-tags.env) created with credentials
- [ ] Walmart SKU matrix CSV in correct location
- [ ] ZPL template in `config/templates/`
- [ ] Printer configuration YAML loaded
- [ ] Database test passes: `java -jar cli-*.jar db-test`
- [ ] Dry-run test passes: `java -jar cli-*.jar run --shipment-id <ID> --dry-run`
- [ ] Printer test passes: Single label sent to DISPATCH printer

---

## Performance Notes

- **Label Generation:** ~2-3 seconds per shipment (100 pallets)
- **Database Query:** ~1-2 seconds (depends on WMS load)
- **Printing:** ~1 second per label + network latency
- **Total Time:** Typically 5-15 seconds for average shipment

---

## Support & Feedback

- **Author:** Zeyad Rashed (zeyad.rashed@tropicana.com)
- **Repository:** https://github.com/notzune/wms-pallet-tag-system
- **Issues:** Report in GitHub issues with reproduction steps
- **Questions:** Contact Zeyad Rashed

---

## Roadmap for v1.1.0

- [ ] Template command (generate blank templates)
- [ ] Manual command (manual label entry with validation)
- [ ] Replay command (replay from JSON snapshots)
- [ ] Print-template command
- [ ] Configurable template paths
- [ ] Enhanced printer failover logic
- [ ] Web-based admin dashboard
- [ ] Batch shipment processing
- [ ] Label audit trail database
- [ ] Multi-site printer management

---

## License

Licensed under the terms in `LICENSE`. Copyright © 2026 Zeyad Rashed.

---

**Thank you for using WMS Pallet Tag System!**

