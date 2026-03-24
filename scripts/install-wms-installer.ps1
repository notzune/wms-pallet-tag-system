[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$LogPath,
    [string]$InstallDir,
    [long]$WaitForProcessId,
    [string]$RelaunchPath,
    [string]$ProductDisplayName = 'WMS Pallet Tag System',
    [switch]$ReplaceExisting,
    [switch]$QuietInstall
)

$ErrorActionPreference = "Stop"

function Get-InstalledWmsProduct {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProductDisplayName
    )

    $registryRoots = @(
        'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
    )
    $userHives = Get-ChildItem 'Registry::HKEY_USERS' -ErrorAction SilentlyContinue |
        Where-Object {
            $_.PSChildName -match '^S-1-5-21-' -and
                    $_.PSChildName -notlike '*_Classes'
        } |
        ForEach-Object { 'Registry::HKEY_USERS\{0}\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -f $_.PSChildName }
    $registryRoots += $userHives

    $entries = Get-ItemProperty $registryRoots -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -eq $ProductDisplayName } |
        Sort-Object DisplayVersion -Descending -Unique

    if (-not $entries) {
        return $null
    }

    return $entries | Select-Object -First 1
}

function Get-InstallerVersion {
    param([string]$Path)

    $name = [System.IO.Path]::GetFileNameWithoutExtension($Path)
    if ($name -match '(\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?)$') {
        return $Matches[1]
    }
    return $null
}

function Parse-SemVerLike {
    param([string]$VersionText)

    if ([string]::IsNullOrWhiteSpace($VersionText)) {
        return $null
    }

    $normalized = $VersionText.Trim()
    if ($normalized -notmatch '^(?<core>\d+\.\d+\.\d+)(?:-(?<suffix>[0-9A-Za-z.-]+))?$') {
        return $null
    }

    $coreParts = $Matches['core'].Split('.') | ForEach-Object { [int]$_ }
    $suffix = $Matches['suffix']
    $suffixParts = @()
    if (-not [string]::IsNullOrWhiteSpace($suffix)) {
        $suffixParts = $suffix.Split('.')
    }

    return [pscustomobject]@{
        Major = $coreParts[0]
        Minor = $coreParts[1]
        Patch = $coreParts[2]
        Suffix = $suffix
        SuffixParts = $suffixParts
    }
}

function Compare-SemVerLike {
    param(
        [string]$LeftVersion,
        [string]$RightVersion
    )

    $left = Parse-SemVerLike -VersionText $LeftVersion
    $right = Parse-SemVerLike -VersionText $RightVersion
    if ($null -eq $left -or $null -eq $right) {
        throw "Could not compare installer versions '$LeftVersion' and '$RightVersion'."
    }

    foreach ($property in 'Major', 'Minor', 'Patch') {
        if ($left.$property -lt $right.$property) {
            return -1
        }
        if ($left.$property -gt $right.$property) {
            return 1
        }
    }

    $leftHasSuffix = -not [string]::IsNullOrWhiteSpace($left.Suffix)
    $rightHasSuffix = -not [string]::IsNullOrWhiteSpace($right.Suffix)
    if (-not $leftHasSuffix -and -not $rightHasSuffix) {
        return 0
    }
    if (-not $leftHasSuffix) {
        return 1
    }
    if (-not $rightHasSuffix) {
        return -1
    }

    $maxLength = [Math]::Max($left.SuffixParts.Count, $right.SuffixParts.Count)
    for ($i = 0; $i -lt $maxLength; $i++) {
        if ($i -ge $left.SuffixParts.Count) {
            return -1
        }
        if ($i -ge $right.SuffixParts.Count) {
            return 1
        }

        $leftPart = $left.SuffixParts[$i]
        $rightPart = $right.SuffixParts[$i]
        $leftIsNumeric = $leftPart -match '^\d+$'
        $rightIsNumeric = $rightPart -match '^\d+$'

        if ($leftIsNumeric -and $rightIsNumeric) {
            $leftNumber = [int]$leftPart
            $rightNumber = [int]$rightPart
            if ($leftNumber -lt $rightNumber) {
                return -1
            }
            if ($leftNumber -gt $rightNumber) {
                return 1
            }
            continue
        }

        if ($leftIsNumeric -and -not $rightIsNumeric) {
            return -1
        }
        if (-not $leftIsNumeric -and $rightIsNumeric) {
            return 1
        }

        $comparison = [string]::CompareOrdinal($leftPart, $rightPart)
        if ($comparison -lt 0) {
            return -1
        }
        if ($comparison -gt 0) {
            return 1
        }
    }

    return 0
}

