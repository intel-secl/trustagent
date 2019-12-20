@echo off
setlocal enabledelayedexpansion

REM # Automatic install steps:
REM # 1. Script configuration
REM # 2. Function library, Version Information, and Local configuration
REM # 3. Export variables
REM # 4. Clean up install log
REM # 5. Backup old files
REM # 6. Install prereqs
REM # 6.1. Install third party packages
REM # 6.2. Private Java install $JAVA_PACKAGE
REM # 6.3. INSTALL the citbootdriver to support geotag
REM # 7. Create trustagent.version file
REM # 8. Fix AIK certificate
REM # 9. Set trustagent IP
REM # 10. Update extension cache
REM # 11. Fix shiro.ini
REM # 12. Call default trustagent setup tasks
REM # 13. Encrypt the property file
REM # 14. Start trustagent

REM # 1. Script configuration
set package_name=trustagent

REM the package directory should be where the trust agent installed (e.g. package_dir=C:\Program Files (x86)\Intel\trustagent)
set package_bin=%~dp0
for %%a in ("%package_bin:~0,-1%") do set package_dir=%%~dpa

REM echo. %package_bin%
REM ECHO.Setup trust agent with CIT server...
REM echo. ==Trust Agent located at: %package_dir%

set TRUSTAGENT_HOME=%package_dir%
set TRUSTAGENT_BIN=%package_dir%\bin
set TRUSTAGENT_CONF=%TRUSTAGENT_HOME%\configuration
set TRUSTAGENT_LOGS=%TRUSTAGENT_HOME%\logs

REM ==set PATH for the current cmd 
set PATH=%PATH%;%package_bin%
REM ==set global PATH
REM setx PATH "%PATH%;%package_bin" /M

set intel_conf_dir=%package_dir%\configuration
set package_config_filename=%intel_conf_dir%\%package_name%.properties
set tpm_version_filename=%intel_conf_dir%\tpm-version
set package_env_filename=%package_dir%\%package_name%.env
set package_version_filename=%package_dir%\env.d\trustagent.version
set ASSET_TAG_SETUP="y"
set trustagent_cmd=%package_dir%\bin\agenthandler.cmd
set bootdriver_dir=%package_dir%\bootdriver
set REGISTER_TPM_PASSWORD=y
set PROVISION_ATTESTATION=y

set logfile=%package_dir%\logs\install.log


REM # FUNCTION LIBRARY, VERSION INFORMATION, and LOCAL CONFIGURATION
Call :tee Trust agent setup: Configure trust agent version and environment variables
cd %package_dir%
IF EXIST "%package_dir%\version" (
  REM echo. "Version file found"
  for /f  "tokens=*" %%a in (version) do (
    set %%a
  )
) ELSE (
  Call :tee Version file not found
)

CALL :tee Trust agent setup: Configure trust agent environment variables from trustagent.env
IF EXIST "%package_dir%\trustagent.env" (
  REM echo. trustagent.env found
  for /f  "USEBACKQ tokens=*" %%a in ("%package_dir%\trustagent.env") do (
    REM echo.   %%a
    set %%a
  )
) ELSE (
  Call :tee Missing file: trustagent.env
)

REM # 3. Export variables
REM # this is a list of all the variables we expect to find in trustagent.env
set TRUSTAGENT_ENV_VARS=TRUSTAGENT_ADMIN_USERNAME TRUSTAGENT_ADMIN_PASSWORD CURRENT_IP AUTOMATIC_REGISTRATION PROVISION_ATTESTATION MTWILSON_API_URL TPM_OWNER_SECRET TPM_SRK_SECRET AIK_SECRET AIK_INDEX TPM_QUOTE_IPV4 TRUSTAGENT_HTTP_TLS_PORT TRUSTAGENT_TLS_CERT_DN TRUSTAGENT_TLS_CERT_IP TRUSTAGENT_TLS_CERT_DNS TRUSTAGENT_KEYSTORE_PASSWORD DAA_ENABLED TRUSTAGENT_PASSWORD JAVA_REQUIRED_VERSION HARDWARE_UUID

