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

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Expected,
        [Parameter(Mandatory = $true)]
        [string]$Actual,
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $normalizedExpected = ($Expected -replace "\r\n", "`n").Trim()
    $normalizedActual = ($Actual -replace "\r\n", "`n").Trim()
    if ($normalizedExpected -ne $normalizedActual) {
        throw "$Message`nExpected: $normalizedExpected`nActual:   $normalizedActual"
    }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$installerScript = Join-Path $sourceRoot "scripts\Install-Tropicana-Config.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("wms-install-tropicana-" + [guid]::NewGuid().ToString("N"))
$localAppDataRoot = Join-Path $tempRoot "LocalAppData"
$installDir = Join-Path $tempRoot "InstallDir"
$configSourcePath = Join-Path $tempRoot "tropicana.env"
$targetDir = Join-Path $localAppDataRoot "Tropicana\WMS-Pallet-Tag-System"
$targetPath = Join-Path $targetDir "wms-tags.env"

try {
    New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $installDir "run.bat") -Value "@echo off" -Encoding ASCII

    $configContent = @"
ACTIVE_SITE=TBG3002
ORACLE_USERNAME=trop_user
ORACLE_PASSWORD=trop_pass
"@
    Set-Content -LiteralPath $configSourcePath -Value $configContent -Encoding ASCII

    & $installerScript -ConfigSourcePath $configSourcePath -LocalAppDataRoot $localAppDataRoot -InstallDir $installDir -SkipVerify

    Assert-True -Condition (Test-Path -LiteralPath $targetPath) -Message "Installer should create per-user Tropicana config"
    Assert-Equal -Expected $configContent -Actual (Get-Content -LiteralPath $targetPath -Raw) -Message "Installer should write provided config content"

    Set-Content -LiteralPath $targetPath -Value "ACTIVE_SITE=OLD" -Encoding ASCII
    & $installerScript -ConfigSourcePath $configSourcePath -LocalAppDataRoot $localAppDataRoot -InstallDir $installDir -SkipVerify

    $backups = Get-ChildItem -LiteralPath $targetDir -Filter "wms-tags.env.bak-*" -File -ErrorAction SilentlyContinue
    Assert-True -Condition ($backups.Count -ge 1) -Message "Installer should create a backup before overwriting config"
    Assert-Equal -Expected $configContent -Actual (Get-Content -LiteralPath $targetPath -Raw) -Message "Installer should rewrite the latest config after backup"

    Write-Host "PASS: Install-Tropicana-Config writes per-user config and backs up prior state"
} finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
