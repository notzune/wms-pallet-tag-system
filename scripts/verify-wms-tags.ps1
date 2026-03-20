[CmdletBinding()]
param(
    [string]$InstallDir = "C:\WMS-Pallet-Tag",
    [string]$ExpectedJarSha256,
    [string]$ShipmentId,
    [switch]$SkipDbTest,
    [switch]$SkipDryRun,
    [switch]$UseReleaseSmoke,
    [string]$ConfigPath
)

$ErrorActionPreference = "Stop"

if ($UseReleaseSmoke) {
    $scriptRoot = Split-Path -Parent $PSCommandPath
    $smokeScript = Join-Path $scriptRoot "run-smoke-tests.ps1"
    if (-not (Test-Path -LiteralPath $smokeScript)) {
        throw "Release smoke runner not found: $smokeScript"
    }

    $smokeArgs = @{
        Mode       = "packaged"
        TargetPath = $InstallDir
    }
    if ($ConfigPath) {
        $smokeArgs["ConfigPath"] = $ConfigPath
    }
    if ($ShipmentId) {
        $smokeArgs["ShipmentId"] = $ShipmentId
    }

    & $smokeScript @smokeArgs
    exit $LASTEXITCODE
}

function Add-Result {
    param(
        [string]$Check,
        [bool]$Passed,
        [string]$Details
    )
    $script:results.Add([pscustomobject]@{
        Check   = $Check
        Passed  = $Passed
        Details = $Details
    }) | Out-Null
}

function Get-JavaInfo {
    param([string]$JavaCommand = "java")

    $resolved = $null
    if (Test-Path -LiteralPath $JavaCommand) {
        $resolved = $JavaCommand
    } else {
        $cmd = Get-Command $JavaCommand -ErrorAction SilentlyContinue
        if ($cmd) {
            $resolved = $cmd.Source
        }
    }

    if (-not $resolved) {
        return [pscustomobject]@{
            Exists  = $false
            Major   = $null
            Version = $null
        }
    }

    $versionOutput = (& $resolved --version 2>&1) | Out-String
    if (-not $versionOutput.Trim()) {
        $versionOutput = (& $resolved -version 2>&1) | Out-String
    }
    $match = [regex]::Match($versionOutput, '"(?<version>\d+(?:\.\d+){0,2}).*"')
    if (-not $match.Success) {
        $match = [regex]::Match($versionOutput, '(?im)(?:openjdk|java)\s+(?<version>\d+(?:\.\d+){0,2})')
    }
    if (-not $match.Success) {
        return [pscustomobject]@{
            Exists  = $true
            Major   = $null
            Version = $versionOutput.Trim()
        }
    }

    $versionText = $match.Groups["version"].Value
    $major = [int]($versionText.Split(".")[0])
    return [pscustomobject]@{
        Exists  = $true
        Major   = $major
        Version = $versionText
    }
}

function Get-BundledRuntimeInfo {
    param([string]$InstallDir)

    $releaseFile = Join-Path $InstallDir "runtime\release"
    if (-not (Test-Path -LiteralPath $releaseFile)) {
        return $null
    }

    $versionLine = Get-Content -LiteralPath $releaseFile | Where-Object { $_ -like 'JAVA_VERSION=*' } | Select-Object -First 1
    if (-not $versionLine) {
        return [pscustomobject]@{
            Exists  = $true
            Major   = $null
            Version = "Bundled runtime"
        }
    }

    $versionText = ($versionLine -replace '^JAVA_VERSION="?([^"]+)"?$', '$1').Trim()
    $major = $null
    if ($versionText) {
        $major = [int]($versionText.Split(".")[0])
    }
    return [pscustomobject]@{
        Exists  = $true
        Major   = $major
        Version = $versionText
    }
}

function Resolve-JavaCommand {
    param([string]$InstallDir)
    $bundled = Join-Path $InstallDir "runtime\bin\java.exe"
    if (Test-Path -LiteralPath $bundled) {
        return (Resolve-Path -LiteralPath $bundled).Path
    }
    return "java"
}

function Resolve-AppCommand {
    param([string]$InstallDir)

    $appExe = Join-Path $InstallDir "WMS Pallet Tag System.exe"
    if (Test-Path -LiteralPath $appExe) {
        return (Resolve-Path -LiteralPath $appExe).Path
    }

    return $null
}

