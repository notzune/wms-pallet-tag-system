# Tropicana Installer And Config Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local-only Tropicana installer flow that installs the app from scratch, persists Tropicana config in a per-user location across updates, keeps public builds free of live secrets, and preserves a fallback config-only installer script.

**Architecture:** Harden the public packaging scripts first so they always ship template config only. Then add a per-user config precedence layer in the Java config locator, add a fallback `Install-Tropicana-Config.ps1`, and add a local-only Tropicana bootstrap packaging script that wraps the normal installer and writes the per-user config after install.

**Tech Stack:** Java 17 core config loading, PowerShell packaging/setup scripts, jpackage/WiX installer flow, README/CHANGELOG docs

---

### Task 1: Add failing tests for per-user Tropicana config precedence

**Files:**
- Modify: `core/src/test/java/com/tbg/wms/core/AppConfigTest.java`
- Modify: `core/src/main/java/com/tbg/wms/core/ConfigFileLocator.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void testPerUserLocalAppDataConfigWinsOverWorkingDirectoryConfig() throws Exception {
    Path appDataConfig = Files.createTempDirectory("appdata").resolve("Tropicana/WMS-Pallet-Tag-System/wms-tags.env");
    Files.createDirectories(appDataConfig.getParent());
    Files.writeString(appDataConfig, "ORACLE_USERNAME=local_user\nORACLE_PASSWORD=local_pass\n", UTF_8);

    Map<String, String> env = Map.of("LOCALAPPDATA", appDataConfig.getParent().getParent().toString());
    AppConfig cfg = new AppConfig(env, null);

    assertEquals("local_user", cfg.oracleUsername());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl core -Dtest=AppConfigTest#testPerUserLocalAppDataConfigWinsOverWorkingDirectoryConfig test`
Expected: FAIL because `ConfigFileLocator` does not search `%LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System`

- [ ] **Step 3: Write minimal implementation**

```java
Path perUserConfig = resolvePerUserConfig(defaultFileName);
if (perUserConfig != null) {
    candidates.add(perUserConfig);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl core -Dtest=AppConfigTest#testPerUserLocalAppDataConfigWinsOverWorkingDirectoryConfig test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/src/test/java/com/tbg/wms/core/AppConfigTest.java core/src/main/java/com/tbg/wms/core/ConfigFileLocator.java
git commit -m "fix: prefer per-user Tropicana config path"
```

### Task 2: Add failing tests for public packaging config seeding

**Files:**
- Create: `scripts/tests/build-bundle-config-seeding.tests.ps1`
- Modify: `scripts/build-portable-bundle.ps1`
- Modify: `scripts/build-jpackage-bundle.ps1`

- [ ] **Step 1: Write the failing test**

```powershell
Describe 'public bundle config seeding' {
    It 'uses config\wms-tags.env.example even when repo root .env exists' {
        # Arrange temp source root with both files
        # Invoke portable/jpackage seeding helper
        # Assert bundled wms-tags.env matches example content
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\build-bundle-config-seeding.tests.ps1`
Expected: FAIL because current builders can seed from the repo root `.env`

- [ ] **Step 3: Write minimal implementation**

