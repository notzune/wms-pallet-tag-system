[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("repo", "packaged")]
    [string]$Mode,
    [string]$ConfigPath,
    [string]$TargetPath,
    [string]$ShipmentId,
    [string]$CarrierMoveId,
    [string]$TrainId,
    [string]$OutputRoot,
    [switch]$IncludeInstallerScenarios
)

$ErrorActionPreference = "Stop"

function Get-ResolvedPathOrThrow {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PathValue,
        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    if ([string]::IsNullOrWhiteSpace($PathValue)) {
        throw "$Label is required."
    }
    if (-not (Test-Path -LiteralPath $PathValue)) {
        throw "$Label not found: $PathValue"
    }
    return (Resolve-Path -LiteralPath $PathValue).Path
}

function Resolve-RepoJarPath {
    param([string]$SourceRoot)

    $jar = Get-ChildItem -Path (Join-Path $SourceRoot "cli\target") -Filter "cli-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "original-*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $jar) {
        throw "Repo smoke target not found. Build the CLI jar first."
    }
    return $jar.FullName
}

function Resolve-PackagedTargetPath {
    param(
        [string]$SourceRoot,
        [string]$TargetPath
    )

    if (-not [string]::IsNullOrWhiteSpace($TargetPath)) {
        return Get-ResolvedPathOrThrow -PathValue $TargetPath -Label "Packaged target"
    }

    $appDir = Get-ChildItem -Path (Join-Path $SourceRoot "dist") -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "wms-pallet-tag-system-*-app" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $appDir) {
        throw "Packaged smoke target not found. Build the app image first or pass -TargetPath."
    }
    return $appDir.FullName
}

function Invoke-SmokeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Mode,
        [Parameter(Mandatory = $true)]
        [string]$ResolvedTargetPath,
        [Parameter(Mandatory = $true)]
        [string]$CommandText,
        [string]$ConfigPath,
        [string]$LocalAppDataRoot
    )

    $oldConfig = $env:WMS_CONFIG_FILE
    $oldLocalAppData = $env:LOCALAPPDATA
    try {
        if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
            Remove-Item Env:WMS_CONFIG_FILE -ErrorAction SilentlyContinue
        } else {
            $env:WMS_CONFIG_FILE = $ConfigPath
        }
        if (-not [string]::IsNullOrWhiteSpace($LocalAppDataRoot)) {
            $env:LOCALAPPDATA = $LocalAppDataRoot
        }

        $parts = @($CommandText -split ' ')
        if ($Mode -eq "repo") {
            return Invoke-ProcessCommand -FilePath "java" -Arguments (@("-jar", $ResolvedTargetPath) + $parts)
        }

        $runner = Join-Path $ResolvedTargetPath "run.bat"
        if (-not (Test-Path -LiteralPath $runner)) {
            throw "Packaged target run.bat not found: $runner"
        }
        return Invoke-ProcessCommand -FilePath $runner -Arguments $parts
    } finally {
        if ($null -eq $oldConfig) {
            Remove-Item Env:WMS_CONFIG_FILE -ErrorAction SilentlyContinue
        } else {
            $env:WMS_CONFIG_FILE = $oldConfig
        }
        if ($null -eq $oldLocalAppData) {
            Remove-Item Env:LOCALAPPDATA -ErrorAction SilentlyContinue
        } else {
            $env:LOCALAPPDATA = $oldLocalAppData
        }
    }
}

function Invoke-ProcessCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]]$Arguments,
        [hashtable]$EnvironmentOverrides,
        [int]$TimeoutSeconds = 0
    )

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $FilePath
    $psi.Arguments = (($Arguments | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object {
        if ($_ -match '\s|"') {
            '"' + ($_ -replace '"', '\"') + '"'
        } else {
            $_
        }
    }) -join ' ')
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    if ($EnvironmentOverrides) {
        foreach ($key in $EnvironmentOverrides.Keys) {
            $psi.EnvironmentVariables[$key] = [string]$EnvironmentOverrides[$key]
        }
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    [void]$process.Start()
    if ($TimeoutSeconds -gt 0) {
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try {
                $process.Kill($true)
            } catch {
                $process.Kill()
            }
            return [pscustomobject]@{
                ExitCode = 124
                Output   = ("Process timed out after {0} seconds: {1}" -f $TimeoutSeconds, $FilePath)
            }
        }
    } else {
        $process.WaitForExit()
    }
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()

    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        Output   = ($stdout + [Environment]::NewLine + $stderr).Trim()
    }
}

