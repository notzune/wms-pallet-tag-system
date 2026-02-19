# WMS Pallet Tag System

Licensed under the terms in `LICENSE`.

Production Java CLI for generating and printing Zebra ZPL pallet labels from Oracle WMS data.

## Current Scope

Implemented and supported now:
- `config` command (resolved runtime config with redaction)
- `db-test` command (database connectivity diagnostics)
- `run` command (shipment label generation/printing)
- `gui` command (desktop workflow with shipment preview, pallet math, and confirm-print)
- Oracle read-only access
- Printer routing via site YAML
- Walmart SKU matrix lookup for Walmart item field
- Pallet planning summary from footprint maintenance (`PRTFTP` / `PRTFTP_DTL`)

Not implemented in this codebase yet:
- `template`, `print-template`, `manual`, `replay` commands

## Prerequisites

- Java 21 recommended (validated)
- Maven Wrapper included (`mvnw`, `mvnw.cmd`)
- Oracle WMS network access
- Zebra printer network access (for non-dry-run printing)

## Quick Start

1. Configure environment:

```bash
copy .env.example .env
```

Alternative for packaged JAR deployments:
- Place a `wms-tags.env` file in the same folder as the JAR (or working directory).
- Optional explicit override: set `WMS_CONFIG_FILE=<absolute-or-relative-path>`.
- Precedence is: environment variables -> `WMS_CONFIG_FILE`/`wms-tags.env`/`.env` -> built-in defaults.
- Starter template: `config/wms-tags.env.example`.
- Runtime env key compatibility: `WMS_ENV` and `ACTIVE_ENV` are both supported.
- DB URL compatibility: if `ORACLE_DSN` (or `ORACLE_JDBC_URL`) is set, it overrides host/port/service assembly.

2. Build and test:

```bash
mvnw.cmd test
```

3. Show effective config:

```bash
java -jar cli/target/cli-*.jar config
```

4. Test DB connectivity:

```bash
java -jar cli/target/cli-*.jar db-test
```

5. Run dry-run label generation:

```bash
java -jar cli/target/cli-*.jar run --shipment-id <SHIP_ID> --dry-run --output-dir out/
```

6. Launch GUI:

```bash
java -jar cli/target/cli-*.jar
# or explicitly
java -jar cli/target/cli-*.jar gui
```

## Run Command

```bash
java -jar cli/target/cli-*.jar run --shipment-id <ID> [OPTIONS]
```

Options:
- `--shipment-id` (required)
- `--dry-run`
- `--output-dir <DIR>` (default `./labels`)
- `--printer <ID>`

## GUI Workflow

- Enter shipment ID.
- Select printer.
- Click `Preview` to load shipment details and pallet math (`full`, `partial`, `total pallets`).
- Verify summary + counts.
- Click `Confirm Print` to send labels and save generated ZPL artifacts under `out/gui-<shipment>-<timestamp>/`.

Examples:

```bash
java -jar cli/target/cli-*.jar run --shipment-id 8000141715 --dry-run --output-dir out/
java -jar cli/target/cli-*.jar run --shipment-id 8000141715 --output-dir out/
java -jar cli/target/cli-*.jar run --shipment-id 8000141715 --printer DISPATCH --output-dir out/
```

## Walmart SKU Behavior

- The label field `WAL-MART ITEM #` is populated only when SKU matches the Walmart matrix CSV.
- If SKU does not match (for non-Walmart/non-Canada orders), that field is intentionally left blank on the generated label.
- If a shipment has no LPN records but has shipment SKU rows, labels are generated from SKU data using virtual pallet rows so LPN absence does not block output.

## Configuration

Environment defaults and examples are in `.env.example`.

Key settings:
- `WMS_ENV=PROD` (default)
- `ACTIVE_SITE=TBG3002`
- `ORACLE_USERNAME`, `ORACLE_PASSWORD`, `ORACLE_PORT`, `ORACLE_SERVICE`
- `SITE_<CODE>_<ENV>_HOST` (for example `SITE_TBG3002_PROD_HOST`)
- `PRINTER_ROUTING_FILE=config/TBG3002/printer-routing.yaml`

## Safety and Data Access

- DB pool is configured read-only.
- Application flow uses SELECT queries only.
- Printing can be skipped entirely with `--dry-run`.

## Operational Notes

- `run` prints a pallet planning summary before label generation:
  - total shipment units
  - estimated pallets from footprint data
  - actual LPN count
  - SKUs missing footprint setup

## Build/Test

Use wrapper:

```bash
mvnw.cmd test
mvnw.cmd -pl cli -am package
```

## Project Structure

- `core/` domain, config, template, label mapping, planning
- `db/` Oracle repository and connection pool
- `cli/` Picocli entrypoint and commands
- `config/` site printers/routing and ZPL templates

## Troubleshooting

- Config check: `java -jar cli/target/cli-*.jar config`
- DB diagnostics: `java -jar cli/target/cli-*.jar db-test`
- Safe execution first: `--dry-run`

## Contact

- Zeyad Rashed
- zeyad.rashed@tropicana.com