REM export_vars $TRUSTAGENT_ENV_VARS
FOR %%a in (%TRUSTAGENT_ENV_VARS%) do (
REM  ECHO. %%a
)

REM # 4. Clean up install log
REM ECHO.Trust agent setup: Before start, clear the installation log file %logfile%
REM > "%logfile%" echo. %date%
REM >> "%logfile%" echo. %time%
Call :tee Trust agent setup: started

REM ## update logback.xml with configured trustagent log directory
  echo. ^<configuration^> > "%TRUSTAGENT_CONF%\logback.xml.edited"
  echo.     ^<appender name="LogFile" class="ch.qos.logback.core.FileAppender"^> >> "%TRUSTAGENT_CONF%\logback.xml.edited"
  echo.        ^<file^>%TRUSTAGENT_LOGS%\trustagent.log^</file^> >> "%TRUSTAGENT_CONF%\logback.xml.edited"
  type "%TRUSTAGENT_CONF%\logback.xml.base" >> "%TRUSTAGENT_CONF%\logback.xml.edited"
  copy /y "%TRUSTAGENT_CONF%\logback.xml.edited" "%TRUSTAGENT_CONF%\logback.xml"
  del /q "%TRUSTAGENT_CONF%\logback.xml.edited"
  REM del /q "%TRUSTAGENT_CONF%\logback.xml.base"

REM FIXIT ##### stop existing trust agent if running
REM # before we stop the trust agent, remove it from the monit config (if applicable)

REM # bug #288 we do not uninstall previous version because there are files including trustagent.jks  under the /opt tree and we need to keep them during an upgrade
REM # But if trust agent is already installed and running, stop it now (and start the new one later)
REM existing_tagent=`which tagent 2>/dev/null`
REM if [ -f "$existing_tagent" ]; then
REM  echo "Stopping trust agent..."
REM   $existing_tagent stop
REM fi

REM FIXIT groupadd trustagent >> $logfile  2>&1
REM FIXIT useradd -d /opt/trustagent -r -s /bin/false -g trustagent trustagent >> $logfile  2>&1

REM # 6. Install prereqs
REM # 6.1. Install third party packages
REM install_packages "TrustAgent requirements" "APPLICATION"

REM FIXIT hex2bin_install() {
REM  return_dir=`pwd`
REM  cd hex2bin
REM  make && cp hex2bin /usr/local/bin
REM  cd $return_dir
REM }
REM hex2bin_install

REM hex2bin=`which hex2bin 2>/dev/null`
REM if [[ ! -h "${package_dir}/bin/hex2bin" ]]; then
REM  ln -s "$hex2bin" "${package_dir}/bin"
REM fi

REM # 6.2. Private Java install $JAVA_PACKAGE
Call :tee Trust agent setup: Unpack JAVA JRE
  cd "%package_dir%\jre"
  call "jre.cmd"
  set JAVA_HOME=%package_dir%\jre
  cd "%package_bin%" 

REM patch java.security file
Call :tee Trust agent setup: Patch java.security file
if exist "%JAVA_HOME%\lib\security\java.security" (
  REM echo. ==Replacing java.security file, existing file will be backed up==
  copy "%JAVA_HOME%\lib\security\java.security" "%JAVA_HOME%\lib\security\java.security.old" > nul
  copy "%package_dir%\java.security" "%JAVA_HOME%\lib\security\java.security" > nul
)

REM # 6.3. INSTALL the citbootdriver to support geotag
REM  cd "%bootdriver_dir%"
REM #Remove the old driver first in case it is still there
REM  start /d "%bootdriver_dir%\" /b citbootdriversetup.exe uninstall
REM  del /F /Q "C:\Windows\System32\Drivers\citbootdriver.sys"
REM  REM #install the new one
REM  start /d "%bootdriver_dir%\" /b citbootdriversetup.exe install
REM  cd "%package_bin%"

