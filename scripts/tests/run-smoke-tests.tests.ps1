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
$runnerPath = Join-Path $sourceRoot "scripts\run-smoke-tests.ps1"

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

$installerScenario = $manifestJson.scenarios | Where-Object { $_.name -eq "tropicana-package-install" } | Select-Object -First 1
Assert-True -Condition ($null -ne $installerScenario) -Message "Smoke manifest should define a Tropicana package installer scenario"
Assert-True -Condition ($installerScenario.modes -contains "packaged") -Message "Installer scenario should declare packaged mode support"
Assert-True -Condition ($installerScenario.releaseOnly -eq $true) -Message "Installer scenario should be marked release-only"
Assert-True -Condition ($installerScenario.kind -eq "tropicana-package-install") -Message "Installer scenario should use tropicana-package-install kind"

Assert-True -Condition (Test-Path -LiteralPath $runnerPath) -Message "Smoke runner script should exist"

$runnerContent = Get-Content -LiteralPath $runnerPath -Raw
Assert-True -Condition ($runnerContent.Contains("ValidateSet(""repo"", ""packaged"")")) -Message "Smoke runner should declare repo and packaged modes"
Assert-True -Condition ($runnerContent.Contains("[switch]`$IncludeInstallerScenarios")) -Message "Smoke runner should support opting into installer scenarios"
Assert-True -Condition ($runnerContent.Contains("releaseOnly")) -Message "Smoke runner should recognize release-only scenarios"
Assert-True -Condition ($runnerContent.Contains("tropicana-package-install")) -Message "Smoke runner should handle Tropicana package installer scenarios"
Assert-True -Condition ($runnerContent.Contains("Invoke-TropicanaPackageInstallScenario")) -Message "Smoke runner should route installer smoke through the Tropicana package helper"
Assert-True -Condition ($runnerContent.Contains("smoke-report.json")) -Message "Smoke runner should emit machine-readable smoke report"
Assert-True -Condition ($runnerContent.Contains("smoke-report.txt")) -Message "Smoke runner should emit text smoke report"

Write-Host "PASS: run-smoke-tests manifest and runner contain required smoke schema"
