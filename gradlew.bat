@echo off
setlocal
set ROOT_DIR=%~dp0
set GRADLE_VERSION=9.3.1
set CACHE_DIR=%USERPROFILE%\.gradle\neo-wrapper\%GRADLE_VERSION%
set DIST_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%
set ZIP=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%DIST_DIR%\bin\gradle.bat" (
  if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
  if not exist "%ZIP%" powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
  if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
  powershell -NoProfile -Command "Expand-Archive -Force '%ZIP%' '%CACHE_DIR%'"
)
call "%DIST_DIR%\bin\gradle.bat" %*
endlocal
