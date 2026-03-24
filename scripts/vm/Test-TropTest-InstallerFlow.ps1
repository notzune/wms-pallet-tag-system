[CmdletBinding()]
param(
    [string]$VmName = "TropTest",
    [string]$GuestUser,
    [string]$GuestPassword,
    [Parameter(Mandatory)]
    [string]$OldInstallerPath,
    [Parameter(Mandatory)]
    [string]$NewInstallerPath,
    [string]$OutputDir,
    [string]$CredentialPath
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. (Join-Path $PSScriptRoot "VmVisibleSessionSupport.ps1")

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    $OutputDir = Join-Path $repoRoot "out\vm-installer-flow"
}

function Resolve-GuestCredential {
    param(
        [string]$GuestUser,
        [string]$GuestPassword,
        [string]$CredentialPath
    )

    if (-not [string]::IsNullOrWhiteSpace($GuestUser) -and -not [string]::IsNullOrWhiteSpace($GuestPassword)) {
        return [pscustomobject]@{
            UserName = $GuestUser.Trim()
            Password = $GuestPassword
        }
    }

    $resolvedCredentialPath = $CredentialPath
    if ([string]::IsNullOrWhiteSpace($resolvedCredentialPath)) {
        $resolvedCredentialPath = Join-Path $env:LOCALAPPDATA "wms-pallet-tag-system\TropTest-guest.credential.clixml"
    }

    if (-not (Test-Path -LiteralPath $resolvedCredentialPath)) {
        throw "Guest credentials were not provided and no credential file was found at $resolvedCredentialPath."
    }

    $credential = Import-Clixml -LiteralPath $resolvedCredentialPath
    if (-not $credential -or -not $credential.UserName) {
        throw "Credential file did not contain a valid PSCredential: $resolvedCredentialPath"
    }

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($credential.Password)
    try {
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }

    return [pscustomobject]@{
        UserName = $credential.UserName
        Password = $plainPassword
    }
}

function Invoke-GuestPowerShell {
    param(
        [string]$VmName,
        [string]$GuestUser,
        [string]$GuestPassword,
        [string]$ScriptText
    )

    $localScriptPath = Join-Path ([System.IO.Path]::GetTempPath()) ("wms-vm-" + [guid]::NewGuid().ToString("N") + ".ps1")
    $guestScriptPath = "C:\Temp\" + [System.IO.Path]::GetFileName($localScriptPath)

    Set-Content -LiteralPath $localScriptPath -Value $ScriptText -Encoding ASCII
    try {
        Copy-ToVmGuest -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -SourcePath $localScriptPath -DestinationPath $guestScriptPath
        $output = & (Get-VBoxManagePath) guestcontrol $VmName run `
            --username $GuestUser `
            --password $GuestPassword `
            --exe 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe' `
            -- powershell -NoProfile -ExecutionPolicy Bypass -File $guestScriptPath
        if ($LASTEXITCODE -ne 0) {
            throw "Guest PowerShell execution failed with exit code $LASTEXITCODE."
        }
        return $output
    } finally {
        Remove-Item -LiteralPath $localScriptPath -Force -ErrorAction SilentlyContinue
    }
}

