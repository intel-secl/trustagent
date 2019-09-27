#!/bin/bash

# Postconditions:
# * exit with error code 1 only if there was a fatal error:
#   functions.sh not found (must be adjacent to this file in the package)
#   

# TRUSTAGENT install script
# Outline:
# 1. load application environment variables if already defined from env directory
# 2. load installer environment file, if present
# 3. source the utility script file "functions.sh":  mtwilson-linux-util-3.0-SNAPSHOT.sh
# 4. source the version script file "version"
# 5. define application directory layout
# 6. install pre-required packages
# 7. determine if we are installing as root or non-root, create groups and users accordingly
# 8. create application directories (chown will be repeated near end of this script, after setup)
# 9. backup current configuration and data, if they exist
# 10. store directory layout in env file
# 11. store trustagent username in env file
# 12. store log level in env file, if it's set
# 13. If VIRSH_DEFAULT_CONNECT_URI is defined in environment copy it to env directory
# 14. extract trustagent zip
# 15. symlink tagent
# 16. install application agent
# 17. migrate any old data to the new locations (v1 - v3)
# 18. create tpm-tools and additional binary symlinks
# 19. copy utilities script file to application folder
# 20. delete existing dependencies from java folder, to prevent duplicate copies
# 21. fix_libcrypto for RHEL
# 22. create trustagent-version file
# 23. fix_existing_aikcert
# 24. create TRUSTAGENT_TLS_CERT_IP list of system host addresses
# 25. update the extensions cache file
# 26. ensure the trustagent owns all the content created during setup
# 27. update tpm devices permissions to ensure it can be accessed by trustagent
# 28. config logrotate
# 29. tagent setup
# 30. tagent start


#####


# WARNING:
# *** do NOT use TABS for indentation, use SPACES
# *** TABS will cause errors in some linux distributions

# application defaults (these are not configurable and used only in this script so no need to export)
DEFAULT_TRUSTAGENT_HOME=/opt/trustagent
DEFAULT_TRUSTAGENT_USERNAME=tagent
if [[ ${container} == "docker" ]]; then
    DOCKER=true
else
    DOCKER=false
fi

JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.8}

# default settings
export LOG_ROTATION_PERIOD=${LOG_ROTATION_PERIOD:-monthly}
export LOG_COMPRESS=${LOG_COMPRESS:-compress}
export LOG_DELAYCOMPRESS=${LOG_DELAYCOMPRESS:-delaycompress}
export LOG_COPYTRUNCATE=${LOG_COPYTRUNCATE:-copytruncate}
export LOG_SIZE=${LOG_SIZE:-1G}
export LOG_OLD=${LOG_OLD:-12}
export PROVISION_ATTESTATION=${PROVISION_ATTESTATION:-y}
export AUTOMATIC_PULL_MANIFEST=${AUTOMATIC_PULL_MANIFEST:-y}
export TRUSTAGENT_ADMIN_USERNAME=${TRUSTAGENT_ADMIN_USERNAME:-tagent-admin}
export REGISTER_TPM_PASSWORD=${REGISTER_TPM_PASSWORD:-y}
export TRUSTAGENT_LOGIN_REGISTER=${TRUSTAGENT_LOGIN_REGISTER:-true}
export TRUSTAGENT_HOME=${TRUSTAGENT_HOME:-$DEFAULT_TRUSTAGENT_HOME}
TRUSTAGENT_LAYOUT=${TRUSTAGENT_LAYOUT:-home}
# PID file needed for startup service registration and as set in tagent.sh
TRUSTAGENT_PID_FILE=$TRUSTAGENT_HOME/trustagent.pid
TRUSTAGENT_TMPCLN_INT=${TRUSTAGENT_TMPCLN_INT:-* 0 * * *}
TRUSTAGENT_TMPCLN_AGE=${TRUSTAGENT_TMPCLN_AGE:-7}


# the env directory is not configurable; it is defined as TRUSTAGENT_HOME/env.d and the
# administrator may use a symlink if necessary to place it anywhere else
export TRUSTAGENT_ENV=$TRUSTAGENT_HOME/env.d

