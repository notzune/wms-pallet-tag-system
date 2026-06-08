# ZIP-First Shareable Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the portable ZIP the primary company-shareable release artifact again while keeping the installer build available for private/local use.

**Architecture:** Keep the existing portable and installer builders intact, but change release positioning and documentation so the ZIP is the default distribution path. Validate both repo and packaged paths still work, then generate a fresh portable ZIP artifact from the current version.

**Tech Stack:** PowerShell build scripts, GitHub Actions release workflow, Maven, existing smoke-test harness, Markdown docs

---

### Task 1: Reposition release documentation around the portable ZIP

**Files:**
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\README.md`
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\CHANGELOG.md`
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\docs\gui-backend-coverage-inventory.md`

- [ ] **Step 1: Write the failing doc expectations**

Define the expected wording changes before editing:
- portable ZIP is the recommended company-shareable artifact
- installer remains available for private/local/Tropicana use
- release notes mention the ZIP-first positioning change

- [ ] **Step 2: Inspect current wording to verify the mismatch**

Run:
```powershell
Select-String -Path README.md,CHANGELOG.md,docs\gui-backend-coverage-inventory.md -Pattern 'Portable|installer|shareable|release'
```
Expected: current text still presents installer and ZIP as peers or installer-forward in some sections.

- [ ] **Step 3: Write the minimal documentation updates**

Update the three docs so they consistently say:
- the portable ZIP is the default shareable/operator package
- the installer path is optional/private/internal
- the current release notes capture that change

- [ ] **Step 4: Run a quick doc sanity check**

Run:
```powershell
git diff -- README.md CHANGELOG.md docs\gui-backend-coverage-inventory.md
```
Expected: only ZIP-first/release-positioning wording changes appear.

- [ ] **Step 5: Commit**

```bash
git add README.md CHANGELOG.md docs/gui-backend-coverage-inventory.md
git commit -m "docs: restore portable zip as shareable release path"
```

### Task 2: Keep release workflow aligned with dual artifacts

**Files:**
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\.github\workflows\release.yml`
- Test: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\README.md`

- [ ] **Step 1: Identify whether workflow changes are actually needed**

Run:
```powershell
Get-Content .github\workflows\release.yml
```
Expected: workflow already builds and uploads the portable ZIP plus installer assets.

- [ ] **Step 2: If workflow wording or artifact ordering needs adjustment, make the minimal change**

Only change the workflow if it is needed to better reflect ZIP-first release behavior. Keep the installer build and upload intact.

- [ ] **Step 3: Verify workflow diff is minimal**

Run:
```powershell
git diff -- .github\workflows\release.yml
```
Expected: either no diff, or a very small non-behavioral update.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: keep portable zip as primary shareable release artifact"
```

### Task 3: Build and verify a fresh shareable portable ZIP

**Files:**
- Modify: none
- Test: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\build-portable-bundle.ps1`
- Test: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\run-smoke-tests.ps1`

- [ ] **Step 1: Build the current jar if needed**

Run:
```powershell
.\mvnw.cmd -q -pl cli -am "-Dmaven.test.skip=true" package
```
Expected: exit code 0 and current `cli-*.jar` in `cli\target`.

- [ ] **Step 2: Build the portable ZIP**

Run:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-portable-bundle.ps1 -BundleDir .\dist\wms-pallet-tag-system-1.7.6-portable
```
Expected: `dist\wms-pallet-tag-system-1.7.6-portable.zip` is created.

- [ ] **Step 3: Verify the portable ZIP contents exist**

Run:
```powershell
Get-Item .\dist\wms-pallet-tag-system-1.7.6-portable.zip
Get-ChildItem .\dist\wms-pallet-tag-system-1.7.6-portable
```
Expected: ZIP exists and extracted bundle contains `run.bat`, `wms-tags-gui.bat`, `runtime\`, `config\`, and `wms-tags.jar`.

- [ ] **Step 4: Re-run the repo smoke or targeted portable verification**

Run:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode repo
```
Expected: PASS for all repo scenarios.

- [ ] **Step 5: Commit**

No commit required for generated artifacts unless the user explicitly asks to version them in git.

### Task 4: Deliver the shareable artifact path and repo status

**Files:**
- Modify: none

- [ ] **Step 1: Capture final git status**

Run:
```powershell
git status --short
```
Expected: only unrelated pre-existing worktree changes remain.

- [ ] **Step 2: Report the exact artifact paths**

Provide:
- `dist\wms-pallet-tag-system-1.7.6-portable.zip`
- any matching bundle folder path
- note that the installer path still exists for private/local use

- [ ] **Step 3: If repository files changed, push them**

Run:
```bash
git push origin main
```
Expected: docs/workflow updates are on `origin/main`.
