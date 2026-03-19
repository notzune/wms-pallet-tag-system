[CmdletBinding()]
param(
    [string]$InstallerPath,
    [string]$ConfigSourcePath,
    [string]$OutputDir,
    [string]$SourceRoot
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
        [string]$InstallerFileName
    )

    $content = @"
`$ErrorActionPreference = 'Stop'

function Get-InstalledWmsLocation {
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
`$logPath = Join-Path `$env:TEMP ('install-wms-tags-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')

Start-Process -FilePath `$installerPath -ArgumentList @('/log', `$logPath) -Wait
`$installDir = Get-InstalledWmsLocation
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `$configInstaller -InstallDir `$installDir -SkipVerify
Start-Process -FilePath (Join-Path `$installDir 'WMS Pallet Tag System.exe') -ErrorAction SilentlyContinue | Out-Null
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
Copy-Item -LiteralPath $resolvedInstallerPath -Destination (Join-Path $stageDir $installerFileName) -Force

$supportScriptPath = Join-Path $OutputDir "Install-Tropicana-Config.ps1"
New-EmbeddedSupportScript -TemplatePath (Join-Path $SourceRoot "scripts\Install-Tropicana-Config.ps1") `
    -DestinationPath $supportScriptPath `
    -ConfigContent $configContent
Copy-Item -LiteralPath $supportScriptPath -Destination (Join-Path $stageDir "Install-Tropicana-Config.ps1") -Force

New-BootstrapScript -DestinationPath (Join-Path $stageDir "bootstrap-install.ps1") -InstallerFileName $installerFileName
New-BootstrapCmd -DestinationPath (Join-Path $stageDir "bootstrap.cmd")

$targetName = Join-Path $OutputDir "WMS Pallet Tag System - Tropicana Setup.exe"
Remove-Item -LiteralPath $targetName -Force -ErrorAction SilentlyContinue
$sedPath = Join-Path $stageDir "tropicana-bootstrap.sed"
$files = @(
    $installerFileName,
    'Install-Tropicana-Config.ps1',
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
