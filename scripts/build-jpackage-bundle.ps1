[CmdletBinding()]
param(
    [ValidateSet("none", "exe", "msi")]
    [string]$InstallerType = "none",
    [ValidateSet("none", "signtool")]
    [string]$SigningMode = "none",
    [string]$BundleDir,
    [string]$BundleVersionLabel,
    [string]$JarPath,
    [string]$SourceRoot,
    [string]$RuntimeImage,
    [string]$AppVersion,
    [string]$AppName,
    [string]$InstallDirName,
    [string]$WinUpgradeUuid,
    [string]$SignToolPath,
    [string]$CertificateThumbprint,
    [string]$CertificateSubjectName,
    [string]$CertificatePath,
    [string]$CertificatePassword,
    [string]$TimestampUrl,
    [string]$FileDigestAlgorithm = "SHA256",
    [string]$TimestampDigestAlgorithm = "SHA256",
    [string[]]$AdditionalSignToolArgs,
    [switch]$SystemWideInstall
)

$ErrorActionPreference = "Stop"

function Get-VersionText {
    param(
        [string]$SourceRoot,
        [string]$JarPath
    )

    $pomProperties = Join-Path $SourceRoot "cli\target\maven-archiver\pom.properties"
    if (Test-Path -LiteralPath $pomProperties) {
        $versionLine = Get-Content -LiteralPath $pomProperties | Where-Object { $_ -like "version=*" } | Select-Object -First 1
        if ($versionLine) {
            return ($versionLine -replace "^version=", "").Trim()
        }
    }

    $jarName = [System.IO.Path]::GetFileNameWithoutExtension($JarPath)
    if ($jarName -match '^cli-(?<version>.+)$') {
        return $Matches["version"]
    }

    $pomXml = Join-Path $SourceRoot "pom.xml"
    if (Test-Path -LiteralPath $pomXml) {
        $match = Select-String -Path $pomXml -Pattern '<version>([^<]+)</version>' | Select-Object -First 1
        if ($match) {
            return $match.Matches[0].Groups[1].Value.Trim()
        }
    }

    throw "Could not resolve application version."
}

function Resolve-JpackageHome {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\jpackage.exe"))) {
        return $env:JAVA_HOME
    }

    $jpackageCmd = Get-Command jpackage -ErrorAction SilentlyContinue
    if (-not $jpackageCmd -or -not $jpackageCmd.Source) {
        throw "jpackage not found. Install a JDK with jpackage support or set JAVA_HOME."
    }

    $javaBin = Split-Path -Parent $jpackageCmd.Source
    $javaHome = Split-Path -Parent $javaBin
    if (-not (Test-Path -LiteralPath (Join-Path $javaHome "bin\jpackage.exe"))) {
        throw "Could not resolve JAVA_HOME from jpackage path: $($jpackageCmd.Source)"
    }
    return $javaHome
}

function Resolve-SignTool {
    param([string]$RequestedPath)

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        if (-not (Test-Path -LiteralPath $RequestedPath)) {
            throw "SignTool not found: $RequestedPath"
        }
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }

    $command = Get-Command signtool.exe -ErrorAction SilentlyContinue
    if ($command -and $command.Source) {
        return $command.Source
    }

    $sdkCandidates = @(
        (Join-Path ${env:ProgramFiles(x86)} "Windows Kits\10\bin"),
        (Join-Path ${env:ProgramFiles} "Windows Kits\10\bin")
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    foreach ($sdkRoot in $sdkCandidates) {
        $match = Get-ChildItem -Path $sdkRoot -Recurse -Filter signtool.exe -File -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($match) {
            return $match.FullName
        }
    }

    throw "signtool.exe not found. Install Windows SDK SignTool or pass -SignToolPath."
}

function Get-SignableAppPaths {
    param(
        [Parameter(Mandatory)]
        [string]$BundleDir,
        [Parameter(Mandatory)]
        [string]$AppName
    )

    $candidates = @(
        (Join-Path $BundleDir "$AppName.exe"),
        (Join-Path $BundleDir "app\$AppName.exe")
    )

    return $candidates |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -Unique
}

