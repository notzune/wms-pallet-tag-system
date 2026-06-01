# Oracle ODBC Setup Installer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local-only Intune-ready Windows executable that installs Oracle 19c 64-bit ODBC connectivity and creates all Warehouse Analyzer System DSNs.

**Architecture:** Keep the installer engine in readable PowerShell scripts under `scripts/odbc-setup/`, with pure functions isolated in `ODBCSetup.Core.ps1` so most behavior can be tested without Oracle installed. `build-exe.ps1` stages the scripts plus a local Oracle ZIP and generates a single self-extracting executable under `dist/odbc-setup/`; the Oracle ZIP and generated EXE remain local artifacts only.

**Tech Stack:** Windows PowerShell 5.1 compatible scripts, `cmd.exe` launcher, .NET Framework/C# compiler for the self-extracting wrapper when available, Windows ODBC cmdlets and 64-bit registry fallback, simple repo-local PowerShell tests.

---

## File Structure

- Create: `scripts/odbc-setup/ODBCSetup.Core.ps1`
  - Pure functions and small side-effect helpers shared by install, detect, uninstall, and tests.
  - Owns site inventory, `tnsnames.ora` rendering, response-file patching, Oracle driver discovery, DSN registry payload generation, verification result formatting, and log redaction helpers.
- Create: `scripts/odbc-setup/install.cmd`
  - Minimal entry point that launches 64-bit PowerShell with execution policy bypass and passes through arguments.
- Create: `scripts/odbc-setup/install-odbc.ps1`
  - Orchestrates install: elevation/platform checks, payload extraction, Oracle silent install, network config write, DSN creation, optional credential smoke tests, verification, logging, and exit codes.
- Create: `scripts/odbc-setup/detect.ps1`
  - Intune detection script. It imports core functions, runs structural/offline verification only, prints concise results, and exits `0` only when installed state is complete.
- Create: `scripts/odbc-setup/uninstall.ps1`
  - Removes package-owned DSNs and metadata by default. Optional `-RemoveOracleClient` switch may be stubbed or implemented only if safe.
- Create: `scripts/odbc-setup/build-exe.ps1`
  - Builds local bundle/executable from committed scripts plus a local Oracle ZIP path.
- Create: `scripts/odbc-setup/README.md`
  - IT-facing build/deploy notes, including the local-only Oracle ZIP rule.
- Create: `scripts/odbc-setup/payload/oracle-client-install.rsp.template`
  - Response file template with token placeholders.
- Create: `scripts/tests/odbc-setup.tests.ps1`
  - Pure-function and packaging-staging tests that do not require Oracle.
- Modify: `.gitignore`
  - Ensure local ODBC artifacts and payload ZIP are ignored even if someone stages them accidentally.
- Modify: `docs/superpowers/specs/2026-05-15-oracle-odbc-setup-design.md`
  - Already updated for optional credential smoke-test policy; commit this with the plan.

---

### Task 1: Ignore Local Oracle Payload And Generated ODBC Artifacts

**Files:**
- Modify: `.gitignore`
- Test: `git status --short -- scripts/odbc-setup dist/odbc-setup`

- [ ] **Step 1: Add ignore patterns**

Add these patterns near existing build/output ignores:

```gitignore
# Local-only Oracle ODBC setup payloads and generated packages
dist/odbc-setup/
scripts/odbc-setup/payload/*.zip
scripts/odbc-setup/payload/*.7z
scripts/odbc-setup/payload/*.exe
```

- [ ] **Step 2: Verify ignore behavior**

Create a temporary local file manually or with PowerShell:

```powershell
New-Item -ItemType Directory -Path scripts\odbc-setup\payload -Force
Set-Content -LiteralPath scripts\odbc-setup\payload\O19-64bit.zip -Value "fake" -Encoding ASCII
git status --short -- scripts/odbc-setup/payload/O19-64bit.zip
Remove-Item -LiteralPath scripts\odbc-setup\payload\O19-64bit.zip -Force
```

