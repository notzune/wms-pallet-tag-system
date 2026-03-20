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

- Save both generated reports:
  - `smoke-report.txt`
  - `smoke-report.json`

## Required Production-Safe Inputs

- Use bounded smoke identifiers only.
- Current default smoke IDs:
  - shipment: operator-supplied for the active release window
  - carrier move: operator-supplied for the active release window
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

## Release Gate

Do not create or push a release tag unless:

- repo smoke passes
- packaged smoke passes
- no Tier 1 scenario remains uncovered in [release-smoke-coverage-matrix.md](/C:/Users/zrashed/Documents/Code/wms-pallet-tag-system/.worktrees/v1.7.5-tropicana-bootstrap-fix/docs/release-smoke-coverage-matrix.md)
- generated reports are available for review