function Invoke-SignToolSignature {
    param(
        [Parameter(Mandatory)]
        [string]$SignTool,
        [Parameter(Mandatory)]
        [string]$TargetPath,
        [string]$CertificateThumbprint,
        [string]$CertificateSubjectName,
        [string]$CertificatePath,
        [string]$CertificatePassword,
        [string]$TimestampUrl,
        [string]$FileDigestAlgorithm,
        [string]$TimestampDigestAlgorithm,
        [string[]]$AdditionalArgs
    )

    $arguments = @(
        'sign',
        '/fd', $FileDigestAlgorithm
    )

    if (-not [string]::IsNullOrWhiteSpace($TimestampUrl)) {
        $arguments += @('/tr', $TimestampUrl, '/td', $TimestampDigestAlgorithm)
    }

    if (-not [string]::IsNullOrWhiteSpace($CertificateThumbprint)) {
        $arguments += @('/sha1', $CertificateThumbprint)
    } elseif (-not [string]::IsNullOrWhiteSpace($CertificateSubjectName)) {
        $arguments += @('/n', $CertificateSubjectName)
    } elseif (-not [string]::IsNullOrWhiteSpace($CertificatePath)) {
        $arguments += @('/f', $CertificatePath)
        if (-not [string]::IsNullOrWhiteSpace($CertificatePassword)) {
            $arguments += @('/p', $CertificatePassword)
        }
    } elseif (-not $AdditionalArgs) {
        throw "SigningMode 'signtool' requires certificate info (-CertificateThumbprint, -CertificateSubjectName, or -CertificatePath) or trusted-signing style -AdditionalSignToolArgs."
    }

    if ($AdditionalArgs) {
        $arguments += $AdditionalArgs
    }

    $arguments += $TargetPath

    & $SignTool @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "SignTool signing failed for $TargetPath with exit code $LASTEXITCODE."
    }
}

function Invoke-CodeSigning {
    param(
        [Parameter(Mandatory)]
        [string]$TargetPath,
        [Parameter(Mandatory)]
        [string]$SigningMode,
        [string]$SignTool,
        [string]$CertificateThumbprint,
        [string]$CertificateSubjectName,
        [string]$CertificatePath,
        [string]$CertificatePassword,
        [string]$TimestampUrl,
        [string]$FileDigestAlgorithm,
        [string]$TimestampDigestAlgorithm,
        [string[]]$AdditionalSignToolArgs
    )

    if ($SigningMode -eq 'none') {
        return
    }

    switch ($SigningMode) {
        'signtool' {
            Invoke-SignToolSignature `
                -SignTool $SignTool `
                -TargetPath $TargetPath `
                -CertificateThumbprint $CertificateThumbprint `
                -CertificateSubjectName $CertificateSubjectName `
                -CertificatePath $CertificatePath `
                -CertificatePassword $CertificatePassword `
                -TimestampUrl $TimestampUrl `
                -FileDigestAlgorithm $FileDigestAlgorithm `
                -TimestampDigestAlgorithm $TimestampDigestAlgorithm `
                -AdditionalArgs $AdditionalSignToolArgs
            break
        }
        default {
            throw "Unsupported signing mode: $SigningMode"
        }
    }
}

function Write-LauncherScript {
    param(
        [string]$Path,
        [string]$ExtraArgs,
        [string]$AppExecutableName
    )

    $content = @(
        '@echo off'
        'setlocal'
        'set "APP_HOME=%~dp0"'
        'if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"'
        'pushd "%APP_HOME%" >nul'
        ('set "APP_EXE=%APP_HOME%\{0}"' -f $AppExecutableName)
        'set "JAVA_EXE=%APP_HOME%\runtime\bin\java.exe"'
        'set "JAR_FILE=%APP_HOME%\wms-tags.jar"'
        'if not exist "%JAR_FILE%" set "JAR_FILE=%APP_HOME%\app\wms-tags.jar"'
        'if exist "%APP_EXE%" ('
        ('  "%APP_EXE%" ' + $ExtraArgs + ' %*').Trim()
        '  set EXITCODE=%ERRORLEVEL%'
        '  popd >nul'
        '  endlocal & exit /b %EXITCODE%'
        ')'
        'if not exist "%JAVA_EXE%" ('
        '  echo ERROR: Bundled launcher not found at "%APP_EXE%" and runtime not found at "%JAVA_EXE%".'
        '  popd >nul'
        '  exit /b 1'
        ')'
        'if not exist "%JAR_FILE%" ('
        '  echo ERROR: Jar not found at "%JAR_FILE%".'
        '  popd >nul'
        '  exit /b 1'
        ')'
        ('"%JAVA_EXE%" -Dwms.app.home="%APP_HOME%" -jar "%JAR_FILE%" ' + $ExtraArgs + ' %*').Trim()
        'set EXITCODE=%ERRORLEVEL%'
        'popd >nul'
        'endlocal & exit /b %EXITCODE%'
    ) -join "`r`n"
    Set-Content -LiteralPath $Path -Value $content -Encoding ASCII
}