REM # 7. Create trustagent.version file
Call :tee Trust agent setup: Create trustagent.version
> "%package_version_filename%"  echo. "# Installed Trust Agent on %date% %time%"
>> "%package_version_filename%"  echo. "TRUSTAGENT_VERSION=%VERSION%"
>> "%package_version_filename%"  echo "TRUSTAGENT_RELEASE=\"%BUILD%\""

REM # create tpm-version file
tpmtool.exe getTpmVersion > "%TRUSTAGENT_CONF%\tpm-version"

Call :tee Trust agent setup: Registering tagent in start up
REM register_startup_script /usr/local/bin/tagent tagent 21 >>$logfile 2>&1

REM # 8. Fix AIK certificate
REM fix_existing_aikcert() {
REM  local aikdir=${intel_conf_dir}/cert
REM  if [ -f $aikdir/aikcert.cer ]; then
REM    # trust agent aikcert.cer is in broken PEM format... it needs newlines every 76 characters to be correct
REM    cat $aikdir/aikcert.cer | sed 's/.\{76\}/&\n/g' > $aikdir/aikcert.pem
REM    rm $aikdir/aikcert.cer
REM    if [ -f ${package_config_filename} ]; then 
REM       # update aikcert.filename=aikcert.cer to aikcert.filename=aikcert.pem
REM       update_property_in_file aikcert.filename ${package_config_filename} aikcert.pem
REM    fi
REM  fi
REM }
REM fix_existing_aikcert

REM # 9. Set trustagent IP
REM # collect all the localhost ip addresses and make the list available as the
REM # default if the user has not already set the TRUSTAGENT_TLS_CERT_IP variable
Call :tee Trust agent setup: Find the IP address of the host
set DEFAULT_TRUSTAGENT_TLS_CERT_IP=""
for /f "tokens=14 delims= " %%a in ('ipconfig ^| findstr "IPv4"') do (
  IF !DEFAULT_TRUSTAGENT_TLS_CERT_IP!=="" (
    set DEFAULT_TRUSTAGENT_TLS_CERT_IP=%%a
  ) ELSE (
    set DEFAULT_TRUSTAGENT_TLS_CERT_IP=%%a,!DEFAULT_TRUSTAGENT_TLS_CERT_IP!
  )
)
set TRUSTAGENT_TLS_CERT_IP=""
IF %TRUSTAGENT_TLS_CERT_IP%=="" (
  set TRUSTAGENT_TLS_CERT_IP=%DEFAULT_TRUSTAGENT_TLS_CERT_IP%
)
Call :tee %TRUSTAGENT_TLS_CERT_IP%

REM # 10. Update extension cache
REM # before running any tagent commands update the extensions cache file
Call :tee Trust agent setup: Updating the extensions cache file... 
>>"%logfile%" call "%trustagent_cmd%" setup update-extensions-cache-file --force

REM # create a trustagent username "mtwilson" with no password and all privileges
REM # which allows mtwilson to access it until mtwilson UI is updated to allow
REM # entering username and password for accessing the trust agent
REM ECHO. ==Create a trustagent username "mtwilson" with no password
REM >>"%logfile%" call "%trustagent_cmd%" password mtwilson --nopass *:*

