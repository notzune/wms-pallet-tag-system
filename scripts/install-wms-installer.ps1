[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$LogPath,
    [string]$InstallDir,
    [switch]$ReplaceExisting,
    [switch]$QuietInstall
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

function Get-InstallerType {
    param([string]$Path)

    $extension = [System.IO.Path]::GetExtension($Path)
    switch -Regex ($extension) {
        '^\.msi$' { return 'msi' }
        '^\.exe$' { return 'exe' }
        default { throw "Unsupported installer type: $extension" }
    }
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
    $argumentString = '/x "{0}" /passive /norestart /l*v "{1}"' -f $productCode, $LogPath
    $process = Start-Process -FilePath 'msiexec.exe' -ArgumentList $argumentString -Wait -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Uninstall failed with exit code $($process.ExitCode). See log: $LogPath"
    }
}

function Invoke-MsiInstall {
    param(
        [Parameter(Mandatory = $true)]
        [string]$InstallerPath,
        [Parameter(Mandatory = $true)]
        [string]$LogPath,
        [string]$InstallDir,
        [switch]$QuietInstall
    )

    $argumentParts = @('/i "{0}"' -f $InstallerPath)
    if ($QuietInstall) {
        $argumentParts += '/passive /norestart'
    }
    if (-not [string]::IsNullOrWhiteSpace($InstallDir)) {
        $argumentParts += ('INSTALLDIR="{0}"' -f $InstallDir)
    }
    $argumentParts += ('/l*v "{0}"' -f $LogPath)

    $process = Start-Process -FilePath 'msiexec.exe' -ArgumentList ($argumentParts -join ' ') -Wait -PassThru
    if ($process.ExitCode -ne 0) {
        throw "MSI installer failed with exit code $($process.ExitCode). See log: $LogPath"
    }
}

function Invoke-ExeInstall {
    param(
        [Parameter(Mandatory = $true)]
        [string]$InstallerPath,
        [Parameter(Mandatory = $true)]
        [string]$LogPath,
        [string]$InstallDir,
        [switch]$QuietInstall
    )

    if ($QuietInstall) {
        throw "QuietInstall is not supported for EXE installers in this script. Use an MSI-backed installer path for smoke automation."
    }
    if (-not [string]::IsNullOrWhiteSpace($InstallDir)) {
        throw "InstallDir overrides are not supported for EXE installers in this script. Use an MSI-backed installer path for smoke automation."
    }

    $process = Start-Process -FilePath $InstallerPath -ArgumentList @('/log', $LogPath) -Wait -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Installer failed with exit code $($process.ExitCode). See log: $LogPath"
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
$installerType = Get-InstallerType -Path $resolvedInstallerPath
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
if ($installerType -eq 'msi') {
    Invoke-MsiInstall -InstallerPath $resolvedInstallerPath -LogPath $LogPath -InstallDir $InstallDir -QuietInstall:$QuietInstall
} else {
    Invoke-ExeInstall -InstallerPath $resolvedInstallerPath -LogPath $LogPath -InstallDir $InstallDir -QuietInstall:$QuietInstall
}

Write-Host "Installation complete."
