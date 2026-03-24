# WMS Pallet Tag System

[![Release Bundle](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/release.yml/badge.svg?branch=main)](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/release.yml)
[![Javadoc Pages](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/javadoc-pages.yml/badge.svg?branch=main)](https://github.com/notzune/wms-pallet-tag-system/actions/workflows/javadoc-pages.yml)
[![API Docs](https://img.shields.io/badge/docs-javadoc-blue)](https://notzune.github.io/wms-pallet-tag-system/)
![Version](https://img.shields.io/badge/version-1.7.6-blue)
![Java](https://img.shields.io/badge/java-17%2B-orange)
![License](https://img.shields.io/badge/license-Custom-green)

Licensed under the terms in `LICENSE`.

Production Java CLI and GUI for generating and printing Zebra ZPL pallet labels from Oracle WMS data.
Current branch target: `1.7.6` prerelease validation.

## Versioning and History

- Releases follow [Semantic Versioning](https://semver.org/).
- `CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
- Git commit messages should follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).
- New release notes should be staged under `## [Unreleased]` before version cut/tagging.

For open work and follow-up items, see the [GitHub issues tracker](https://github.com/notzune/wms-pallet-tag-system/issues).

## Current Scope

Implemented and supported:

- `config` command (resolved runtime config with redaction)
- `db-test` command (database connectivity diagnostics)
- `ems-recon` command (legacy EMS reconciliation XLS analysis and fix-plan output)
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

## Engineering Quality Notes

- DB shipment hydration avoids per-LPN line-item queries by loading all shipment line-items in one query and grouping in memory.
- DB shipment hydration now also coalesces duplicate LPN rows from mixed inventory-detail joins so one physical pallet cannot generate duplicate labels.
- GUI workflow caches are site-scoped and thread-safe to prevent stale cross-site printer/site metadata reuse.
- GUI preview selection refresh now snapshots the selected labels once per update cycle instead of rebuilding shipment/carrier subsets repeatedly.
- Query and command execution paths remain hardened with prepared statements and argumentized process invocation patterns.

## Prerequisites

- JDK 17+ for development builds (`javac` must be available; JRE-only installs will fail Maven compile)
- Java 17+ runtime for running packaged bundles (JRE is acceptable for runtime only)
- Maven Wrapper included (`mvnw`, `mvnw.cmd`)
- Oracle WMS network access
- Zebra printer network access (for non-dry-run printing)

Portable bundles include a JRE and do not require a separate Java install.

## Setup and Quick Start

Choose one of these paths:

- Portable/manual install: extract the ZIP, replace the dummy `wms-tags.env` with your real values, and run `run.bat` or `wms-tags-gui.bat`
- Packaged installer: run `WMS Pallet Tag System-<version>.exe` or `install-wms-installer.ps1`
- Tropicana internal install: generate and distribute the inert Tropicana package ZIP from a trusted internal machine
- Manual build from repo: build with Maven, then optionally package with `build-portable-bundle.ps1` or `build-jpackage-bundle.ps1`

### Manual Build From Repo

1. Configure environment:

```bash
copy .env.example .env
```

2. Build and test:

```bash
.\mvnw.cmd test
```

If you get `No compiler is provided in this environment`, install a JDK and ensure `JAVA_HOME` points to it.

3. Run commands directly from the repo:

```bash
java -jar cli/target/cli-*.jar config
java -jar cli/target/cli-*.jar db-test
java -jar cli/target/cli-*.jar ems-recon --report <REPORT.xls> --output-dir out/ems-recon
java -jar cli/target/cli-*.jar run --shipment-id <SHIP_ID> --dry-run --output-dir out/
java -jar cli/target/cli-*.jar run --carrier-move-id <CMID> --dry-run --output-dir out/
java -jar cli/target/cli-*.jar rail-helper --input-csv <INPUT.csv> --item-footprint-csv <ITEM_FAMILY.csv> --output-dir out/rail-helper
java -jar cli/target/cli-*.jar rail-print --train <TRAIN_ID> --output-dir out/rail-print
java -jar cli/target/cli-*.jar rail-print --template --output-dir out/rail-print
java -jar cli/target/cli-*.jar gui
```

4. Optional: build operator-ready artifacts from the repo:

```powershell
.\mvnw.cmd -q -pl cli -am "-Dmaven.test.skip=true" package
.\scripts\build-portable-bundle.ps1
.\scripts\build-jpackage-bundle.ps1
.\scripts\build-jpackage-bundle.ps1 -InstallerType exe
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
run.bat ems-recon --report <REPORT.xls> --output-dir out/ems-recon
run.bat run --shipment-id <SHIP_ID> --dry-run --output-dir out/
run.bat run --carrier-move-id <CMID> --dry-run --output-dir out/
run.bat rail-helper --input-csv <INPUT.csv> --item-footprint-csv <ITEM_FAMILY.csv> --output-dir out/rail-helper
run.bat rail-print --train <TRAIN_ID> --output-dir out/rail-print
```

On Linux/macOS:

```bash
./run.sh
```

### Manual Install From Portable ZIP

1. Download the latest `wms-pallet-tag-system-<version>-portable.zip` from GitHub Releases.
2. Extract it to a writable folder, for example `C:\wms-pallet-tag-system`.
3. Edit `wms-tags.env` in the extracted root directory.
4. Launch:

```powershell
.\run.bat gui
```

5. For CLI usage:

```powershell
.\run.bat config
.\run.bat run --shipment-id <SHIP_ID> --dry-run
```

### App image / executable bundle

Use the `jpackage` builder when you want a native executable layout while keeping the portable ZIP/manual install path as fallback.

1. Build the app image:

```powershell
.\scripts\build-jpackage-bundle.ps1
```

2. Optional: also build a per-user Windows installer:

```powershell
.\scripts\build-jpackage-bundle.ps1 -InstallerType exe
```

2a. Optional: sign the app-image launcher(s) and installer with local SignTool certificate settings:

```powershell
.\scripts\build-jpackage-bundle.ps1 `
  -InstallerType exe `
  -SigningMode signtool `
  -CertificateThumbprint <CERT_THUMBPRINT> `
  -TimestampUrl http://timestamp.digicert.com
```

2b. Optional: sign with Microsoft Trusted Signing via SignTool plugin arguments:

```powershell
.\scripts\build-jpackage-bundle.ps1 `
  -InstallerType exe `
  -SigningMode signtool `
  -TimestampUrl http://timestamp.acs.microsoft.com `
  -AdditionalSignToolArgs @(
    '/dlib', 'C:\path\to\Azure.CodeSigning.Dlib.dll',
    '/dmdf', 'C:\path\to\trusted-signing-metadata.json'
  )
```

3. Optional: install with logging or replace an existing same-version install:

```powershell
.\dist\install-wms-installer.ps1
.\dist\install-wms-installer.ps1 -ReplaceExisting
```

4. Optional: uninstall or prep for a clean reinstall:

```powershell
.\dist\uninstall-wms-tags.ps1
```

Notes:

- Default app-image output is `dist/wms-pallet-tag-system-<version>-app`
- The app image keeps `config/`, template `wms-tags.env`, `out/`, and `logs/` next to the executable
- The generated app image includes `WMS Pallet Tag System.exe`, plus `run.bat` and `wms-tags-gui.bat` wrappers for CLI and GUI entrypoints
- The bundled runtime comes from the `jpackage` JDK unless you pass `-RuntimeImage`; use a Java 17 runtime image for release parity with the project baseline
- The optional installer defaults to per-user install to avoid admin privileges when possible
- Newer installer builds now use a stable Windows upgrade UUID so normal version-to-version upgrades can reuse the same install identity
- `build-jpackage-bundle.ps1` can optionally sign the app-image launcher(s) and the final installer via `-SigningMode signtool`
- For standard certificate signing, pass one of `-CertificateThumbprint`, `-CertificateSubjectName`, or `-CertificatePath`
- For Trusted Signing, pass the required `/dlib` and `/dmdf` values through `-AdditionalSignToolArgs`
- Prerelease tags such as `v1.7.6-rc.2` are supported in CI and publish GitHub Releases marked as prereleases automatically
- The installer helper writes an MSI log and can uninstall an existing same-version install first when `-ReplaceExisting` is used
- `uninstall-wms-tags.ps1` / `uninstall-wms-tags.bat` provide a direct uninstall path for packaged installs
- GUI `Tools` / `Settings` now include `Update Manager...` and `Uninstall / Clean Install Prep...` actions for packaged installs
- The update manager always checks the latest stable release and can optionally include prerelease builds when `Enable experimental / prerelease updates` is turned on
- The update manager shows the running version, latest stable version, latest experimental version, stable releases behind, and whether the current build is current, recommended for update, or behind a newer release line
- One stable release behind in the same release line is treated as `update recommended`
- Behind a newer stable release line triggers a startup warning prompt plus a persistent `Tools` badge until the app is updated or the release state changes
- Ignoring a startup prompt suppresses only that exact target version popup; it does not clear the toolbar warning badge
- The update manager can target any published release with both installer and checksum assets, including downgrades and prereleases
- Downgrades and prerelease installs require explicit confirmation because features may be missing or unstable
- Guided install requires both the installer asset and its published `.sha256` checksum; if either is missing, the app falls back to the release page
- Guided install now shows a visible installer/update status window and relaunches the app after success
- Full silent auto-update is still out of scope because safe self-replacement on Windows needs a detached updater, signing, lock handling, and rollback support that are not implemented yet
- `uninstall-wms-tags.ps1` now supports clean-install prep by removing the installed product and then wiping the install directory plus non-secret runtime settings
- Building an `.exe` or `.msi` installer requires WiX Toolset v3+ on `PATH`
- The portable ZIP/manual install path remains supported for machines where the packaged executable is not viable
- If an operator launches the raw installer `.exe` directly outside the helper path, Windows Installer UI remains the controlling experience; the richer status/relaunch flow is provided by `install-wms-installer.ps1` / `.bat` and the in-app guided updater
- Clean-VM installer automation can be rerun with `scripts\vm\Test-TropTest-InstallerFlow.ps1` when VirtualBox Guest Additions and a guest automation account are available
- Latest clean-VM evidence shows installer remove/install/upgrade transitions work, but Microsoft Defender on a fresh Windows 11 VM quarantines the installed native launcher as `Trojan:Win32/Bearfoos.B!ml`, which blocks first-run validation of the installed EXE without an exclusion or code-signing/reputation change

### Tropicana internal installer

Use the local-only Tropicana packaging flow when you want an internal package that ships the signed-capable installer plus the Tropicana config script without a self-extracting wrapper.

```powershell
.\scripts\build-tropicana-installer.ps1 -ConfigSourcePath .\.env
```

Outputs:

- `dist\WMS Pallet Tag System - Tropicana Package.zip`
- `dist\Install-Tropicana-Config.ps1`
- `dist\Tropicana-Package-Readme.txt`

Behavior:

- The package ZIP intentionally avoids the previous self-extracting EXE and bootstrap launcher chain.
- Operators install the normal packaged app first, then run `Install-Tropicana-Config.ps1` to apply Tropicana config for the current user.
- Tropicana config persists under `%LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System\wms-tags.env`, so normal app updates do not require rerunning config install.
- `Install-Tropicana-Config.ps1` remains the supported repair and credential-rotation path.
- These Tropicana artifacts are for internal distribution only and should not be attached to public GitHub Releases.

### Update Paths

- Portable/manual install:
  download the latest portable ZIP or installer from GitHub Releases and replace/reinstall manually
- Packaged install:
  use the update actions under `Tools -> Settings` for release checks, guided installer download, and packaged-install maintenance when the release includes both the `.exe` and `.sha256`
- Tropicana internal install:
  rerun `Install-Tropicana-Config.ps1` only when credentials rotate or per-user config needs repair
- Clean reinstall:
  use `Tools -> Settings -> Uninstall / Clean Install Prep...` or `uninstall-wms-tags.ps1`

### Release Order

- Merge the release-prep PR to `main`
- Tag `vX.Y.Z-rc1` (or another SemVer prerelease tag) to publish a GitHub prerelease automatically
- Validate portable ZIP, installer `.exe`, `.exe.sha256`, and updater behavior against that prerelease
- Tag `vX.Y.Z` when the prerelease is accepted

### Release Smoke

Release tagging should be blocked until smoke evidence exists for both repo and packaged targets.

Primary smoke entrypoints:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode repo -ConfigPath .\.env
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode packaged -ConfigPath .\.env
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode packaged -ConfigPath .\.env -IncludeInstallerScenarios
```

Smoke policy:

- `repo` mode is the fast developer gate against the built CLI jar and shared workflow services
- `packaged` mode is the release gate against the actual packaged app layout

### VM End-to-End Testing

For clean-machine Windows installer validation in VirtualBox:

```powershell
.\scripts\vm\Test-TropTest-InstallerFlow.ps1 `
  -GuestUser <GUEST_USER> `
  -GuestPassword <GUEST_PASSWORD> `
  -OldInstallerPath C:\path\to\WMS` Pallet` Tag` System-1.7.4.exe `
  -NewInstallerPath C:\path\to\WMS` Pallet` Tag` System-1.7.6.exe
```

Outputs:

- screenshot sequence for maintenance remove, fresh install, upgrade, and launch attempts
- `installer-flow-report.txt`
- `defender-events.txt`

The helper uses `VBoxManage controlvm ... keyboardputscancode/keyboardputstring` to drive the visible desktop session, so it remains usable even when `guestcontrol` can only reach the background automation session.
- `-IncludeInstallerScenarios` adds the slower release-only Tropicana bootstrap install check
- smoke printing must avoid live printer output by default
- printer validation should use reachability checks unless a live print run is explicitly requested
- current smoke defaults are intended for bounded production-safe IDs and can be overridden by explicit parameters
- GUI workflows are validated through shared backend paths first; remaining GUI-only gaps must stay documented in the release coverage matrix
- the installer scenario performs a real isolated local install, then verifies Tropicana `%LOCALAPPDATA%` config precedence and post-install `db-test`

See [docs/release-smoke-coverage-matrix.md](docs/release-smoke-coverage-matrix.md) for the current coverage contract.
See [docs/release-checklist.md](docs/release-checklist.md) for the release gate.

## Configuration

Configuration file precedence:

1. Environment variables
2. `WMS_CONFIG_FILE` path, if set
3. `%LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System\wms-tags.env`
4. `wms-tags.env` next to the JAR (or working directory)
5. `.env`
6. Built-in defaults

Public release artifacts always ship with the dummy template `config\wms-tags.env.example` copied into place as `wms-tags.env`; that file is intentionally non-functional until replaced with real credentials.
Tropicana internal installs should get working credentials only through `WMS Pallet Tag System - Tropicana Setup.exe` or `Install-Tropicana-Config.ps1`.

Key settings:

- `WMS_ENV=PROD` (default)
- `ACTIVE_SITE=TBG3002`
- `ORACLE_USERNAME`, `ORACLE_PASSWORD`, `ORACLE_PORT`, `ORACLE_SERVICE`
- `ORACLE_ODBC_DSN` (optional Oracle Net alias fallback, e.g. `TBG3002`)
- `ORACLE_DSN` (optional explicit DSN/JDBC descriptor; use carefully to avoid wrong environment)
- `SITE_<CODE>_<ENV>_HOST` (example `SITE_TBG3002_PROD_HOST`)
- `SITE_<CODE>_SHIP_FROM_NAME`, `SITE_<CODE>_SHIP_FROM_ADDRESS`, `SITE_<CODE>_SHIP_FROM_CITY_STATE_ZIP`
- `PRINTER_ROUTING_FILE=config/TBG3002/printer-routing.yaml`
- `RAIL_DEFAULT_PRINTER_ID` (optional: rail PDF print target; printer ID from `printers.yaml`)
- `RAIL_LABEL_CENTER_GAP_IN=0.125` (rail 2-column center gap, inches)
- `RAIL_LABEL_OFFSET_X_IN=0.02` (rail label grid X nudge, inches; + is right)
- `RAIL_LABEL_OFFSET_Y_IN=0.02` (rail label grid Y nudge, inches; + is down)
- `RIGHT_CLICK_COOLDOWN_MS=250` (GUI right-click copy/paste debounce)
  - Applies terminal-like right-click behavior to GUI text-entry fields, including Rail Labels `Train ID` and output path.

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
- `--labels <EXPR>` shipment-only subset selection using `all` or 1-based indexes/ranges like `1,3,5-7`
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
- `--template` (generate 10-position 4x2 alignment template PDF and exit)
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
- Optionally print: tries configured rail printer first (`RAIL_DEFAULT_PRINTER_ID`), then opens system print dialog as fallback

## Rail Labels GUI Workflow

- Open `gui`, then go to `Tools -> Rail Labels...`.
- Enter train ID and click `Load Preview`.
- Press `Ctrl+F` to trigger `Load Preview` from the keyboard while the workflow window is focused.
- System pulls rail rows from WMS and resolves footprints by short code from WMS.
- Preview includes:
- Railcar table (`SEQ`, `VEHICLE`, `CAN`, `DOM`, `KEV`, `LOAD_NBR`)
- Railcar card preview panel (item lines + CAN/DOM/KEV + pass/fuel/BH fields)
- Diagnostics panel (row counts and unresolved footprints)
- Rail print target dropdown only shows printers marked with the `RAIL` capability, plus `System default printer` and `Print to file`.
- Click `Generate PDF` to produce a letter-size multi-card PDF.
- Click `Print` to generate PDF and send it to the selected rail printer, or keep `Print to file` selected to save only.

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
- Main window footer shows `Version <x.y.z>` and resolves from package metadata with Maven `pom.properties` fallback.
- Enter ID, select printer, and click `Preview`.
- Press `Ctrl+F` to trigger `Preview` from the keyboard while the workflow window is focused.
- Label-generation printer dropdown only shows printers marked with the `ZPL` capability, plus `Print to file`.
- Shipment preview shows shipment summary, label plan, and SKU-level pallet math (full vs partial).
- Carrier Move preview shows job summary and expandable stop sections; each stop renders shipment-level detail and SKU
  breakdown.
- Click `Confirm Print` to execute and persist artifacts.
- Click `Show Labels` after preview to open the live ZPL renderer with the exact generated shipment/carrier-move documents that would be printed, including info tags when selected.
- Shipment mode output path: `out/gui-<shipment>-<timestamp>/`.
- Carrier mode output path: `out/gui-cmid-<cmid>-<timestamp>/`.
- Carrier mode prints all shipment labels in stop order, then per-stop info tags, then one final info tag.
- Shipment mode prints shipment labels and one shipment info tag.
- Select `Print to file` from the printer dropdown to save ZPL under `out/` without printer I/O.
- Preview now supports per-label subset selection, starts with all labels selected, and keeps the label-selection panel collapsed by default to reduce noise on large jobs.
- Preview includes an `Include info tags` toggle and shows `labels + info tags = total documents` in the selection status and math summary.
- Use `Tools -> Barcode Generator...` for standalone barcode ZPL generation/printing.
- Barcode Generator now includes a `Preview` action that opens the live ZPL renderer for the currently configured barcode before printing or exporting.
- Barcode Generator now includes a toggleable `Utility Keyboard...` pop-out with function keys (`F1`-`F12`), Enter/Tab/Esc, clear/edit actions, and navigation keys that can be sent to the currently focused system field.
- Use `Tools -> ZPL Preview...` to paste or open raw ZPL and render a live label preview with configurable density, label size, and label index.
- The ZPL Preview tool can also open generated barcode/shipment/carrier documents directly and page through them with previous/next controls or the document spinner while keeping the rendered preview scrollable.
- Use `Tools -> Rail Labels...` for end-to-end rail merge generation from live WMS train data.
- GUI printer scoping is driven by printer `capabilities` in `config/<site>/printers.yaml`.
- Use `capabilities: [ ZPL ]` for pallet-label workflows and `capabilities: [ RAIL ]` for the rail labels tool.
- Barcode dialog now defaults to an operator-focused layout and moves low-level controls under `Advanced Settings...`.
- Use queue/resume actions from the GUI to process mixed job batches and recover interrupted jobs.
- Queue input accepts new lines or semicolons, ignores surrounding whitespace, and auto-detects unprefixed numeric IDs as shipment (`800...`) versus carrier move (other numeric values); explicit `S:` / `C:` prefixes still override.
- The main window footer shows a compact Oracle status LED in the bottom-right corner (`Checking`, `Connected`, or `Not connected - ORA-xxxxx`), with the full Oracle error/remediation detail available on hover.
- `Tools` shows an alert badge when an application update is available, and `Settings...` exposes manual update checks plus packaged-install uninstall / clean-wipe launchers.
- If the latest release includes the packaged installer `.exe`, update checks can use a guided download-and-install path instead of only opening the release page.
- `Settings...` also exposes `Advanced Settings...` for non-secret runtime config files under `config/`; `wms-tags.env` stays outside the GUI because it contains database/network secrets.
- Runtime output cleanup now prunes stale `out/` artifacts older than 14 days by default, and the retention window is configurable from `Settings...`.
- See [docs/update-security-evaluation.md](docs/update-security-evaluation.md) for the current security boundary and why silent self-updating is still intentionally out of scope.

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

Recent documentation maintenance:

- missing `package-info.java` coverage was filled for the newer `core` subpackages (`barcode`, `db`, `ems`, `label`, `labeling`, `location`, `sku`, `update`)
- GUI settings/update/install maintenance responsibilities are now documented separately from the main frame through `MainSettingsDialog`
- GUI print-task planning and artifact naming are now documented separately from workflow orchestration through `PrintTaskPlanner` and `ArtifactNameSupport`

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
- Tag pushes matching `v*.*.*` or `v*.*.*-*` (creates a GitHub Release).

Behavior:

- Builds the CLI JAR.
- Builds the portable bundle using `dist/temurin17-jre.zip`.
- Builds the packaged Windows installer `.exe` through `build-jpackage-bundle.ps1 -InstallerType exe`.
- Generates and publishes the installer SHA-256 sidecar `WMS Pallet Tag System-<version>.exe.sha256` for guided update verification.
- PRs: uploads `dist/wms-pallet-tag-system-<version>-portable.zip` as an artifact. `<version>` is read from
  `cli/target/maven-archiver/pom.properties`.
- PRs: also upload the installer `.exe` and `.exe.sha256` as workflow artifacts.
- Stable tags (`vX.Y.Z`): attach the portable ZIP, installer `.exe`, and matching `.exe.sha256` to a normal GitHub Release.
- Prerelease tags (`vX.Y.Z-<suffix>` such as `v1.7.3-rc1`): attach the same artifacts to a GitHub Release marked as a prerelease.
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
|   |-- walmart-sku-matrix.csv
|   |-- walm_loc_num_matrix.csv
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
|       |   |   |-- ConfigFileLocator.java          # runtime env file discovery and validation
|       |   |   |-- EnvStyleConfigParser.java       # dotenv/env-style parsing
|       |   |   |-- OutDirectoryRetentionService.java # stale out/ cleanup policy
|       |   |   |-- RuntimePathResolver.java        # runtime-relative path resolution
|       |   |   |-- RuntimeSettings.java            # non-secret user runtime preferences
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
|       |   |   |-- template/                       # template parsing/merge logic
|       |   |   `-- update/                         # release/version/update helpers
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
|       |   |-- MainSettingsDialog.java             # primary settings and maintenance dialog
|       |   |-- AdvancedSettingsDialog.java         # non-secret runtime config editor
|       |   |-- BarcodeDialogFactory.java           # barcode UI dialog wiring
|       |   |-- PrintTaskPlanner.java               # shipment/carrier print task planning
|       |   |-- ArtifactNameSupport.java            # shared artifact filename slugging
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
|   |-- build-jpackage-bundle.ps1 # app-image / installer builder + installer sha256
|   |-- install-wms-installer.ps1 # logged installer runner / replace-existing helper
|   |-- uninstall-wms-tags.ps1    # uninstall / clean-install prep helper
|   |-- verify-wms-tags.ps1       # packaged smoke-test helper
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
|-- docs/                         # Handoff notes, security notes, PR drafts
|-- dist/                         # Generated portable/app-image/installer bundles
|-- logs/                         # Runtime logs
`-- out/                          # Generated print artifacts
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
