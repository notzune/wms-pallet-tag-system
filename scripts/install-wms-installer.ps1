[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$LogPath,
    [switch]$ReplaceExisting
)

$ErrorActionPreference = "Stop"

function Get-InstalledWmsProduct {
    $entries = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -eq 'WMS Pallet Tag System' }

    if (-not $entries) {
        return $null
    }

    return $entries | Sort-Object DisplayVersion -Descending | Select-Object -First 1
}

function Get-InstallerVersion {
    param([string]$Path)

    $name = [System.IO.Path]::GetFileNameWithoutExtension($Path)
    if ($name -match '(\d+\.\d+\.\d+)$') {
        return $Matches[1]
    }
    return $null
}

function Invoke-Uninstall {
    param(
        [Parameter(Mandatory = $true)]
        $InstalledProduct,
        [Parameter(Mandatory = $true)]
        [string]$LogPath
    )

    $productCode = $null
    if ($InstalledProduct.PSChildName -match '^\{.+\}$') {
        $productCode = $InstalledProduct.PSChildName
    } elseif ($InstalledProduct.UninstallString -match '\{.+\}') {
        $productCode = $Matches[0]
    }

    if (-not $productCode) {
        throw "Could not resolve installed product code from uninstall entry."
    }

    Write-Host "Uninstalling existing WMS Pallet Tag System ($($InstalledProduct.DisplayVersion))..."
    $arguments = @('/x', $productCode, '/passive', '/norestart', '/log', $LogPath)
    $process = Start-Process -FilePath 'msiexec.exe' -ArgumentList $arguments -Wait -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Uninstall failed with exit code $($process.ExitCode). See log: $LogPath"
    }
}

if (-not $InstallerPath) {
    $scriptRoot = Split-Path -Parent $PSCommandPath
    $candidate = Get-ChildItem -Path $scriptRoot -Filter 'WMS Pallet Tag System-*.exe' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($candidate) {
        $InstallerPath = $candidate.FullName
    }
}

if (-not $InstallerPath -or -not (Test-Path -LiteralPath $InstallerPath)) {
    throw "Installer not found. Pass -InstallerPath or place the script next to the built installer."
}

$resolvedInstallerPath = (Resolve-Path -LiteralPath $InstallerPath).Path
$installerVersion = Get-InstallerVersion -Path $resolvedInstallerPath
if (-not $LogPath) {
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $LogPath = Join-Path (Split-Path -Parent $resolvedInstallerPath) "install-wms-tags-$timestamp.log"
}

$installed = Get-InstalledWmsProduct
if ($installed) {
    $installedVersion = [string]$installed.DisplayVersion
    Write-Host "Detected installed version: $installedVersion"
    if ($ReplaceExisting -or ($installerVersion -and $installedVersion -eq $installerVersion)) {
        $uninstallLog = [System.IO.Path]::ChangeExtension($LogPath, '.uninstall.log')
        Invoke-Uninstall -InstalledProduct $installed -LogPath $uninstallLog
    }
}

Write-Host "Launching installer: $resolvedInstallerPath"
Write-Host "Installer log: $LogPath"
$process = Start-Process -FilePath $resolvedInstallerPath -ArgumentList @('/log', $LogPath) -Wait -PassThru
if ($process.ExitCode -ne 0) {
    throw "Installer failed with exit code $($process.ExitCode). See log: $LogPath"
}

Write-Host "Installation complete."
