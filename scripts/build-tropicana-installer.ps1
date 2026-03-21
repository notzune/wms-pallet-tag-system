[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$ConfigSourcePath,
    [string]$OutputDir,
    [string]$SourceRoot,
    [string]$ProductDisplayName
)

$ErrorActionPreference = "Stop"

function Resolve-ConfigSourcePath {
    param(
        [string]$ConfigSourcePath,
        [string]$SourceRoot
    )

    if (-not [string]::IsNullOrWhiteSpace($ConfigSourcePath)) {
        return $ConfigSourcePath
    }

    $defaultPath = Join-Path $SourceRoot ".env"
    if (Test-Path -LiteralPath $defaultPath) {
        return $defaultPath
    }

    throw "Config source not found. Pass -ConfigSourcePath with the Tropicana env payload."
}

function Resolve-AppInstallerPath {
    param(
        [string]$InstallerPath,
        [string]$SourceRoot,
        [string]$OutputDir
    )

    if (-not [string]::IsNullOrWhiteSpace($InstallerPath)) {
        return $InstallerPath
    }

    & (Join-Path $SourceRoot "scripts\build-jpackage-bundle.ps1") -SourceRoot $SourceRoot -InstallerType exe

    $searchRoots = @($OutputDir, (Join-Path $SourceRoot "dist")) | Select-Object -Unique
    $installerCandidates = foreach ($root in $searchRoots) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -Path $root -Filter 'WMS Pallet Tag System-*.exe' -File -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -notlike '*Tropicana*' }
        }
    }
    $candidate = $installerCandidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $candidate) {
        throw "Could not locate the packaged installer after building it."
    }
    return $candidate.FullName
}

function New-EmbeddedSupportScript {
    param(
        [string]$TemplatePath,
        [string]$DestinationPath,
        [string]$ConfigContent
    )

    $base64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($ConfigContent))
    $template = Get-Content -LiteralPath $TemplatePath -Raw
    $rendered = $template.Replace('__EMBEDDED_CONFIG_BASE64_VALUE__', $base64)
    Set-Content -LiteralPath $DestinationPath -Value $rendered -Encoding ASCII
}