function Should-ReplaceExistingInstall {
    param(
        [switch]$ReplaceExisting,
        [string]$InstallerVersion,
        [string]$InstalledVersion
    )

    if ($ReplaceExisting) {
        return $true
    }
    if ([string]::IsNullOrWhiteSpace($InstallerVersion) -or [string]::IsNullOrWhiteSpace($InstalledVersion)) {
        return $false
    }

    return (Compare-SemVerLike -LeftVersion $InstallerVersion -RightVersion $InstalledVersion) -le 0
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
    Invoke-MsiexecWithRetry -ArgumentString $argumentString -FailurePrefix 'Uninstall failed' -LogPath $LogPath
}

function Invoke-MsiexecWithRetry {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ArgumentString,
        [Parameter(Mandatory = $true)]
        [string]$FailurePrefix,
        [Parameter(Mandatory = $true)]
        [string]$LogPath,
        [int]$MaxAttempts = 4,
        [int]$RetryDelaySeconds = 5
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $process = Start-Process -FilePath 'msiexec.exe' -ArgumentList $ArgumentString -Wait -PassThru
        if ($process.ExitCode -eq 0) {
            return
        }
        if ($process.ExitCode -ne 1618 -or $attempt -eq $MaxAttempts) {
            throw "$FailurePrefix with exit code $($process.ExitCode). See log: $LogPath"
        }
        Write-Host "Windows Installer is busy (1618). Retrying in $RetryDelaySeconds second(s)..."
        Start-Sleep -Seconds $RetryDelaySeconds
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

    Invoke-MsiexecWithRetry -ArgumentString ($argumentParts -join ' ') -FailurePrefix 'MSI installer failed' -LogPath $LogPath
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

function Wait-ForProcessExit {
    param(
        [long]$ProcessId,
        [int]$TimeoutSeconds = 60
    )

    if ($ProcessId -le 0) {
        return
    }

    try {
        $process = Get-Process -Id $ProcessId -ErrorAction Stop
        Write-Host "Waiting for process $ProcessId to exit before installing..."
        $process | Wait-Process -Timeout $TimeoutSeconds -ErrorAction Stop
    } catch [Microsoft.PowerShell.Commands.ProcessCommandException] {
        return
    } catch {
        throw "Timed out waiting for process $ProcessId to exit."
    }
}

function Resolve-RelaunchPath {
    param(
        [string]$RequestedPath,
        [string]$InstallDir,
        [string]$ScriptRoot
    )

    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        $candidates += $RequestedPath
    }
    if (-not [string]::IsNullOrWhiteSpace($InstallDir)) {
        $candidates += (Join-Path $InstallDir 'run.bat')
        $candidates += (Join-Path $InstallDir 'WMS Pallet Tag System.exe')
        $candidates += (Join-Path $InstallDir 'app\WMS Pallet Tag System.exe')
    }
    if (-not [string]::IsNullOrWhiteSpace($ScriptRoot)) {
        $scriptInstallRoot = Split-Path -Parent $ScriptRoot
        $candidates += (Join-Path $scriptInstallRoot 'run.bat')
        $candidates += (Join-Path $scriptInstallRoot 'WMS Pallet Tag System.exe')
        $candidates += (Join-Path $scriptInstallRoot 'app\WMS Pallet Tag System.exe')
    }

    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    return $null
}

function Invoke-Relaunch {
    param(
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    $extension = [System.IO.Path]::GetExtension($Path)
    if ($extension -ieq '.bat' -or $extension -ieq '.cmd') {
        Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', "`"$Path`"" | Out-Null
        return
    }

    Start-Process -FilePath $Path | Out-Null
}

if ($env:WMS_INSTALLER_HELPER_TEST_MODE -eq '1') {
    return
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
$scriptRoot = Split-Path -Parent $PSCommandPath
if (-not $LogPath) {
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $LogPath = Join-Path (Split-Path -Parent $resolvedInstallerPath) "install-wms-tags-$timestamp.log"
}

Wait-ForProcessExit -ProcessId $WaitForProcessId

$installed = Get-InstalledWmsProduct -ProductDisplayName $ProductDisplayName
if ($installed) {
    $installedVersion = [string]$installed.DisplayVersion
    Write-Host "Detected installed version: $installedVersion"
    if (Should-ReplaceExistingInstall -ReplaceExisting:$ReplaceExisting -InstallerVersion $installerVersion -InstalledVersion $installedVersion) {
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
$resolvedRelaunchPath = Resolve-RelaunchPath -RequestedPath $RelaunchPath -InstallDir $InstallDir -ScriptRoot $scriptRoot
if ($resolvedRelaunchPath) {
    Write-Host "Relaunching application: $resolvedRelaunchPath"
    Invoke-Relaunch -Path $resolvedRelaunchPath
}