# 1. load application environment variables if already defined from env directory
if [ -d $TRUSTAGENT_ENV ]; then
  TRUSTAGENT_ENV_FILES=$(ls -1 $TRUSTAGENT_ENV/*)
  for env_file in $TRUSTAGENT_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# Deployment phase
# 2. load installer environment file, if present
if [ -f ~/trustagent.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/trustagent.env"
  . ~/trustagent.env
  env_file_exports=$(cat ~/trustagent.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
fi


# 3. source the utility script file "functions.sh":  mtwilson-linux-util-3.0-SNAPSHOT.sh
# FUNCTION LIBRARY
if [ -f functions ]; then . functions; else echo "Missing file: functions"; exit 1; fi

# 4. source the version script file "version"
# VERSION INFORMATION
if [ -f version ]; then . version; else echo_warning "Missing file: version"; fi
# The version script is automatically generated at build time and looks like this:
#ARTIFACT=mtwilson-trustagent-installer
#VERSION=3.0
#BUILD="Fri, 5 Jun 2015 15:55:20 PDT (release-3.0)"


# LOCAL CONFIGURATION
directory_layout() {
if [ "$TRUSTAGENT_LAYOUT" == "linux" ]; then
  export TRUSTAGENT_CONFIGURATION=${TRUSTAGENT_CONFIGURATION:-/etc/trustagent}
  export TRUSTAGENT_REPOSITORY=${TRUSTAGENT_REPOSITORY:-/var/opt/trustagent}
  export TRUSTAGENT_LOGS=${TRUSTAGENT_LOGS:-/var/log/trustagent}
  export TRUSTAGENT_TMP=${TRUSTAGENT_TMP:-/var/opt/tmp}
elif [ "$TRUSTAGENT_LAYOUT" == "home" ]; then
  export TRUSTAGENT_CONFIGURATION=${TRUSTAGENT_CONFIGURATION:-$TRUSTAGENT_HOME/configuration}
  export TRUSTAGENT_REPOSITORY=${TRUSTAGENT_REPOSITORY:-$TRUSTAGENT_HOME/repository}
  export TRUSTAGENT_LOGS=${TRUSTAGENT_LOGS:-$TRUSTAGENT_HOME/logs}
  export TRUSTAGENT_TMP=${TRUSTAGENT_TMP:-$TRUSTAGENT_HOME/var/tmp}
fi
export TRUSTAGENT_VAR=${TRUSTAGENT_VAR:-$TRUSTAGENT_HOME/var}
export TRUSTAGENT_BIN=${TRUSTAGENT_BIN:-$TRUSTAGENT_HOME/bin}
export TRUSTAGENT_JAVA=${TRUSTAGENT_JAVA:-$TRUSTAGENT_HOME/java}
export TRUSTAGENT_BACKUP=${TRUSTAGENT_BACKUP:-$TRUSTAGENT_REPOSITORY/backup}
export INSTALL_LOG_FILE=$TRUSTAGENT_LOGS/install.log
}

# 5. define application directory layout
directory_layout

detect_tpm_version

if [ $TPM_VERSION == "1.2" ]; then
  echo
  echo "Trust Agent: TPM1.2 is Not Supported in this release. TrustAgent could not be installed."
  echo
  exit 1
fi

# 6. install pre-required packages
chmod +x install_prereq.sh
./install_prereq.sh
ipResult=$?

if [ "${TRUSTAGENT_SETUP_PREREQS:-yes}" == "yes" ]; then
  # set TRUSTAGENT_REBOOT=no (in trustagent.env) if you want to ensure it doesn't reboot
  # set TRUSTAGENT_SETUP_PREREQS=no (in trustagent.env) if you want to skip this step 
  chmod +x setup_prereqs.sh
  ./setup_prereqs.sh
  spResult=$?
fi

mkdir -p "$TRUSTAGENT_HOME/var/ramfs"

# 7. determine if we are installing as root or non-root, create groups and users accordingly
if [ "$(whoami)" == "root" ]; then
  # create a trustagent user if there isn't already one created
  TRUSTAGENT_USERNAME=${TRUSTAGENT_USERNAME:-$DEFAULT_TRUSTAGENT_USERNAME}
  if ! getent passwd $TRUSTAGENT_USERNAME 2>&1 >/dev/null; then
    useradd --comment "Mt Wilson Trust Agent" --home $TRUSTAGENT_HOME --system --shell /bin/false $TRUSTAGENT_USERNAME
    usermod --lock $TRUSTAGENT_USERNAME
    # note: to assign a shell and allow login you can run "usermod --shell /bin/bash --unlock $TRUSTAGENT_USERNAME"
  fi
else
  # already running as trustagent user
  TRUSTAGENT_USERNAME=$(whoami)
  if [ ! -w "$TRUSTAGENT_HOME" ] && [ ! -w $(dirname $TRUSTAGENT_HOME) ]; then
    TRUSTAGENT_HOME=$(cd ~ && pwd)
  fi
  echo_warning "Installing as $TRUSTAGENT_USERNAME into $TRUSTAGENT_HOME"  
fi
directory_layout

# before we start, clear the install log (directory must already exist; created above)
mkdir -p $(dirname $INSTALL_LOG_FILE)
if [ $? -ne 0 ]; then
  echo_failure "Cannot write to log directory: $(dirname $INSTALL_LOG_FILE)"
  exit 1
fi
date > $INSTALL_LOG_FILE
if [ $? -ne 0 ]; then
  echo_failure "Cannot write to log file: $INSTALL_LOG_FILE"
  exit 1
fi
chown $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $INSTALL_LOG_FILE
logfile=$INSTALL_LOG_FILE

# 8. create application directories (chown will be repeated near end of this script, after setup)
for directory in $TRUSTAGENT_HOME $TRUSTAGENT_CONFIGURATION $TRUSTAGENT_ENV $TRUSTAGENT_REPOSITORY $TRUSTAGENT_VAR $TRUSTAGENT_LOGS $TRUSTAGENT_TMP; do
  # mkdir -p will return 0 if directory exists or is a symlink to an existing directory or directory and parents can be created
  mkdir -p $directory
  if [ $? -ne 0 ]; then
    echo_failure "Cannot create directory: $directory"
    exit 1
  fi
  chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $directory
  chmod 700 $directory
done

# ensure we have our own tagent programs in the path
export PATH=$TRUSTAGENT_BIN:$PATH

# ensure that trousers and tpm tools are in the path
export PATH=$PATH:/usr/sbin:/usr/local/sbin

profile_dir=$HOME
if [ "$(whoami)" == "root" ] && [ -n "$TRUSTAGENT_USERNAME" ] && [ "$TRUSTAGENT_USERNAME" != "root" ]; then
  profile_dir=$TRUSTAGENT_HOME
fi
profile_name=$profile_dir/$(basename $(getUserProfileFile))

appendToUserProfileFile "export TRUSTAGENT_HOME=$TRUSTAGENT_HOME" $profile_name


# if an existing tagent is already running, stop it while we install
existing_tagent=`which tagent 2>/dev/null`
if [ -f "$existing_tagent" ]; then
  $existing_tagent stop
fi

trustagent_backup_configuration() {
  if [ -n "$TRUSTAGENT_CONFIGURATION" ] && [ -d "$TRUSTAGENT_CONFIGURATION" ]; then
    mkdir -p $TRUSTAGENT_BACKUP
    if [ $? -ne 0 ]; then
      echo_warning "Cannot create backup directory: $TRUSTAGENT_BACKUP"
      echo_warning "Backup will be stored in /tmp"
      TRUSTAGENT_BACKUP=/tmp
    fi
    datestr=`date +%Y%m%d.%H%M`
    backupdir=$TRUSTAGENT_BACKUP/trustagent.configuration.$datestr
    cp -r $TRUSTAGENT_CONFIGURATION $backupdir
  fi
}
trustagent_backup_repository() {
  if [ -n "$TRUSTAGENT_REPOSITORY" ] && [ -d "$TRUSTAGENT_REPOSITORY" ]; then
    mkdir -p $TRUSTAGENT_BACKUP
    if [ $? -ne 0 ]; then
      echo_warning "Cannot create backup directory: $TRUSTAGENT_BACKUP"
      echo_warning "Backup will be stored in /tmp"
      TRUSTAGENT_BACKUP=/tmp
    fi
    datestr=`date +%Y%m%d.%H%M`
    backupdir=$TRUSTAGENT_BACKUP/trustagent.repository.$datestr
    cp -r $TRUSTAGENT_REPOSITORY $backupdir
  fi
}

# 9. backup current configuration and data, if they exist
trustagent_backup_configuration
#trustagent_backup_repository

# 10. store directory layout in env file
echo "# $(date)" > $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_HOME=$TRUSTAGENT_HOME" >> $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_CONFIGURATION=$TRUSTAGENT_CONFIGURATION" >> $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_JAVA=$TRUSTAGENT_JAVA" >> $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_BIN=$TRUSTAGENT_BIN" >> $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_REPOSITORY=$TRUSTAGENT_REPOSITORY" >> $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_LOGS=$TRUSTAGENT_LOGS" >> $TRUSTAGENT_ENV/trustagent-layout
echo "TRUSTAGENT_TMP=$TRUSTAGENT_TMP" >> $TRUSTAGENT_ENV/trustagent-layout

# 11. store trustagent username in env file
echo "# $(date)" > $TRUSTAGENT_ENV/trustagent-username
echo "TRUSTAGENT_USERNAME=$TRUSTAGENT_USERNAME" >> $TRUSTAGENT_ENV/trustagent-username

# 12. store log level in env file, if it's set
if [ -n "$TRUSTAGENT_LOG_LEVEL" ]; then
  echo "# $(date)" > $TRUSTAGENT_ENV/trustagent-logging
  echo "TRUSTAGENT_LOG_LEVEL=$TRUSTAGENT_LOG_LEVEL" >> $TRUSTAGENT_ENV/trustagent-logging
fi

# store the auto-exported environment variables in temporary env file
# to make them available after the script uses sudo to switch users;
# we delete that file later
echo "# $(date)" > $TRUSTAGENT_ENV/trustagent-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "export $env_file_var_name='$env_file_var_value'" >> $TRUSTAGENT_ENV/trustagent-setup
done


# ORIGINAL SCRIPT CONFIGURATION:
TRUSTAGENT_V_1_2_HOME=/opt/intel/cloudsecurity/trustagent
TRUSTAGENT_V_1_2_CONFIGURATION=/etc/intel/cloudsecurity
package_config_filename=${TRUSTAGENT_V_1_2_CONFIGURATION}/trustagent.properties
ASSET_TAG_SETUP="y"

# save tpm version in trust agent configuration directory
# if we are building a container, defer this until docker run (first setup)
if [ $DOCKER != "true" ]; then
    echo -n "$TPM_VERSION" > $TRUSTAGENT_CONFIGURATION/tpm-version
fi

# 13. If VIRSH_DEFAULT_CONNECT_URI is defined in environment copy it to env directory (likely from ~/.bashrc)
# copy it to our new env folder so it will be available to tagent on startup
if [ -n "$LIBVIRT_DEFAULT_URI" ]; then
  echo "LIBVIRT_DEFAULT_URI=$LIBVIRT_DEFAULT_URI" > $TRUSTAGENT_ENV/virsh
elif [ -n "$VIRSH_DEFAULT_CONNECT_URI" ]; then
  echo "VIRSH_DEFAULT_CONNECT_URI=$VIRSH_DEFAULT_CONNECT_URI" > $TRUSTAGENT_ENV/virsh
fi

cp version $TRUSTAGENT_CONFIGURATION/trustagent-version

# delete existing java files, to prevent a situation where the installer copies
# a newer file but the older file is also there
if [ -d $TRUSTAGENT_HOME/java ]; then
  rm -f $TRUSTAGENT_HOME/java/*.jar 2>/dev/null
fi

# 14. extract trustagent zip  (trustagent-zip-0.1-SNAPSHOT.zip)
echo "Extracting application..."
TRUSTAGENT_ZIPFILE=`ls -1 trustagent-*.zip 2>/dev/null | head -n 1`
unzip -oq $TRUSTAGENT_ZIPFILE -d $TRUSTAGENT_HOME

# add bin and sbin directories in trustagent home directory to path
bin_directories=$(find_subdirectories ${TRUSTAGENT_HOME} bin; find_subdirectories ${TRUSTAGENT_HOME} sbin)
bin_directories_path=$(join_by : ${bin_directories[@]})
for directory in ${bin_directories[@]}; do
  chmod -R 700 $directory
done
export PATH=$bin_directories_path:$PATH
appendToUserProfileFile "export PATH=${bin_directories_path}:\$PATH" $profile_name

# add lib directories in trustagent home directory to LD_LIBRARY_PATH variable env file
lib_directories=$(find_subdirectories ${TRUSTAGENT_HOME}/share lib)
lib_directories_path=$(join_by : ${lib_directories[@]})
export LD_LIBRARY_PATH=$lib_directories_path
echo "export LD_LIBRARY_PATH=${lib_directories_path}" > $TRUSTAGENT_ENV/trustagent-lib

# update logback.xml with configured trustagent log directory
if [ -f "$TRUSTAGENT_CONFIGURATION/logback.xml" ]; then
  sed -e "s|<file>.*/trustagent.log</file>|<file>$TRUSTAGENT_LOGS/trustagent.log</file>|" $TRUSTAGENT_CONFIGURATION/logback.xml > $TRUSTAGENT_CONFIGURATION/logback.xml.edited
  if [ $? -eq 0 ]; then
    mv $TRUSTAGENT_CONFIGURATION/logback.xml.edited $TRUSTAGENT_CONFIGURATION/logback.xml
  fi
else
  echo_warning "Logback configuration not found: $TRUSTAGENT_CONFIGURATION/logback.xml"
fi

chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $TRUSTAGENT_HOME
chmod 755 $TRUSTAGENT_BIN/*

# 15. symlink tagent
# if prior version had control script in /usr/local/bin, delete it
if [ "$(whoami)" == "root" ] && [ -f /usr/local/bin/tagent ]; then
  rm /usr/local/bin/tagent
fi
EXISTING_TAGENT_COMMAND=`which tagent 2>/dev/null`
if [ -n "$EXISTING_TAGENT_COMMAND" ]; then
  rm -f "$EXISTING_TAGENT_COMMAND"
fi
# link /usr/local/bin/tagent -> /opt/trustagent/bin/tagent
ln -s $TRUSTAGENT_BIN/tagent.sh /usr/local/bin/tagent
if [[ ! -h $TRUSTAGENT_BIN/tagent ]]; then
  ln -s $TRUSTAGENT_BIN/tagent.sh $TRUSTAGENT_BIN/tagent
fi

#16. install application agent
if [[ "$(whoami)" == "root" && ${DOCKER} == "false" ]]; then
  if [ "$TBOOTXM_INSTALL" != "N" ] && [ "$TBOOTXM_INSTALL" != "No" ] && [ "$TBOOTXM_INSTALL" != "n" ] && [ "$TBOOTXM_INSTALL" != "no" ]; then
    echo "Installing application agent..."
    TBOOTXM_PACKAGE=`ls -1 application-agent*.bin 2>/dev/null | tail -n 1`
    if [ -z "$TBOOTXM_PACKAGE" ]; then
      echo_failure "Failed to find application agent installer package"
      exit -1
    fi
    ./$TBOOTXM_PACKAGE
    if [ $? -ne 0 ]; then echo_failure "Failed to install application agent"; exit -1; fi
  fi

  #Added execute permission for measure binary
  chmod o+x /opt/tbootxm
  chmod o+x /opt/tbootxm/bin/
  chmod o+x /opt/tbootxm/lib/
  chmod o+x /opt/tbootxm/bin/measure
  chmod o+x /opt/tbootxm/lib/libwml.so
fi

# 17. migrate any old data to the new locations (v1 - v3)  (should be rewritten in java)
v1_aik=$TRUSTAGENT_V_1_2_CONFIGURATION/cert
v2_aik=$TRUSTAGENT_CONFIGURATION
v1_conf=$TRUSTAGENT_V_1_2_CONFIGURATION
v2_conf=$TRUSTAGENT_CONFIGURATION
if [ -d "$v1_aik" ]; then
  cp $v1_aik/aikblob.dat $v2_aik/aik.blob
  cp $v1_aik/aikcert.pem $v2_aik/aik.pem
fi
if [ -d "$v1_conf" ]; then
  # find the existing tpm owner and aik secrets
  TpmOwnerAuth_121=`read_property_from_file TpmOwnerAuth ${v1_conf}/hisprovisioner.properties`
  HisIdentityAuth_121=`read_property_from_file HisIdentityAuth ${v1_conf}/hisprovisioner.properties`
  TpmOwnerAuth_122=`read_property_from_file TpmOwnerAuth ${v1_conf}/trustagent.properties`
  HisIdentityAuth_122=`read_property_from_file HisIdentityAuth ${v1_conf}/trustagent.properties`
  if [ -z "$TpmOwnerAuth_122" ] && [ -n "$TpmOwnerAuth_121" ]; then
    export TPM_OWNER_SECRET=$TpmOwnerAuth_121
  elif [ -n "$TpmOwnerAuth_122" ]; then
    export TPM_OWNER_SECRET=$TpmOwnerAuth_122
  fi
  if [ -z "$HisIdentityAuth_122" ] && [ -n "$HisIdentityAuth_121" ]; then
    export AIK_SECRET=$HisIdentityAuth_121
  elif [ -n "$HisIdentityAuth_122" ]; then
    export AIK_SECRET=$HisIdentityAuth_122
  fi

  # now copy the keystore and the keystore password
  KeystorePassword_122=`read_property_from_file trustagent.keystore.password ${v1_conf}/trustagent.properties`
  if [ -n "$KeystorePassword_122" ]; then
    export TRUSTAGENT_KEYSTORE_PASSWORD=$KeystorePassword_122
    if [ -f "$v1_conf/trustagent.p12" ]; then
      cp $v1_conf/trustagent.p12 $v2_conf
    fi
  fi
fi

# Redefine the variables to the new locations
package_config_filename=$TRUSTAGENT_CONFIGURATION/trustagent.properties

if is_suefi_enabled; then
  export SKIP_INSTALL_TBOOT="y"
fi

# if we are building a docker image, skip this step
if [[ "$(whoami)" == "root" && ${DOCKER} != "true" && "$SKIP_INSTALL_TBOOT" != "y" && "$SKIP_INSTALL_TBOOT" != "Y" && "$SKIP_INSTALL_TBOOT" != "yes" ]]; then
  # this section adds tagent sudoers file so that user can execute txt-stat command
  txtStat=$(which txt-stat 2>/dev/null)
  if [ -z "$txtStat" ]; then
    echo_failure "cannot find command: txt-stat (from tboot)"
    exit 1
  else
    echo -e "Cmnd_Alias PACKAGE_MANAGER = ${txtStat}\nDefaults:${TRUSTAGENT_USERNAME} "'!'"requiretty\n${TRUSTAGENT_USERNAME} ALL=(root) NOPASSWD: PACKAGE_MANAGER" > "/etc/sudoers.d/${TRUSTAGENT_USERNAME}"
    chmod 440 "/etc/sudoers.d/${TRUSTAGENT_USERNAME}"
  fi
fi

hex2bin=`which hex2bin 2>/dev/null`
if [ -z "$hex2bin" ]; then
  echo_failure "cannot find command: hex2bin"
  exit 1
fi

# 19. copy utilities script file to application folder
mkdir -p "$TRUSTAGENT_HOME"/share/scripts
cp version "$TRUSTAGENT_HOME"/share/scripts/version.sh
cp functions "$TRUSTAGENT_HOME"/share/scripts/functions.sh
chmod -R 700 "$TRUSTAGENT_HOME"/share/scripts
chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME "$TRUSTAGENT_HOME"/share/scripts
chmod +x $TRUSTAGENT_BIN/*

# 20. delete existing dependencies from java folder, to prevent duplicate copies ## DD: Move configs to one place
JAVA_CMD=$(type -p java | xargs readlink -f)
JAVA_HOME=$(dirname $JAVA_CMD | xargs dirname | xargs dirname)
JAVA_REQUIRED_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
# store java location in env file
echo "# $(date)" > $TRUSTAGENT_ENV/trustagent-java
echo "export JAVA_HOME=$JAVA_HOME" >> $TRUSTAGENT_ENV/trustagent-java
echo "export JAVA_CMD=$JAVA_CMD" >> $TRUSTAGENT_ENV/trustagent-java
echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> $TRUSTAGENT_ENV/trustagent-java

if [ -f "${JAVA_HOME}/jre/lib/security/java.security" ]; then
  echo "Replacing java.security file, existing file will be backed up"
  backup_file "${JAVA_HOME}/jre/lib/security/java.security"
  cp java.security "${JAVA_HOME}/jre/lib/security/java.security"
fi

# 21. fix_libcrypto for RHEL
# REDHAT ISSUE:
# After installing libcrypto via the package manager, the library cannot be
# found for linking. Solution is to create a missing symlink in /usr/lib64.
# So in general, what we want to do is:
# 1. identify the best version of libcrypto (choose 1.0.0 over 0.9.8)
# 2. identify which lib directory it's in (/usr/lib64, etc)
# 3. create a symlink from libcrypto.so to libcrypto.so.1.0.0 
# 4. run ldconfig to capture it
# 5. run ldconfig -p to ensure it is found
fix_libcrypto() {
  #yum_detect; yast_detect; zypper_detect; rpm_detect; aptget_detect; dpkg_detect;
  local has_libcrypto=`find / -name libcrypto.so.1.0.0 2>/dev/null | head -1`
  local libdir=`dirname $has_libcrypto`
  local has_libdir_symlink=`find $libdir -name libcrypto.so`
  local has_usrbin_symlink=`find /usr/bin -name libcrypto.so`
  local has_usrlib_symlink=`find /usr/lib -name libcrypto.so`
  if [ -n "$has_libcrypto" ]; then
    if [ -z "$has_libdir_symlink" ] && [ ! -h $libdir/libcrypto.so ]; then
      echo "Creating missing symlink for $has_libcrypto"
      ln -s $libdir/libcrypto.so.1.0.0 $libdir/libcrypto.so
    fi
    #if [ -z "$has_usrbin_symlink" ] && [ ! -h /usr/lib/libcrypto.so ]; then
    if [ -z "$has_usrbin_symlink" ] && [ -z "$has_usrlib_symlink" ]; then
      echo "Creating missing symlink for $has_libcrypto"
      ln -s $libdir/libcrypto.so.1.0.0 /usr/lib/libcrypto.so
    fi
    
    #if [ -n "$yum" ]; then #RHEL
    #elif [[ -n "$zypper" || -n "$yast" ]]; then #SUSE
    #fi

    ldconfig
  fi
}
if [ "$(whoami)" == "root" ]; then
  fix_libcrypto
fi

chmod 755 openssl.sh
cp openssl.sh $TRUSTAGENT_HOME/bin

# 22. create trustagent-version file
package_version_filename=$TRUSTAGENT_ENV/trustagent-version
datestr=`date +%Y-%m-%d.%H%M`
touch $package_version_filename
chmod 600 $package_version_filename
chown $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $package_version_filename
echo "# Installed Trust Agent on ${datestr}" > $package_version_filename
echo "TRUSTAGENT_VERSION=${VERSION}" >> $package_version_filename
echo "TRUSTAGENT_RELEASE=\"${BUILD}\"" >> $package_version_filename

if [[ "$(whoami)" == "root" && ${DOCKER} == "false" ]]; then
  echo "Registering tagent in start up"
  register_startup_script $TRUSTAGENT_BIN/tagent tagent $TRUSTAGENT_PID_FILE 21 >>$logfile 2>&1
else
  echo_warning "Skipping startup script registration"
fi

fix_existing_aikcert() {
  local aikdir=$TRUSTAGENT_CONFIGURATION/cert
  if [ -f $aikdir/aikcert.cer ]; then
    # trust agent aikcert.cer is in broken PEM format... it needs newlines every 76 characters to be correct
    cat $aikdir/aikcert.cer | sed 's/.\{76\}/&\n/g' > $aikdir/aikcert.pem
    rm $aikdir/aikcert.cer
    if [ -f ${package_config_filename} ]; then 
       # update aikcert.filename=aikcert.cer to aikcert.filename=aikcert.pem
       update_property_in_file aikcert.filename ${package_config_filename} aikcert.pem
    fi
  fi
}
# 23. fix_existing_aikcert
fix_existing_aikcert



# 24. create TRUSTAGENT_TLS_CERT_IP list of system host addresses
# collect all the localhost ip addresses and make the list available as the
# default if the user has not already set the TRUSTAGENT_TLS_CERT_IP variable
#DEFAULT_TRUSTAGENT_TLS_CERT_IP=`hostaddress_list_csv`
#if [ -n "$TRUSTAGENT_TLS_CERT_IP" ]; then
#  export TRUSTAGENT_TLS_CERT_IP=$DEFAULT_TRUSTAGENT_TLS_CERT_IP
#fi
# corresponding hostnames to be a default for TRUSTAGENT_TLS_CERT_DNS
#DEFAULT_TRUSTAGENT_TLS_CERT_DNS=`hostaddress_list_csv`
#if [ -n "$TRUSTAGENT_TLS_CERT_DNS" ]; then
#  export TRUSTAGENT_TLS_CERT_DNS=$DEFAULT_TRUSTAGENT_TLS_CERT_DNS
#fi

# Ensure we have given trustagent access to its files
for directory in $TRUSTAGENT_HOME $TRUSTAGENT_CONFIGURATION $TRUSTAGENT_ENV $TRUSTAGENT_REPOSITORY $TRUSTAGENT_VAR $TRUSTAGENT_LOGS $TRUSTAGENT_TMP; do
  echo "chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $directory" >>$logfile
  chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $directory 2>>$logfile
done

if [[ "$(whoami)" == "root" && ${DOCKER} != "true" ]]; then
  echo "Updating system information"
  tagent update-system-info 2>/dev/null
else
  echo_warning "Skipping updating system information"
fi

# Make the logs dir owned by tagent user
chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $TRUSTAGENT_LOGS/

# 25. update the extensions cache file
# before running any tagent commands update the extensions cache file
tagent setup update-extensions-cache-file --force 2>/dev/null

# 26. ensure the trustagent owns all the content created during setup
for directory in $TRUSTAGENT_HOME $TRUSTAGENT_CONFIGURATION $TRUSTAGENT_JAVA $TRUSTAGENT_BIN $TRUSTAGENT_ENV $TRUSTAGENT_REPOSITORY $TRUSTAGENT_LOGS $TRUSTAGENT_TMP; do
  chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $directory
done

# 27. update tpm devices permissions to ensure it can be accessed by trustagent
if [ "$TPM_VERSION" == "2.0" ]; then
  if [ "$(whoami)" == "root" ]; then
    # After installing tpm2-abmrd the tss user and group permission rules get written to 60-tpm-udev.rules.
    # The following commands reload and trigger the rules from 60-tpm-udev.rules thus chaging the
    # ownership of tpm device
    /sbin/udevadm control --reload-rules
    /sbin/udevadm trigger --type=devices --action=change
  else
    echo_warning "Skipping update tpm devices permissions"
  fi

  # add tagent user to tss group
  usermod -a -G tss $TRUSTAGENT_USERNAME

  # enable and start the tpm2-abrmd service
  systemctl enable tpm2-abrmd.service
  systemctl start tpm2-abrmd.service
fi

if [ "${LOCALHOST_INTEGRATION}" == "yes" ]; then
  /opt/trustagent/bin/tagent.sh localhost-integration
fi

#AAS configuration
tagent config "aas.api.url" "$AAS_API_URL" >/dev/null
#CMS configuration
tagent config "cms.base.url" "$CMS_BASE_URL" >/dev/null
#Get CMS CA Certificate
curl --insecure -X GET -H "Accept: application/x-pem-file" -w "%{http_code}" {$CMS_BASE_URL}/ca-certificates -o {$TRUSTAGENT_CONFIGURATION}/cms-ca.cert

#tagent config mtwilson.extensions.fileIncludeFilter.contains "${MTWILSON_EXTENSIONS_FILEINCLUDEFILTER_CONTAINS:-mtwilson,trustagent,jersey-media-multipart}" >/dev/null
#tagent config mtwilson.extensions.packageIncludeFilter.startsWith "${MTWILSON_EXTENSIONS_PACKAGEINCLUDEFILTER_STARTSWITH:-com.intel,org.glassfish.jersey.media.multipart}" >/dev/null



## dashboard
#tagent config mtwilson.navbar.buttons trustagent-keys,mtwilson-configuration-settings-ws-v2,mtwilson-core-html5 >/dev/null
#tagent config mtwilson.navbar.hometab keys >/dev/null

#tagent config jetty.port ${JETTY_PORT:-80} >/dev/null
#tagent config jetty.secure.port ${JETTY_SECURE_PORT:-443} >/dev/null

#tagent setup
#tagent start

# 30. create a trustagent username "mtwilson" with no password and all privileges for mtwilson access
# create a trustagent username "mtwilson" with no password and all privileges
# which allows mtwilson to access it until mtwilson UI is updated to allow
# entering username and password for accessing the trust agent

# Starting with 3.0, we have a separate task that creates a new user name and password per host
# So we do not need to create this user without password. This is would address the security issue as well
#/usr/local/bin/tagent password mtwilson --nopass *:*

# give tagent a chance to do any other setup (such as the .env file and pcakey)
# and make sure it's successful before trying to start the trust agent
# NOTE: only the output from start-http-server is redirected to the logfile;
#       the stdout from the setup command will be displayed
#tagent setup
#tagent start >>$logfile  2>&1

########################################################################################################################
# 28. config logrotate
mkdir -p /etc/logrotate.d

if [ ! -a /etc/logrotate.d/trustagent ]; then
 echo "/opt/trustagent/logs/trustagent.log {
    missingok
	notifempty
	rotate $LOG_OLD
	maxsize $LOG_SIZE
    nodateext
	$LOG_ROTATION_PERIOD
	$LOG_COMPRESS
	$LOG_DELAYCOMPRESS
	$LOG_COPYTRUNCATE
}" > /etc/logrotate.d/trustagent
fi

# exit trustagent setup if TRUSTAGENT_NOSETUP is set
if [ -n "$TRUSTAGENT_NOSETUP" ]; then
  echo "TRUSTAGENT_NOSETUP value is set. So, skipping the trustagent setup task."
  exit 0;
fi

#Copy default and workload software manifest to /opt/trustagent/var/
if ! stat $TRUSTAGENT_VAR/manifest_* 1> /dev/null 2>&1; then
  TA_VERSION=`tagent version | grep Version | awk '{print $2}' |  cut -d '-' -f1`
  UUID=$(uuidgen)
  cp manifest_tpm20.xml $TRUSTAGENT_VAR/manifest_"$UUID".xml
  sed -i "s/Uuid=\"\"/Uuid=\"${UUID}\"/g" $TRUSTAGENT_VAR/manifest_"$UUID".xml
  sed -i "s/Label=\"ISecL_Default_Application_Flavor_v\"/Label=\"ISecL_Default_Application_Flavor_v${TA_VERSION}_TPM2.0\"/g" $TRUSTAGENT_VAR/manifest_"$UUID".xml

  UUID=$(uuidgen)
  cp manifest_wlagent.xml $TRUSTAGENT_VAR/manifest_"$UUID".xml
  sed -i "s/Uuid=\"\"/Uuid=\"${UUID}\"/g" $TRUSTAGENT_VAR/manifest_"$UUID".xml
  sed -i "s/Label=\"ISecL_Default_Workload_Flavor_v\"/Label=\"ISecL_Default_Workload_Flavor_v${TA_VERSION}\"/g" $TRUSTAGENT_VAR/manifest_"$UUID".xml
fi

#Stop further installation if there is no TPM driver loaded
if [[ $ipResult -eq 254 ]]; then
  mkdir -p "$TRUSTAGENT_HOME/var"
  touch "$TRUSTAGENT_HOME/var/reboot_required"
  echo
  echo_warning "Trust Agent: TPM driver is not loaded. Please load it and run trustagent setup."
  echo
  exit 254
fi

# 29. tagent setup
tagent setup

configure_cron add "$TRUSTAGENT_TMPCLN_INT" "find "$TRUSTAGENT_TMP" -mtime +"$TRUSTAGENT_TMPCLN_AGE" -exec /bin/rm -- '{}' \;"
# 30. tagent start
tagent start

if [[ "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ]]; then
    authorize_vars="MTWILSON_API_URL MTWILSON_API_USERNAME MTWILSON_API_PASSWORD MTWILSON_TLS_CERT_SHA384"
    for var in $authorize_vars
    do
        if [ ! -v $var ]
        then
            echo "Parameters needed to provision Trust agent not provided. So skipping the provision step."
            exit 0;
        fi
    done    
    tagent provision-attestation
    if [ $? -ne 0 ]; then
        exit 1
    fi
fi

# install workload agent
if [[ "$INSTALL_WORKLOAD_AGENT" != "n" && "$INSTALL_WORKLOAD_AGENT" != "N" && "$INSTALL_WORKLOAD_AGENT" != "no" ]]; then
  # make sure that we have exported the TRUSTAGENT_CONFIGURATION and TRUSTAGENT_USERNAME as these are required by
  # the workload_agent installer
  export TRUSTAGENT_CONFIGURATION
  export TRUSTAGENT_USERNAME

  ./workload-agent-*.bin
  if [ $? -ne 0 ]; then
    echo_failure "Workload Agent Installation Failed"
    exit 1
  fi
fi

# TODO: when sequence of events are optimized
tagent restart

# register host with HVS
if [[( "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ) && ( "$AUTOMATIC_REGISTRATION" == "y" || "$AUTOMATIC_REGISTRATION" == "Y" || "$AUTOMATIC_REGISTRATION" == "yes" )]]; then
    tagent create-host
fi

if [[( "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ) && ( "$AUTOMATIC_PULL_MANIFEST" == "y" || "$AUTOMATIC_PULL_MANIFEST" == "Y" || "$AUTOMATIC_PULL_MANIFEST" == "yes" )]]; then
    tagent get-configured-manifest
fi

if [[( "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ) && ( "$AUTOMATIC_FLAVOR_CREATION" == "y" || "$AUTOMATIC_FLAVOR_CREATION" == "Y" || "$AUTOMATIC_FLAVOR_CREATION" == "yes" )]]; then
    if [[ $ipResult -ne 255 ]]; then
        tagent create-host-unique-flavor
    else
        echo "Host not in measured launch environment. So skipping the automatic flavor creation step."
    fi
fi

#Prompt for reboot if there is no Measured Launch Environment
if [[ $ipResult -eq 255 ]]; then
  mkdir -p "$TRUSTAGENT_HOME/var"
  touch "$TRUSTAGENT_HOME/var/reboot_required"
  echo
  echo_warning "Trust Agent: Not in Measured Launch Environment. Please reboot host."
  echo
  exit 255
fi

#exit

# 32. register tpm password with mtwilson
# optional: register tpm password with mtwilson so pull provisioning can
#           be accomplished with less reboots (no ownership transfer)
#           default is not to register the password.
#prompt_with_default REGISTER_TPM_PASSWORD       "Register TPM password with service to support asset tag automation? [y/n]" ${REGISTER_TPM_PASSWORD:-no}
#if [[ "$REGISTER_TPM_PASSWORD" == "y" || "$REGISTER_TPM_PASSWORD" == "Y" || "$REGISTER_TPM_PASSWORD" == "yes" ]]; then 
  #prompt_with_default ASSET_TAG_URL "Asset Tag Server URL: (https://[SERVER]:[PORT]/mtwilson/v2)" ${ASSET_TAG_URL}
#  prompt_with_default MTWILSON_API_USERNAME "Username:" ${MTWILSON_API_USERNAME}
#  prompt_with_default_password MTWILSON_API_PASSWORD "Password:" ${MTWILSON_API_PASSWORD}
#  export MTWILSON_API_USERNAME MTWILSON_API_PASSWORD
#  export HARDWARE_UUID=`dmidecode |grep UUID | awk '{print $2}'`
#  tagent setup register-tpm-password
#fi


# remove the temporary setup env file
#rm -f $TRUSTAGENT_ENV/trustagent-setup
