@echo off
REM #####################################################################
REM This script build the trustagent service on windows platform
REM #####################################################################
setlocal enabledelayedexpansion

set me=%~n0
set pwd=%~dp0
set "service_home=%pwd%"

set VsDevCmd="C:\Program Files (x86)\Microsoft Visual Studio\2017\Professional\Common7\Tools\VsDevCmd.bat"

call:build_service
GOTO:EOF

:build_service
  setlocal
  echo. Building trustagent_service....
  cd
  call %VsDevCmd%
  IF NOT %ERRORLEVEL% EQU 0 (
    echo. %me%: Visual Studio Dev Env could not be set
	call:ExitBatch
  )
  cd %service_home%
  cd
  msbuild TrustAgent.sln
  IF NOT %ERRORLEVEL% EQU 0 (
    echo. %me%: Build Failed
    call:ExitBatch
  )
  copy  /Y "%service_home%\TrustAgent\bin\Debug\TrustAgent.exe" "%service_home%\..\nsis\"
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
