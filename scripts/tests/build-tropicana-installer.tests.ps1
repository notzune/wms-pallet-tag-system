[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$builderScript = Join-Path $sourceRoot "scripts\build-tropicana-installer.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("wms-build-tropicana-" + [guid]::NewGuid().ToString("N"))
$fakeInstallerPath = Join-Path $tempRoot "WMS Pallet Tag System-9.9.9.exe"
$configSourcePath = Join-Path $tempRoot "tropicana.env"
$outputDir = Join-Path $tempRoot "dist"
$bootstrapperPath = Join-Path $outputDir "WMS Pallet Tag System - Tropicana Setup.exe"
$supportScriptPath = Join-Path $outputDir "Install-Tropicana-Config.ps1"
$bootstrapScriptPath = Join-Path $outputDir "bootstrap-install.ps1"
$installDir = Join-Path $tempRoot "InstallDir"
$localAppDataRoot = Join-Path $tempRoot "LocalAppData"
$targetPath = Join-Path $localAppDataRoot "Tropicana\WMS-Pallet-Tag-System\wms-tags.env"

$configContent = @"
ACTIVE_SITE=TBG3002
ORACLE_USERNAME=trop_user
ORACLE_PASSWORD=trop_pass
"@

try {
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    Set-Content -LiteralPath $fakeInstallerPath -Value "fake-installer" -Encoding ASCII
    Set-Content -LiteralPath $configSourcePath -Value $configContent -Encoding ASCII
    New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $installDir "run.bat") -Value "@echo off" -Encoding ASCII

    & $builderScript -InstallerPath $fakeInstallerPath -ConfigSourcePath $configSourcePath -OutputDir $outputDir -ProductDisplayName 'WMS Pallet Tag System'

    Assert-True -Condition (Test-Path -LiteralPath $bootstrapperPath) -Message "Builder should create the Tropicana setup EXE"
    Assert-True -Condition (Test-Path -LiteralPath $supportScriptPath) -Message "Builder should emit the fallback Tropicana config installer script"
    Assert-True -Condition (Test-Path -LiteralPath $bootstrapScriptPath) -Message "Builder should emit the bootstrap installer script"

    $supportScriptContent = Get-Content -LiteralPath $supportScriptPath -Raw
    $bootstrapScriptContent = Get-Content -LiteralPath $bootstrapScriptPath -Raw
    Assert-True -Condition ($supportScriptContent.Contains("ConfigContentBase64")) -Message "Support script should embed config content for one-file redistribution"
    Assert-True -Condition ($bootstrapScriptContent.Contains("-ProductDisplayName', 'WMS Pallet Tag System'")) -Message "Bootstrap script should pass the app display name to the installer helper"

    & $supportScriptPath -LocalAppDataRoot $localAppDataRoot -InstallDir $installDir -SkipVerify

    Assert-True -Condition (Test-Path -LiteralPath $targetPath) -Message "Generated support script should install embedded config without external .env"
    Assert-True -Condition ((Get-Content -LiteralPath $targetPath -Raw).Trim() -eq $configContent.Trim()) -Message "Generated support script should write the embedded config content"

    Write-Host "PASS: build-tropicana-installer creates bootstrap and fallback support artifacts"
} finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