Expected: `git status` prints nothing for the fake ZIP.

- [ ] **Step 3: Commit**

```powershell
git add .gitignore
git commit -m "chore(odbc): ignore local installer artifacts"
```

---

### Task 2: Add Core Site Inventory And TNS Rendering Tests

**Files:**
- Create: `scripts/tests/odbc-setup.tests.ps1`
- Create: `scripts/odbc-setup/ODBCSetup.Core.ps1`

- [ ] **Step 1: Write failing tests for site inventory and TNS output**

Create `scripts/tests/odbc-setup.tests.ps1` with local assertion helpers matching the existing script-test style:

```powershell
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Assert-Equal {
    param([string]$Expected, [string]$Actual, [string]$Message)
    if ($Expected -ne $Actual) { throw "$Message`nExpected: $Expected`nActual:   $Actual" }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$coreScript = Join-Path $sourceRoot "scripts\odbc-setup\ODBCSetup.Core.ps1"
. $coreScript

$sites = Get-OdbcSetupSites
Assert-Equal "7" ([string]$sites.Count) "Should define seven Warehouse Analyzer sites"
Assert-True ($sites.Code -contains "TBG3002") "Should include Jersey City TBG3002"

$tns = New-TnsNamesContent -Sites $sites
foreach ($site in $sites) {
    Assert-True ($tns.Contains($site.Code + " =")) "tnsnames should include alias $($site.Code)"
    Assert-True ($tns.Contains("(HOST = $($site.Host))")) "tnsnames should include host $($site.Host)"
}
Assert-True ($tns.Contains("(PORT = 1521)")) "tnsnames should include port 1521"
Assert-True ($tns.Contains("(SERVICE_NAME = WMSP)")) "tnsnames should include service WMSP"

Write-Host "PASS: odbc setup core generates site inventory and tnsnames.ora"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL because `ODBCSetup.Core.ps1` or functions do not exist.

- [ ] **Step 3: Implement minimal core functions**

Create `scripts/odbc-setup/ODBCSetup.Core.ps1`:

```powershell
$script:OdbcSetupSites = @(
    [pscustomobject]@{ Code = 'TBG1000'; Name = 'Bradenton'; Host = '10.18.228.52' },
    [pscustomobject]@{ Code = 'TBG1010'; Name = 'City of Industry'; Host = '10.19.96.103' },
    [pscustomobject]@{ Code = 'TBG1011'; Name = 'Fort Pierce'; Host = '10.18.228.72' },
    [pscustomobject]@{ Code = 'TBG1279'; Name = 'Kevita'; Host = '10.19.96.104' },
    [pscustomobject]@{ Code = 'TBG3002'; Name = 'Jersey'; Host = '10.19.68.61' },
    [pscustomobject]@{ Code = 'TBG3230'; Name = 'Midwest'; Host = '10.18.228.66' },
    [pscustomobject]@{ Code = 'TBG3322'; Name = 'Walnut'; Host = '10.19.68.53' }
)

function Get-OdbcSetupSites {
    return @($script:OdbcSetupSites)
}

