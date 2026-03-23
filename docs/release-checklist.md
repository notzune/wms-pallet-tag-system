# Release Checklist

Use this checklist before pushing a release tag.

## Required Smoke Evidence

- Repo-mode smoke:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode repo -ConfigPath .\.env
```

- Packaged-mode smoke:

```powershell
.\scripts\build-jpackage-bundle.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode packaged -ConfigPath .\.env
```

- Release-only Tropicana installer smoke:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode packaged -ConfigPath .\.env -IncludeInstallerScenarios
```

- Save both generated reports:
  - `smoke-report.txt`
  - `smoke-report.json`

Latest validated reports on `patch/v1.7.5-tropicana-bootstrap-fix`:

- repo: `out/smoke-repo-20260321-122513/smoke-report.txt`
- packaged + installer: `out/smoke-packaged-20260321-123051/smoke-report.txt`

Latest validated reports on `1.7.6`:

- packaged + installer: `C:\Users\zrashed\AppData\Local\Temp\wms-host-smoke-auto-9fc2b53b2122477cb22c3864fa906786\smoke-report.txt`

## Required Production-Safe Inputs

- Use bounded smoke identifiers only.
- Current default smoke IDs:
  - shipment: auto-resolved from the most recent WMS shipment when `-ShipmentId` is omitted
  - carrier move: auto-resolved from the most recent WMS carrier move with shipments when `-CarrierMoveId` is omitted
  - rail train: `JC03182026` or another current known-good train
- Keep all release smoke DB usage read-only.

## Printer Validation Policy

- Default release smoke must not send live print jobs.
- Validate printer connectivity with:
  - host reachability
  - TCP `9100` reachability
  - `rail-print --validate-system-default-print`

## Packaging Checks

- Confirm packaged app contains:
  - `config\walmart-sku-matrix.csv`
  - `config\walm_loc_num_matrix.csv`
  - `config\TBG3002\printers.yaml`
  - `config\TBG3002\printer-routing.yaml`
- Confirm packaged config precedence still honors `%LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System\wms-tags.env`
- Confirm release-only installer smoke performs:
  - isolated local install
  - Tropicana config write to isolated `%LOCALAPPDATA%`
  - installed app `config`
  - installed app `db-test`

## Release Gate

Do not create or push a release tag unless:

- repo smoke passes
- packaged smoke passes
- packaged smoke with `-IncludeInstallerScenarios` passes
- no Tier 1 scenario remains uncovered in [release-smoke-coverage-matrix.md](release-smoke-coverage-matrix.md)
- remaining GUI-only/manual surface is documented in [gui-backend-coverage-inventory.md](gui-backend-coverage-inventory.md)
- generated reports are available for review
