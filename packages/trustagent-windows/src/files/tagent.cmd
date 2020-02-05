@echo off
setlocal enabledelayedexpansion

set DESC=Trust Agent
set NAME=tagent

REM ###################################################################################################
REM #Set environment specific variables here 
REM ###################################################################################################

REM set the trustagent home directory
REM set TRUSTAGENT_HOME=C:\Program Files (x86)\Intel\trustagent
for %%i in ("%~dp0..") do set "parentfolder=%%~fi"
set TRUSTAGENT_HOME=%parentfolder%

set DAEMON=%TRUSTAGENT_HOME%\bin\%NAME%.cmd

set JAVA_HOME=%TRUSTAGENT_HOME%\jre
set JAVABIN=%JAVA_HOME%\bin\java

set TRUSTAGENT_CONF=%TRUSTAGENT_HOME%\configuration
set TRUSTAGENT_LOGS=%TRUSTAGENT_HOME%\logs
set TRUSTAGENT_LOGFILE=%TRUSTAGENT_LOGS%\trustagent.log
set TRUSTAGENT_PASSWORD_FILE=%TRUSTAGENT_CONF%\.trustagent_password
set TRUSTAGENT_JAVA=%TRUSTAGENT_HOME%\java
set TRUSTAGENT_BIN=%TRUSTAGENT_HOME%\bin
set TRUSTAGENT_ENV=%TRUSTAGENT_HOME%\env
set TRUSTAGENT_VAR=%TRUSTAGENT_HOME%\var
set TRUSTAGENT_PID_FILE=%TRUSTAGENT_VAR%\run\trustagent.pid
set TRUSTAGENT_PROPERTY_FILE=%TRUSTAGENT_CONF%\trustagent.properties
set TRUSTAGENT_HTTP_LOG_FILE=%TRUSTAGENT_LOGS%\http.log
set TRUSTAGENT_AUTHORIZE_TASKS=download-mtwilson-privacy-ca-certificate download-mtwilson-saml-certificate request-endorsement-certificate request-aik-certificate
set TRUSTAGENT_REGISTRATION_TASKS=attestation-registration
set TRUSTAGENT_TPM_TASKS=create-tpm-owner-secret create-tpm-srk-secret create-aik-secret take-ownership
set TRUSTAGENT_START_TASKS=secure-store jetty-tls-keystore take-ownership
set TRUSTAGENT_SETUP_TASKS=update-extensions-cache-file secure-store jetty-tls-keystore %TRUSTAGENT_TPM_TASKS% %TRUSTAGENT_AUTHORIZE_TASKS%
REM ECHO. ==Running tagent service==
REM # load environment variables (these may override the defaults set above)
if exist "%TRUSTAGENT_ENV%\" (
REM  TRUSTAGENT_ENV_FILES=$(ls -1 $TRUSTAGENT_ENV/*)
REM  for env_file in $TRUSTAGENT_ENV_FILES; do
REM    . $env_file
REM  done
  for /f  "delims=" %%a in ('dir "%TRUSTAGENT_ENV%" /b') do (
    echo. %%a
    REM cd "%TRUSTAGENT_ENV%"
    REM call %%a
  )
)

REM # not including configure-from-environment because we are running it always before the user-chosen tasks
REM # not including register-tpm-password because we are prompting for it in the setup.sh

REM # load stored master_password
IF not defined TRUSTAGENT_PASSWORD (
	IF exist "%TRUSTAGENT_CONF%\.trustagent_password" (
	  REM for /f "usebackq delims=" %%a in ("%TRUSTAGENT_CONF%\.trustagent_password") do (
	  REM  set TRUSTAGENT_PASSWORD=%%a
	  REM )
	  set /p TRUSTAGENT_PASSWORD=<"%TRUSTAGENT_CONF%\.trustagent_password"
	)
)

set JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.7}
set JAVA_OPTS=-Dlogback.configurationFile="%TRUSTAGENT_CONF%"\logback.xml -Dfs.name=trustagent -Djdk.tls.ephemeralDHKeySize=2048

REM @###################################################################################################

REM set TA_JARS=
REM # generated variables
REM for /f  "delims=" %%a in ('dir "%TRUSTAGENT_JAVA%" /s /b') do (
REM  set TA_JARS=%%a;!TA_JARS!
REM )
REM set CLASSPATH=%TA_JARS%
REM echo %CLASSPATH%

set CLASSPATH=%TRUSTAGENT_JAVA%\*
REM echo. %JAVA_HOME%

set TASTATUS=
REM parsing the command arguments
set wcommand=%1
set options=%2
set cmdparams=
for /f "usebackq tokens=1*" %%i in (`echo %*`) DO @ set cmdparams=%%j
REM echo. Running command: %wcommand% with %cmdparams%

if "%wcommand%"=="start" (
  call:trustagent_start
) ELSE IF "%wcommand%"=="stop" (
  call:trustagent_stop
) ELSE IF "%wcommand%"=="restart" (
  call:trustagent_restart
) ELSE IF "%wcommand%"=="status" (
  call:trustagent_status
) ELSE IF "%wcommand%"=="setup" (
  call:trustagent_setup %cmdparams%
) ELSE IF "%wcommand%"=="provision-attestation" (

  for /F "tokens=*" %%A in (%2) do (
    set %%A
  )

  call:trustagent_setup
  call:trustagent_restart

) ELSE IF "%wcommand%"=="authorize" (
  call:trustagent_authorize
) ELSE IF "%wcommand%"=="create-host" (
  call:trustagent_host_register
) ELSE IF "%wcommand%"=="start-http-server" (
  call:trustagent_start
) ELSE IF "%wcommand%"=="java-detect" (
  call:trustagent_java_detect
) ELSE IF "%wcommand%"=="zeroize" (
  call:trustagent_zeroize
) ELSE IF "%wcommand%"=="help" (
  call:print_help
) ELSE IF "%wcommand%"=="uninstall" (
  call:tagent_uninstall
) ELSE IF "%wcommand%"=="generate-password" (
  call:trustagent_generate_password
) ELSE IF "%wcommand%"=="export-config" (
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
  EXIT /B 0
) ELSE IF "%wcommand%"=="version" (
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
  EXIT /B 0
) ELSE (
  IF "%*"=="" (
    call:print_help
  ) ELSE (
    REM echo. Running command: %*
    "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
  )
)
EXIT /B !errorlevel!

REM functions
:trustagent_start
  echo. Starting trustagent service
  sc start trustagent >null
  echo. Trustagent started
EXIT /B !errorlevel!

:trustagent_status
  REM set TASTATUS=
  call :get_status
  echo. Trustagent status: %TASTATUS%
EXIT /B !errorlevel!

:trustagent_restart
  call :get_status
  IF "%TASTATUS%"=="Stopped" (
     echo.   Trustagent was not running
  ) ELSE (
    call:trustagent_stop
    timeout /t 1 /NOBREAK
  )
  call :trustagent_start
EXIT /B !errorlevel!

:trustagent_stop
  echo. Stopping trustagent service
  sc stop trustagent > null
  echo. Trustagent stopped
EXIT /B !errorlevel!

:trustagent_setup
  echo.  Setup the trust agent
  set HARDWARE_UUID=
  for /f  "USEBACKQ" %%a in (`wmic csproduct get UUID /VALUE ^| findstr /C:"UUID"`) do ( 
    set _tmpvar=%%a
    set _tmpvar1=!_tmpvar:~5!
    set HARDWARE_UUID=!_tmpvar1:~0,-1!
  )
  echo. HARDWARE_UUID: %HARDWARE_UUID%
  set tasklist=%*
  IF "%tasklist%"=="" (
    set tasklist=%TRUSTAGENT_SETUP_TASKS%
  ) ELSE IF "%tasklist%"=="--force" (
      set tasklist=%TRUSTAGENT_SETUP_TASKS% --force
  )
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main setup configure-from-environment %tasklist%
EXIT /B !errorlevel!

:trustagent_authorize
  echo. trustagent authorization
  echo. it could do a lot of things
  set HARDWARE_UUID=
  for /f  "USEBACKQ" %%a in (`wmic csproduct get UUID /VALUE ^| findstr /C:"UUID"`) do ( 
    set _tmpvar=%%a
    set _tmpvar1=!_tmpvar:~5!
    set HARDWARE_UUID=!_tmpvar1:~0,-1!
  )
  echo. HARDWARE_UUID: %HARDWARE_UUID%

  REM set authorize_vars="TPM_OWNER_SECRET TPM_SRK_SECRET MTWILSON_API_URL"
  set authorize_vars="CURRENT_IP MTWILSON_API_URL"

  REM local default_value
  REM for v in $authorize_vars
  REM do
  REM  default_value=$(eval "echo \$$v")
  REM  prompt_with_default $v "Required: $v" $default_value
  REM done
  REM export_vars $authorize_vars
  call:trustagent_setup --force %TRUSTAGENT_AUTHORIZE_TASKS%
EXIT /B !errorlevel!

:trustagent_host_register
  set host_register_vars="CURRENT_IP AUTOMATIC_REGISTRATION"
  call:trustagent_setup --force %TRUSTAGENT_REGISTRATION_TASKS%
EXIT /B !errorlevel!


:trustagent_generate_password
  >"%TRUSTAGENT_PASSWORD_FILE%" "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main generate-password
EXIT /B !errorlevel!

:trustagent_export_config
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
EXIT /B !errorlevel!

:trustagent_generate_password
  >"%TRUSTAGENT_PASSWORD_FILE%" "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main generate-password
EXIT /B !errorlevel!

:trustagent_export_config
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
EXIT /B !errorlevel!

:trustagent_java_detect
  echo. Detecting Java on Trust Agent
  echo. %JAVA_HOME%
  echo. %JAVABIN%
EXIT /B !errorlevel!

:trustagent_zeroize
  echo. Shredding Trust Agent configuration
  del /s /q "%TRUSTAGENT_CONF%\" >null
EXIT /B !errorlevel!

:print_help
    REM echo. Usage: %~n0 start^|stop^|authorize^|start-http-server^|version
    echo. Usage: %~n0 start^|stop^|restart^|java-detect^|uninstall^|zeroize^|status^|version^|provision-attestation^|create-host
    echo. Usage: %~n0 setup [--force^|--noexec] [task1 task2 ...]
    echo. Usage: %~n0 export-config [outfile^|--in=infile^|--out=outfile^|--stdout] [--env-password=PASSWORD_VAR]
    echo. Usage: %~n0 config [key] [--delete^|newValue]
    echo. Available setup tasks:
    REM echo. %TRUSTAGENT_SETUP_TASKS%
    for %%a in (%TRUSTAGENT_SETUP_TASKS%) do echo. %%a
    echo. register-tpm-password
EXIT /B !errorlevel!

:tagent_uninstall
    start /d "%TRUSTAGENT_HOME%" Uninstall.exe
EXIT /B !errorlevel!

:tagent_uninstall
    start /d "%TRUSTAGENT_HOME%" Uninstall.exe
EXIT /B !errrolevel!

:get_status
  REM set TASTATUS=
  for /f  "USEBACKQ" %%a in (`wmic service trustagent get state /VALUE ^| findstr /C:"State"`) do ( 
    set _tmpvar=%%a
    set _tmpvar1=!_tmpvar:~6!
    set TASTATUS=!_tmpvar1:~0,-1!
  )
  EXIT /B 0

endlocal
