@echo off
REM #####################################################################
REM This script will build the trustagent system tray on windows platform
REM #####################################################################
setlocal enabledelayedexpansion

set me=%~n0
set pwd=%~dp0
set "system_tray_home=%pwd%"

set VsDevCmd="C:\Program Files (x86)\Microsoft Visual Studio\2017\Professional\Common7\Tools\VsDevCmd.bat"

call:system_tray
GOTO:EOF

:system_tray
  setlocal
  echo. Building trustagent system_tray....
  cd
  call %VsDevCmd%
  IF NOT %ERRORLEVEL% EQU 0 (
    echo. %me%: Visual Studio Dev Env could not be set
	call:ExitBatch
  )
  cd %system_tray_home%
  cd
  msbuild TrustAgentTray.sln
  IF NOT %ERRORLEVEL% EQU 0 (
	echo. %me%: Build Failed
	call:ExitBatch
	)
  copy  /Y "%system_tray_home%\TrustAgentTray\bin\Debug\TrustAgentTray.exe" "%system_tray_home%\..\nsis\"
  endlocal
GOTO:EOF

:ExitBatch - Cleanly exit batch processing, regardless how many CALLs
if not exist "%temp%\ExitBatchYes.txt" call :buildYes
call :CtrlC <"%temp%\ExitBatchYes.txt" 1>nul 2>&1
:CtrlC
cmd /c exit -1073741510

:buildYes - Establish a Yes file for the language used by the OS
pushd "%temp%"
set "yes="
copy nul ExitBatchYes.txt >nul
for /f "delims=(/ tokens=2" %%Y in (
  '"copy /-y nul ExitBatchYes.txt <nul"'
) do if not defined yes set "yes=%%Y"
echo %yes%>ExitBatchYes.txt
popd
exit /b

endlocal
