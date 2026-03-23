@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
powershell -File "%SCRIPT_DIR%uninstall-wms-tags.ps1" %*
set EXITCODE=%ERRORLEVEL%
endlocal & exit /b %EXITCODE%
