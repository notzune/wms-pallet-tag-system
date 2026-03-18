[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$LogPath,
    [switch]$ReplaceExisting,
    [switch]$LaunchAfterInstall = $true
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$script:StatusForm = $null
$script:StatusLabel = $null
$script:CloseButton = $null
$script:ProgressBar = $null
$script:StatusWindowKeepOpenDelayMs = 1200

function Initialize-StatusWindow {
    $script:StatusForm = New-Object System.Windows.Forms.Form
    $script:StatusForm.Text = 'WMS Pallet Tag System Setup'
    $script:StatusForm.StartPosition = 'CenterScreen'
    $script:StatusForm.ClientSize = New-Object System.Drawing.Size(480, 180)
    $script:StatusForm.FormBorderStyle = 'FixedDialog'
    $script:StatusForm.MaximizeBox = $false
    $script:StatusForm.MinimizeBox = $false
    $script:StatusForm.TopMost = $true

    $script:StatusLabel = New-Object System.Windows.Forms.Label
    $script:StatusLabel.Location = New-Object System.Drawing.Point(16, 20)
    $script:StatusLabel.Size = New-Object System.Drawing.Size(448, 72)
    $script:StatusLabel.Text = 'Preparing install...'
    $script:StatusLabel.Font = New-Object System.Drawing.Font('Segoe UI', 10)

    $script:ProgressBar = New-Object System.Windows.Forms.ProgressBar
    $script:ProgressBar.Location = New-Object System.Drawing.Point(16, 104)
    $script:ProgressBar.Size = New-Object System.Drawing.Size(448, 20)
    $script:ProgressBar.Style = 'Marquee'
    $script:ProgressBar.MarqueeAnimationSpeed = 20

    $script:CloseButton = New-Object System.Windows.Forms.Button
    $script:CloseButton.Text = 'Close'
    $script:CloseButton.Location = New-Object System.Drawing.Point(372, 136)
    $script:CloseButton.Size = New-Object System.Drawing.Size(92, 28)
    $script:CloseButton.Visible = $false
    $script:CloseButton.Add_Click({ $script:StatusForm.Close() })

    $script:StatusForm.Controls.Add($script:StatusLabel)
    $script:StatusForm.Controls.Add($script:ProgressBar)
    $script:StatusForm.Controls.Add($script:CloseButton)
    $script:StatusForm.Show()
    [System.Windows.Forms.Application]::DoEvents()
}

function Update-StatusWindow {
    param([string]$Message)

    if ($script:StatusLabel) {
        $script:StatusLabel.Text = $Message
        [System.Windows.Forms.Application]::DoEvents()
    }
}

function Complete-StatusWindow {
    param(
        [string]$Message,
        [bool]$KeepOpen
    )

    if ($script:StatusLabel) {
        $script:StatusLabel.Text = $Message
    }
    if ($script:ProgressBar) {
        $script:ProgressBar.Style = 'Blocks'
        $script:ProgressBar.Value = 100
    }
    if ($KeepOpen -and $script:CloseButton) {
        $script:CloseButton.Visible = $true
        if ($script:StatusForm) {
            $script:StatusForm.TopMost = $false
            $script:StatusForm.Activate()
        }
        Wait-ForStatusWindowClose
        return
    }
    [System.Windows.Forms.Application]::DoEvents()
    Start-Sleep -Milliseconds $script:StatusWindowKeepOpenDelayMs
    if ($script:StatusForm) {
        $script:StatusForm.Close()
    }
}

function Wait-ForStatusWindowClose {
    while ($script:StatusForm -and $script:StatusForm.Visible) {
        [System.Windows.Forms.Application]::DoEvents()
        Start-Sleep -Milliseconds 100
    }
}

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
    if ($name -match '(\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?)$') {
        return $Matches[1]
    }
    return $null
}

function Resolve-InstalledExecutable {
    param($InstalledProduct)

    if (-not $InstalledProduct) {
        return $null
    }

    if ($InstalledProduct.DisplayIcon) {
        $displayIcon = [string]$InstalledProduct.DisplayIcon
        $candidate = $displayIcon.Split(',')[0].Trim('" ')
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    if ($InstalledProduct.InstallLocation) {
        $candidate = Join-Path ([string]$InstalledProduct.InstallLocation) 'WMS Pallet Tag System.exe'
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $commonCandidates = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\WMS-Pallet-Tag-System\WMS Pallet Tag System.exe'),
        (Join-Path ${env:ProgramFiles} 'WMS-Pallet-Tag-System\WMS Pallet Tag System.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'WMS-Pallet-Tag-System\WMS Pallet Tag System.exe')
    ) | Where-Object { $_ }

    foreach ($candidate in $commonCandidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
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

function Invoke-WmsInstallerMain {
    if (-not $InstallerPath) {
        $scriptRoot = Split-Path -Parent $PSCommandPath
        $candidate = Get-ChildItem -Path $scriptRoot -Filter 'WMS Pallet Tag System-*.exe' -File -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($candidate) {
            $InstallerPath = $candidate.FullName
        }
    }

    try {
        if (-not $InstallerPath -or -not (Test-Path -LiteralPath $InstallerPath)) {
            throw "Installer not found. Pass -InstallerPath or place the script next to the built installer."
        }

        Initialize-StatusWindow
        Update-StatusWindow 'Preparing install...'

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
                Update-StatusWindow "Removing existing version $installedVersion..."
                $uninstallLog = [System.IO.Path]::ChangeExtension($LogPath, '.uninstall.log')
                Invoke-Uninstall -InstalledProduct $installed -LogPath $uninstallLog
            }
        }

        Update-StatusWindow "Installing version $installerVersion..."
        Write-Host "Launching installer: $resolvedInstallerPath"
        Write-Host "Installer log: $LogPath"
        $process = Start-Process -FilePath $resolvedInstallerPath -ArgumentList @('/log', $LogPath) -Wait -PassThru
        if ($process.ExitCode -ne 0) {
            throw "Installer failed with exit code $($process.ExitCode). See log: $LogPath"
        }

        if ($LaunchAfterInstall) {
            Update-StatusWindow 'Launching application...'
            $installedAfter = Get-InstalledWmsProduct
            $installedExe = Resolve-InstalledExecutable -InstalledProduct $installedAfter
            if ($installedExe) {
                Start-Process -FilePath $installedExe | Out-Null
            } else {
                Write-Warning 'Installed application executable could not be resolved after install.'
            }
        }

        Write-Host "Installation complete."
        Complete-StatusWindow -Message 'Installation complete.' -KeepOpen:$false
    } catch {
        $message = $_.Exception.Message
        Write-Error $message
        if ($script:StatusForm) {
            Complete-StatusWindow -Message ("Installation failed.`n" + $message) -KeepOpen:$true
        }
        throw
    }
}

if (-not $env:WMS_INSTALLER_HELPER_TEST_MODE) {
    Invoke-WmsInstallerMain
}
