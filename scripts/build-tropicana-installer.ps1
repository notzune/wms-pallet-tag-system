[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$ConfigSourcePath,
    [string]$OutputDir,
    [string]$SourceRoot,
    [string]$ProductDisplayName
)

$ErrorActionPreference = "Stop"

function Resolve-ConfigSourcePath {
    param(
        [string]$ConfigSourcePath,
        [string]$SourceRoot
    )

    if (-not [string]::IsNullOrWhiteSpace($ConfigSourcePath)) {
        return $ConfigSourcePath
    }

    $defaultPath = Join-Path $SourceRoot ".env"
    if (Test-Path -LiteralPath $defaultPath) {
        return $defaultPath
    }

    throw "Config source not found. Pass -ConfigSourcePath with the Tropicana env payload."
}

function Resolve-AppInstallerPath {
    param(
        [string]$InstallerPath,
        [string]$SourceRoot,
        [string]$OutputDir
    )

    if (-not [string]::IsNullOrWhiteSpace($InstallerPath)) {
        return $InstallerPath
    }

    & (Join-Path $SourceRoot "scripts\build-jpackage-bundle.ps1") -SourceRoot $SourceRoot -InstallerType exe

    $searchRoots = @($OutputDir, (Join-Path $SourceRoot "dist")) | Select-Object -Unique
    $installerCandidates = foreach ($root in $searchRoots) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -Path $root -Filter 'WMS Pallet Tag System-*.exe' -File -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -notlike '*Tropicana*' }
        }
    }
    $candidate = $installerCandidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $candidate) {
        throw "Could not locate the packaged installer after building it."
    }
    return $candidate.FullName
}

function New-EmbeddedSupportScript {
    param(
        [string]$TemplatePath,
        [string]$DestinationPath,
        [string]$ConfigContent
    )

    $base64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($ConfigContent))
    $template = Get-Content -LiteralPath $TemplatePath -Raw
    $rendered = $template.Replace('__EMBEDDED_CONFIG_BASE64_VALUE__', $base64)
    Set-Content -LiteralPath $DestinationPath -Value $rendered -Encoding ASCII
}

function New-PackageReadme {
    param(
        [string]$DestinationPath,
        [string]$InstallerFileName
    )

    $content = @"
Tropicana Package Instructions
==============================

This package intentionally avoids a self-extracting wrapper and bootstrap launcher.

Install steps:
1. Run $InstallerFileName and finish the normal WMS installation.
2. Run Install-Tropicana-Config.ps1 to apply the Tropicana environment for the current user.
3. If needed, validate the install with scripts\verify-wms-tags.ps1 from the installed app directory.

Notes:
- Distribute this package only through trusted internal channels.
- Sign $InstallerFileName with a trusted certificate before redistribution when possible.
- Install-Tropicana-Config.ps1 is the supported repair and credential-rotation path.
"@
    Set-Content -LiteralPath $DestinationPath -Value $content -Encoding ASCII
}

$scriptRoot = Split-Path -Parent $PSCommandPath
if (-not $SourceRoot) {
    $SourceRoot = Split-Path -Parent $scriptRoot
}
if (-not $OutputDir) {
    $OutputDir = Join-Path $SourceRoot "dist"
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$resolvedConfigSource = Resolve-ConfigSourcePath -ConfigSourcePath $ConfigSourcePath -SourceRoot $SourceRoot
$resolvedInstallerPath = Resolve-AppInstallerPath -InstallerPath $InstallerPath -SourceRoot $SourceRoot -OutputDir $OutputDir

if (-not (Test-Path -LiteralPath $resolvedInstallerPath)) {
    throw "Installer not found: $resolvedInstallerPath"
}

$configContent = Get-Content -LiteralPath $resolvedConfigSource -Raw
$stageDir = Join-Path $OutputDir "tropicana-bootstrap-stage"
Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null

$installerFileName = Split-Path -Leaf $resolvedInstallerPath
$resolvedProductDisplayName = if ([string]::IsNullOrWhiteSpace($ProductDisplayName)) {
    ([System.IO.Path]::GetFileNameWithoutExtension($installerFileName) -replace '-\d+\.\d+\.\d+$', '')
} else {
    $ProductDisplayName.Trim()
}
Copy-Item -LiteralPath $resolvedInstallerPath -Destination (Join-Path $stageDir $installerFileName) -Force
$outputInstallerPath = Join-Path $OutputDir $installerFileName
if (-not [System.StringComparer]::OrdinalIgnoreCase.Equals($resolvedInstallerPath, $outputInstallerPath)) {
    Copy-Item -LiteralPath $resolvedInstallerPath -Destination $outputInstallerPath -Force
}

$supportScriptPath = Join-Path $OutputDir "Install-Tropicana-Config.ps1"
$packageReadmePath = Join-Path $OutputDir "Tropicana-Package-Readme.txt"
New-EmbeddedSupportScript -TemplatePath (Join-Path $SourceRoot "scripts\Install-Tropicana-Config.ps1") `
    -DestinationPath $supportScriptPath `
    -ConfigContent $configContent
Copy-Item -LiteralPath $supportScriptPath -Destination (Join-Path $stageDir "Install-Tropicana-Config.ps1") -Force
New-PackageReadme -DestinationPath $packageReadmePath -InstallerFileName $installerFileName
Copy-Item -LiteralPath $packageReadmePath -Destination (Join-Path $stageDir "Tropicana-Package-Readme.txt") -Force

$targetName = Join-Path $OutputDir "WMS Pallet Tag System - Tropicana Package.zip"
Remove-Item -LiteralPath $targetName -Force -ErrorAction SilentlyContinue
Compress-Archive -Path (Join-Path $stageDir '*') -DestinationPath $targetName -Force
if (-not (Test-Path -LiteralPath $targetName)) {
    throw "Compress-Archive did not create the Tropicana package ZIP."
}

Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Tropicana package ready: $targetName"
Write-Host "Fallback config installer: $supportScriptPath"
