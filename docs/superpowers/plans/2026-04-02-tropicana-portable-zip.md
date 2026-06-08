# Tropicana Portable ZIP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Tropicana-specific portable ZIP build that ships the real Tropicana `wms-tags.env` in the bundle root so operators can extract and run with no separate config step.

**Architecture:** Keep the existing generic portable builder for sanitized public ZIPs, and add a small opt-in path that swaps the root bundle env file with a caller-provided Tropicana config payload. Reuse the current portable layout and verification flow instead of introducing a new first-run installer path.

**Tech Stack:** PowerShell build scripts, existing portable bundle layout, Markdown docs, CLI config verification

---

### Task 1: Extend portable packaging to support a caller-provided root config

**Files:**
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\build-portable-bundle.ps1`
- Test: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\build-portable-bundle.ps1`

- [ ] **Step 1: Write the failing packaging expectation**

Define the expected behavior:
- default portable build still seeds `config\wms-tags.env.example`
- new Tropicana build path can inject a real config file into the bundle root as `wms-tags.env`

- [ ] **Step 2: Verify current builder lacks the override**

Run:
```powershell
Get-Content .\scripts\build-portable-bundle.ps1
```
Expected: builder always copies `config\wms-tags.env.example` into the bundle root.

- [ ] **Step 3: Implement the minimal override**

Add an optional parameter such as `-RootConfigSourcePath` and use it only when provided. Default behavior must remain unchanged.

- [ ] **Step 4: Run a focused verification**

Run:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-portable-bundle.ps1 -BundleDir <temp-bundle-dir>
```
Expected: generic bundle still builds successfully.

- [ ] **Step 5: Commit**

```bash
git add scripts/build-portable-bundle.ps1
git commit -m "feat(build): allow portable bundle root config override"
```

### Task 2: Add a Tropicana-specific portable ZIP build entrypoint

**Files:**
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\build-tropicana-installer.ps1`
- Create or Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\build-tropicana-portable.ps1`
- Test: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\scripts\build-tropicana-portable.ps1`

- [ ] **Step 1: Decide the cleanest entrypoint**

Prefer a dedicated `build-tropicana-portable.ps1` script so Tropicana portable packaging is explicit and does not overload the installer-focused script.

- [ ] **Step 2: Implement the dedicated builder**

The script should:
- accept `-ConfigSourcePath`
- call `build-portable-bundle.ps1` with the new root-config override
- emit a clearly named artifact such as `dist\wms-pallet-tag-system-<version>-portable-tropicana.zip`

- [ ] **Step 3: Verify it builds**

Run:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-tropicana-portable.ps1 -ConfigSourcePath .\.env
```
Expected: Tropicana portable bundle folder and ZIP are created.

- [ ] **Step 4: Verify the bundled config is the real Tropicana payload**

Run:
```powershell
Get-Content .\dist\wms-pallet-tag-system-<version>-portable-tropicana\wms-tags.env
```
Expected: contents match the provided Tropicana config instead of the example template.

- [ ] **Step 5: Commit**

```bash
git add scripts/build-tropicana-portable.ps1 scripts/build-tropicana-installer.ps1
git commit -m "feat(build): add tropicana portable zip packaging"
```

### Task 3: Update documentation for the new Tropicana shareable ZIP

**Files:**
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\README.md`
- Modify: `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system\CHANGELOG.md`

- [ ] **Step 1: Write the failing doc expectations**

Docs should clearly distinguish:
- generic portable ZIP = sanitized/shareable baseline
- Tropicana portable ZIP = preconfigured internal share package with no separate config step

- [ ] **Step 2: Implement the minimal doc updates**

Document:
- new Tropicana portable builder command
- output artifact names
- no separate config step for the Tropicana portable ZIP

- [ ] **Step 3: Verify doc diffs are focused**

Run:
```powershell
git diff -- README.md CHANGELOG.md
```
Expected: only Tropicana portable ZIP documentation changes.

- [ ] **Step 4: Commit**

```bash
git add README.md CHANGELOG.md
git commit -m "docs: add tropicana portable zip distribution path"
```

### Task 4: Verify and deliver the Tropicana portable ZIP

**Files:**
- Modify: none

- [ ] **Step 1: Run functional verification through the portable launcher**

Run:
```powershell
cmd /c "<tropicana-portable-bundle>\\run.bat config"
```
Expected: config resolves from the bundle root `wms-tags.env` with no separate setup step.

- [ ] **Step 2: Capture final artifact paths**

Report:
- Tropicana portable folder path
- Tropicana portable ZIP path
- existing generic portable ZIP path

- [ ] **Step 3: Push repo changes**

Run:
```bash
git push origin main
```
Expected: packaging/doc updates are on `origin/main`.
