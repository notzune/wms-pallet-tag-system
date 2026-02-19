[CmdletBinding()]
param(
    [string]$BundleDir,
    [string]$JarPath,
    [string]$SourceRoot,
    [string]$RuntimeSource
)

$ErrorActionPreference = "Stop"

function Get-JavaInfo {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if (-not $cmd) {
        return [pscustomobject]@{
            Exists       = $false
            Version      = $null
            Major        = $null
            JavaExePath  = $null
            JavaHomePath = $null
        }
    }

    $javaExe = $cmd.Source
    $prevErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $versionOutput = (& java --version 2>&1) | Out-String
    } catch {
        $versionOutput = ""
    }
    $ErrorActionPreference = $prevErrorAction
    if (-not $versionOutput.Trim()) {
        $prevErrorAction = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $versionOutput = (& java -version 2>&1) | Out-String
        $ErrorActionPreference = $prevErrorAction
    }

    $match = [regex]::Match($versionOutput, '"(?<version>\d+(?:\.\d+){0,2}).*"')
    if (-not $match.Success) {
        $match = [regex]::Match($versionOutput, '(?im)(?:openjdk|java)\s+(?<version>\d+(?:\.\d+){0,2})')
    }

    if (-not $match.Success) {
        return [pscustomobject]@{
            Exists       = $true
            Version      = $versionOutput.Trim()
            Major        = $null
            JavaExePath  = $javaExe
            JavaHomePath = Split-Path -Parent (Split-Path -Parent $javaExe)
        }
    }

    $versionText = $match.Groups["version"].Value
    $major = [int]($versionText.Split(".")[0])
    return [pscustomobject]@{
        Exists       = $true
        Version      = $versionText
        Major        = $major
        JavaExePath  = $javaExe
        JavaHomePath = Split-Path -Parent (Split-Path -Parent $javaExe)
    }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
if (-not $SourceRoot) {
    $SourceRoot = Split-Path -Parent $scriptRoot
}

if (-not $BundleDir) {
    $BundleDir = Join-Path $SourceRoot "dist\wms-tags-portable"
}

if (-not $JarPath) {
    $JarPath = Join-Path $SourceRoot "cli\target\cli-1.2.1.jar"
}

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Jar not found: $JarPath. Build first with .\mvnw.cmd -DskipTests package"
}

if (-not $RuntimeSource) {
    $javaInfo = Get-JavaInfo
    if (-not $javaInfo.Exists) {
        throw "Java not found on build machine. Install Java 17+ or pass -RuntimeSource."
    }
    if (-not $javaInfo.Major -or $javaInfo.Major -lt 17) {
        throw "Java 17+ required on build machine. Found: $($javaInfo.Version). Use -RuntimeSource to override."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $javaInfo.JavaHomePath "bin\\java.exe"))) {
        throw "Could not resolve Java home from installed java.exe: $($javaInfo.JavaExePath)"
    }
} else {
    if (-not (Test-Path -LiteralPath $RuntimeSource)) {
        throw "RuntimeSource not found: $RuntimeSource"
    }
}

Write-Host "Creating portable bundle at: $BundleDir"
New-Item -ItemType Directory -Path $BundleDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "config\TBG3002") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "config\templates") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $BundleDir "scripts") -Force | Out-Null

Copy-Item -LiteralPath $JarPath -Destination (Join-Path $BundleDir "wms-tags.jar") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\wms-tags.env.example") -Destination (Join-Path $BundleDir "wms-tags.env") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\TBG3002\printers.yaml") -Destination (Join-Path $BundleDir "config\TBG3002\printers.yaml") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\TBG3002\printer-routing.yaml") -Destination (Join-Path $BundleDir "config\TBG3002\printer-routing.yaml") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "config\walmart-sku-matrix.csv") -Destination (Join-Path $BundleDir "config\walmart-sku-matrix.csv") -Force
Copy-Item -Path (Join-Path $SourceRoot "config\templates\*") -Destination (Join-Path $BundleDir "config\templates") -Recurse -Force

Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\wms-tags.bat") -Destination (Join-Path $BundleDir "wms-tags.bat") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\wms-tags-gui.bat") -Destination (Join-Path $BundleDir "wms-tags-gui.bat") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\verify-wms-tags.ps1") -Destination (Join-Path $BundleDir "scripts\verify-wms-tags.ps1") -Force
Copy-Item -LiteralPath (Join-Path $SourceRoot "scripts\verify-wms-tags.bat") -Destination (Join-Path $BundleDir "scripts\verify-wms-tags.bat") -Force

$runtimeTarget = Join-Path $BundleDir "runtime"
if (Test-Path -LiteralPath $runtimeTarget) {
    Remove-Item -LiteralPath $runtimeTarget -Recurse -Force
}

if ($RuntimeSource) {
    Write-Host "Copying bundled runtime from: $RuntimeSource"
    Copy-Item -LiteralPath $RuntimeSource -Destination $runtimeTarget -Recurse -Force
} else {
    Write-Host "Copying bundled runtime from: $($javaInfo.JavaHomePath)"
    Copy-Item -LiteralPath $javaInfo.JavaHomePath -Destination $runtimeTarget -Recurse -Force
}

$jarPathResolved = Join-Path $BundleDir "wms-tags.jar"
$jarHashCmd = Get-Command Get-FileHash -ErrorAction SilentlyContinue
if ($jarHashCmd) {
    $jarHash = (Get-FileHash -LiteralPath $jarPathResolved -Algorithm SHA256).Hash
} else {
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::OpenRead($jarPathResolved)
    try {
        $hashBytes = $sha256.ComputeHash($stream)
        $jarHash = [BitConverter]::ToString($hashBytes).Replace("-", "")
    } finally {
        $stream.Dispose()
        $sha256.Dispose()
    }
}
$manifest = [pscustomobject]@{
    BuiltAt = (Get-Date).ToString("s")
    BuiltBy = "$env:USERDOMAIN\$env:USERNAME"
    BuildComputer = $env:COMPUTERNAME
    JarSha256 = $jarHash
    JavaVersion = if ($RuntimeSource) { "runtime-source" } else { $javaInfo.Version }
    JavaHomeSource = if ($RuntimeSource) { $RuntimeSource } else { $javaInfo.JavaHomePath }
    BundleDir = $BundleDir
}
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $BundleDir "bundle-manifest.json") -Encoding UTF8

Write-Host ""
Write-Host "Portable bundle ready."
Write-Host "Bundle folder : $BundleDir"
Write-Host "Jar SHA256    : $jarHash"
Write-Host "Launch GUI    : $BundleDir\wms-tags-gui.bat"
Write-Host "Verify        : $BundleDir\scripts\verify-wms-tags.bat -ShipmentId <SHIP_ID>"