function Resolve-InstalledAppDir {
    param([string]$InstallRoot)

    if (Test-Path -LiteralPath (Join-Path $InstallRoot "run.bat")) {
        return $InstallRoot
    }

    $candidate = Get-ChildItem -Path $InstallRoot -Directory -Recurse -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "run.bat") } |
        Select-Object -First 1
    if ($candidate) {
        return $candidate.FullName
    }

    throw "Installed app directory not found under: $InstallRoot"
}

function Invoke-TropicanaPackageInstallScenario {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourceRoot,
        [Parameter(Mandatory = $true)]
        [string]$ScenarioOutputDir,
        [Parameter(Mandatory = $true)]
        [string]$ConfigPath,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedConfigSourcePattern
    )

    $shortRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("wms-tropicana-package-smoke-" + [guid]::NewGuid().ToString("N"))
    $smokeAppName = "WMS Pallet Tag System Smoke"
    $smokeInstallDirName = "WMS-Pallet-Tag-System-Smoke"
    $bundleDir = Join-Path $shortRoot "app"
    $packageDir = Join-Path $shortRoot "package"
    $installRoot = Join-Path $shortRoot ("install\{0}" -f $smokeInstallDirName)
    $localAppDataRoot = Join-Path $shortRoot "localappdata"
    $installerLogPath = Join-Path $ScenarioOutputDir "tropicana-package-install.log"
    $uniqueUpgradeUuid = [guid]::NewGuid().ToString()

    New-Item -ItemType Directory -Path $ScenarioOutputDir -Force | Out-Null
    New-Item -ItemType Directory -Path $shortRoot -Force | Out-Null
    New-Item -ItemType Directory -Path $localAppDataRoot -Force | Out-Null

    & (Join-Path $SourceRoot "scripts\build-jpackage-bundle.ps1") `
        -SourceRoot $SourceRoot `
        -InstallerType msi `
        -BundleDir $bundleDir `
        -BundleVersionLabel "smoke-installer" `
        -AppName $smokeAppName `
        -InstallDirName $smokeInstallDirName `
        -WinUpgradeUuid $uniqueUpgradeUuid | Out-Null

    $msiPath = Get-ChildItem -Path $shortRoot -Filter '*.msi' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $msiPath) {
        return [pscustomobject]@{
            ExitCode = 1
            Output   = "Tropicana package scenario could not find the MSI installer artifact."
        }
    }

    & (Join-Path $SourceRoot "scripts\build-tropicana-installer.ps1") `
        -InstallerPath $msiPath.FullName `
        -ConfigSourcePath $ConfigPath `
        -OutputDir $packageDir `
        -ProductDisplayName $smokeAppName `
        -SourceRoot $SourceRoot | Out-Null

    $packageZip = Join-Path $packageDir "WMS Pallet Tag System - Tropicana Package.zip"
    $configScript = Join-Path $packageDir "Install-Tropicana-Config.ps1"
    if (-not (Test-Path -LiteralPath $packageZip)) {
        return [pscustomobject]@{
            ExitCode = 1
            Output   = "Tropicana package scenario could not find the Tropicana package ZIP."
        }
    }
    if (-not (Test-Path -LiteralPath $configScript)) {
        return [pscustomobject]@{
            ExitCode = 1
            Output   = "Tropicana package scenario could not find the config installer script."
        }
    }

    $installHelper = Join-Path $SourceRoot "scripts\install-wms-installer.ps1"
    $installResult = Invoke-ProcessCommand -FilePath "powershell.exe" -Arguments @(
        "-NoProfile",
        "-File", $installHelper,
        "-InstallerPath", $msiPath.FullName,
        "-InstallDir", $installRoot,
        "-LogPath", $installerLogPath,
        "-ProductDisplayName", $smokeAppName,
        "-QuietInstall",
        "-ReplaceExisting"
    ) -TimeoutSeconds 900

    if ($installResult.ExitCode -ne 0) {
        return $installResult
    }

    $configApplyResult = Invoke-ProcessCommand -FilePath "powershell.exe" -Arguments @(
        "-NoProfile",
        "-File", $configScript,
        "-InstallDir", $installRoot,
        "-LocalAppDataRoot", $localAppDataRoot,
        "-SkipVerify"
    ) -TimeoutSeconds 300
    if ($configApplyResult.ExitCode -ne 0) {
        return $configApplyResult
    }

    $targetConfigPath = Join-Path $localAppDataRoot "Tropicana\WMS-Pallet-Tag-System\wms-tags.env"
    if (-not (Test-Path -LiteralPath $targetConfigPath)) {
        return [pscustomobject]@{
            ExitCode = 1
            Output   = "Tropicana package install did not create config at $targetConfigPath"
        }
    }

    $installedAppDir = Resolve-InstalledAppDir -InstallRoot (Split-Path -Parent $installRoot)
    $configResult = Invoke-SmokeCommand -Mode "packaged" -ResolvedTargetPath $installedAppDir -CommandText "config" `
        -ConfigPath $null -LocalAppDataRoot $localAppDataRoot
    $dbTestResult = Invoke-SmokeCommand -Mode "packaged" -ResolvedTargetPath $installedAppDir -CommandText "db-test" `
        -ConfigPath $null -LocalAppDataRoot $localAppDataRoot

    $output = @(
        "package-zip=$packageZip"
        "config-script=$configScript"
        "installer-exit=$($installResult.ExitCode)"
        "config-exit=$($configApplyResult.ExitCode)"
        "install-dir=$installedAppDir"
        "config-path=$targetConfigPath"
        "config-output=$($configResult.Output)"
        "db-test-output=$($dbTestResult.Output)"
    ) -join [Environment]::NewLine

    if ($configResult.ExitCode -ne 0 -or $dbTestResult.ExitCode -ne 0) {
        return [pscustomobject]@{
            ExitCode = 1
            Output   = $output
        }
    }
    if ($configResult.Output -notmatch [regex]::Escape($ExpectedConfigSourcePattern)) {
        return [pscustomobject]@{
            ExitCode = 1
            Output   = ($output + [Environment]::NewLine + "Expected config source pattern missing: $ExpectedConfigSourcePattern")
        }
    }

    return [pscustomobject]@{
        ExitCode = 0
        Output   = $output
    }
}

