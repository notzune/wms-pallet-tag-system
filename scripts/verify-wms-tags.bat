@echo off
setlocal
powershell -NoProfile -File "%~dp0verify-wms-tags.ps1" %*
set EXITCODE=%ERRORLEVEL%
endlocal & exit /b %EXITCODE%
