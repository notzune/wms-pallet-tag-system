# Tropicana Installer And Config Design

**Date:** 2026-03-19

## Goal

Keep public releases free of live database credentials while giving Tropicana users a single normal installer EXE that installs the app from scratch, persists the Tropicana config across updates, and still leaves a fallback config-only script available for repair or credential rotation.

## Problem

The current bundle builders can seed `wms-tags.env` from the repository root `.env`, which risks packaging live credentials into public artifacts. A config file stored only in the install directory is also not a reliable one-time setup location because portable/manual replacement and packaged updates can overwrite install-local files. Tropicana users need a simpler setup than a public installer plus manual config editing, and support still needs a lightweight config-only redistribution path for future rotations.

## Constraints

- Public GitHub releases must never ship live DB credentials.
- Tropicana users should run one normal installer EXE with visible progress UI.
- The Tropicana config must persist across normal per-user app updates.
- The app is per-user, so per-user config storage is acceptable and preferred.
- A separate fallback config installer script should still exist for repair/rotation scenarios.
- Changes should follow SemVer patch scope unless a larger application change is unavoidable.

## Selected Approach

Use a Tropicana-only local packaging flow that produces a self-contained installer EXE with normal installer UI. That bootstrap installer installs the app and writes the Tropicana config into a stable per-user path under `%LOCALAPPDATA%`. The application/runtime must prefer that external per-user config before the install-folder template config. Keep a separate `Install-Tropicana-Config.ps1` fallback script that writes the same per-user config without reinstalling the app.

## User Flow

### Primary Tropicana flow

1. User runs `WMS Pallet Tag System - Tropicana Setup.exe`.
2. Installer shows a normal install UI with progress.
3. Installer installs the application from scratch if needed.
4. Installer writes Tropicana config to `%LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System\wms-tags.env`.
5. Installer optionally validates setup.
6. User finishes and launches the app.

### Fallback support flow

1. Support sends `Install-Tropicana-Config.ps1`.
2. User runs the script.
3. Script overwrites the per-user config file in `%LOCALAPPDATA%`.
4. User relaunches the app.

## Technical Design

### Public release hardening

- `scripts/build-portable-bundle.ps1` must always seed `wms-tags.env` from a sanitized example/template file.
- `scripts/build-jpackage-bundle.ps1` must follow the same rule.
- Public ZIP and installer outputs must continue shipping only placeholder config.

### Persistent per-user config

- Introduce a stable per-user config location:
  - `%LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System\wms-tags.env`
- Update config discovery so this path takes precedence over install-local `wms-tags.env`, `.env`, and template files.
- Keep the bundled install-local `wms-tags.env` as a placeholder for public/manual installs.

### Tropicana bootstrap installer

- Add a local-only build script that produces `WMS Pallet Tag System - Tropicana Setup.exe`.
- The bootstrapper should:
  - include the normal packaged installer payload
  - display a standard installer/progress UI
  - run the normal app install
  - write the Tropicana config to the per-user path
  - optionally launch verification and/or the app
- This artifact is for local/internal distribution only and must not be published with public release assets.

### Fallback config installer script

- Add `scripts/Install-Tropicana-Config.ps1`.
- The script should:
  - write the Tropicana config to the same `%LOCALAPPDATA%` path
  - create a timestamped backup before overwrite
  - avoid printing secrets to the console
  - optionally run a lightweight validation step
- This script is the rotation/repair path when reinstalling the app is unnecessary.

## Error Handling

- Invalid install target or missing app installer payload: fail with a clear support message.
- Existing per-user config present: back up before overwrite.
- Config write failure: stop immediately and preserve backup when possible.
- Validation failure: distinguish between install success and config/connectivity failure.
- Public build misuse: public artifacts remain functional but require user-supplied config.

## Security Boundary

- Public releases contain no live DB secrets.
- Tropicana secrets live in locally generated internal artifacts only.
- The persisted per-user config remains readable on the local machine, which is an accepted tradeoff for this workflow.
- The controlled secret distribution channels become:
  - the Tropicana setup EXE
  - the fallback config installer script

## Testing Strategy

- Add regression coverage so public bundle generation never copies the repo root `.env`.
- Add application/config tests for the new per-user config precedence.
- Add focused tests or smoke checks for the fallback config installer behavior.
- Run targeted verification on the Tropicana bootstrap build script if the bootstrapper is script-driven.

## Documentation

- Update `README.md` to clarify:
  - public builds ship with placeholder config only
  - Tropicana uses a separate internal installer EXE
  - config persists in a per-user external location
  - fallback config installer script is the repair/rotation path

## SemVer

This remains a patch-level change because it hardens release packaging, fixes update persistence for deployed config, and adds internal-only installation helpers without changing the public feature set or public-facing APIs.