function Resolve-VisibleSessionUser {
    param(
        [string]$VmName,
        [string]$GuestUser,
        [string]$GuestPassword
    )

    $output = Invoke-GuestPowerShell -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -ScriptText @'
$activeUser = (Get-CimInstance Win32_ComputerSystem).UserName
if (-not $activeUser) {
    throw "Could not resolve active visible session user."
}
Write-Host $activeUser
'@
    $resolved = ($output | Select-Object -Last 1).Trim()
    if ([string]::IsNullOrWhiteSpace($resolved) -or $resolved -notmatch '\\') {
        throw "Unexpected active user result: $resolved"
    }
    return ($resolved.Split('\') | Select-Object -Last 1)
}

$guestCredential = Resolve-GuestCredential -GuestUser $GuestUser -GuestPassword $GuestPassword -CredentialPath $CredentialPath
$GuestUser = $guestCredential.UserName
$GuestPassword = $guestCredential.Password
$visibleSessionUser = Resolve-VisibleSessionUser -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message =="
}

function Save-StepShot {
    param([string]$Name)
    $path = Join-Path $OutputDir "$Name.png"
    Save-VmScreenshot -VmName $VmName -Path $path
    return $path
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

$reportPath = Join-Path $OutputDir "installer-flow-report.txt"
$reportLines = [System.Collections.Generic.List[string]]::new()
$reportLines.Add("VM: $VmName")
$reportLines.Add("Old installer: $OldInstallerPath")
$reportLines.Add("New installer: $NewInstallerPath")
$reportLines.Add("Started: $(Get-Date -Format s)")

$runSuffix = Get-Date -Format "yyyyMMddHHmmss"
$guestOldInstaller = "C:\Temp\174-$runSuffix.exe"
$guestNewInstaller = "C:\Temp\175-$runSuffix.exe"
$reportLines.Add("Guest old installer: $guestOldInstaller")
$reportLines.Add("Guest new installer: $guestNewInstaller")
$guestLauncherPath = ('C:\Users\{0}\AppData\Local\WMS-Pallet-Tag-System\WMS Pallet Tag System.exe' -f $visibleSessionUser)
$guestCliPath = ('C:\Users\{0}\AppData\Local\WMS-Pallet-Tag-System\run.bat' -f $visibleSessionUser)
$visibleInstallDir = ('C:\Users\{0}\AppData\Local\WMS-Pallet-Tag-System' -f $visibleSessionUser)
$visibleProgramsDir = ('C:\Users\{0}\AppData\Local\Programs\WMS-Pallet-Tag-System' -f $visibleSessionUser)
$visibleProbeFile = 'C:\Users\Public\wms-visible-check.txt'
$visibleCliConfigFile = 'C:\Users\Public\wms-cli-config.txt'
$visibleCliDbTestFile = 'C:\Users\Public\wms-cli-dbtest.txt'
$reportLines.Add("Visible session user: $visibleSessionUser")
$reportLines.Add("Guest launcher path: $guestLauncherPath")

Write-Step "Copy installers into guest temp"
Copy-ToVmGuest -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -SourcePath $OldInstallerPath -DestinationPath $guestOldInstaller
Copy-ToVmGuest -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -SourcePath $NewInstallerPath -DestinationPath $guestNewInstaller

Write-Step "Scrub stale visible-user install directories"
Start-VmVisibleCommand -VmName $VmName -CommandText ('cmd /c if exist "{0}" rmdir /s /q "{0}" & if exist "{1}" rmdir /s /q "{1}"' -f $visibleInstallDir, $visibleProgramsDir) -PostRunDelaySeconds 4
Start-Sleep -Seconds 2

Write-Step "Remove current install through old-version maintenance flow"
Start-VmVisibleCommand -VmName $VmName -CommandText $guestOldInstaller -PostRunDelaySeconds 4
$reportLines.Add("Maintenance start screenshot: $(Save-StepShot -Name '01-maintenance-start')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 2
Invoke-VmAltTap -VmName $VmName -ScanCode '13'
Start-Sleep -Seconds 2
$reportLines.Add("Maintenance remove screenshot: $(Save-StepShot -Name '02-maintenance-remove')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 6
$reportLines.Add("Maintenance complete screenshot: $(Save-StepShot -Name '03-maintenance-complete')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 2

Write-Step "Install old version fresh"
Start-VmVisibleCommand -VmName $VmName -CommandText $guestOldInstaller -PostRunDelaySeconds 4
$reportLines.Add("Old-version install start screenshot: $(Save-StepShot -Name '04-install-old-start')")
1..3 | ForEach-Object {
    Invoke-VmTap -VmName $VmName -ScanCode '1c'
    Start-Sleep -Seconds 2
}
$reportLines.Add("Old-version ready screenshot: $(Save-StepShot -Name '05-install-old-ready')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 10
$reportLines.Add("Old-version complete screenshot: $(Save-StepShot -Name '06-install-old-complete')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 2

Write-Step "Launch installed old version"
Start-VmVisibleCommand -VmName $VmName -CommandText ('"{0}"' -f $guestLauncherPath) -PostRunDelaySeconds 6
$reportLines.Add("Old-version app screenshot: $(Save-StepShot -Name '07-launch-old')")
Invoke-VmAltTap -VmName $VmName -ScanCode '3e'
Start-Sleep -Seconds 2

Write-Step "Upgrade install to new version"
Start-VmVisibleCommand -VmName $VmName -CommandText $guestNewInstaller -PostRunDelaySeconds 4
$reportLines.Add("New-version install start screenshot: $(Save-StepShot -Name '08-install-new-start')")
1..3 | ForEach-Object {
    Invoke-VmTap -VmName $VmName -ScanCode '1c'
    Start-Sleep -Seconds 2
}
$reportLines.Add("New-version ready screenshot: $(Save-StepShot -Name '09-install-new-ready')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 10
$reportLines.Add("New-version complete screenshot: $(Save-StepShot -Name '10-install-new-complete')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 2

Write-Step "Launch installed new version"
Start-VmVisibleCommand -VmName $VmName -CommandText ('"{0}"' -f $guestLauncherPath) -PostRunDelaySeconds 6
$reportLines.Add("New-version launch attempt screenshot: $(Save-StepShot -Name '11-launch-new')")

Write-Step "Collect Defender evidence"
$defenderLogPath = Join-Path $OutputDir "defender-events.txt"
$defenderOutput = & (Get-VBoxManagePath) guestcontrol $VmName run `
    --username $GuestUser `
    --password $GuestPassword `
    --exe 'C:\Windows\System32\cmd.exe' `
    -- cmd /c wevtutil qe "Microsoft-Windows-Windows Defender/Operational" /c:8 /rd:true /f:text
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read Defender operational log."
}
$defenderOutput | Set-Content -Path $defenderLogPath -Encoding UTF8
$reportLines.Add("Defender log: $defenderLogPath")

Write-Step "Check installed launcher through visible session"
Start-VmVisibleCommand -VmName $VmName -CommandText ('cmd /c if exist "{0}" (echo EXISTS > "{1}") else (echo MISSING > "{1}")' -f $guestLauncherPath, $visibleProbeFile) -PostRunDelaySeconds 3
$missingOutput = Invoke-GuestPowerShell -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -ScriptText @"
Get-Content -LiteralPath '$visibleProbeFile'
"@
$reportLines.Add("Installed launcher path check: $($missingOutput -join ' ')")

Write-Step "Run installed CLI config through visible session"
Start-VmVisibleCommand -VmName $VmName -CommandText ('cmd /c ""{0}" config > "{1}" 2>&1""' -f $guestCliPath, $visibleCliConfigFile) -PostRunDelaySeconds 5
$configOutput = Invoke-GuestPowerShell -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -ScriptText @"
Get-Content -LiteralPath '$visibleCliConfigFile'
"@
$reportLines.Add("Installed CLI config output: $($configOutput -join ' | ')")

Write-Step "Run installed CLI db-test through visible session"
Start-VmVisibleCommand -VmName $VmName -CommandText ('cmd /c ""{0}" db-test > "{1}" 2>&1""' -f $guestCliPath, $visibleCliDbTestFile) -PostRunDelaySeconds 8
$dbTestOutput = Invoke-GuestPowerShell -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -ScriptText @"
Get-Content -LiteralPath '$visibleCliDbTestFile'
"@
$reportLines.Add("Installed CLI db-test output: $($dbTestOutput -join ' | ')")

$reportLines.Add("Finished: $(Get-Date -Format s)")
$reportLines | Set-Content -Path $reportPath -Encoding UTF8

Write-Host ""
Write-Host "Installer flow complete."
Write-Host "Report     : $reportPath"
Write-Host "Artifacts  : $OutputDir"