function Test-ExpectedArtifacts {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]]$ExpectedArtifacts,
        [Parameter(Mandatory = $true)]
        [string]$ArtifactBaseDir
    )

    if ($ExpectedArtifacts.Count -eq 0) {
        return @{
            Passed = $true
            Details = "No artifacts required."
        }
    }

    $missing = New-Object System.Collections.Generic.List[string]
    foreach ($pattern in $ExpectedArtifacts) {
        $candidate = Join-Path $ArtifactBaseDir $pattern
        $match = Get-ChildItem -Path $candidate -ErrorAction SilentlyContinue
        if (-not $match) {
            $missing.Add($pattern) | Out-Null
        }
    }

    if ($missing.Count -gt 0) {
        return @{
            Passed = $false
            Details = "Missing artifacts: " + ($missing -join ", ")
        }
    }

    return @{
        Passed = $true
        Details = "Artifacts present: " + ($ExpectedArtifacts -join ", ")
    }
}

function Invoke-PrinterReachabilityScenario {
    param($Scenario)

    $pingOk = Test-Connection $Scenario.host -Count 1 -Quiet
    $portOk = Test-NetConnection $Scenario.host -Port $Scenario.port -InformationLevel Quiet
    return [pscustomobject]@{
        ExitCode = if ($pingOk -and $portOk) { 0 } else { 1 }
        Output   = "ping=$pingOk tcp=$portOk host=$($Scenario.host) port=$($Scenario.port)"
    }
}

function New-ScenarioResult {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [int]$Tier,
        [Parameter(Mandatory = $true)]
        [string]$Kind,
        [Parameter(Mandatory = $true)]
        [int]$ExitCode,
        [Parameter(Mandatory = $true)]
        [int]$ExpectedExitCode,
        [Parameter(Mandatory = $true)]
        [string]$Output,
        [Parameter(Mandatory = $true)]
        [bool]$ArtifactsPassed,
        [Parameter(Mandatory = $true)]
        [string]$ArtifactDetails
    )

    [pscustomobject]@{
        Name             = $Name
        Tier             = $Tier
        Kind             = $Kind
        ExitCode         = $ExitCode
        ExpectedExitCode = $ExpectedExitCode
        Passed           = ($ExitCode -eq $ExpectedExitCode) -and $ArtifactsPassed
        Output           = $Output
        ArtifactDetails  = $ArtifactDetails
    }
}

