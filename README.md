# WMS Pallet Tag System

[![Release Bundle](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/release.yml/badge.svg?branch=dev)](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/release.yml)
[![Javadoc Pages](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/javadoc-pages.yml/badge.svg?branch=dev)](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/javadoc-pages.yml)
[![API Docs](https://img.shields.io/badge/docs-javadoc-blue)](https://notzune.github.io/wms-pallet-tag-system/)
![Version](https://img.shields.io/badge/version-1.6.0-blue)
![Java](https://img.shields.io/badge/java-11%2B-orange)
![License](https://img.shields.io/badge/license-Custom-green)

Licensed under the terms in `LICENSE`.

Production Java CLI and GUI for generating and printing Zebra ZPL pallet labels from Oracle WMS data.
Current release: `1.6.0` (2026-03-04).

## Current Scope

Implemented and supported:

- `config` command (resolved runtime config with redaction)
- `db-test` command (database connectivity diagnostics)
- `run` command (shipment or carrier-move label generation and printing)
- `gui` command (desktop workflow with shipment/carrier-move preview and confirm-print)
- `barcode` command (standalone barcode ZPL generation and optional printing)
- `rail-helper` command (rail office merge CSV generation from item footprint data)
- `rail-print` command (WMS-first railcar preview, direct PDF card rendering, optional printing)
- Oracle read-only access
- Printer routing via site YAML
- Walmart SKU matrix lookup for Walmart item field
- Pallet planning summary from footprint maintenance (`PRTFTP` / `PRTFTP_DTL`)
- Bulk queue processing (mixed shipment and carrier move jobs)
- Job persistence and resume for interrupted print runs
- Dedicated `gui` Maven module for Swing workflows (separated from CLI command module)

Not implemented yet:

- `template`, `print-template`, `manual`, `replay` commands

## Prerequisites

- JDK 11+ for development builds (`javac` must be available; JRE-only installs will fail Maven compile)
- Java 11+ runtime for running packaged bundles (JRE is acceptable for runtime only)
- Maven Wrapper included (`mvnw`, `mvnw.cmd`)
- Oracle WMS network access
- Zebra printer network access (for non-dry-run printing)

Portable bundles include a JRE and do not require a separate Java install.

## Setup and Quick Start

### Development build

1. Configure environment:

```bash
copy .env.example .env
```

2. Build and test:

```bash
mvnw.cmd test
```

If you get `No compiler is provided in this environment`, install a JDK and ensure `JAVA_HOME` points to it.

3. Run commands:

```bash
java -jar cli/target/cli-*.jar config
java -jar cli/target/cli-*.jar db-test
java -jar cli/target/cli-*.jar run --shipment-id <SHIP_ID> --dry-run --output-dir out/
java -jar cli/target/cli-*.jar run --carrier-move-id <CMID> --dry-run --output-dir out/
java -jar cli/target/cli-*.jar rail-helper --input-csv <INPUT.csv> --item-footprint-csv <ITEM_FAMILY.csv> --output-dir out/rail-helper
java -jar cli/target/cli-*.jar rail-print --train <TRAIN_ID> --output-dir out/rail-print
java -jar cli/target/cli-*.jar gui
```

### Portable bundle (recommended for operators)

1. Extract the portable package to a folder (example: `C:\wms-pallet-tag-system`).
2. Copy your real environment values into `wms-tags.env`.
3. Run:

```bash
run.bat
```

For CLI usage:

```bash
run.bat config
run.bat db-test
run.bat run --shipment-id <SHIP_ID> --dry-run --output-dir out/
run.bat run --carrier-move-id <CMID> --dry-run --output-dir out/
run.bat rail-helper --input-csv <INPUT.csv> --item-footprint-csv <ITEM_FAMILY.csv> --output-dir out/rail-helper
run.bat rail-print --train <TRAIN_ID> --output-dir out/rail-print
```

On Linux/macOS:

```bash
./run.sh
```

## Configuration

Configuration file precedence:

1. Environment variables
2. `WMS_CONFIG_FILE` path, if set
3. `wms-tags.env` next to the JAR (or working directory)
4. `.env`
5. Built-in defaults

Key settings:

- `WMS_ENV=PROD` (default)
- `ACTIVE_SITE=TBG3002`
- `ORACLE_USERNAME`, `ORACLE_PASSWORD`, `ORACLE_PORT`, `ORACLE_SERVICE`
- `ORACLE_ODBC_DSN` (optional Oracle Net alias fallback, e.g. `TBG3002`)
- `ORACLE_DSN` (optional explicit DSN/JDBC descriptor; use carefully to avoid wrong environment)
- `SITE_<CODE>_<ENV>_HOST` (example `SITE_TBG3002_PROD_HOST`)
- `SITE_<CODE>_SHIP_FROM_NAME`, `SITE_<CODE>_SHIP_FROM_ADDRESS`, `SITE_<CODE>_SHIP_FROM_CITY_STATE_ZIP`
- `PRINTER_ROUTING_FILE=config/TBG3002/printer-routing.yaml`
- `RIGHT_CLICK_COOLDOWN_MS=250` (GUI right-click copy/paste debounce)

Connection fallback order:

1. Primary JDBC URL (`ORACLE_JDBC_URL`, then `ORACLE_DSN`, then host/port/service)
2. Oracle Net alias (`ORACLE_ODBC_DSN` or `ACTIVE_SITE`, e.g. `TBG3002`)
3. Host/port/service URL

## Run Command

```bash
java -jar cli/target/cli-*.jar run (--shipment-id <ID> | --carrier-move-id <ID>) [OPTIONS]
```

Options:

- `--shipment-id` (mutually exclusive with `--carrier-move-id`)
- `--carrier-move-id` (mutually exclusive with `--shipment-id`)
- `--dry-run`
- `--output-dir <DIR>` (default `./labels`)
- `--printer <ID>`
- `--print-to-file` or `--ptf` (write ZPL to `/out` next to the JAR and skip printing)

## Barcode Command

```bash
java -jar cli/target/cli-*.jar barcode --data <PAYLOAD> [OPTIONS]
```

Options:

- `--data <PAYLOAD>` (required)
- `--type CODE128|GS1_128` (default `CODE128`)
- `--orientation PORTRAIT|LANDSCAPE` (default `PORTRAIT`)
- `--label-width-dots <N>` (default `812`)
- `--label-height-dots <N>` (default `1218`)
- `--origin-x <N>` (default `60`)
- `--origin-y <N>` (default `60`)
- `--module-width <N>` (default `3`)
- `--module-ratio <N>` (default `3`)
- `--barcode-height <N>` (default `220`)
- `--human-readable <true|false>` (default `true`)
- `--copies <N>` (default `1`)
- `--output-dir <DIR>` (default `./barcodes`)
- `--dry-run` (skip printing)
- `--printer <ID>` (required unless `--dry-run`)
- `--print-to-file` or `--ptf` (write ZPL to `/out` next to the JAR and skip printing)

## Rail Helper Command

```bash
java -jar cli/target/cli-*.jar rail-helper --input-csv <INPUT.csv> --item-footprint-csv <ITEM_FAMILY.csv> [OPTIONS]
```

Options:

- `--input-csv <FILE>` (required): rail rows with metadata and `ITEM_NBR*` / `TOTAL_CS_ITM*` columns
- `--item-footprint-csv <FILE>` (required): item-to-family footprint lookup
- `--output-dir <DIR>` (default `out/rail-helper`)
- `--template-docx <FILE>` (optional): copy Word template beside generated CSV
- `--train-id <ID>` (optional): export only one train

Output:

- `_TrainDetail.csv` (Word merge-ready columns used by `Print .docx`)
- `rail-helper-summary.txt` (missing-footprint diagnostics and run summary)

Notes:

- This CLI path is CSV-driven and is best used for offline/backfill runs.
- For day-to-day rail operations, use GUI `Tools -> Rail Labels...` (WMS-first workflow).
- Portrait is the default.
- Landscape rotates barcode fields in ZPL (`^FWR` and rotated `^BC`) while keeping the printer in portrait mode (
  `^PON`).
- Printer-level landscape must be configured on the device if true landscape output is required.

## Rail Print Command

```bash
java -jar cli/target/cli-*.jar rail-print --train <TRAIN_ID> [OPTIONS]
```

Options:

- `--train <ID>` (required): full WMS train ID (example: `JC08312025`)
- `--output-dir <DIR>` (default `out/rail-print`)
- `--print` (send generated PDF to default printer after confirmation)
- `--yes` / `-y` (non-interactive mode; skip confirmation prompts)

Workflow:

- Query rail rows from WMS by train
- Aggregate rows by railcar
- Compute CAN/DOM/KEV pallets using per-item `CEILING(cases / casesPerPallet)` math
- Compute deterministic top-family percentages with largest-remainder rounding (stable ordering and 100% total)
- Show preview table (`SEQ`, `VEHICLE`, `CAN`, `DOM`, `KEV`)
- Confirm
- Render direct letter-size rail card PDF (no Word mail merge dependency)
- Include `MISSING: <count>` warning on cards when any short codes are unresolved
- Optionally print

## Rail Labels GUI Workflow

- Open `gui`, then go to `Tools -> Rail Labels...`.
- Enter train ID and click `Load Preview`.
- System pulls rail rows from WMS and resolves footprints by short code from WMS.
- Preview includes:
- Railcar table (`SEQ`, `VEHICLE`, `CAN`, `DOM`, `KEV`, `LOAD_NBR`)
- Railcar card preview panel (item lines + CAN/DOM/KEV + pass/fuel/BH fields)
- Diagnostics panel (row counts and unresolved footprints)
- Click `Generate PDF` to produce a letter-size multi-card PDF.
- Click `Print` to generate PDF and send it to the host default printer.

## Excel VBA Macro Helpers

- VBA helper modules for the legacy Excel rail macro flow are versioned under `vba/`.
- These modules support worksheet setup, WMS query refreshes, footprint refreshes, rollup math, and report orchestration.
- Current helper modules:
- `vba/m_RunReports.bas`
- `vba/m_RefreshData.bas`
- `vba/m_RefreshFootprints.bas`
- `vba/m_Count_Product.bas`
- `vba/m_Formatting.bas`
- `vba/m_Delete.bas`

## GUI Workflow

- Mode defaults to `Carrier Move ID`; `Shipment ID` mode remains available.
- Enter ID, select printer, and click `Preview`.
- Shipment preview shows shipment summary, label plan, and SKU-level pallet math (full vs partial).
- Carrier Move preview shows job summary and expandable stop sections; each stop renders shipment-level detail and SKU
  breakdown.
- Click `Confirm Print` to execute and persist artifacts.
- Shipment mode output path: `out/gui-<shipment>-<timestamp>/`.
- Carrier mode output path: `out/gui-cmid-<cmid>-<timestamp>/`.
- Carrier mode prints all shipment labels in stop order, then per-stop info tags, then one final info tag.
- Shipment mode prints shipment labels and one shipment info tag.
- Select `Print to file` from the printer dropdown to save ZPL under `out/` without printer I/O.
- Use `Tools -> Barcode Generator...` for standalone barcode ZPL generation/printing.
- Use `Tools -> Rail Labels...` for end-to-end rail merge generation from live WMS train data.
- Barcode dialog now defaults to an operator-focused layout and moves low-level controls under `Advanced Settings...`.
- Use queue/resume actions from the GUI to process mixed job batches and recover interrupted jobs.

## Walmart SKU Behavior

- The label field `WAL-MART ITEM #` is populated only when SKU matches the Walmart matrix CSV.
- If SKU does not match (for non-Walmart or non-Canada orders), that field is intentionally left blank.
- If a shipment has no LPN records but has shipment SKU rows, labels are generated from SKU data using virtual pallet
  rows.
- Walmart orders are palletized per pallet, so each pallet receives its own label even for identical SKUs.

## Safety and Data Access

- DB pool is configured read-only.
- Application flow uses SELECT queries only.
- Printing can be skipped entirely with `--dry-run`.

## Build and Test

```bash
mvnw.cmd test
mvnw.cmd -pl cli -am package
```

## Javadoc

Generate aggregated Javadoc locally:

```bash
mvnw.cmd -DskipTests javadoc:aggregate
```

Output is written to `target/site/apidocs`.

The `Javadoc Pages` GitHub Actions workflow publishes the aggregated site to GitHub Pages when enabled in repository
settings.

## Documentation Coverage

Package-level documentation is maintained in every `package-info.java` under:

- `cli/src/main/java/com/tbg/wms/cli/**`
- `core/src/main/java/com/tbg/wms/core/**`
- `db/src/main/java/com/tbg/wms/db`
- `gui/src/main/java/com/tbg/wms/cli/gui/**`

Documentation expectations for helper classes:

- State why the helper was created.
- State the exact responsibility it owns.
- State why it should remain separate (SRP, determinism, reuse, or performance).

Recent examples:

- `DescriptionTextHeuristics` (shared description readability policy)
- `PrtmstDescriptionColumnResolver` (cached PRTMST schema probing boundary)
- `RailFootprintResolver` (deterministic candidate-consistency gate before pallet math)

## CI Workflows

### Javadoc Pages (`.github/workflows/javadoc-pages.yml`)

Triggers on pushes to `main` and `dev`, plus manual dispatch.

- Builds aggregated Javadoc with `mvn javadoc:aggregate`.
- Publishes `target/site/apidocs` to GitHub Pages.

### Release Bundle (`.github/workflows/release.yml`)

Triggers on:

- PRs targeting `main` (builds and uploads a portable ZIP as a workflow artifact).
- Tag pushes matching `v*.*.*` (creates a GitHub Release).

Behavior:

- Builds the CLI JAR.
- Builds the portable bundle using `dist/temurin11-jre.zip`.
- PRs: uploads `dist/wms-pallet-tag-system-<version>-portable.zip` as an artifact. `<version>` is read from
  `cli/target/maven-archiver/pom.properties`.
- Tags: attaches `dist/wms-pallet-tag-system-<version>-portable.zip` to the GitHub Release.
- Uses the matching section from `CHANGELOG.md` for the release body on tag builds.

## Project Structure

```text
wms-pallet-tag-system/
|-- README.md
|-- CHANGELOG.md
|-- INSTRUCTIONS.md               # Development standards and system requirements
|-- LICENSE
|-- pom.xml
|-- .env.example
|-- config/
|   |-- wms-tags.env.example
|   |-- templates/
|   |   `-- walmart-canada-label.zpl
|   `-- TBG3002/
|       |-- printers.yaml
|       `-- printer-routing.yaml
|-- core/
|   |-- pom.xml
|   `-- src/
|       |-- main/
|       |   |-- java/com/tbg/wms/core/
|       |   |   |-- AppConfig.java                  # env + runtime configuration loading
|       |   |   |-- barcode/                        # barcode ZPL builders and barcode logic
|       |   |   |-- db/                             # DB-related core abstractions/models
|       |   |   |-- exception/                      # typed app exceptions + exit code mapping
|       |   |   |-- label/                          # label data composition and label types
|       |   |   |-- labeling/                       # label-domain helper services
|       |   |   |-- location/                       # sold-to / location mapping services
|       |   |   |-- model/                          # core domain models (Shipment, LPN, etc.)
|       |   |   |-- print/                          # printer config, routing, network print
|       |   |   |-- rail/                           # rail planners, CSV support, merge exporters
|       |   |   |-- sku/                            # SKU matrix loading/mapping services
|       |   |   `-- template/                       # template parsing/merge logic
|       |   `-- resources/
|       `-- test/
|           `-- java/com/tbg/wms/core/              # unit tests by package area
|-- db/
|   |-- pom.xml
|   `-- src/
|       |-- main/java/com/tbg/wms/db/
|       |   |-- DbConnectionPool.java               # Oracle/Hikari pool lifecycle
|       |   |-- DbQueryRepository.java              # query contracts used by CLI/GUI
|       |   `-- OracleDbQueryRepository.java        # WMS SQL implementations (shipment, carrier, rail)
|       `-- test/java/com/tbg/wms/db/
|-- gui/
|   |-- pom.xml
|   `-- src/
|       |-- main/java/com/tbg/wms/cli/gui/
|       |   |-- LabelGuiFrame.java                  # desktop shell and tool entrypoints
|       |   |-- LabelWorkflowService.java           # shipment preview/print orchestration
|       |   |-- AdvancedPrintWorkflowService.java   # carrier move / queue / resume orchestration
|       |   |-- BarcodeDialogFactory.java           # barcode UI dialog wiring
|       |   |-- TextFieldClipboardController.java   # terminal-like right-click behavior
|       |   `-- rail/                               # rail GUI workflow package
|       |       |-- RailLabelsDialog.java           # rail workflow dialog
|       |       |-- RailWorkflowService.java        # WMS-first rail prep/diagnostics/orchestration
|       |       `-- RailArtifactService.java        # DOCX/PDF/PRN automation helper
|       `-- test/java/com/tbg/wms/cli/gui/
|           |-- QueueInputParserTest.java
|           `-- rail/
|               `-- RailArtifactServiceTest.java
|-- cli/
|   |-- pom.xml
|   `-- src/
|       |-- main/java/com/tbg/wms/cli/
|       |   |-- CliMain.java                        # CLI entrypoint
|       |   `-- commands/
|       |       |-- RootCommand.java                # top-level command registration
|       |       |-- RunCommand.java                 # shipment/carrier print workflow command
|       |       |-- BarcodeCommand.java             # barcode command
|       |       |-- DbTestCommand.java              # DB diagnostics command
|       |       |-- ShowConfigCommand.java          # resolved config command
|       |       |-- GuiCommand.java                 # launches Swing GUI
|       |       |-- VersionCommand.java             # version output
|       |       |-- BuildVersionProvider.java       # build-filtered version provider
|       |       `-- rail/
|       |           |-- RailHelperCommand.java      # CSV-driven rail helper command
|       |           `-- RailPrintCommand.java       # WMS-first rail print workflow command
|       |-- main/resources/
|       |   `-- version.txt                         # filtered from Maven project.version
|       `-- test/java/com/tbg/wms/cli/commands/
|           `-- rail/
|               `-- RailHelperCommandTest.java
|-- scripts/                      # Build and launcher helpers
|   |-- setup-wms-tags.ps1        # local install helper
|   |-- build-portable-bundle.ps1 # portable package builder
|   |-- run.bat                   # bundle launcher
|   `-- wms-tags-gui.bat          # bundle GUI launcher
|-- vba/                          # Excel macro helper modules (.bas)
|   |-- m_RunReports.bas          # rail macro entrypoint/orchestration
|   |-- m_RefreshData.bas         # train data query refresh helper
|   |-- m_RefreshFootprints.bas   # footprint refresh helper
|   |-- m_Count_Product.bas       # rail pallet/family rollup helper
|   |-- m_Formatting.bas          # train detail formatting helper
|   `-- m_Delete.bas              # temp worksheet cleanup helper
|-- analysis/                     # Internal analysis notes and DB dumps
|   |-- docs/
|   `-- python-tools/
|-- dist/                         # Generated portable bundles
|-- logs/                         # Runtime logs
`-- walmart_sku_matrix.csv
```

## Troubleshooting

- Config check: `java -jar cli/target/cli-*.jar config`
- DB diagnostics: `java -jar cli/target/cli-*.jar db-test`
- Safe execution first: `--dry-run`
- Build failure `No compiler is provided in this environment`: switch from JRE to JDK, then verify with `javac -version`
  and rerun `mvnw.cmd test`.

## Contact

- Zeyad Rashed
- zeyad.rashed@tropicana.com
