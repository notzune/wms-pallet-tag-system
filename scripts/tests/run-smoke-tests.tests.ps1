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
$manifestPath = Join-Path $sourceRoot "scripts\smoke\smoke-manifest.json"

$manifestJson = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

Assert-True -Condition ($null -ne $manifestJson) -Message "Smoke manifest should deserialize"
Assert-True -Condition ($null -ne $manifestJson.scenarios) -Message "Smoke manifest should define scenarios"
Assert-True -Condition ($manifestJson.scenarios.Count -gt 0) -Message "Smoke manifest should define at least one scenario"

$scenario = $manifestJson.scenarios[0]
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($scenario.name)) -Message "Smoke scenario should include a name"
Assert-True -Condition ($scenario.modes -contains "repo") -Message "Smoke scenario should declare repo mode support"
Assert-True -Condition ($scenario.modes -contains "packaged") -Message "Smoke scenario should declare packaged mode support"
Assert-True -Condition (-not [string]::IsNullOrWhiteSpace($scenario.command)) -Message "Smoke scenario should declare a command"
Assert-True -Condition ($null -ne $scenario.expectedExitCode) -Message "Smoke scenario should declare an expected exit code"

Write-Host "PASS: run-smoke-tests manifest contains required schema"
