@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1"
if errorlevel 1 (
  echo.
  echo Startup failed. Check the error output and *.err.log files.
  pause
)
endlocal
