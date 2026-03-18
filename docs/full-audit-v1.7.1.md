# v1.7.1 Full Audit Findings

## Must-Fix Before Merge

### 1. Release workflow did not support SemVer prerelease tags

Status: fixed in this branch

Findings:

- `.github/workflows/release.yml` only triggered on `vX.Y.Z`
- prerelease tags such as `v1.7.1-rc1` would not publish any GitHub release
- changelog lookup and artifact version flow assumed the tag and the internal app version were always identical

Resolution:

- added explicit stable + prerelease tag handling
- derived separate values for artifact label, numeric app version, changelog version, and prerelease flag
- configured GitHub releases to set `prerelease: true` automatically for prerelease tags

### 2. `build-jpackage-bundle.ps1` could fail before its intended "build first" error path

Status: fixed in this branch

Findings:

- omitting `-JarPath` before a local package build could leave `$JarPath` blank
- the script then called `Test-Path -LiteralPath $JarPath`, which raised a parameter-binding error instead of the intended guidance message

Resolution:

- added a blank-string guard before the `Test-Path` call
- preserved the existing user-facing "build first" error message

### 3. App-image handoff in `build-jpackage-bundle.ps1` was fragile

Status: fixed in this branch

Findings:

- moving the generated app-image directory with `Move-Item` failed under the worktree validation path with `Access denied`
- this made prerelease packaging validation brittle even though `jpackage` itself succeeded

Resolution:

- replaced the directory move with copy-then-cleanup semantics
- validated the prerelease-style app-image / installer path locally after the change

## Low-Risk Improvements Landed Now

### 4. Release and updater docs did not describe prerelease flow

Status: fixed in this branch

Resolution:

- updated `README.md` with prerelease-tag support, release order, and release workflow behavior
- updated `docs/pr-v1.7.1-draft.md` to describe prerelease and final release preparation

### 5. Update-version docs were stale after adding prerelease support

Status: fixed in this branch

Resolution:

- updated `core.update.VersionSupport` class Javadoc
- updated `core.update.package-info.java`

### 6. `docs/next-agent-notes.md` contained stale completed backlog items

Status: fixed in this branch

Findings:

- per-label printing and GUI menu cleanup were still listed as open even though they were already completed in prior commits

Resolution:

- removed stale backlog entries
- updated the handoff note to reference newer release/prerelease validation expectations

## Deferred Follow-Ups

### A. `LabelGuiFrame` remains very large

Status: defer

Reason:

- the file is still the largest GUI class in the repo
- however, recent extraction work already moved settings, task planning, and several helper responsibilities outward
- a deeper split should be done as a focused post-release task so behavior changes remain easy to verify

### B. `OracleDbQueryRepository` remains a large concentration of query and hydration logic

Status: defer

Reason:

- the file is still the largest DB class in the repo
- recent work already addressed duplicate-LPN hydration and several normalization/query concerns
- further decomposition should be driven by the next concrete defect or feature rather than speculative churn during release prep

## Audit Coverage Notes

Reviewed areas:

- release workflow and packaging scripts
- updater/release support classes and package docs
- top-level README and PR draft
- next-agent handoff notes
- package-info coverage across active Java packages
- aggregate Javadoc generation (`.\mvnw.cmd -q -DskipTests javadoc:aggregate`) completed successfully

Current assessment:

- repository-wide package-doc coverage is present for active Java packages
- no broad Javadoc failure surfaced under aggregate generation
- the most important actionable issues found during this pass were in release automation, packaging robustness, and stale release/handoff documentation

## Validation Notes

- `.\mvnw.cmd -q -pl core,db,gui,cli -am test` passed in the isolated worktree
- `.\mvnw.cmd -q -pl cli -am "-Dmaven.test.skip=true" package` passed
- prerelease-style packaging validation succeeded with:
  - app image: `dist\wms-pallet-tag-system-1.7.1-rc1-app`
  - installer: `dist\WMS Pallet Tag System-1.7.1-rc1.exe`
