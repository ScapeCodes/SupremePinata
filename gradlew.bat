@rem
@rem Lightweight Gradle bootstrap for SupremePinata development environments where the
@rem generated Gradle wrapper jar is not present. Downloads Gradle into .gradle/bootstrap
@rem and delegates all arguments to that Gradle installation.
@rem
@echo off
setlocal
set DIR=%~dp0
set GRADLE_VERSION=9.4.0
set BOOTSTRAP_DIR=%DIR%.gradle\bootstrap
set GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat

if not exist "%GRADLE_BIN%" (
  if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip'; $zip='%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip'; if (!(Test-Path $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip }; Expand-Archive -Path $zip -DestinationPath '%BOOTSTRAP_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_BIN%" %*
endlocal
