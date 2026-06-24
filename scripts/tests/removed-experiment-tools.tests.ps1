[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$scriptRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)

$removedPaths = @(
    "gui\src\main\java\com\tbg\wms\cli\gui\analyzers",
    "gui\src\test\java\com\tbg\wms\cli\gui\analyzers",
    "gui\src\main\java\com\tbg\wms\cli\gui\sscc",
    "gui\src\test\java\com\tbg\wms\cli\gui\sscc",
    "core\src\main\java\com\tbg\wms\core\sscc",
    "core\src\test\java\com\tbg\wms\core\sscc"
)

foreach ($relativePath in $removedPaths) {
    Assert-True -Condition (-not (Test-Path -LiteralPath (Join-Path $sourceRoot $relativePath))) `
        -Message "Removed experiment path should not exist: $relativePath"
}

$runtimePaths = @(
    "gui\src\main\java",
    "core\src\main\java",
    "domain\src\main\java",
    "app\src\main\java",
    "oracle\src\main\java",
    "printing\src\main\java",
    "files\src\main\java",
    "cli\src\main\java",
    "desktop\src\main\java",
    "smoke\src\main\java"
)

$patterns = @(
    "AnalyzerDialog",
    "Daily Operations",
    "Open Loads",
    "All Dock Doors",
    "Unpicked Partials",
    "SsccLabel",
    "SSCC Labels",
    "core.sscc",
    "gui.sscc"
)

foreach ($runtimePath in $runtimePaths) {
    $absolutePath = Join-Path $sourceRoot $runtimePath
    if (-not (Test-Path -LiteralPath $absolutePath)) {
        continue
    }
    foreach ($pattern in $patterns) {
        $files = Get-ChildItem -LiteralPath $absolutePath -Recurse -File -ErrorAction SilentlyContinue
        $matches = $files | Select-String -Pattern $pattern -SimpleMatch -ErrorAction SilentlyContinue
        Assert-True -Condition ($null -eq $matches) -Message "Runtime path still references removed experiment '$pattern' in $runtimePath"
    }
}

$readme = Get-Content -LiteralPath (Join-Path $sourceRoot "README.md") -Raw
Assert-True -Condition ($readme.Contains("Analyzers were removed from the 2.0 track.")) -Message "README should document analyzer removal"
Assert-True -Condition ($readme.Contains("SSCC-specific tooling is deferred for a future redesign.")) -Message "README should document SSCC deferral"
Assert-True -Condition ($readme.Contains("Generic barcode tooling remains supported.")) -Message "README should document barcode support"

Write-Host "PASS: removed experiment tooling stays out of runtime paths and README documents the 2.0 scope"
