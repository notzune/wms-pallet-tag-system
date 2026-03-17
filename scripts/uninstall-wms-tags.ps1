[CmdletBinding()]
param(
    [string]$LogPath
)

$ErrorActionPreference = "Stop"

$installed = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -eq 'WMS Pallet Tag System' } |
    Sort-Object DisplayVersion -Descending |
    Select-Object -First 1

if (-not $installed) {
    Write-Host "WMS Pallet Tag System is not currently installed."
    exit 0
}

$productCode = $null
if ($installed.PSChildName -match '^\{.+\}$') {
    $productCode = $installed.PSChildName
} elseif ($installed.UninstallString -match '\{.+\}') {
    $productCode = $Matches[0]
}

if (-not $productCode) {
    throw "Could not resolve installed product code from uninstall entry."
}

if (-not $LogPath) {
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $LogPath = Join-Path $env:TEMP "uninstall-wms-tags-$timestamp.log"
}

Write-Host "Uninstalling WMS Pallet Tag System $($installed.DisplayVersion)..."
Write-Host "Uninstall log: $LogPath"
$process = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/x', $productCode, '/passive', '/norestart', '/log', $LogPath) -Wait -PassThru
if ($process.ExitCode -ne 0) {
    throw "Uninstall failed with exit code $($process.ExitCode). See log: $LogPath"
}

Write-Host "Uninstall complete."
