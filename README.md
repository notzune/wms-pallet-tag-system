# WMS Pallet Tag System

Licensed under the terms in `LICENSE`.

Production Java CLI and GUI for generating and printing Zebra ZPL pallet labels from Oracle WMS data.

## Current Scope

Implemented and supported:
- `config` command (resolved runtime config with redaction)
- `db-test` command (database connectivity diagnostics)
- `run` command (shipment label generation and printing)
- `gui` command (desktop workflow with shipment preview and confirm-print)
- `barcode` command (standalone barcode ZPL generation and optional printing)
- Oracle read-only access
- Printer routing via site YAML
- Walmart SKU matrix lookup for Walmart item field
- Pallet planning summary from footprint maintenance (`PRTFTP` / `PRTFTP_DTL`)

Not implemented yet:
- `template`, `print-template`, `manual`, `replay` commands

## Prerequisites

- Java 11+ (21 recommended for development)
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

3. Run commands:

```bash
java -jar cli/target/cli-*.jar config
java -jar cli/target/cli-*.jar db-test
java -jar cli/target/cli-*.jar run --shipment-id <SHIP_ID> --dry-run --output-dir out/
java -jar cli/target/cli-*.jar gui
```

### Portable bundle (recommended for operators)

1. Extract the ZIP to a folder (example: `C:\wms-pallet-tag-system`).
2. Copy the template config into place:

```bash
copy config\\wms-tags.env.example wms-tags.env
```

3. Edit `wms-tags.env` with Oracle and site credentials.
4. Run:

```bash
run.cmd
```

For CLI usage:

```bash
run.cmd config
run.cmd db-test
run.cmd run --shipment-id <SHIP_ID> --dry-run --output-dir out/
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
- `SITE_<CODE>_<ENV>_HOST` (example `SITE_TBG3002_PROD_HOST`)
- `SITE_<CODE>_SHIP_FROM_NAME`, `SITE_<CODE>_SHIP_FROM_ADDRESS`, `SITE_<CODE>_SHIP_FROM_CITY_STATE_ZIP`
- `PRINTER_ROUTING_FILE=config/TBG3002/printer-routing.yaml`

## Run Command

```bash
java -jar cli/target/cli-*.jar run --shipment-id <ID> [OPTIONS]
```

Options:
- `--shipment-id` (required)
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
- `--origin-x <N>` (default `40`)
- `--origin-y <N>` (default `40`)
- `--module-width <N>` (default `2`)
- `--module-ratio <N>` (default `3`)
- `--barcode-height <N>` (default `120`)
- `--human-readable <true|false>` (default `true`)
- `--copies <N>` (default `1`)
- `--output-dir <DIR>` (default `./barcodes`)
- `--dry-run` (skip printing)
- `--printer <ID>` (required unless `--dry-run`)
- `--print-to-file` or `--ptf` (write ZPL to `/out` next to the JAR and skip printing)

Notes:
- Portrait is the default.
- Landscape uses ZPL printer orientation (`^POL`) to switch the printer into landscape.

## GUI Workflow

- Enter shipment ID.
- Select printer.
- Click `Preview` to load shipment details and pallet math (`full`, `partial`, `total pallets`).
- Verify summary and counts.
- Click `Confirm Print` to send labels and save generated ZPL artifacts under `out/gui-<shipment>-<timestamp>/`.
- Toggle `Print to file` to save ZPL under the same `out/` path without printing.

## Walmart SKU Behavior

- The label field `WAL-MART ITEM #` is populated only when SKU matches the Walmart matrix CSV.
- If SKU does not match (for non-Walmart or non-Canada orders), that field is intentionally left blank.
- If a shipment has no LPN records but has shipment SKU rows, labels are generated from SKU data using virtual pallet rows.
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

The `Javadoc Pages` GitHub Actions workflow publishes the aggregated site to GitHub Pages when enabled in repository settings.

## CI Workflows

### Javadoc Pages (`.github/workflows/javadoc-pages.yml`)

Triggers on pushes to `main` and `dev`, plus manual dispatch.

- Builds aggregated Javadoc with `mvn javadoc:aggregate`.
- Publishes `target/site/apidocs` to GitHub Pages.

### Release Bundle (`.github/workflows/release.yml`)

Triggers on tag pushes matching `v*.*.*`.

- Builds the CLI JAR.
- Builds the portable bundle using `dist/temurin11-jre.zip`.
- Attaches `dist/wms-pallet-tag-system-<version>-portable.zip` to the GitHub Release.
- Uses the matching section from `CHANGELOG.md` for the release body.

## Project Structure

```
wms-pallet-tag-system/
├── README.md
├── CHANGELOG.md
├── INSTRUCTIONS.md               # Development standards and system requirements
├── LICENSE
├── pom.xml
├── .env.example
├── config/
│   ├── wms-tags.env.example
│   ├── templates/
│   │   └── walmart-canada-label.zpl
│   └── TBG3002/
│       ├── printers.yaml
│       └── printer-routing.yaml
├── core/
├── db/
├── cli/
├── scripts/                      # Build and launcher helpers
├── analysis/                     # Internal analysis notes and DB dumps
├── dist/                         # Generated portable bundles
├── logs/                         # Runtime logs
└── walmart_sku_matrix.csv
```

## Troubleshooting

- Config check: `java -jar cli/target/cli-*.jar config`
- DB diagnostics: `java -jar cli/target/cli-*.jar db-test`
- Safe execution first: `--dry-run`

## Contact

- Zeyad Rashed
- zeyad.rashed@tropicana.com