$scriptRoot = Split-Path -Parent $PSCommandPath
if (-not $SourceRoot) {
    $SourceRoot = Split-Path -Parent $scriptRoot
}
$iconPath = Join-Path $scriptRoot "assets\orange-slice.ico"

if (-not $JarPath) {
    $jarCandidate = Get-ChildItem -Path (Join-Path $SourceRoot "cli\target") -Filter "cli-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*original*" -and $_.Name -notlike "*shaded*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($jarCandidate) {
        $JarPath = $jarCandidate.FullName
    }
}

if ([string]::IsNullOrWhiteSpace($JarPath) -or -not (Test-Path -LiteralPath $JarPath)) {
    throw "Jar not found: $JarPath. Build first with .\mvnw.cmd -pl cli -am -Dmaven.test.skip=true package"
}

$resolvedVersion = Get-VersionText -SourceRoot $SourceRoot -JarPath $JarPath
$version = if ($AppVersion) { $AppVersion.Trim() } else { $resolvedVersion }
if (-not $BundleVersionLabel) {
    $BundleVersionLabel = $resolvedVersion
}
$javaHome = Resolve-JpackageHome
$resolvedSignTool = $null
if ($SigningMode -eq 'signtool') {
    $resolvedSignTool = Resolve-SignTool -RequestedPath $SignToolPath
}
$appName = if ([string]::IsNullOrWhiteSpace($AppName)) { "WMS Pallet Tag System" } else { $AppName.Trim() }
$installDirName = if ([string]::IsNullOrWhiteSpace($InstallDirName)) { "WMS-Pallet-Tag-System" } else { $InstallDirName.Trim() }
$winUpgradeUuid = if ([string]::IsNullOrWhiteSpace($WinUpgradeUuid)) {
    "0d6f4c87-1ec5-4f65-a9d3-4f7a0d4f4d4f"
} else {
    $WinUpgradeUuid.Trim()
}

if (-not $BundleDir) {
    $BundleDir = Join-Path $SourceRoot "dist\wms-pallet-tag-system-$BundleVersionLabel-app"
}

$distDir = Split-Path -Parent $BundleDir
$bundleLeaf = Split-Path -Leaf $BundleDir
$workRoot = Join-Path $distDir "jpackage-work"
$inputDir = Join-Path $workRoot "input"
$destDir = Join-Path $workRoot "dest"

Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
New-Item -ItemType Directory -Path $destDir -Force | Out-Null

Copy-Item -LiteralPath $JarPath -Destination (Join-Path $inputDir "wms-tags.jar") -Force

$jpackageArgs = @(
    '--type', 'app-image',
    '--name', $appName,
    '--dest', $destDir,
    '--input', $inputDir,
    '--main-jar', 'wms-tags.jar',
    '--main-class', 'com.tbg.wms.cli.CliMain',
    '--app-version', $version,
    '--vendor', 'Tropicana Brands Group',
    '--java-options', '-Dwms.app.home=$APPDIR\..'
)
if (Test-Path -LiteralPath $iconPath) {
    $jpackageArgs += @('--icon', (Resolve-Path -LiteralPath $iconPath).Path)
}
if ($RuntimeImage) {
    $resolvedRuntimeImage = (Resolve-Path -LiteralPath $RuntimeImage).Path
    $jpackageArgs += @('--runtime-image', $resolvedRuntimeImage)
}

