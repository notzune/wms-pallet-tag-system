[CmdletBinding()]
param(
    [string]$ConfigSourcePath,
    [string]$ConfigContentBase64,
    [string]$LocalAppDataRoot = $env:LOCALAPPDATA,
    [string]$InstallDir,
    [switch]$SkipVerify
)

$ErrorActionPreference = "Stop"
$embeddedConfigBase64 = '__EMBEDDED_CONFIG_BASE64__'
if ([string]::IsNullOrWhiteSpace($ConfigContentBase64) -and $embeddedConfigBase64 -ne '__EMBEDDED_CONFIG_BASE64__') {
    $ConfigContentBase64 = $embeddedConfigBase64
}

function Resolve-TargetConfigPath {
    param([string]$LocalAppDataRoot)

    if ([string]::IsNullOrWhiteSpace($LocalAppDataRoot)) {
        throw "LOCALAPPDATA is not available. Pass -LocalAppDataRoot explicitly."
    }

    return (Join-Path $LocalAppDataRoot "Tropicana\WMS-Pallet-Tag-System\wms-tags.env")
}

function Assert-InstallDirLooksValid {
    param([string]$InstallDir)

    if ([string]::IsNullOrWhiteSpace($InstallDir)) {
        return
    }

    if (-not (Test-Path -LiteralPath $InstallDir)) {
        throw "InstallDir not found: $InstallDir"
    }

    $markers = @(
        (Join-Path $InstallDir "run.bat"),
        (Join-Path $InstallDir "wms-tags-gui.bat"),
        (Join-Path $InstallDir "WMS Pallet Tag System.exe"),
        (Join-Path $InstallDir "wms-tags.jar")
    )

    if (-not ($markers | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1)) {
        throw "InstallDir does not look like a WMS install: $InstallDir"
    }
}

function Resolve-ConfigSourcePath {
    param(
        [string]$ConfigSourcePath,
        [string]$ScriptRoot
    )

    if (-not [string]::IsNullOrWhiteSpace($ConfigSourcePath)) {
        return $ConfigSourcePath
    }

    $sourceRoot = Split-Path -Parent $ScriptRoot
    $defaultPath = Join-Path $sourceRoot ".env"
    if (Test-Path -LiteralPath $defaultPath) {
        return $defaultPath
    }

    throw "Config source not found. Pass -ConfigSourcePath with the Tropicana env payload."
}

function Resolve-ConfigContent {
    param(
        [string]$ConfigSourcePath,
        [string]$ConfigContentBase64,
        [string]$ScriptRoot
    )

    if (-not [string]::IsNullOrWhiteSpace($ConfigContentBase64)) {
        $bytes = [Convert]::FromBase64String($ConfigContentBase64)
        return [System.Text.Encoding]::UTF8.GetString($bytes)
    }

    $resolvedConfigSource = Resolve-ConfigSourcePath -ConfigSourcePath $ConfigSourcePath -ScriptRoot $ScriptRoot
    if (-not (Test-Path -LiteralPath $resolvedConfigSource)) {
        throw "Config source not found: $resolvedConfigSource"
    }
    return Get-Content -LiteralPath $resolvedConfigSource -Raw
}

$scriptRoot = Split-Path -Parent $PSCommandPath
Assert-InstallDirLooksValid -InstallDir $InstallDir

$targetPath = Resolve-TargetConfigPath -LocalAppDataRoot $LocalAppDataRoot
$targetDir = Split-Path -Parent $targetPath
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

if (Test-Path -LiteralPath $targetPath) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backupPath = Join-Path $targetDir "wms-tags.env.bak-$timestamp"
    Copy-Item -LiteralPath $targetPath -Destination $backupPath -Force
    Write-Host "Backed up existing Tropicana config to: $backupPath"
}

$configContent = Resolve-ConfigContent -ConfigSourcePath $ConfigSourcePath -ConfigContentBase64 $ConfigContentBase64 -ScriptRoot $scriptRoot
Set-Content -LiteralPath $targetPath -Value $configContent -Encoding ASCII

Write-Host "Installed Tropicana config to: $targetPath"

if (-not $SkipVerify -and -not [string]::IsNullOrWhiteSpace($InstallDir)) {
    $verifyScript = Join-Path $InstallDir "scripts\verify-wms-tags.ps1"
    if (Test-Path -LiteralPath $verifyScript) {
        Write-Host "Run this next to validate the install:"
        Write-Host "powershell -NoProfile -ExecutionPolicy Bypass -File `"$verifyScript`" -InstallDir `"$InstallDir`" -SkipDryRun"
    }
}
