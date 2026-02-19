@echo off
setlocal
set "APP_HOME=%~dp0"
set "JAVA_EXE=%APP_HOME%runtime\bin\java.exe"
set "JAR_FILE=%APP_HOME%wms-tags.jar"

if not exist "%JAVA_EXE%" (
  echo ERROR: Bundled runtime not found at "%JAVA_EXE%".
  echo Reinstall using the portable bundle package.
  exit /b 1
)

if not exist "%JAR_FILE%" (
  echo ERROR: Jar not found at "%JAR_FILE%".
  echo Reinstall using the portable bundle package.
  exit /b 1
)

"%JAVA_EXE%" -jar "%JAR_FILE%" %*
set EXITCODE=%ERRORLEVEL%
endlocal & exit /b %EXITCODE%
