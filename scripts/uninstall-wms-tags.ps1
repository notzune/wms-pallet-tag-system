[CmdletBinding()]
param(
    [string]$LogPath,
    [switch]$WipeInstallRoot,
    [switch]$RemoveRuntimeSettings
)

$ErrorActionPreference = "Stop"

function Start-PostUninstallCleanup {
    param(
        [string]$InstallLocation,
        [switch]$RemoveRuntimeSettings
    )

    if (-not $InstallLocation -and -not $RemoveRuntimeSettings) {
        return
    }

    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $cleanupScript = Join-Path $env:TEMP "wms-post-uninstall-cleanup-$timestamp.ps1"
    $cleanupContent = @(
        '$ErrorActionPreference = "SilentlyContinue"'
        'Start-Sleep -Seconds 3'
    )

    if ($InstallLocation) {
        $escapedInstallLocation = $InstallLocation.Replace("'", "''")
        $cleanupContent += @(
            ('$target = ''{0}''' -f $escapedInstallLocation),
            'for ($attempt = 0; $attempt -lt 8; $attempt++) {',
            '    if (-not (Test-Path -LiteralPath $target)) { break }',
            '    Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction SilentlyContinue',
            '    Start-Sleep -Seconds 2',
            '}'
        )
    }

    if ($RemoveRuntimeSettings) {
        $cleanupContent += @(
            'Remove-Item -LiteralPath ''HKCU:\Software\JavaSoft\Prefs\com\tbg\wms\runtime'' -Recurse -Force -ErrorAction SilentlyContinue'
        )
    }

    $cleanupContent += 'Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue'
    Set-Content -LiteralPath $cleanupScript -Value ($cleanupContent -join [Environment]::NewLine) -Encoding ASCII
    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-WindowStyle', 'Hidden',
        '-File', $cleanupScript
    ) | Out-Null
}

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

$installLocation = $null
if ($installed.InstallLocation) {
    $installLocation = [string]$installed.InstallLocation
}

Write-Host "Uninstalling WMS Pallet Tag System $($installed.DisplayVersion)..."
Write-Host "Uninstall log: $LogPath"
$process = Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/x', $productCode, '/passive', '/norestart', '/log', $LogPath) -Wait -PassThru
if ($process.ExitCode -ne 0) {
    throw "Uninstall failed with exit code $($process.ExitCode). See log: $LogPath"
}

if ($WipeInstallRoot -or $RemoveRuntimeSettings) {
    $cleanupInstallLocation = $null
    if ($WipeInstallRoot) {
        $cleanupInstallLocation = $installLocation
    }
    Start-PostUninstallCleanup -InstallLocation $cleanupInstallLocation -RemoveRuntimeSettings:$RemoveRuntimeSettings
}

Write-Host "Uninstall complete."
