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

    $script:StatusLabel = [pscustomobject]@{ Text = '' }
    $script:ProgressBar = [pscustomobject]@{ Style = ''; Value = 0 }
    $script:CloseButton = [pscustomobject]@{ Visible = $false }
    $script:StatusForm = [pscustomobject]@{
        Visible = $false
        TopMost = $true
        ShowDialogCalls = 0
        ActivateCalls = 0
        CloseCalls = 0
    }
    $script:StatusForm | Add-Member -MemberType ScriptMethod -Name Activate -Value {
        $this.ActivateCalls++
    }
    $script:StatusForm | Add-Member -MemberType ScriptMethod -Name ShowDialog -Value {
        $this.ShowDialogCalls++
    }
    $script:StatusForm | Add-Member -MemberType ScriptMethod -Name Close -Value {
        $this.CloseCalls++
        $this.Visible = $false
    }

    Complete-StatusWindow -Message 'Installation failed.' -KeepOpen:$true

    Assert-Equal 'Installation failed.' $script:StatusLabel.Text 'Completion message was not propagated.'
    Assert-True $script:CloseButton.Visible 'Close button should be visible for failure state.'
    Assert-Equal 'Blocks' $script:ProgressBar.Style 'Progress bar should switch to block style on completion.'
    Assert-Equal 100 $script:ProgressBar.Value 'Progress bar should be completed.'
    Assert-Equal 0 $script:StatusForm.ShowDialogCalls 'Failure state should not reopen the form modally.'
    Assert-True ($script:StatusForm.ActivateCalls -ge 1) 'Failure state should activate the existing form.'

    Write-Host 'install-wms-installer tests passed.'
} finally {
    Remove-Item Env:WMS_INSTALLER_HELPER_TEST_MODE -ErrorAction SilentlyContinue
}