function New-BootstrapScript {
    param(
        [string]$DestinationPath,
        [string]$InstallerFileName,
        [string]$ProductDisplayName
    )

    $content = @"
param(
    [string]`$InstallDir,
    [string]`$LocalAppDataRoot,
    [string]`$LogPath,
    [switch]`$QuietInstall,
    [switch]`$ReplaceExisting,
    [switch]`$NoLaunch
)

`$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace(`$InstallDir) -and -not [string]::IsNullOrWhiteSpace(`$env:WMS_BOOTSTRAP_INSTALL_DIR)) {
    `$InstallDir = `$env:WMS_BOOTSTRAP_INSTALL_DIR
}
if ([string]::IsNullOrWhiteSpace(`$LocalAppDataRoot) -and -not [string]::IsNullOrWhiteSpace(`$env:WMS_BOOTSTRAP_LOCALAPPDATA)) {
    `$LocalAppDataRoot = `$env:WMS_BOOTSTRAP_LOCALAPPDATA
}
if ([string]::IsNullOrWhiteSpace(`$LogPath) -and -not [string]::IsNullOrWhiteSpace(`$env:WMS_BOOTSTRAP_INSTALL_LOG)) {
    `$LogPath = `$env:WMS_BOOTSTRAP_INSTALL_LOG
}
if (-not `$QuietInstall -and `$env:WMS_BOOTSTRAP_QUIET -eq '1') {
    `$QuietInstall = `$true
}
if (-not `$ReplaceExisting -and `$env:WMS_BOOTSTRAP_REPLACE_EXISTING -eq '1') {
    `$ReplaceExisting = `$true
}
if (-not `$NoLaunch -and `$env:WMS_BOOTSTRAP_NO_LAUNCH -eq '1') {
    `$NoLaunch = `$true
}

function Invoke-ExternalOrThrow {
    param(
        [Parameter(Mandatory = `$true)]
        [string]`$FilePath,
        [Parameter(Mandatory = `$true)]
        [string[]]`$Arguments,
        [Parameter(Mandatory = `$true)]
        [string]`$FailureMessage
    )

    & `$FilePath @Arguments
    if (`$LASTEXITCODE -ne 0) {
        throw ('{0} ExitCode={1}' -f `$FailureMessage, `$LASTEXITCODE)
    }
}

function Get-InstalledWmsLocation {
    if (-not [string]::IsNullOrWhiteSpace(`$InstallDir)) {
        return `$InstallDir
    }

    `$entries = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
        Where-Object { `$_.DisplayName -eq 'WMS Pallet Tag System' } |
        Sort-Object DisplayVersion -Descending

    if (`$entries) {
        `$installLocation = [string](`$entries | Select-Object -First 1).InstallLocation
        if (-not [string]::IsNullOrWhiteSpace(`$installLocation)) {
            return `$installLocation
        }
    }

    return (Join-Path `$env:LOCALAPPDATA 'Programs\WMS-Pallet-Tag-System')
}

`$scriptDir = Split-Path -Parent `$PSCommandPath
`$installerPath = Join-Path `$scriptDir '$InstallerFileName'
`$configInstaller = Join-Path `$scriptDir 'Install-Tropicana-Config.ps1'
`$installHelper = Join-Path `$scriptDir 'install-wms-installer.ps1'
`$resolvedLogPath = if ([string]::IsNullOrWhiteSpace(`$LogPath)) {
    Join-Path `$env:TEMP ('install-wms-tags-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
} else {
    `$LogPath
}

`$installArgs = @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', `$installHelper,
    '-InstallerPath', `$installerPath,
    '-LogPath', `$resolvedLogPath,
    '-ProductDisplayName', '$ProductDisplayName'
)
if (-not [string]::IsNullOrWhiteSpace(`$InstallDir)) {
    `$installArgs += @('-InstallDir', `$InstallDir)
}
if (`$QuietInstall) {
    `$installArgs += '-QuietInstall'
}
if (`$ReplaceExisting) {
    `$installArgs += '-ReplaceExisting'
}
Invoke-ExternalOrThrow -FilePath 'powershell.exe' -Arguments `$installArgs -FailureMessage 'Installer helper failed.'
`$installDir = Get-InstalledWmsLocation
`$configArgs = @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', `$configInstaller,
    '-InstallDir', `$installDir,
    '-SkipVerify'
)
if (-not [string]::IsNullOrWhiteSpace(`$LocalAppDataRoot)) {
    `$configArgs += @('-LocalAppDataRoot', `$LocalAppDataRoot)
}
Invoke-ExternalOrThrow -FilePath 'powershell.exe' -Arguments `$configArgs -FailureMessage 'Tropicana config install failed.'
if (-not `$NoLaunch) {
    Start-Process -FilePath (Join-Path `$installDir 'WMS Pallet Tag System.exe') -ErrorAction SilentlyContinue | Out-Null
}
"@
    Set-Content -LiteralPath $DestinationPath -Value $content -Encoding ASCII
}

function New-BootstrapCmd {
    param([string]$DestinationPath)

    $content = @"
@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap-install.ps1"
endlocal
"@
    Set-Content -LiteralPath $DestinationPath -Value $content -Encoding ASCII
}

function New-IexpressSed {
    param(
        [string]$DestinationPath,
        [string]$TargetName,
        [string]$FriendlyName,
        [string]$StageDir,
        [string[]]$Files
    )

    $lines = @(
        '[Version]',
        'Class=IEXPRESS',
        'SEDVersion=3',
        '[Options]',
        'PackagePurpose=InstallApp',
        'ShowInstallProgramWindow=1',
        'HideExtractAnimation=0',
        'UseLongFileName=1',
        'InsideCompressed=0',
        'CAB_FixedSize=0',
        'CAB_ResvCodeSigning=0',
        'RebootMode=N',
        'InstallPrompt=',
        'DisplayLicense=',
        'FinishMessage=',
        "TargetName=$TargetName",
        "FriendlyName=$FriendlyName",
        'AppLaunched=cmd /c bootstrap.cmd',
        'PostInstallCmd=<None>',
        'AdminQuietInstCmd=',
        'UserQuietInstCmd=',
        'SourceFiles=SourceFiles',
        '[SourceFiles]',
        "SourceFiles0=$StageDir\"
    )

    $lines += '[SourceFiles0]'
    for ($i = 0; $i -lt $Files.Count; $i++) {
        $lines += ("%FILE{0}%={1}" -f $i, $Files[$i])
    }

    $lines += '[Strings]'
    for ($i = 0; $i -lt $Files.Count; $i++) {
        $lines += ("FILE{0}={1}" -f $i, $Files[$i])
    }

    Set-Content -LiteralPath $DestinationPath -Value ($lines -join "`r`n") -Encoding ASCII
}

$scriptRoot = Split-Path -Parent $PSCommandPath
if (-not $SourceRoot) {
    $SourceRoot = Split-Path -Parent $scriptRoot
}
if (-not $OutputDir) {
    $OutputDir = Join-Path $SourceRoot "dist"
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$resolvedConfigSource = Resolve-ConfigSourcePath -ConfigSourcePath $ConfigSourcePath -SourceRoot $SourceRoot
$resolvedInstallerPath = Resolve-AppInstallerPath -InstallerPath $InstallerPath -SourceRoot $SourceRoot -OutputDir $OutputDir

if (-not (Test-Path -LiteralPath $resolvedInstallerPath)) {
    throw "Installer not found: $resolvedInstallerPath"
}

$configContent = Get-Content -LiteralPath $resolvedConfigSource -Raw
$stageDir = Join-Path $OutputDir "tropicana-bootstrap-stage"
Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null

$installerFileName = Split-Path -Leaf $resolvedInstallerPath
$resolvedProductDisplayName = if ([string]::IsNullOrWhiteSpace($ProductDisplayName)) {
    ([System.IO.Path]::GetFileNameWithoutExtension($installerFileName) -replace '-\d+\.\d+\.\d+$', '')
} else {
    $ProductDisplayName.Trim()
}
Copy-Item -LiteralPath $resolvedInstallerPath -Destination (Join-Path $stageDir $installerFileName) -Force
Copy-Item -LiteralPath $resolvedInstallerPath -Destination (Join-Path $OutputDir $installerFileName) -Force

$supportScriptPath = Join-Path $OutputDir "Install-Tropicana-Config.ps1"
$installHelperPath = Join-Path $OutputDir "install-wms-installer.ps1"
$bootstrapInstallScriptPath = Join-Path $OutputDir "bootstrap-install.ps1"
New-EmbeddedSupportScript -TemplatePath (Join-Path $SourceRoot "scripts\Install-Tropicana-Config.ps1") `
    -DestinationPath $supportScriptPath `
    -ConfigContent $configContent
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\install-wms-installer.ps1") -Destination $installHelperPath -Force
New-BootstrapScript -DestinationPath $bootstrapInstallScriptPath -InstallerFileName $installerFileName -ProductDisplayName $resolvedProductDisplayName
Copy-Item -LiteralPath $supportScriptPath -Destination (Join-Path $stageDir "Install-Tropicana-Config.ps1") -Force
Copy-Item -LiteralPath $installHelperPath -Destination (Join-Path $stageDir "install-wms-installer.ps1") -Force

Copy-Item -LiteralPath $bootstrapInstallScriptPath -Destination (Join-Path $stageDir "bootstrap-install.ps1") -Force
New-BootstrapCmd -DestinationPath (Join-Path $stageDir "bootstrap.cmd")

$targetName = Join-Path $OutputDir "WMS Pallet Tag System - Tropicana Setup.exe"
Remove-Item -LiteralPath $targetName -Force -ErrorAction SilentlyContinue
$sedPath = Join-Path $stageDir "tropicana-bootstrap.sed"
$files = @(
    $installerFileName,
    'Install-Tropicana-Config.ps1',
    'install-wms-installer.ps1',
    'bootstrap-install.ps1',
    'bootstrap.cmd'
)
New-IexpressSed -DestinationPath $sedPath `
    -TargetName $targetName `
    -FriendlyName 'WMS Pallet Tag System - Tropicana Setup' `
    -StageDir $stageDir `
    -Files $files

& iexpress.exe /N $sedPath | Out-Null
if (-not (Test-Path -LiteralPath $targetName)) {
    throw "IExpress did not create the Tropicana setup executable."
}

Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Tropicana setup ready: $targetName"
Write-Host "Fallback config installer: $supportScriptPath"