& (Join-Path $javaHome "bin\jpackage.exe") @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage app-image build failed with exit code $LASTEXITCODE."
}

$builtAppDir = Join-Path $destDir $appName
if (-not (Test-Path -LiteralPath $builtAppDir)) {
    throw "Expected app-image output not found: $builtAppDir"
}

Remove-Item -LiteralPath $BundleDir -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item -LiteralPath $builtAppDir -Destination $BundleDir -Recurse -Force
Remove-Item -LiteralPath $builtAppDir -Recurse -Force -ErrorAction SilentlyContinue

$signedArtifacts = [System.Collections.Generic.List[string]]::new()
foreach ($signablePath in (Get-SignableAppPaths -BundleDir $BundleDir -AppName $appName)) {
    Invoke-CodeSigning `
        -TargetPath $signablePath `
        -SigningMode $SigningMode `
        -SignTool $resolvedSignTool `
        -CertificateThumbprint $CertificateThumbprint `
        -CertificateSubjectName $CertificateSubjectName `
        -CertificatePath $CertificatePath `
        -CertificatePassword $CertificatePassword `
        -TimestampUrl $TimestampUrl `
        -FileDigestAlgorithm $FileDigestAlgorithm `
        -TimestampDigestAlgorithm $TimestampDigestAlgorithm `
        -AdditionalSignToolArgs $AdditionalSignToolArgs
    if ($SigningMode -ne 'none') {
        $signedArtifacts.Add($signablePath)
    }
}

New-Item -ItemType Directory -Path (Join-Path $BundleDir "config\TBG3002") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "config\templates") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "scripts") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "out") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "logs") -Force | Out-Null

$bundleEnvPath = Join-Path $BundleDir "wms-tags.env"
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\wms-tags.env.example") -Destination $bundleEnvPath -Force

Copy-Item -LiteralPath (Join-Path $SourceRoot "config\TBG3002\printers.yaml") -Destination (Join-Path $BundleDir "config\TBG3002\printers.yaml") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\TBG3002\printer-routing.yaml") -Destination (Join-Path $BundleDir "config\TBG3002\printer-routing.yaml") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\walmart-sku-matrix.csv") -Destination (Join-Path $BundleDir "config\walmart-sku-matrix.csv") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\walm_loc_num_matrix.csv") -Destination (Join-Path $BundleDir "config\walm_loc_num_matrix.csv") -Force
Copy-Item -Path (Join-Path $SourceRoot "config\templates\*") -Destination (Join-Path $BundleDir "config\templates") -Recurse -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\verify-wms-tags.ps1") -Destination (Join-Path $BundleDir "scripts\verify-wms-tags.ps1") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\verify-wms-tags.bat") -Destination (Join-Path $BundleDir "scripts\verify-wms-tags.bat") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\uninstall-wms-tags.ps1") -Destination (Join-Path $BundleDir "scripts\uninstall-wms-tags.ps1") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\uninstall-wms-tags.bat") -Destination (Join-Path $BundleDir "scripts\uninstall-wms-tags.bat") -Force

Write-LauncherScript -Path (Join-Path $BundleDir "run.bat") -ExtraArgs '' -AppExecutableName "$appName.exe"
Write-LauncherScript -Path (Join-Path $BundleDir "wms-tags-gui.bat") -ExtraArgs '' -AppExecutableName "$appName.exe"

$jarPathResolved = Join-Path $BundleDir "app\wms-tags.jar"
$jarHash = (Get-FileHash -LiteralPath $jarPathResolved -Algorithm SHA256).Hash
$manifest = [pscustomobject]@{
    BuiltAt = (Get-Date).ToString("s")
    BuiltBy = "$env:USERDOMAIN\$env:USERNAME"
    BuildComputer = $env:COMPUTERNAME
    JavaHomeSource = $javaHome
    JavaVersion = (& (Join-Path $javaHome "bin\java.exe") --version 2>&1 | Select-Object -First 1)
    JarSha256 = $jarHash
    AppImageDir = $BundleDir
    InstallerType = $InstallerType
    AppVersion = $version
    BundleVersionLabel = $BundleVersionLabel
    SigningMode = $SigningMode
    SignedArtifacts = $signedArtifacts
}
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $BundleDir "appimage-manifest.json") -Encoding UTF8

$installerPath = $null
if ($InstallerType -ne 'none') {
    $lightCmd = Get-Command light.exe -ErrorAction SilentlyContinue
    $candleCmd = Get-Command candle.exe -ErrorAction SilentlyContinue
    if (-not $lightCmd -or -not $candleCmd) {
        throw "WiX Toolset is required for -InstallerType $InstallerType. Install WiX v3+ and ensure light.exe and candle.exe are on PATH."
    }

    $installerDest = Join-Path $workRoot "installer"
    New-Item -ItemType Directory -Path $installerDest -Force | Out-Null
    $installerArgs = @(
        '--type', $InstallerType,
        '--name', $appName,
        '--dest', $installerDest,
        '--app-image', $BundleDir,
        '--app-version', $version,
        '--vendor', 'Tropicana Brands Group',
        '--win-upgrade-uuid', $winUpgradeUuid,
        '--install-dir', $installDirName,
        '--win-dir-chooser',
        '--win-menu',
        '--win-shortcut'
    )
    if (-not $SystemWideInstall) {
        $installerArgs += '--win-per-user-install'
    }

    & (Join-Path $javaHome "bin\jpackage.exe") @installerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage installer build failed with exit code $LASTEXITCODE."
    }

    $installerCandidate = Get-ChildItem -Path $installerDest -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($installerCandidate) {
        $installerExtension = [System.IO.Path]::GetExtension($installerCandidate.Name)
        $installerLeaf = "$appName-$BundleVersionLabel$installerExtension"
        $installerPath = Join-Path $distDir $installerLeaf
        Remove-Item -LiteralPath $installerPath -Force -ErrorAction SilentlyContinue
        Move-Item -LiteralPath $installerCandidate.FullName -Destination $installerPath
        Invoke-CodeSigning `
            -TargetPath $installerPath `
            -SigningMode $SigningMode `
            -SignTool $resolvedSignTool `
            -CertificateThumbprint $CertificateThumbprint `
            -CertificateSubjectName $CertificateSubjectName `
            -CertificatePath $CertificatePath `
            -CertificatePassword $CertificatePassword `
            -TimestampUrl $TimestampUrl `
            -FileDigestAlgorithm $FileDigestAlgorithm `
            -TimestampDigestAlgorithm $TimestampDigestAlgorithm `
            -AdditionalSignToolArgs $AdditionalSignToolArgs
        if ($SigningMode -ne 'none') {
            $signedArtifacts.Add($installerPath)
        }
        Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\install-wms-installer.ps1") -Destination (Join-Path $distDir "install-wms-installer.ps1") -Force
        Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\install-wms-installer.bat") -Destination (Join-Path $distDir "install-wms-installer.bat") -Force
        Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\uninstall-wms-tags.ps1") -Destination (Join-Path $distDir "uninstall-wms-tags.ps1") -Force
        Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\uninstall-wms-tags.bat") -Destination (Join-Path $distDir "uninstall-wms-tags.bat") -Force
        $installerSha256 = (Get-FileHash -LiteralPath $installerPath -Algorithm SHA256).Hash.ToLowerInvariant()
        Set-Content -LiteralPath ($installerPath + ".sha256") -Value "$installerSha256  $installerLeaf" -Encoding ASCII
    }
}

Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "jpackage build ready."
Write-Host "App image   : $BundleDir"
if ($installerPath) {
    Write-Host "Installer   : $installerPath"
} else {
    Write-Host "Installer   : not requested"
}
Write-Host "Config file : $bundleEnvPath"
Write-Host "GUI exe     : $(Join-Path $BundleDir "$appName.exe")"
Write-Host "CLI wrapper : $(Join-Path $BundleDir 'run.bat')"
if ($SigningMode -ne 'none') {
    Write-Host "Signing     : $SigningMode"
    Write-Host "SignTool    : $resolvedSignTool"
}
