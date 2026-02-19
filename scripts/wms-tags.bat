@echo off
setlocal
set "APP_HOME=%~dp0"
set "RUN_BAT=%APP_HOME%run.bat"

if exist "%RUN_BAT%" (
  call "%RUN_BAT%" %*
  set EXITCODE=%ERRORLEVEL%
  endlocal & exit /b %EXITCODE%
)

echo ERROR: run.bat not found at "%RUN_BAT%".
echo Reinstall using the portable bundle package.
exit /b 1