$results = New-Object System.Collections.Generic.List[object]
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportPath = Join-Path $env:TEMP "wms-tags-verify-$timestamp.txt"

$resolvedInstallDir = $InstallDir
if (Test-Path -LiteralPath $InstallDir) {
    $resolvedInstallDir = (Resolve-Path -LiteralPath $InstallDir).Path
}

$jarPath = Join-Path $resolvedInstallDir "wms-tags.jar"
if (-not (Test-Path -LiteralPath $jarPath)) {
    $jarPath = Join-Path $resolvedInstallDir "app\wms-tags.jar"
}
$envPath = Join-Path $resolvedInstallDir "wms-tags.env"
$printersPath = Join-Path $resolvedInstallDir "config\TBG3002\printers.yaml"
$routingPath = Join-Path $resolvedInstallDir "config\TBG3002\printer-routing.yaml"
$skuMatrixPath = Join-Path $resolvedInstallDir "config\walmart-sku-matrix.csv"
$locationMatrixPath = Join-Path $resolvedInstallDir "config\walm_loc_num_matrix.csv"
$manifestPath = Join-Path $resolvedInstallDir "install-manifest.json"
$bundleManifestPath = Join-Path $resolvedInstallDir "bundle-manifest.json"
$appImageManifestPath = Join-Path $resolvedInstallDir "appimage-manifest.json"
$javaCmd = Resolve-JavaCommand -InstallDir $resolvedInstallDir
$appCmd = Resolve-AppCommand -InstallDir $resolvedInstallDir

Add-Result -Check "Install directory exists" -Passed (Test-Path -LiteralPath $resolvedInstallDir) -Details $resolvedInstallDir
Add-Result -Check "Jar file exists" -Passed (Test-Path -LiteralPath $jarPath) -Details $jarPath
Add-Result -Check "Launcher exists" -Passed ($null -ne $appCmd) -Details ($(if ($appCmd) { $appCmd } else { "Launcher not bundled" }))
Add-Result -Check "Env file exists" -Passed (Test-Path -LiteralPath $envPath) -Details $envPath
Add-Result -Check "Printers file exists" -Passed (Test-Path -LiteralPath $printersPath) -Details $printersPath
Add-Result -Check "Routing file exists" -Passed (Test-Path -LiteralPath $routingPath) -Details $routingPath
Add-Result -Check "SKU matrix exists" -Passed (Test-Path -LiteralPath $skuMatrixPath) -Details $skuMatrixPath
Add-Result -Check "Location matrix exists" -Passed (Test-Path -LiteralPath $locationMatrixPath) -Details $locationMatrixPath

$javaInfo = Get-BundledRuntimeInfo -InstallDir $resolvedInstallDir
if (-not $javaInfo) {
    $javaInfo = Get-JavaInfo -JavaCommand $javaCmd
}
if (-not $javaInfo.Exists) {
    Add-Result -Check "Java installed" -Passed $false -Details "java command not found"
    Add-Result -Check "Java version >= 17" -Passed $false -Details "Not installed"
} else {
    Add-Result -Check "Java installed" -Passed $true -Details $javaInfo.Version
    Add-Result -Check "Java version >= 17" -Passed (($javaInfo.Major -as [int]) -ge 17) -Details $javaInfo.Version
}

