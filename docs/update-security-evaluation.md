# Update Security Evaluation

## Decision

Do not enable silent or true in-place self-replacement at this stage.

Use a verified guided-upgrade model instead:

- app checks GitHub Releases for a newer version
- if a published installer `.exe` and matching `.sha256` checksum are both present, the app downloads the installer into `updates/`
- the app verifies the installer SHA-256 against the published checksum
- after verification, the packaged installer helper launches the installer

## Why true in-place auto-update is not enabled

- Windows file locking means the running packaged app cannot safely replace its own executable tree without a separate updater process
- unsigned installers/binaries do not provide a strong publisher trust boundary
- rollback behavior is not implemented
- restricted or offline environments must fail safely and continue to support manual update paths

## Supported update paths

### Supported now

- manual download from GitHub Releases
- guided installer download and launch for packaged installs when:
  - the latest release publishes the installer `.exe`
  - the latest release publishes the matching `.sha256`
  - the packaged install helper script is present locally

### Explicitly not supported now

- silent background updates
- unattended self-replacement of the running app
- automatic rollback
- auto-updating portable/manual ZIP installs in place

## Future requirements before reconsidering true auto-update

- code signing for release artifacts
- detached updater/bootstrapper process with explicit lock handling
- rollback strategy and retained previous version
- clearer install-type detection and eligibility policy
- explicit review for unattended update behavior
