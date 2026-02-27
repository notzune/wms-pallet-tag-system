[CmdletBinding()]
param(
    [string]$InstallDir = "C:\WMS-Pallet-Tag",
    [string]$JarPath,
    [string]$SourceRoot,
    [string]$RuntimePath
)

$ErrorActionPreference = "Stop"

function Test-IsAdministrator {
    $currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentIdentity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-JavaInfo {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if (-not $cmd) {
        return [pscustomobject]@{
            Exists  = $false
            Major   = $null
            Version = $null
        }
    }

    $versionOutput = (& java --version 2>&1) | Out-String
    if (-not $versionOutput.Trim()) {
        $versionOutput = (& java -version 2>&1) | Out-String
    }
    $match = [regex]::Match($versionOutput, '"(?<version>\d+(?:\.\d+){0,2}).*"')
    if (-not $match.Success) {
        $match = [regex]::Match($versionOutput, '(?im)(?:openjdk|java)\s+(?<version>\d+(?:\.\d+){0,2})')
    }
    if (-not $match.Success) {
        return [pscustomobject]@{
            Exists  = $true
            Major   = $null
            Version = $versionOutput.Trim()
        }
    }

    $versionText = $match.Groups["version"].Value
    $major = [int]($versionText.Split(".")[0])
    return [pscustomobject]@{
        Exists  = $true
        Major   = $major
        Version = $versionText
    }
}

function Install-Java21 {
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $winget) {
        throw "winget is not available. Install Java 21 manually, then rerun this script."
    }

    Write-Host "Installing Temurin Java 21 runtime with winget..."
    & winget install `
        --id EclipseAdoptium.Temurin.21.JRE `
        --exact `
        --accept-package-agreements `
        --accept-source-agreements `
        --scope machine `
        --silent

    if ($LASTEXITCODE -ne 0) {
        throw "winget failed to install Java 21 runtime (exit code $LASTEXITCODE)."
    }
}

if (-not (Test-IsAdministrator)) {
    throw "This setup script must be run as Administrator."
}

$scriptRoot = Split-Path -Parent $PSCommandPath
if (-not $SourceRoot) {
    $SourceRoot = Split-Path -Parent $scriptRoot
}

if (-not (Test-Path -LiteralPath $SourceRoot)) {
    throw "SourceRoot does not exist: $SourceRoot"
}

if (-not $JarPath) {
    $jarCandidate = Join-Path $SourceRoot "cli\target\cli-1.3.1.jar"
    if (Test-Path -LiteralPath $jarCandidate) {
        $JarPath = $jarCandidate
    } else {
        $fallback = Get-ChildItem -Path (Join-Path $SourceRoot "cli\target") -Filter "cli-*.jar" -File |
            Where-Object { $_.Name -notlike "*original*" -and $_.Name -notlike "*shaded*" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if (-not $fallback) {
            throw "No CLI jar found. Build first with .\mvnw.cmd -DskipTests package"
        }
        $JarPath = $fallback.FullName
    }
}

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Jar file not found: $JarPath"
}

$targetConfigDir = Join-Path $InstallDir "config"
$targetSiteDir = Join-Path $targetConfigDir "TBG3002"
$targetTemplateDir = Join-Path $targetConfigDir "templates"
$targetScriptsDir = Join-Path $InstallDir "scripts"
$targetRuntimeDir = Join-Path $InstallDir "runtime"

Write-Host "Creating install directories..."
New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
New-Item -ItemType Directory -Path $targetConfigDir -Force | Out-Null
New-Item -ItemType Directory -Path $targetSiteDir -Force | Out-Null
New-Item -ItemType Directory -Path $targetTemplateDir -Force | Out-Null
New-Item -ItemType Directory -Path $targetScriptsDir -Force | Out-Null
New-Item -ItemType Directory -Path $targetRuntimeDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $InstallDir "out") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $InstallDir "logs") -Force | Out-Null

if (-not $RuntimePath) {
    $sourceRuntime = Join-Path $SourceRoot "runtime"
    if (Test-Path -LiteralPath $sourceRuntime) {
        $RuntimePath = $sourceRuntime
    }
}

