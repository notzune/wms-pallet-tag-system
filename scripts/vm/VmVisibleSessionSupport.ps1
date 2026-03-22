Set-StrictMode -Version Latest

function Get-VBoxManagePath {
    $candidates = @(
        (Join-Path ${env:ProgramFiles} "Oracle\VirtualBox\VBoxManage.exe"),
        (Join-Path ${env:ProgramW6432} "Oracle\VirtualBox\VBoxManage.exe")
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    $cmd = Get-Command VBoxManage.exe -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source) {
        return $cmd.Source
    }

    if ($candidates) {
        return $candidates[0]
    }

    throw "VBoxManage.exe not found. Install VirtualBox or add VBoxManage to PATH."
}

function Invoke-VBoxManage {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    $vboxManage = Get-VBoxManagePath
    & $vboxManage @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "VBoxManage failed with exit code $LASTEXITCODE for arguments: $($Arguments -join ' ')"
    }
}

function Send-VmScancodes {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string[]]$Scancodes
    )

    $arguments = @('controlvm', $VmName, 'keyboardputscancode') + $Scancodes
    Invoke-VBoxManage -Arguments $arguments
}

function Invoke-VmTap {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$ScanCode,
        [int]$DelayMs = 120
    )

    $make = $ScanCode.ToLowerInvariant()
    $break = '{0:x2}' -f (([Convert]::ToInt32($make, 16)) -bor 0x80)
    Send-VmScancodes -VmName $VmName -Scancodes @($make, $break)
    Start-Sleep -Milliseconds $DelayMs
}

function Invoke-VmAltTap {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$ScanCode,
        [int]$DelayMs = 150
    )

    Send-VmScancodes -VmName $VmName -Scancodes @('38', $ScanCode.ToLowerInvariant())
    Start-Sleep -Milliseconds 60
    $break = '{0:x2}' -f (([Convert]::ToInt32($ScanCode, 16)) -bor 0x80)
    Send-VmScancodes -VmName $VmName -Scancodes @($break, 'b8')
    Start-Sleep -Milliseconds $DelayMs
}

function Open-VmRunDialog {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [int]$DelayMs = 700
    )

    Send-VmScancodes -VmName $VmName -Scancodes @('e0', '5b', '13', '93', 'e0', 'db')
    Start-Sleep -Milliseconds $DelayMs
}

function Send-VmText {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$Text,
        [int]$DelayMs = 250
    )

    Invoke-VBoxManage -Arguments @('controlvm', $VmName, 'keyboardputstring', $Text)
    Start-Sleep -Milliseconds $DelayMs
}

function Start-VmVisibleCommand {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$CommandText,
        [int]$PostRunDelaySeconds = 2
    )

    Open-VmRunDialog -VmName $VmName
    Send-VmText -VmName $VmName -Text $CommandText
    Invoke-VmTap -VmName $VmName -ScanCode '1c'
    Start-Sleep -Seconds $PostRunDelaySeconds
}

function Save-VmScreenshot {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$Path
    )

    $directory = Split-Path -Parent $Path
    if ($directory) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    Invoke-VBoxManage -Arguments @('controlvm', $VmName, 'screenshotpng', $Path)
}

function Invoke-VmGuestCommand {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$GuestUser,
        [Parameter(Mandatory)]
        [string]$GuestPassword,
        [Parameter(Mandatory)]
        [string]$ExePath,
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    Invoke-VBoxManage -Arguments @(
        'guestcontrol', $VmName, 'run',
        '--username', $GuestUser,
        '--password', $GuestPassword,
        '--exe', $ExePath,
        '--'
    ) + $Arguments
}

function Copy-ToVmGuest {
    param(
        [Parameter(Mandatory)]
        [string]$VmName,
        [Parameter(Mandatory)]
        [string]$GuestUser,
        [Parameter(Mandatory)]
        [string]$GuestPassword,
        [Parameter(Mandatory)]
        [string]$SourcePath,
        [Parameter(Mandatory)]
        [string]$DestinationPath
    )

    Invoke-VBoxManage -Arguments @(
        'guestcontrol', $VmName, 'copyto',
        '--username', $GuestUser,
        '--password', $GuestPassword,
        $SourcePath,
        $DestinationPath
    )
}
