@echo off
setlocal
set "APP_HOME=%~dp0"
pushd "%APP_HOME%" >nul
set "APP_EXE=%APP_HOME%WMS Pallet Tag System.exe"
set "JAVA_EXE=%APP_HOME%runtime\bin\java.exe"
set "JAR_FILE=%APP_HOME%wms-tags.jar"
if not exist "%JAR_FILE%" set "JAR_FILE=%APP_HOME%app\wms-tags.jar"

if exist "%APP_EXE%" (
  "%APP_EXE%" %*
  set EXITCODE=%ERRORLEVEL%
  popd >nul
  endlocal & exit /b %EXITCODE%
)

if not exist "%JAVA_EXE%" (
  echo ERROR: Bundled launcher not found at "%APP_EXE%" and runtime not found at "%JAVA_EXE%".
  echo Reinstall using the portable bundle package.
  popd >nul
  exit /b 1
)

if not exist "%JAR_FILE%" (
  echo ERROR: Jar not found at "%JAR_FILE%".
  echo Reinstall using the portable bundle package.
  popd >nul
  exit /b 1
)

"%JAVA_EXE%" -Dwms.app.home="%APP_HOME%" -jar "%JAR_FILE%" %*
set EXITCODE=%ERRORLEVEL%
popd >nul
endlocal & exit /b %EXITCODE%
