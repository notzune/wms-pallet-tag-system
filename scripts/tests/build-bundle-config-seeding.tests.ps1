[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)]
        $Expected,
        [Parameter(Mandatory = $true)]
        $Actual,
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $normalizedExpected = ($Expected -replace "\r\n", "`n").Trim()
    $normalizedActual = ($Actual -replace "\r\n", "`n").Trim()

    if ($normalizedExpected -ne $normalizedActual) {
        throw "$Message`nExpected: $normalizedExpected`nActual:   $normalizedActual"
    }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("wms-bundle-seeding-" + [guid]::NewGuid().ToString("N"))
$fixtureSourceRoot = Join-Path $tempRoot "fixture-source"
$portableBundleDir = Join-Path $tempRoot "portable"
$jpackageBundleDir = Join-Path $tempRoot "app-image"
$jarPath = Get-ChildItem -Path (Join-Path $sourceRoot "cli\target") -Filter "cli-*.jar" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*original*" -and $_.Name -notlike "*shaded*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jarPath) {
    & (Join-Path $sourceRoot "mvnw.cmd") -q -pl cli -am -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to build CLI jar required for bundle seeding test."
    }
    $jarPath = Get-ChildItem -Path (Join-Path $sourceRoot "cli\target") -Filter "cli-*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*original*" -and $_.Name -notlike "*shaded*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

if (-not $jarPath) {
    throw "No CLI jar found under cli\target after building the project."
}

$expectedEnv = @"
ACTIVE_SITE=TEMPLATE
ORACLE_USERNAME=template_user
ORACLE_PASSWORD=template_pass
"@

try {
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $fixtureSourceRoot "config\TBG3002") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $fixtureSourceRoot "config\templates") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $fixtureSourceRoot "scripts") -Force | Out-Null

    Set-Content -LiteralPath (Join-Path $fixtureSourceRoot ".env") -Value @"
ACTIVE_SITE=LIVE
ORACLE_USERNAME=live_user
ORACLE_PASSWORD=live_pass
"@ -Encoding ASCII
    Set-Content -LiteralPath (Join-Path $fixtureSourceRoot "config\wms-tags.env.example") -Value $expectedEnv -Encoding ASCII

    Copy-Item -LiteralPath (Join-Path $sourceRoot "config\TBG3002\printers.yaml") -Destination (Join-Path $fixtureSourceRoot "config\TBG3002\printers.yaml") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "config\TBG3002\printer-routing.yaml") -Destination (Join-Path $fixtureSourceRoot "config\TBG3002\printer-routing.yaml") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "config\walmart-sku-matrix.csv") -Destination (Join-Path $fixtureSourceRoot "config\walmart-sku-matrix.csv") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "config\walm_loc_num_matrix.csv") -Destination (Join-Path $fixtureSourceRoot "config\walm_loc_num_matrix.csv") -Force
    Copy-Item -Path (Join-Path $sourceRoot "config\templates\*") -Destination (Join-Path $fixtureSourceRoot "config\templates") -Recurse -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\run.bat") -Destination (Join-Path $fixtureSourceRoot "scripts\run.bat") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\wms-tags-gui.bat") -Destination (Join-Path $fixtureSourceRoot "scripts\wms-tags-gui.bat") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\verify-wms-tags.ps1") -Destination (Join-Path $fixtureSourceRoot "scripts\verify-wms-tags.ps1") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\verify-wms-tags.bat") -Destination (Join-Path $fixtureSourceRoot "scripts\verify-wms-tags.bat") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\install-wms-installer.ps1") -Destination (Join-Path $fixtureSourceRoot "scripts\install-wms-installer.ps1") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\install-wms-installer.bat") -Destination (Join-Path $fixtureSourceRoot "scripts\install-wms-installer.bat") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\uninstall-wms-tags.ps1") -Destination (Join-Path $fixtureSourceRoot "scripts\uninstall-wms-tags.ps1") -Force
    Copy-Item -LiteralPath (Join-Path $sourceRoot "scripts\uninstall-wms-tags.bat") -Destination (Join-Path $fixtureSourceRoot "scripts\uninstall-wms-tags.bat") -Force

    & (Join-Path $sourceRoot "scripts\build-portable-bundle.ps1") `
        -SourceRoot $fixtureSourceRoot `
        -BundleDir $portableBundleDir `
        -JarPath $jarPath.FullName

    $portableEnv = Get-Content -LiteralPath (Join-Path $portableBundleDir "wms-tags.env") -Raw
    Assert-Equal -Expected $expectedEnv -Actual $portableEnv -Message "Portable bundle should seed template env"

    $jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($jpackageCommand) {
        & (Join-Path $sourceRoot "scripts\build-jpackage-bundle.ps1") `
            -SourceRoot $fixtureSourceRoot `
            -BundleDir $jpackageBundleDir `
            -JarPath $jarPath.FullName

        $jpackageEnv = Get-Content -LiteralPath (Join-Path $jpackageBundleDir "wms-tags.env") -Raw
        Assert-Equal -Expected $expectedEnv -Actual $jpackageEnv -Message "jpackage app image should seed template env"
    } else {
        Write-Host "Skipping jpackage env seeding assertion because jpackage.exe is not available."
    }

    Write-Host "PASS: public bundle config seeding uses config\wms-tags.env.example"
} finally {
    Remove-Item -LiteralPath $fixtureSourceRoot -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $portableBundleDir -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $jpackageBundleDir -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