function New-ScenarioResultFromChecks {
    param(
        [Parameter(Mandatory = $true)]
        $Scenario,
        [Parameter(Mandatory = $true)]
        [int]$ExitCode,
        [Parameter(Mandatory = $true)]
        [string]$Output,
        [Parameter(Mandatory = $true)]
        [string]$ArtifactBaseDir
    )

    $artifactCheck = Test-ExpectedArtifacts -ExpectedArtifacts @($Scenario.expectedArtifacts) -ArtifactBaseDir $ArtifactBaseDir
    return New-ScenarioResult -Name $Scenario.name -Tier $Scenario.tier -Kind $Scenario.kind `
        -ExitCode $ExitCode -ExpectedExitCode $Scenario.expectedExitCode -Output $Output `
        -ArtifactsPassed $artifactCheck.Passed -ArtifactDetails $artifactCheck.Details
}

function Update-ConfigSourceResult {
    param(
        [Parameter(Mandatory = $true)]
        $Scenario,
        [Parameter(Mandatory = $true)]
        $Result,
        [Parameter(Mandatory = $true)]
        [string]$OutputText
    )

    if ($Scenario.kind -ne "config-source" -or [string]::IsNullOrWhiteSpace($Scenario.expectedConfigSourcePattern)) {
        return $Result
    }

    if ($OutputText -notmatch [regex]::Escape($Scenario.expectedConfigSourcePattern)) {
        $Result.Passed = $false
        $Result.ArtifactDetails = "Expected config source pattern missing: $($Scenario.expectedConfigSourcePattern)"
    }
    return $Result
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Split-Path -Parent $scriptRoot
$manifestPath = Join-Path $sourceRoot "scripts\smoke\smoke-manifest.json"
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

$resolvedConfigPath = $null
if (-not [string]::IsNullOrWhiteSpace($ConfigPath)) {
    $resolvedConfigPath = Get-ResolvedPathOrThrow -PathValue $ConfigPath -Label "ConfigPath"
}

if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $sourceRoot ("out\smoke-" + $Mode + "-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
}
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

$resolvedTargetPath = if ($Mode -eq "repo") {
    Resolve-RepoJarPath -SourceRoot $sourceRoot
} else {
    Resolve-PackagedTargetPath -SourceRoot $sourceRoot -TargetPath $TargetPath
}

$effectiveShipmentId = if ($ShipmentId) { $ShipmentId } else { "4885021" }
$effectiveCarrierMoveId = if ($CarrierMoveId) { $CarrierMoveId } else { "10001866" }
$effectiveTrainId = if ($TrainId) { $TrainId } else { "JC03182026" }

$localAppDataRoot = Join-Path $OutputRoot "localappdata"
$tropicanaConfigDir = Join-Path $localAppDataRoot "Tropicana\WMS-Pallet-Tag-System"
New-Item -ItemType Directory -Path $tropicanaConfigDir -Force | Out-Null
if ($resolvedConfigPath) {
    Copy-Item -LiteralPath $resolvedConfigPath -Destination (Join-Path $tropicanaConfigDir "wms-tags.env") -Force
}

$results = New-Object System.Collections.Generic.List[object]

foreach ($scenario in $manifest.scenarios) {
    if (-not ($scenario.modes -contains $Mode)) {
        continue
    }
    if ($scenario.releaseOnly -and -not $IncludeInstallerScenarios) {
        continue
    }

    $scenarioOutputDir = Join-Path $OutputRoot $scenario.name
    New-Item -ItemType Directory -Path $scenarioOutputDir -Force | Out-Null

    if ($scenario.kind -eq "printer-reachability") {
        $invokeResult = Invoke-PrinterReachabilityScenario -Scenario $scenario
        $results.Add((New-ScenarioResultFromChecks -Scenario $scenario -ExitCode $invokeResult.ExitCode `
            -Output $invokeResult.Output -ArtifactBaseDir $scenarioOutputDir)) | Out-Null
        continue
    }

    if ($scenario.kind -eq "asset-check") {
        $artifactBaseDir = if ($Mode -eq "packaged") { $resolvedTargetPath } else { $sourceRoot }
        $results.Add((New-ScenarioResultFromChecks -Scenario $scenario -ExitCode 0 `
            -Output "asset-check" -ArtifactBaseDir $artifactBaseDir)) | Out-Null
        continue
    }

    if ($scenario.kind -eq "tropicana-package-install") {
        if (-not $resolvedConfigPath) {
            $results.Add((New-ScenarioResult -Name $scenario.name -Tier $scenario.tier -Kind $scenario.kind `
                -ExitCode 1 -ExpectedExitCode $scenario.expectedExitCode -Output "ConfigPath is required for Tropicana package smoke." `
                -ArtifactsPassed $false -ArtifactDetails "Tropicana package smoke requires a real config payload.")) | Out-Null
            continue
        }

        $invokeResult = Invoke-TropicanaPackageInstallScenario -SourceRoot $sourceRoot -ScenarioOutputDir $scenarioOutputDir `
            -ConfigPath $resolvedConfigPath -ExpectedConfigSourcePattern $scenario.expectedConfigSourcePattern
        $results.Add((New-ScenarioResultFromChecks -Scenario $scenario -ExitCode $invokeResult.ExitCode `
            -Output $invokeResult.Output -ArtifactBaseDir $scenarioOutputDir)) | Out-Null
        continue
    }

    $commandText = [string]$scenario.command
    $commandText = $commandText.Replace("{shipmentId}", $effectiveShipmentId)
    $commandText = $commandText.Replace("{carrierMoveId}", $effectiveCarrierMoveId)
    $commandText = $commandText.Replace("{trainId}", $effectiveTrainId)
    $commandText = $commandText.Replace("{outputDir}", $scenarioOutputDir)

    $scenarioConfigPath = $resolvedConfigPath
    $scenarioLocalAppDataRoot = $null
    if ($scenario.kind -eq "config-source") {
        $scenarioConfigPath = $null
        $scenarioLocalAppDataRoot = $localAppDataRoot
    }

    $invokeResult = Invoke-SmokeCommand -Mode $Mode -ResolvedTargetPath $resolvedTargetPath `
        -CommandText $commandText -ConfigPath $scenarioConfigPath -LocalAppDataRoot $scenarioLocalAppDataRoot

    $artifactBaseDir = $scenarioOutputDir
    if ($commandText.Contains("--print-to-file")) {
        if ($Mode -eq "repo") {
            $artifactBaseDir = Join-Path (Split-Path -Parent $resolvedTargetPath) "out"
        } else {
            $artifactBaseDir = Join-Path $resolvedTargetPath "out"
        }
    }

    $result = New-ScenarioResultFromChecks -Scenario $scenario -ExitCode $invokeResult.ExitCode `
        -Output $invokeResult.Output -ArtifactBaseDir $artifactBaseDir
    $result = Update-ConfigSourceResult -Scenario $scenario -Result $result -OutputText $invokeResult.Output
    $results.Add($result) | Out-Null
}

