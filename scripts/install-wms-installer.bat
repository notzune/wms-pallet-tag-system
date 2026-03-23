@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
powershell -File "%SCRIPT_DIR%install-wms-installer.ps1" %*
set EXITCODE=%ERRORLEVEL%
endlocal & exit /b %EXITCODE%