if (Test-Path -LiteralPath $jarPath) {
    $actualHash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
    $expectedHash = $ExpectedJarSha256

    if (-not $expectedHash -and (Test-Path -LiteralPath $manifestPath)) {
        try {
            $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
            if ($manifest.JarSha256) {
                $expectedHash = [string]$manifest.JarSha256
            }
        } catch {
            Add-Result -Check "Install manifest readable" -Passed $false -Details $_.Exception.Message
        }
    }
    if (-not $expectedHash -and (Test-Path -LiteralPath $bundleManifestPath)) {
        try {
            $manifest = Get-Content -LiteralPath $bundleManifestPath -Raw | ConvertFrom-Json
            if ($manifest.JarSha256) {
                $expectedHash = [string]$manifest.JarSha256
            }
        } catch {
            Add-Result -Check "Bundle manifest readable" -Passed $false -Details $_.Exception.Message
        }
    }
    if (-not $expectedHash -and (Test-Path -LiteralPath $appImageManifestPath)) {
        try {
            $manifest = Get-Content -LiteralPath $appImageManifestPath -Raw | ConvertFrom-Json
            if ($manifest.JarSha256) {
                $expectedHash = [string]$manifest.JarSha256
            }
        } catch {
            Add-Result -Check "App image manifest readable" -Passed $false -Details $_.Exception.Message
        }
    }

    if ($expectedHash) {
        $hashPassed = ($actualHash.ToUpperInvariant() -eq $expectedHash.ToUpperInvariant())
        Add-Result -Check "Jar SHA256 match" -Passed $hashPassed -Details "Expected=$expectedHash Actual=$actualHash"
    } else {
        Add-Result -Check "Jar SHA256 available" -Passed $false -Details "Pass -ExpectedJarSha256 or install using setup script to create manifest"
    }
}

if (($appCmd -or (Test-Path -LiteralPath $jarPath)) -and $javaInfo.Exists) {
    Push-Location $resolvedInstallDir
    try {
        if ($appCmd) {
            $configOutput = & $appCmd config 2>&1 | Out-String
        } else {
            $configOutput = & $javaCmd -jar $jarPath config 2>&1 | Out-String
        }
        $configOk = ($LASTEXITCODE -eq 0)
        Add-Result -Check "Config command" -Passed $configOk -Details ($configOutput.Trim() -replace "\r?\n", " | ")

        if (-not $SkipDbTest) {
            if ($appCmd) {
                $dbOutput = & $appCmd db-test 2>&1 | Out-String
            } else {
                $dbOutput = & $javaCmd -jar $jarPath db-test 2>&1 | Out-String
            }
            $dbOk = ($LASTEXITCODE -eq 0)
            Add-Result -Check "DB connectivity (db-test)" -Passed $dbOk -Details ($dbOutput.Trim() -replace "\r?\n", " | ")
        } else {
            Add-Result -Check "DB connectivity (db-test)" -Passed $true -Details "Skipped by flag"
        }

        if (-not $SkipDryRun) {
            if ([string]::IsNullOrWhiteSpace($ShipmentId)) {
                Add-Result -Check "Dry run output" -Passed $false -Details "ShipmentId required unless -SkipDryRun"
            } else {
                $dryRunDir = Join-Path $env:TEMP "wms-tags-dryrun-$timestamp"
                New-Item -ItemType Directory -Path $dryRunDir -Force | Out-Null
                if ($appCmd) {
                    $runOutput = & $appCmd run --shipment-id $ShipmentId --dry-run --output-dir $dryRunDir 2>&1 | Out-String
                } else {
                    $runOutput = & $javaCmd -jar $jarPath run --shipment-id $ShipmentId --dry-run --output-dir $dryRunDir 2>&1 | Out-String
                }
                $runOk = ($LASTEXITCODE -eq 0)
                $zplCount = (Get-ChildItem -Path $dryRunDir -Recurse -Filter "*.zpl" -File -ErrorAction SilentlyContinue | Measure-Object).Count
                $details = "Exit=$LASTEXITCODE, ZPL=$zplCount, Dir=$dryRunDir, Output=" + ($runOutput.Trim() -replace "\r?\n", " | ")
                Add-Result -Check "Dry run output" -Passed ($runOk -and $zplCount -gt 0) -Details $details
            }
        } else {
            Add-Result -Check "Dry run output" -Passed $true -Details "Skipped by flag"
        }
    } finally {
        Pop-Location
    }
}

$failed = $results | Where-Object { -not $_.Passed }
$summary = @()
$summary += "WMS Pallet Tag Verification"
$summary += "Timestamp : $(Get-Date -Format s)"
$summary += "Computer  : $env:COMPUTERNAME"
$summary += "User      : $env:USERDOMAIN\$env:USERNAME"
$summary += ""
$summary += ($results | Format-Table -AutoSize | Out-String).TrimEnd()
$summary += ""
$summary += "Failed checks: $($failed.Count)"
$summary += "Report file : $reportPath"

$summaryText = $summary -join [Environment]::NewLine
$summaryText | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host $summaryText

if ($failed.Count -gt 0) {
    exit 1
}

exit 0