function New-TnsNamesContent {
    param([object[]]$Sites = (Get-OdbcSetupSites))

    $blocks = foreach ($site in $Sites) {
@"
$($site.Code) =
  (DESCRIPTION =
    (ADDRESS = (PROTOCOL = TCP)(HOST = $($site.Host))(PORT = 1521))
    (CONNECT_DATA =
      (SERVICE_NAME = WMSP)
    )
  )
"@
    }
    return (($blocks -join [Environment]::NewLine + [Environment]::NewLine) + [Environment]::NewLine)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add scripts/odbc-setup/ODBCSetup.Core.ps1 scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add site inventory and tns rendering"
```

---

### Task 3: Add Response File Patching And Secret Redaction

**Files:**
- Modify: `scripts/tests/odbc-setup.tests.ps1`
- Modify: `scripts/odbc-setup/ODBCSetup.Core.ps1`
- Create: `scripts/odbc-setup/payload/oracle-client-install.rsp.template`

- [ ] **Step 1: Write failing tests**

Append tests:

```powershell
$template = @"
ORACLE_BASE=__ORACLE_BASE__
ORACLE_HOME=__ORACLE_HOME__
oracle.install.IsBuiltInAccount=__BUILT_IN_ACCOUNT__
oracle.install.client.installType=__INSTALL_TYPE__
"@
$response = New-OracleClientResponseContent -TemplateContent $template `
    -OracleBase "C:\App\Client\Oracle" `
    -OracleHome "C:\App\Client\Oracle\Product\19.0.0\Client_1"
Assert-True ($response.Contains("ORACLE_BASE=C:\App\Client\Oracle")) "Response should set Oracle base"
Assert-True ($response.Contains("ORACLE_HOME=C:\App\Client\Oracle\Product\19.0.0\Client_1")) "Response should set Oracle home"
Assert-True ($response.Contains("oracle.install.IsBuiltInAccount=true")) "Response should use built-in account"
Assert-True ($response.Contains("oracle.install.client.installType=Administrator")) "Response should use Administrator install"

$redacted = Protect-OdbcSetupLogText "UID=RPTADM;PWD=Report_Password12!@#;Password=abc"
Assert-True (-not $redacted.Contains("Report_Password12")) "Redaction should remove PWD value"
Assert-True (-not $redacted.Contains("Password=abc")) "Redaction should remove Password value"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL for missing functions.

- [ ] **Step 3: Implement response and redaction functions**

Add to core:

```powershell
function New-OracleClientResponseContent {
    param(
        [Parameter(Mandatory = $true)][string]$TemplateContent,
        [Parameter(Mandatory = $true)][string]$OracleBase,
        [Parameter(Mandatory = $true)][string]$OracleHome
    )

    return $TemplateContent.
        Replace('__ORACLE_BASE__', $OracleBase).
        Replace('__ORACLE_HOME__', $OracleHome).
        Replace('__BUILT_IN_ACCOUNT__', 'true').
        Replace('__INSTALL_TYPE__', 'Administrator')
}

function Protect-OdbcSetupLogText {
    param([string]$Text)
    if ($null -eq $Text) { return $null }
    $safe = $Text -replace '(?i)(PWD|Password)\s*=\s*[^;\s]+', '$1=<redacted>'
    return $safe
}
```

Create response template:

```text
ORACLE_BASE=__ORACLE_BASE__
ORACLE_HOME=__ORACLE_HOME__
oracle.install.IsBuiltInAccount=__BUILT_IN_ACCOUNT__
oracle.install.client.installType=__INSTALL_TYPE__
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add scripts/odbc-setup/ODBCSetup.Core.ps1 scripts/odbc-setup/payload/oracle-client-install.rsp.template scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add response rendering and log redaction"
```

---

### Task 4: Add DSN Payload And Verification Model

**Files:**
- Modify: `scripts/tests/odbc-setup.tests.ps1`
- Modify: `scripts/odbc-setup/ODBCSetup.Core.ps1`

- [ ] **Step 1: Write failing tests for DSN values and credential omission**

Append tests:

```powershell
$dsnValues = New-OdbcDsnPropertyValues -SiteCode "TBG3002"
Assert-True ($dsnValues -contains "ServerName=TBG3002") "DSN values should include ServerName"
Assert-True ($dsnValues -contains "DBQ=TBG3002") "DSN values should include DBQ"
Assert-True (-not (($dsnValues -join ';') -match '(?i)UID|PWD|Password')) "DSN values should omit credentials"

$registry = New-OdbcDsnRegistryValueMap -SiteCode "TBG3002" -DriverName "Oracle in OraClient19Home1"
Assert-Equal "Oracle in OraClient19Home1" $registry["Driver"] "Registry values should include driver"
Assert-Equal "TBG3002" $registry["ServerName"] "Registry values should include ServerName"
Assert-True (-not ($registry.ContainsKey("UID"))) "Registry values should omit UID"
Assert-True (-not ($registry.ContainsKey("PWD"))) "Registry values should omit PWD"

$result = New-OdbcSetupCheckResult -Check "DSN TBG3002" -Passed $true -Details "ok"
Assert-Equal "DSN TBG3002" $result.Check "Verification result should preserve check name"
Assert-Equal "True" ([string]$result.Passed) "Verification result should preserve pass state"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL for missing DSN/verification functions.

- [ ] **Step 3: Implement DSN payload helpers**

Add to core:

```powershell
function New-OdbcDsnPropertyValues {
    param([Parameter(Mandatory = $true)][string]$SiteCode)
    return @(
        "ServerName=$SiteCode",
        "DBQ=$SiteCode"
    )
}

function New-OdbcDsnRegistryValueMap {
    param(
        [Parameter(Mandatory = $true)][string]$SiteCode,
        [Parameter(Mandatory = $true)][string]$DriverName
    )

    return [ordered]@{
        Driver = $DriverName
        ServerName = $SiteCode
        DBQ = $SiteCode
    }
}

function New-OdbcSetupCheckResult {
    param([string]$Check, [bool]$Passed, [string]$Details)
    [pscustomobject]@{
        Check = $Check
        Passed = $Passed
        Details = $Details
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add scripts/odbc-setup/ODBCSetup.Core.ps1 scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add dsn payload helpers"
```

---

### Task 5: Implement Offline Detection

**Files:**
- Modify: `scripts/odbc-setup/ODBCSetup.Core.ps1`
- Create: `scripts/odbc-setup/detect.ps1`
- Modify: `scripts/tests/odbc-setup.tests.ps1`

- [ ] **Step 1: Write failing tests for detection result aggregation**

Append tests that use temporary registry/config paths through test seams rather than real HKLM writes:

```powershell
$missing = Test-OdbcSetupState -OracleHome "Z:\missing-oracle-home" -DriverName $null -TnsContent "" -DsnMap @{}
Assert-True (($missing | Where-Object { -not $_.Passed }).Count -gt 0) "Missing state should produce failures"

$sites = Get-OdbcSetupSites
$presentDsnMap = @{}
foreach ($site in $sites) { $presentDsnMap[$site.Code] = @{ ServerName = $site.Code; DBQ = $site.Code } }
$present = Test-OdbcSetupState -OracleHome $env:TEMP -DriverName "Oracle in OraClient19Home1" -TnsContent (New-TnsNamesContent -Sites $sites) -DsnMap $presentDsnMap
Assert-Equal "0" ([string](($present | Where-Object { -not $_.Passed }).Count)) "Complete test state should pass"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL for missing `Test-OdbcSetupState`.

- [ ] **Step 3: Implement core state verification**

Add `Test-OdbcSetupState` to core. It should accept injectable values for tests, and production wrappers can supply real filesystem/registry data.

- [ ] **Step 4: Implement `detect.ps1`**

Create script that:

```powershell
[CmdletBinding()]
param(
    [string]$OracleHome = "C:\App\Client\Oracle\Product\19.0.0\Client_1"
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $PSCommandPath
. (Join-Path $scriptRoot "ODBCSetup.Core.ps1")

$driver = Get-InstalledOracleOdbcDriverName
$tnsPath = Join-Path $OracleHome "Network\Admin\tnsnames.ora"
$tnsContent = if (Test-Path -LiteralPath $tnsPath) { Get-Content -LiteralPath $tnsPath -Raw } else { "" }
$dsnMap = Get-SystemOdbcDsnMap
$results = Test-OdbcSetupState -OracleHome $OracleHome -DriverName $driver -TnsContent $tnsContent -DsnMap $dsnMap

$results | ForEach-Object {
    Write-Host ("[{0}] {1}: {2}" -f $(if ($_.Passed) { "PASS" } else { "FAIL" }), $_.Check, $_.Details)
}

if (($results | Where-Object { -not $_.Passed }).Count -gt 0) { exit 1 }
exit 0
```

- [ ] **Step 5: Run tests**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add scripts/odbc-setup/ODBCSetup.Core.ps1 scripts/odbc-setup/detect.ps1 scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add offline detection checks"
```

---

### Task 6: Implement Install Orchestration

**Files:**
- Create: `scripts/odbc-setup/install.cmd`
- Create: `scripts/odbc-setup/install-odbc.ps1`
- Modify: `scripts/odbc-setup/ODBCSetup.Core.ps1`
- Modify: `scripts/tests/odbc-setup.tests.ps1`

- [ ] **Step 1: Add tests for credential smoke-test log redaction**

Append tests:

```powershell
$smokeLog = Format-CredentialSmokeTestResult -SiteCode "TBG3002" -Username "RPTADM" -Passed $true -Message "ok"
Assert-True ($smokeLog.Contains("TBG3002")) "Smoke log should include site"
Assert-True ($smokeLog.Contains("RPTADM")) "Smoke log should include username"
Assert-True (-not $smokeLog.Contains("Report_Password")) "Smoke log should not include password"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL for missing formatter.

- [ ] **Step 3: Implement install helpers**

Add side-effect helpers to core:

- `Assert-OdbcSetupIs64BitPowerShell`
- `Test-OdbcSetupIsElevated`
- `Get-InstalledOracleOdbcDriverName`
- `Get-SystemOdbcDsnMap`
- `Set-SystemOdbcDsn`
- `Write-TnsNamesFile`
- `Invoke-OdbcCredentialSmokeTest`
- `Format-CredentialSmokeTestResult`

Keep credential smoke-test optional and in-memory:

```powershell
function Format-CredentialSmokeTestResult {
    param([string]$SiteCode, [string]$Username, [bool]$Passed, [string]$Message)
    $status = if ($Passed) { "passed" } else { "failed" }
    return ("{0} credential test {1} as {2}: {3}" -f $SiteCode, $status, $Username, (Protect-OdbcSetupLogText $Message))
}
```

- [ ] **Step 4: Create `install.cmd`**

Use `%SystemRoot%\SysNative\WindowsPowerShell\v1.0\powershell.exe` when available so 32-bit launch contexts still reach 64-bit PowerShell:

```bat
@echo off
setlocal
set SCRIPT_DIR=%~dp0
set PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe
if exist "%SystemRoot%\SysNative\WindowsPowerShell\v1.0\powershell.exe" set PS=%SystemRoot%\SysNative\WindowsPowerShell\v1.0\powershell.exe
"%PS%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%install-odbc.ps1" %*
exit /b %ERRORLEVEL%
```

- [ ] **Step 5: Create `install-odbc.ps1`**

Parameters:

```powershell
param(
    [string]$OracleZipPath,
    [string]$OracleBase = "C:\App\Client\Oracle",
    [string]$OracleHome = "C:\App\Client\Oracle\Product\19.0.0\Client_1",
    [string]$ProgramDataRoot = "C:\ProgramData\Tropicana\ODBCSetup",
    [switch]$RunCredentialSmokeTest,
    [string]$SmokeTestUsername,
    [string]$SmokeTestPassword
)
```

Behavior:

- Resolve bundled `payload\O19-64bit.zip` if `-OracleZipPath` is not passed.
- Create logs under `$ProgramDataRoot\Logs`.
- Fail early if not 64-bit PowerShell or not elevated.
- Expand Oracle ZIP to `$env:TEMP\Tropicana-ODBCSetup-*`.
- Render response file.
- Run Oracle `setup.exe` silently and capture exit code.
- Write `tnsnames.ora`.
- Create or repair all DSNs.
- If `-RunCredentialSmokeTest`, require username/password and test each site in memory.
- Run structural verification and exit nonzero if failures remain.

- [ ] **Step 6: Run tests**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add scripts/odbc-setup/install.cmd scripts/odbc-setup/install-odbc.ps1 scripts/odbc-setup/ODBCSetup.Core.ps1 scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add installer orchestration"
```

---

### Task 7: Implement Uninstall

**Files:**
- Create: `scripts/odbc-setup/uninstall.ps1`
- Modify: `scripts/odbc-setup/ODBCSetup.Core.ps1`
- Modify: `scripts/tests/odbc-setup.tests.ps1`

- [ ] **Step 1: Write tests for uninstall target list**

Append tests:

```powershell
$removeList = Get-OdbcSetupOwnedDsnNames
Assert-Equal "7" ([string]$removeList.Count) "Uninstall should target seven owned DSNs"
Assert-True ($removeList -contains "TBG3002") "Uninstall should target TBG3002"
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL for missing function.

- [ ] **Step 3: Implement uninstall helpers and script**

`uninstall.ps1` should:

- Import core.
- Require elevation.
- Remove each owned System DSN with ODBC cmdlets when available, then registry fallback.
- Remove package-owned install state files, but preserve logs by default unless `-RemoveLogs` is passed.
- Not remove Oracle Client unless `-RemoveOracleClient` is explicitly passed. If client removal is not implemented in this first pass, fail clearly when the switch is passed rather than silently doing nothing.

- [ ] **Step 4: Run tests**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add scripts/odbc-setup/uninstall.ps1 scripts/odbc-setup/ODBCSetup.Core.ps1 scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add dsn uninstall script"
```

---

### Task 8: Implement Local EXE Builder

**Files:**
- Create: `scripts/odbc-setup/build-exe.ps1`
- Modify: `scripts/tests/odbc-setup.tests.ps1`

- [ ] **Step 1: Write tests for staging without real Oracle media**

Append a test that creates a fake Oracle ZIP and verifies staging output:

```powershell
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("odbc-build-test-" + [guid]::NewGuid().ToString("N"))
try {
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    $fakeZip = Join-Path $tempRoot "O19-64bit.zip"
    Set-Content -LiteralPath $fakeZip -Value "fake zip" -Encoding ASCII
    $out = Join-Path $tempRoot "out"
    & (Join-Path $sourceRoot "scripts\odbc-setup\build-exe.ps1") -OracleZipPath $fakeZip -OutputDir $out -StageOnly
    Assert-True (Test-Path -LiteralPath (Join-Path $out "stage\payload\O19-64bit.zip")) "Builder should stage Oracle ZIP"
    Assert-True (Test-Path -LiteralPath (Join-Path $out "stage\install.cmd")) "Builder should stage install.cmd"
} finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: FAIL because builder does not exist.

- [ ] **Step 3: Implement `build-exe.ps1` stage mode**

Parameters:

```powershell
param(
    [Parameter(Mandatory = $true)][string]$OracleZipPath,
    [string]$OutputDir,
    [switch]$StageOnly
)
```

Behavior:

- Resolve source root from script path.
- Refuse missing Oracle ZIP.
- Create `$OutputDir\stage`.
- Copy committed setup files.
- Copy Oracle ZIP as `payload\O19-64bit.zip`.
- In `-StageOnly`, stop before compiling.

- [ ] **Step 4: Add EXE generation**

Implement self-extracting wrapper generation:

- Compress stage folder into an embedded base64 ZIP or temporary resource.
- Generate a small C# launcher source under temp.
- Compile with the newest available `%WINDIR%\Microsoft.NET\Framework64\*\csc.exe`.
- Launcher extracts payload ZIP to `%TEMP%\Tropicana-ODBCSetup-*`, runs `install.cmd`, waits, forwards exit code, and cleans extraction directory.
- If no C# compiler is available, fail with an actionable message.

- [ ] **Step 5: Run tests**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
```

Expected: PASS. Stage-only test should pass without compiling an EXE.

- [ ] **Step 6: Commit**

```powershell
git add scripts/odbc-setup/build-exe.ps1 scripts/tests/odbc-setup.tests.ps1
git commit -m "feat(odbc): add local executable builder"
```

---

### Task 9: Add IT README And Final Verification

**Files:**
- Create: `scripts/odbc-setup/README.md`
- Modify: `docs/superpowers/specs/2026-05-15-oracle-odbc-setup-design.md` if implementation details changed

- [ ] **Step 1: Write README**

Include:

- Purpose.
- Build command:

```powershell
.\scripts\odbc-setup\build-exe.ps1 -OracleZipPath "C:\Users\zrashed\Downloads\O19-64bit (1).zip"
```

- Generated output path: `dist\odbc-setup\Tropicana-Oracle-ODBC-Setup.exe`.
- Intune install command.
- Intune detection command:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\detect.ps1
```

- Credential smoke-test policy:
  - DSNs stay credential-free.
  - Optional smoke test uses read-only credentials in memory.
  - Password is not logged.
- Uninstall behavior.

- [ ] **Step 2: Run all script tests**

Run:

```powershell
.\scripts\tests\odbc-setup.tests.ps1
.\scripts\tests\build-tropicana-installer.tests.ps1
.\scripts\tests\install-tropicana-config.tests.ps1
.\scripts\tests\install-wms-installer.tests.ps1
.\scripts\tests\run-smoke-tests.tests.ps1
```

Expected: all print `PASS` and exit `0`.

- [ ] **Step 3: Build stage with local Oracle ZIP**

Run when the Oracle ZIP is available locally:

```powershell
.\scripts\odbc-setup\build-exe.ps1 -OracleZipPath "C:\Users\zrashed\Downloads\O19-64bit (1).zip" -StageOnly
```

Expected: `dist\odbc-setup\stage\payload\O19-64bit.zip` exists and remains ignored by git.

- [ ] **Step 4: Build EXE**

Run:

```powershell
.\scripts\odbc-setup\build-exe.ps1 -OracleZipPath "C:\Users\zrashed\Downloads\O19-64bit (1).zip"
```

Expected: `dist\odbc-setup\Tropicana-Oracle-ODBC-Setup.exe` exists and remains ignored by git.

- [ ] **Step 5: Check git status for forbidden artifacts**

Run:

```powershell
git status --short -- scripts/odbc-setup dist/odbc-setup
```

Expected: committed scripts may appear before commit; no `*.zip` or generated `*.exe` appears.

- [ ] **Step 6: Commit**

```powershell
git add scripts/odbc-setup/README.md docs/superpowers/specs/2026-05-15-oracle-odbc-setup-design.md
git commit -m "docs(odbc): add setup build and deployment notes"
```

---

## Manual Integration Validation

Run these on a clean Windows VM or Intune test device, not on a developer machine with unknown Oracle state:

- [ ] Install generated EXE through elevated PowerShell.
- [ ] Confirm logs are written under `C:\ProgramData\Tropicana\ODBCSetup\Logs`.
- [ ] Run `detect.ps1`; expect exit `0`.
- [ ] Open 64-bit ODBC Administrator and confirm all seven System DSNs.
- [ ] Run optional credential smoke test with read-only credentials; confirm logs show pass/fail per site without password.
- [ ] Refresh one Excel Analyzer using its SOP connection string.
- [ ] Rerun installer; confirm idempotent repair and no duplicate DSNs.
- [ ] Run uninstall; confirm owned DSNs are removed and Oracle Client remains installed by default.
