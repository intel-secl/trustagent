@echo off
setlocal enabledelayedexpansion

set DESC=Trust Agent
set NAME=agenthandler

REM ###################################################################################################
REM #Set environment specific variables here 
REM ###################################################################################################

REM set the trustagent home directory
REM set TRUSTAGENT_HOME=C:\Program Files (x86)\Intel\trustagent
for %%i in ("%~dp0..") do set "parentfolder=%%~fi"
set TRUSTAGENT_HOME=%parentfolder%

set DAEMON=%TRUSTAGENT_HOME%\bin\%NAME%.cmd
set logfile=%TRUSTAGENT_HOME%\logs\install.log
set svclogfile=%TRUSTAGENT_HOME%\logs\trustagent.log

set JAVA_HOME=%TRUSTAGENT_HOME%\jre
set JAVABIN=%JAVA_HOME%\bin\java

set TRUSTAGENT_CONF=%TRUSTAGENT_HOME%\configuration
set TRUSTAGENT_LOGS=%TRUSTAGENT_HOME%\logs
set TRUSTAGENT_PASSWORD_FILE=%TRUSTAGENT_CONF%\.trustagent_password
set TRUSTAGENT_JAVA=%TRUSTAGENT_HOME%\java
set TRUSTAGENT_BIN=%TRUSTAGENT_HOME%\bin
set TRUSTAGENT_ENV=%TRUSTAGENT_HOME%\env
set TRUSTAGENT_VAR=%TRUSTAGENT_HOME%\var
set TRUSTAGENT_PID_FILE=%TRUSTAGENT_VAR%\run\trustagent.pid
set TRUSTAGENT_HTTP_LOG_FILE=%TRUSTAGENT_LOGS%\http.log
set TRUSTAGENT_AUTHORIZE_TASKS=download-mtwilson-tls-certificate download-mtwilson-privacy-ca-certificate download-mtwilson-saml-certificate request-endorsement-certificate request-aik-certificate
set TRUSTAGENT_REGISTRATION_TASKS=attestation-registration
set TRUSTAGENT_CREATE_FLAVOR_TASK=create-host-unique-flavor
set TRUSTAGENT_TPM_TASKS=create-tpm-owner-secret create-tpm-srk-secret create-aik-secret take-ownership
set TRUSTAGENT_START_TASKS=secure-store create-keystore-password create-tls-keypair take-ownership
REM set TRUSTAGENT_VM_ATTESTATION_SETUP_TASKS=create-binding-key certify-binding-key create-signing-key certify-signing-key
REM set TRUSTAGENT_VM_ATTESTATION_SETUP_TASKS=
REM  %TRUSTAGENT_VM_ATTESTATION_SETUP_TASKS%
set TRUSTAGENT_SETUP_TASKS=update-extensions-cache-file secure-store create-keystore-password create-tls-keypair create-admin-user %TRUSTAGENT_TPM_TASKS% %TRUSTAGENT_AUTHORIZE_TASKS% login-register
REM ECHO. ==Running tagent service==
REM # load environment variables (these may override the defaults set above)
if exist "%TRUSTAGENT_ENV%\" (
REM  TRUSTAGENT_ENV_FILES=$(ls -1 $TRUSTAGENT_ENV/*)
REM  for env_file in $TRUSTAGENT_ENV_FILES; do
REM    . $env_file
REM  done
  for /f  "delims=" %%a in ('dir "%TRUSTAGENT_ENV%" /b') do (
    echo. hello0
    echo. %%a
    cd "%TRUSTAGENT_ENV%"
    call %%a
    echo. %TEST_TA%
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
set JAVA_OPTS=-Dlogback.configurationFile="%TRUSTAGENT_CONF%"\logback.xml -Dlog4j.configuration=file:"%TRUSTAGENT_CONF%"\log4j.properties -Dfs.name=trustagent -Djdk.tls.ephemeralDHKeySize=2048

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

REM parsing the command arguments
set wcommand=%1
set cmdparams=
for /f "usebackq tokens=1*" %%i in (`echo %*`) DO @ set cmdparams=%%j
REM echo. Running command: %wcommand% with %cmdparams%

if "%wcommand%"=="start" (
  call:trustagent_start
) ELSE IF "%wcommand%"=="stop" (
  call:trustagent_stop
) ELSE IF "%wcommand%"=="status" (
  call:trustagent_status
) ELSE IF "%wcommand%"=="setup" (
  call:trustagent_setup %cmdparams%
) ELSE IF "%wcommand%"=="provision-attestation" (

  for /F "tokens=*" %%A in (%2) do (
    set %%A
  )

  call:trustagent_setup

  REM call:trustagent_stop
  REM timeout /t 1 /NOBREAK
  REM call:trustagent_start

) ELSE IF "%wcommand%"=="authorize" (
  call:trustagent_authorize
) ELSE IF "%wcommand%"=="java-detect" (
  call:trustagent_java_detect
) ELSE IF "%wcommand%"=="fingerprint" (
  call:trustagent_fingerprint
) ELSE IF "%wcommand%"=="zeroize" (
  call:trustagent_zeroize
) ELSE IF "%wcommand%"=="create-host" (
  call:trustagent_host_register
) ELSE IF "%wcommand%"=="create-host-unique-flavor" (
  call:trustagent_create_flavor
) ELSE IF "%wcommand%"=="start-http-server" (
  call:trustagent_start
) ELSE IF "%wcommand%"=="help" (
  call:print_help
) ELSE IF "%wcommand%"=="generate-password" (
  call:trustagent_generate_password
) ELSE IF "%wcommand%"=="export-config" (
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
  EXIT /B 0
) ELSE (
  IF "%*"=="" (
    call:print_help
  ) ELSE (
    echo. Running command: %*
    >>"%logfile%" "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
  )
)
GOTO:EOF

REM functions
:trustagent_start
  echo. Starting trustagent service
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main start-http-server
GOTO:EOF

:trustagent_stop
  echo. stopping the trust agent
  echo. it could do a lot of things
GOTO:EOF

:trustagent_status
  REM set TASTATUS=
  call :get_status
  echo. Trustagent status: %TASTATUS%
GOTO:EOF

:trustagent_setup
  REM echo.  Setup the trust agent
  set HARDWARE_UUID=
  for /f  "USEBACKQ" %%a in (`wmic csproduct get UUID /VALUE ^| findstr /C:"UUID"`) do ( 
    set _tmpvar=%%a
    set _tmpvar1=!_tmpvar:~5!
    set HARDWARE_UUID=!_tmpvar1:~0,-1!
  )
  REM echo. HARDWARE_UUID: %HARDWARE_UUID%
  set tasklist=%*
  REM echo. %tasklist%
  IF "%tasklist%"=="" (
    set tasklist=%TRUSTAGENT_SETUP_TASKS%
  ) ELSE IF "%tasklist%"=="--force" (
      set tasklist=%TRUSTAGENT_SETUP_TASKS% --force
  )
  REM echo. %tasklist%
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main setup configure-from-environment %tasklist%
  type "%svclogfile%" >> "%logfile%"
GOTO:EOF

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

  REM set authorize_vars="TPM_OWNER_SECRET TPM_SRK_SECRET MTWILSON_API_URL MTWILSON_API_USERNAME MTWILSON_API_PASSWORD MTWILSON_TLS_CERT_SHA256"
  set authorize_vars="CURRENT_IP MTWILSON_API_URL MTWILSON_API_USERNAME MTWILSON_API_PASSWORD MTWILSON_TLS_CERT_SHA256"

  REM local default_value
  REM for v in $authorize_vars
  REM do
  REM  default_value=$(eval "echo \$$v")
  REM  prompt_with_default $v "Required: $v" $default_value
  REM done
  REM export_vars $authorize_vars
  call:trustagent_setup --force %TRUSTAGENT_AUTHORIZE_TASKS%
GOTO:EOF

:trustagent_host_register
  set host_register_vars="CURRENT_IP AUTOMATIC_REGISTRATION"
  call:trustagent_setup --force %TRUSTAGENT_REGISTRATION_TASKS%
GOTO:EOF

:trustagent_create_flavor
  set host_register_vars="CURRENT_IP AUTOMATIC_FLAVOR_CREATION"
  call:trustagent_setup --force %TRUSTAGENT_CREATE_FLAVOR_TASK%
GOTO:EOF

:trustagent_generate_password
  >"%TRUSTAGENT_PASSWORD_FILE%" "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main generate-password
GOTO:EOF

:trustagent_export_config
  "%JAVABIN%" %JAVA_OPTS% com.intel.mtwilson.launcher.console.Main %*
GOTO:EOF

:trustagent_java_detect
  echo. Detecting Java on Trust Agent
  echo. %JAVA_HOME%
  echo. %JAVABIN%
GOTO:EOF

:trustagent_zeroize
  echo. Shredding Trust Agent configuration
  del /s /q "%TRUSTAGENT_CONF%\" >null
GOTO:EOF

:trustagent_fingerprint
  set HTTPS_PROPERTIES_FILE="%TRUSTAGENT_HOME%"\configuration\https.properties
  echo. TLS Certificate Fingerprint  
  for /F "delims=" %%a in ('findstr /c:tls.cert.sha256 %HTTPS_PROPERTIES_FILE%') do set var=%%a
  echo. %var%
GOTO:EOF

:print_help
    echo. "Usage: $0 start|stop|restart|java-detect|fingerprint|uninstall|zeroize|status|version|provision-attestation|create-host|configure-from-environment"  
    echo. "Usage: $0 setup [--force|--noexec] [task1 task2 ...]"
    echo. "Available setup tasks:"
    echo. %TRUSTAGENT_SETUP_TASKS% 
    echo. register-tpm-password   
GOTO:EOF

endlocal