$reportJsonPath = Join-Path $OutputRoot "smoke-report.json"
$reportTextPath = Join-Path $OutputRoot "smoke-report.txt"

$report = [pscustomobject]@{
    Mode        = $Mode
    TargetPath  = $resolvedTargetPath
    ConfigPath  = $resolvedConfigPath
    GeneratedAt = (Get-Date).ToString("s")
    Results     = $results
}

$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportJsonPath -Encoding UTF8

$text = New-Object System.Collections.Generic.List[string]
$text.Add("WMS Release Smoke Report") | Out-Null
$text.Add("Mode       : $Mode") | Out-Null
$text.Add("TargetPath : $resolvedTargetPath") | Out-Null
$text.Add("ConfigPath : $(if ($resolvedConfigPath) { $resolvedConfigPath } else { '(none)' })") | Out-Null
$text.Add("") | Out-Null
foreach ($result in $results) {
    $text.Add(("{0} [{1}] exit={2}/{3}" -f $(if ($result.Passed) { "PASS" } else { "FAIL" }), $result.Name, $result.ExitCode, $result.ExpectedExitCode)) | Out-Null
    $text.Add(("  Artifacts: {0}" -f $result.ArtifactDetails)) | Out-Null
    if ($result.Output) {
        $text.Add(("  Output: {0}" -f ($result.Output -replace "\r?\n", " | "))) | Out-Null
    }
}
$text.Add("") | Out-Null
$text.Add(("Reports: {0}, {1}" -f $reportTextPath, $reportJsonPath)) | Out-Null
$text | Set-Content -LiteralPath $reportTextPath -Encoding UTF8

Get-Content -LiteralPath $reportTextPath

if (($results | Where-Object { -not $_.Passed }).Count -gt 0) {
    exit 1
}

exit 0