REM # 11. Fix shiro.ini
REM FIXIT setup correct shiro.ini (should not hardcode the path in shiro.ini setup correct shiro.ini)
Call :tee Trust agent setup: Generate shiro-win.ini
REM copy /Y "%intel_conf_dir%\shiro-win.ini" "%intel_conf_dir%\shiro.ini" > nul
ECHO. [main] >"%intel_conf_dir%\shiro.ini"
ECHO. ssl.enabled = true >>"%intel_conf_dir%\shiro.ini"
ECHO. ssl.port = 1443 >>"%intel_conf_dir%\shiro.ini"
ECHO. filePasswordRealm=com.intel.mtwilson.shiro.file.FilePasswordRealm >>"%intel_conf_dir%\shiro.ini"
ECHO. filePasswordRealm.userFilePath=%intel_conf_dir%\users.txt >>"%intel_conf_dir%\shiro.ini"
ECHO. filePasswordRealm.permissionFilePath=%intel_conf_dir%\permissions.txt >>"%intel_conf_dir%\shiro.ini"
ECHO. passwordMatcher=com.intel.mtwilson.shiro.authc.password.PasswordCredentialsMatcher >>"%intel_conf_dir%\shiro.ini"
ECHO. filePasswordRealm.credentialsMatcher=$passwordMatcher >>"%intel_conf_dir%\shiro.ini"
ECHO. jwtRealm=com.intel.mtwilson.shiro.authc.token.JWTRealm >>"%intel_conf_dir%\shiro.ini"
ECHO. jwtMatcher=com.intel.mtwilson.shiro.authc.token.JWTSignatureMatcher >>"%intel_conf_dir%\shiro.ini"
ECHO. jwtRealm.credentialsMatcher= $jwtMatcher >>"%intel_conf_dir%\shiro.ini"
ECHO. jwtRealm.applicationName = TA >>"%intel_conf_dir%\shiro.ini"
ECHO. securityManager.realms = $jwtRealm >>"%intel_conf_dir%\shiro.ini"
ECHO. # built-in authentication strategy >>"%intel_conf_dir%\shiro.ini"
ECHO. #authcStrategy = org.apache.shiro.authc.pam.FirstSuccessfulStrategy >>"%intel_conf_dir%\shiro.ini"
ECHO. #authcStrategy = org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy >>"%intel_conf_dir%\shiro.ini"
ECHO. authcStrategy = com.intel.mtwilson.shiro.LoggingAtLeastOneSuccessfulStrategy >>"%intel_conf_dir%\shiro.ini"
ECHO. securityManager.authenticator.authenticationStrategy = $authcStrategy >>"%intel_conf_dir%\shiro.ini"
ECHO. authcPassword=com.intel.mtwilson.shiro.authc.password.HttpBasicAuthenticationFilter >>"%intel_conf_dir%\shiro.ini"
ECHO. authcPassword.applicationName=Trust Agent >>"%intel_conf_dir%\shiro.ini"
ECHO. authcPassword.authcScheme=Basic >>"%intel_conf_dir%\shiro.ini"
ECHO. authcPassword.authzScheme=Basic >>"%intel_conf_dir%\shiro.ini"
ECHO. authcToken = com.intel.mtwilson.shiro.authc.token.JWTTokenAuthenticationFilter >>"%intel_conf_dir%\shiro.ini"
ECHO. authcToken.applicationName = TA >>"%intel_conf_dir%\shiro.ini"
ECHO. authcToken.sendChallenge = false >>"%intel_conf_dir%\shiro.ini"
ECHO. authcToken.permissive = false >>"%intel_conf_dir%\shiro.ini"
ECHO. # define security by url matching, the first match wins so order is important >>"%intel_conf_dir%\shiro.ini"
ECHO. # also /path/*  will match /path/a and /path/b but not /path/c/d  >>"%intel_conf_dir%\shiro.ini"
ECHO. # but /path/**  will match /path/a and /path/b and also /path/c/d >>"%intel_conf_dir%\shiro.ini"
ECHO. [urls] >>"%intel_conf_dir%\shiro.ini"
ECHO. /index.html = anon >>"%intel_conf_dir%\shiro.ini"
ECHO. /v2/version = anon >>"%intel_conf_dir%\shiro.ini"
ECHO. /** = ssl, authcToken, perms >>"%intel_conf_dir%\shiro.ini"

REM # 12. Call default trustagent setup tasks
REM # give tagent a chance to do any other setup (such as the .env file and pcakey)
REM # and make sure it's successful before trying to start the trust agent
REM # NOTE: only the output from start-http-server is redirected to the logfile;
REM #       the stdout from the setup command will be displayed
REM # ECHO.Trust agent setup: Registering host access information with CIT server
REM call "%trustagent_cmd%" setup

REM # 13. Encrypt the property file
IF not defined TRUSTAGENT_PASSWORD (
	IF not exist "%TRUSTAGENT_CONF%\.trustagent_password" (
		call "%trustagent_cmd%" generate-password

		icacls "%TRUSTAGENT_CONF%\.trustagent_password" /inheritance:r
		icacls "%TRUSTAGENT_CONF%\.trustagent_password" /grant administrators:F
		icacls "%TRUSTAGENT_CONF%\.trustagent_password" /grant SYSTEM:F
		attrib +h +s "%TRUSTAGENT_CONF%\.trustagent_password"
	)
)
cd "%TRUSTAGENT_CONF%"
call "%trustagent_cmd%" import-config --in="trustagent.properties" --out="trustagent.properties"
call "%trustagent_cmd%" config "jetty.tls.cert.ip" "%TA_TLS_CERT_IP%"
call "%trustagent_cmd%" config "jetty.tls.cert.dns" "%TA_TLS_CERT_DNS%"
cd "%package_bin%"

IF [%MTWILSON_API_URL%] == [""] set MTWILSON_API_URL=
IF [%PROVISION_ATTESTATION%] == [""] set PROVISION_ATTESTATION=

IF DEFINED MTWILSON_API_URL (IF "%PROVISION_ATTESTATION%"=="y" (call "%trustagent_cmd%" provision-attestation)) ELSE (Call :tee Trust agent setup: Attestation service details unavailable. Skipping provisioning)

IF [%AUTOMATIC_REGISTRATION%] == [""] set AUTOMATIC_REGISTRATION=
IF "%AUTOMATIC_REGISTRATION%"=="y" (
    call "%trustagent_cmd%" create-host
)

Call :tee Trust agent setup: finished

if exist "%TRUSTAGENT_HOME%" (
   icacls "%TRUSTAGENT_HOME%\*" /inheritance:r /grant:r administrators:F /T >null
)
REM # 14. Start trustagent
REM # ECHO.Trust agent setup finished (Please verify if there are errors above for TA regsitration with CIT server )
REM # ECHO. ==Start trustagent service
REM call "%trustagent_cmd%" start

REM # optional: register tpm password with mtwilson so pull provisioning can
REM #           be accomplished with less reboots (no ownership transfer)
REM # prompt_with_default REGISTER_TPM_PASSWORD       "Register TPM password with service to support asset tag automation? [y/n]" ${REGISTER_TPM_PASSWORD}
REM if [[ "$REGISTER_TPM_PASSWORD" == "y" || "$REGISTER_TPM_PASSWORD" == "Y" || "$REGISTER_TPM_PASSWORD" == "yes" ]]; then 
REM # prompt_with_default ASSET_TAG_URL "Asset Tag Server URL: (https://[SERVER]:[PORT]/mtwilson/v2)" ${ASSET_TAG_URL}
REM # # json='[{ "subject": "'$UUID'", "selection": "'$selectionUUID'"}]'
REM # # wget --secure-protocol=SSLv3 --no-proxy --ca-certificate=$CERT_FILE_LOCATION --password=$password --user=$username --header="Content-Type: application/json" --post-data="$json"
REM # TPM_PASSWORD=`read_property_from_file tpm.owner.secret /opt/trustagent/configuration/trustagent.properties`
REM # echo "registering $TPM_PASSWORD to $UUID"
REM # wget --secure-protocol=SSLv3 --no-proxy --no-check-certificate --auth-no-challenge --password=$ASSET_TAG_PASSWORD --user=$ASSET_TAG_USERNAME --header="Content-Type: application/json" --post-data='{"id":"'$UUID'","password":"'$TPM_PASSWORD'"}' "$ASSET_TAG_URL/host-tpm-passwords"
REM   export HARDWARE_UUID=`dmidecode |grep UUID | awk '{print $2}'`
REM   /usr/local/bin/tagent setup register-tpm-password
REM fi

:: Function to log and write the output to STDOUT 
:: STDOUT is used by nsis to display on the UI
:tee
        IF "%1"=="" goto:eof
	echo.%date% %time% - %* >> "%logfile%"
	echo.%*
goto:eof