if ($RuntimePath -and -not (Test-Path -LiteralPath (Join-Path $RuntimePath "bin\java.exe"))) {
    throw "Runtime path is invalid (missing bin\java.exe): $RuntimePath"
}

if ($RuntimePath) {
    Write-Host "Using bundled runtime from: $RuntimePath"
    Remove-Item -LiteralPath $targetRuntimeDir -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item -LiteralPath $RuntimePath -Destination $targetRuntimeDir -Recurse -Force
    $javaInfo = [pscustomobject]@{ Version = "Bundled runtime"; Major = 21; Exists = $true }
} else {
    Write-Host "Checking Java runtime..."
    $javaInfo = Get-JavaInfo
    if (-not $javaInfo.Exists -or -not $javaInfo.Major -or $javaInfo.Major -lt 21) {
        Install-Java21
        $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
        $javaInfo = Get-JavaInfo
    }

    if (-not $javaInfo.Exists -or -not $javaInfo.Major -or $javaInfo.Major -lt 21) {
        throw "Java 21+ is required but was not detected after install."
    }

    Write-Host "Java version detected: $($javaInfo.Version)"
}

Write-Host "Copying application files..."
Copy-Item -LiteralPath $JarPath -Destination (Join-Path $InstallDir "wms-tags.jar") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\TBG3002\printers.yaml") -Destination (Join-Path $targetSiteDir "printers.yaml") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\TBG3002\printer-routing.yaml") -Destination (Join-Path $targetSiteDir "printer-routing.yaml") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\walmart-sku-matrix.csv") -Destination (Join-Path $targetConfigDir "walmart-sku-matrix.csv") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\walm_loc_num_matrix.csv") -Destination (Join-Path $targetConfigDir "walm_loc_num_matrix.csv") -Force
Copy-Item -Path (Join-Path $SourceRoot "config\templates\*") -Destination $targetTemplateDir -Recurse -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\verify-wms-tags.ps1") -Destination (Join-Path $targetScriptsDir "verify-wms-tags.ps1") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\verify-wms-tags.bat") -Destination (Join-Path $targetScriptsDir "verify-wms-tags.bat") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\run.bat") -Destination (Join-Path $InstallDir "run.bat") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\wms-tags-gui.bat") -Destination (Join-Path $InstallDir "wms-tags-gui.bat") -Force

$legacyLauncher = Join-Path $InstallDir "wms-tags.bat"
if (Test-Path -LiteralPath $legacyLauncher) {
    Remove-Item -LiteralPath $legacyLauncher -Force
}

$envDest = Join-Path $InstallDir "wms-tags.env"
if (-not (Test-Path -LiteralPath $envDest)) {
    Copy-Item -LiteralPath (Join-Path $SourceRoot "config\wms-tags.env.example") -Destination $envDest -Force
    Write-Host "Created template env file: $envDest"
    Write-Host "Update ORACLE_PASSWORD and any site values before production use."
} else {
    Write-Host "Existing env file kept: $envDest"
}

$jarHash = (Get-FileHash -LiteralPath (Join-Path $InstallDir "wms-tags.jar") -Algorithm SHA256).Hash
$manifest = [pscustomobject]@{
    InstalledAt = (Get-Date).ToString("s")
    ComputerName = $env:COMPUTERNAME
    InstalledBy = "$env:USERDOMAIN\$env:USERNAME"
    JavaVersion = $javaInfo.Version
    RuntimeBundled = [bool]$RuntimePath
    InstallDir = $InstallDir
    JarPath = (Join-Path $InstallDir "wms-tags.jar")
    JarSha256 = $jarHash
}

$manifestPath = Join-Path $InstallDir "install-manifest.json"
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

Write-Host ""
Write-Host "Setup complete."
Write-Host "Install dir : $InstallDir"
Write-Host "Jar SHA256  : $jarHash"
Write-Host "Manifest    : $manifestPath"
Write-Host ""
Write-Host "Next: edit $envDest, then run verify script as a regular user."
