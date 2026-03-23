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
$inPlaceOutputDir = Join-Path $tempRoot "dist-in-place"
$packageZipPath = Join-Path $outputDir "WMS Pallet Tag System - Tropicana Package.zip"
$packageReadmePath = Join-Path $outputDir "Tropicana-Package-Readme.txt"
$inPlacePackageZipPath = Join-Path $inPlaceOutputDir "WMS Pallet Tag System - Tropicana Package.zip"
$supportScriptPath = Join-Path $outputDir "Install-Tropicana-Config.ps1"
$installDir = Join-Path $tempRoot "InstallDir"
$localAppDataRoot = Join-Path $tempRoot "LocalAppData"
$targetPath = Join-Path $localAppDataRoot "Tropicana\WMS-Pallet-Tag-System\wms-tags.env"
$inPlaceInstallerPath = Join-Path $inPlaceOutputDir "WMS Pallet Tag System-9.9.9.exe"

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
    New-Item -ItemType Directory -Path $inPlaceOutputDir -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $installDir "run.bat") -Value "@echo off" -Encoding ASCII
    Set-Content -LiteralPath $inPlaceInstallerPath -Value "fake-installer" -Encoding ASCII

    & $builderScript -InstallerPath $fakeInstallerPath -ConfigSourcePath $configSourcePath -OutputDir $outputDir -ProductDisplayName 'WMS Pallet Tag System'

    Assert-True -Condition (Test-Path -LiteralPath $packageZipPath) -Message "Builder should create the Tropicana package ZIP"
    Assert-True -Condition (Test-Path -LiteralPath $packageReadmePath) -Message "Builder should emit the Tropicana package instructions"
    Assert-True -Condition (Test-Path -LiteralPath $supportScriptPath) -Message "Builder should emit the fallback Tropicana config installer script"
    Assert-True -Condition (-not (Test-Path -LiteralPath (Join-Path $outputDir "bootstrap-install.ps1"))) -Message "Builder should not emit bootstrap installer scripts"
    Assert-True -Condition (-not (Test-Path -LiteralPath (Join-Path $outputDir "bootstrap.cmd"))) -Message "Builder should not emit bootstrap command launchers"
    Assert-True -Condition (-not (Test-Path -LiteralPath (Join-Path $outputDir "WMS Pallet Tag System - Tropicana Setup.exe"))) -Message "Builder should not emit a self-extracting Tropicana setup EXE"

    $supportScriptContent = Get-Content -LiteralPath $supportScriptPath -Raw
    Assert-True -Condition ($supportScriptContent.Contains("ConfigContentBase64")) -Message "Support script should embed config content for one-file redistribution"
    Assert-True -Condition (-not $supportScriptContent.Contains("ExecutionPolicy Bypass")) -Message "Support script should not instruct operators to bypass PowerShell execution policy"

    $packageEntries = [System.IO.Compression.ZipFile]::OpenRead($packageZipPath).Entries | ForEach-Object { $_.FullName }
    Assert-True -Condition ($packageEntries -contains "WMS Pallet Tag System-9.9.9.exe") -Message "Package ZIP should include the packaged installer"
    Assert-True -Condition ($packageEntries -contains "Install-Tropicana-Config.ps1") -Message "Package ZIP should include the Tropicana config installer script"
    Assert-True -Condition ($packageEntries -contains "Tropicana-Package-Readme.txt") -Message "Package ZIP should include installation instructions"

    & $supportScriptPath -LocalAppDataRoot $localAppDataRoot -InstallDir $installDir -SkipVerify

    Assert-True -Condition (Test-Path -LiteralPath $targetPath) -Message "Generated support script should install embedded config without external .env"
    Assert-True -Condition ((Get-Content -LiteralPath $targetPath -Raw).Trim() -eq $configContent.Trim()) -Message "Generated support script should write the embedded config content"

    & $builderScript -InstallerPath $inPlaceInstallerPath -ConfigSourcePath $configSourcePath -OutputDir $inPlaceOutputDir -ProductDisplayName 'WMS Pallet Tag System'

    Assert-True -Condition (Test-Path -LiteralPath $inPlacePackageZipPath) -Message "Builder should support installer paths that already live inside the output directory"

    Write-Host "PASS: build-tropicana-installer creates inert Tropicana package artifacts"
} finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