```powershell
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\wms-tags.env.example") -Destination $bundleEnvPath -Force
```

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\build-bundle-config-seeding.tests.ps1`
Expected: PASS for both bundle builders

- [ ] **Step 5: Commit**

```bash
git add scripts/tests/build-bundle-config-seeding.tests.ps1 scripts/build-portable-bundle.ps1 scripts/build-jpackage-bundle.ps1
git commit -m "fix: ship template config in public bundles"
```

### Task 3: Add failing tests for fallback Tropicana config installer behavior

**Files:**
- Create: `scripts/tests/install-tropicana-config.tests.ps1`
- Create: `scripts/Install-Tropicana-Config.ps1`

- [ ] **Step 1: Write the failing test**

```powershell
Describe 'Install-Tropicana-Config' {
    It 'writes per-user config into LOCALAPPDATA and backs up any prior file' {
        # Arrange temp LOCALAPPDATA with an existing wms-tags.env
        # Invoke installer with deterministic payload
        # Assert new file content and timestamped backup
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\install-tropicana-config.tests.ps1`
Expected: FAIL because the installer script does not exist yet

- [ ] **Step 3: Write minimal implementation**

```powershell
param([string]$LocalAppDataRoot, [switch]$SkipVerify)

# resolve %LOCALAPPDATA%\Tropicana\WMS-Pallet-Tag-System\wms-tags.env
# back up prior file
# write Tropicana payload
```

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\install-tropicana-config.tests.ps1`
Expected: PASS for write and backup scenarios

- [ ] **Step 5: Commit**

```bash
git add scripts/tests/install-tropicana-config.tests.ps1 scripts/Install-Tropicana-Config.ps1
git commit -m "feat: add fallback Tropicana config installer"
```

### Task 4: Add Tropicana bootstrap packaging flow

**Files:**
- Create: `scripts/build-tropicana-installer.ps1`
- Modify: `scripts/build-jpackage-bundle.ps1`
- Modify: `scripts/install-wms-installer.ps1`

- [ ] **Step 1: Write a failing smoke-oriented test or verification harness**

```powershell
# Verify builder produces a Tropicana setup artifact and stages the app installer plus config installer assets
```

- [ ] **Step 2: Run verification to confirm the builder is missing**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-tropicana-installer.ps1`
Expected: FAIL because script does not exist yet

- [ ] **Step 3: Write minimal implementation**

```powershell
# build packaged installer
# stage Tropicana payload + fallback config installer
# wrap them in a single self-contained setup EXE with normal installer UI
```

- [ ] **Step 4: Run verification to confirm artifact creation**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-tropicana-installer.ps1 -...`
Expected: produces `WMS Pallet Tag System - Tropicana Setup.exe`

- [ ] **Step 5: Commit**

```bash
git add scripts/build-tropicana-installer.ps1 scripts/build-jpackage-bundle.ps1 scripts/install-wms-installer.ps1
git commit -m "feat: add Tropicana bootstrap installer"
```

### Task 5: Update docs for the final operator/support flow

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Verify docs are missing the new flow**

Run: `Select-String -Path README.md,CHANGELOG.md -Pattern 'Tropicana Setup|LOCALAPPDATA|Install-Tropicana-Config.ps1'`
Expected: missing or incomplete coverage

- [ ] **Step 2: Write minimal documentation changes**

```markdown
- public builds ship template config only
- Tropicana uses a local-only setup EXE
- config persists in `%LOCALAPPDATA%`
- fallback config installer script is for repair/rotation
```

- [ ] **Step 3: Re-check docs**

Run: `Select-String -Path README.md,CHANGELOG.md -Pattern 'Tropicana Setup|LOCALAPPDATA|Install-Tropicana-Config.ps1'`
Expected: updated matches found

- [ ] **Step 4: Commit**

```bash
git add README.md CHANGELOG.md
git commit -m "docs: describe Tropicana installer and persistent config"
```

### Task 6: Full verification

**Files:**
- Verify: `core/src/main/java/com/tbg/wms/core/ConfigFileLocator.java`
- Verify: `scripts/build-portable-bundle.ps1`
- Verify: `scripts/build-jpackage-bundle.ps1`
- Verify: `scripts/Install-Tropicana-Config.ps1`
- Verify: `scripts/build-tropicana-installer.ps1`

- [ ] **Step 1: Run focused core tests**

Run: `.\mvnw.cmd -q -pl core -Dtest=AppConfigTest test`
Expected: PASS

- [ ] **Step 2: Run script-level tests**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\build-bundle-config-seeding.tests.ps1`
Expected: PASS

- [ ] **Step 3: Run config installer tests**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\install-tropicana-config.tests.ps1`
Expected: PASS

- [ ] **Step 4: Run targeted Tropicana packaging smoke verification**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-tropicana-installer.ps1 -...`
Expected: Tropicana setup EXE created and public bundle artifacts still seed template config only

- [ ] **Step 5: Final status check**

Run: `git status --short`
Expected: only intended files changed
