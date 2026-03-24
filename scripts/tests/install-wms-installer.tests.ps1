$ErrorActionPreference = 'Stop'
$env:WMS_INSTALLER_HELPER_TEST_MODE = '1'

. (Join-Path $PSScriptRoot '..\install-wms-installer.ps1')

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Equal {
    param(
        $Expected,
        $Actual,
        [string]$Message
    )
    if ($Expected -ne $Actual) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

try {
    Assert-Equal '1.7.2-rc1' (Get-InstallerVersion -Path 'C:\temp\WMS Pallet Tag System-1.7.2-rc1.exe') 'Prerelease installer version parsing failed.'
    Assert-Equal '1.7.2' (Get-InstallerVersion -Path 'C:\temp\WMS Pallet Tag System-1.7.2.exe') 'Stable installer version parsing failed.'
    Assert-True ((Compare-SemVerLike -LeftVersion '1.7.5' -RightVersion '1.7.6') -lt 0) 'Stable downgrade comparison should sort older versions first.'
    Assert-True ((Compare-SemVerLike -LeftVersion '1.7.6-rc1' -RightVersion '1.7.6') -lt 0) 'Prerelease should compare lower than the final stable release.'
    Assert-True ((Compare-SemVerLike -LeftVersion '1.7.6-rc2' -RightVersion '1.7.6-rc1') -gt 0) 'Later prerelease tags should compare higher than earlier prereleases.'
    Assert-True (Should-ReplaceExistingInstall -InstallerVersion '1.7.5' -InstalledVersion '1.7.6') 'Downgrade installs should trigger uninstall-first replacement.'
    Assert-True (Should-ReplaceExistingInstall -InstallerVersion '1.7.6' -InstalledVersion '1.7.6') 'Same-version reinstall should trigger uninstall-first replacement.'
    Assert-True (-not (Should-ReplaceExistingInstall -InstallerVersion '1.7.7' -InstalledVersion '1.7.6')) 'Upgrades should not force uninstall-first replacement.'

    Write-Host 'install-wms-installer tests passed.'
} finally {
    Remove-Item Env:WMS_INSTALLER_HELPER_TEST_MODE -ErrorAction SilentlyContinue
}
