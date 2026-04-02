[CmdletBinding()]
param(
    [string]$ConfigSourcePath,
    [string]$OutputDir,
    [string]$SourceRoot,
    [string]$JarPath,
    [string]$RuntimeSource
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

function Resolve-VersionText {
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

    if ($JarPath) {
        $jarName = [System.IO.Path]::GetFileNameWithoutExtension($JarPath)
        if ($jarName -match '^cli-(?<version>.+)$') {
            return $Matches["version"]
        }
    }

    $pomXml = Join-Path $SourceRoot "pom.xml"
    if (Test-Path -LiteralPath $pomXml) {
        $match = Select-String -Path $pomXml -Pattern '<version>([^<]+)</version>' | Select-Object -First 1
        if ($match) {
            return $match.Matches[0].Groups[1].Value.Trim()
        }
    }

    throw "Could not resolve application version for Tropicana portable bundle."
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
$version = Resolve-VersionText -SourceRoot $SourceRoot -JarPath $JarPath
$bundleDir = Join-Path $OutputDir ("wms-pallet-tag-system-{0}-portable-tropicana" -f $version)

& (Join-Path $SourceRoot "scripts\build-portable-bundle.ps1") `
    -BundleDir $bundleDir `
    -JarPath $JarPath `
    -SourceRoot $SourceRoot `
    -RuntimeSource $RuntimeSource `
    -RootConfigSourcePath $resolvedConfigSource `
    -ForceBundleLocalConfig

Write-Host "Tropicana portable bundle ready: $bundleDir"
Write-Host "Tropicana portable ZIP: $bundleDir.zip"
