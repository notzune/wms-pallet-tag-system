[CmdletBinding()]
param(
    [string]$VmName = "TropTest",
    [Parameter(Mandatory)]
    [string]$GuestUser,
    [Parameter(Mandatory)]
    [string]$GuestPassword,
    [Parameter(Mandatory)]
    [string]$OldInstallerPath,
    [Parameter(Mandatory)]
    [string]$NewInstallerPath,
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. (Join-Path $PSScriptRoot "VmVisibleSessionSupport.ps1")

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    $OutputDir = Join-Path $repoRoot "out\vm-installer-flow"
}

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

Write-Step "Copy installers into guest temp"
Copy-ToVmGuest -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -SourcePath $OldInstallerPath -DestinationPath 'C:\Temp\174.exe'
Copy-ToVmGuest -VmName $VmName -GuestUser $GuestUser -GuestPassword $GuestPassword -SourcePath $NewInstallerPath -DestinationPath 'C:\Temp\175.exe'

Write-Step "Remove current install through 1.7.4 maintenance flow"
Start-VmVisibleCommand -VmName $VmName -CommandText 'c:\temp\174.exe' -PostRunDelaySeconds 4
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

Write-Step "Install 1.7.4 fresh"
Start-VmVisibleCommand -VmName $VmName -CommandText 'c:\temp\174.exe' -PostRunDelaySeconds 4
$reportLines.Add("1.7.4 install start screenshot: $(Save-StepShot -Name '04-install-174-start')")
1..3 | ForEach-Object {
    Invoke-VmTap -VmName $VmName -ScanCode '1c'
    Start-Sleep -Seconds 2
}
$reportLines.Add("1.7.4 ready screenshot: $(Save-StepShot -Name '05-install-174-ready')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 10
$reportLines.Add("1.7.4 complete screenshot: $(Save-StepShot -Name '06-install-174-complete')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 2

Write-Step "Launch installed 1.7.4"
Start-VmVisibleCommand -VmName $VmName -CommandText '"C:\Users\zrash\AppData\Local\WMS-Pallet-Tag-System\WMS Pallet Tag System.exe"' -PostRunDelaySeconds 6
$reportLines.Add("1.7.4 app screenshot: $(Save-StepShot -Name '07-launch-174')")
Invoke-VmAltTap -VmName $VmName -ScanCode '3e'
Start-Sleep -Seconds 2

Write-Step "Upgrade install to 1.7.5"
Start-VmVisibleCommand -VmName $VmName -CommandText 'c:\temp\175.exe' -PostRunDelaySeconds 4
$reportLines.Add("1.7.5 install start screenshot: $(Save-StepShot -Name '08-install-175-start')")
1..3 | ForEach-Object {
    Invoke-VmTap -VmName $VmName -ScanCode '1c'
    Start-Sleep -Seconds 2
}
$reportLines.Add("1.7.5 ready screenshot: $(Save-StepShot -Name '09-install-175-ready')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 10
$reportLines.Add("1.7.5 complete screenshot: $(Save-StepShot -Name '10-install-175-complete')")
Invoke-VmTap -VmName $VmName -ScanCode '1c'
Start-Sleep -Seconds 2

Write-Step "Launch installed 1.7.5"
Start-VmVisibleCommand -VmName $VmName -CommandText '"C:\Users\zrash\AppData\Local\WMS-Pallet-Tag-System\WMS Pallet Tag System.exe"' -PostRunDelaySeconds 6
$reportLines.Add("1.7.5 launch attempt screenshot: $(Save-StepShot -Name '11-launch-175')")

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

$missingOutput = & (Get-VBoxManagePath) guestcontrol $VmName run `
    --username $GuestUser `
    --password $GuestPassword `
    --exe 'C:\Windows\System32\cmd.exe' `
    -- cmd /v:on /c "if exist ""C:\Users\zrash\AppData\Local\WMS-Pallet-Tag-System\WMS Pallet Tag System.exe"" (echo EXISTS) else (echo MISSING)"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to check installed launcher path."
}
$reportLines.Add("Installed launcher path check: $($missingOutput -join ' ')")

$reportLines.Add("Finished: $(Get-Date -Format s)")
$reportLines | Set-Content -Path $reportPath -Encoding UTF8

Write-Host ""
Write-Host "Installer flow complete."
Write-Host "Report     : $reportPath"
Write-Host "Artifacts  : $OutputDir"
