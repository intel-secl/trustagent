#!/bin/bash

# chkconfig: 2345 80 30
# description: Intel TrustAgent Service

### BEGIN INIT INFO
# Provides:          tagent
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Should-Start:      $portmap
# Should-Stop:       $portmap
# X-Start-Before:    nis
# X-Stop-After:      nis
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# X-Interactive:     true
# Short-Description: trust agent script
# Description:       Main script to run trust agent tasks
### END INIT INFO
DESC="Trust Agent"
NAME=tagent
DAEMON=/opt/trustagent/bin/$NAME

if [[ ${container} == "docker" ]]; then
    DOCKER=true
else
    DOCKER=false
fi

###################################################################################################
#Set environment specific variables here 
###################################################################################################

# the home directory must be defined before we load any environment or
# configuration files; it is explicitly passed through the sudo command
export TRUSTAGENT_HOME=${TRUSTAGENT_HOME:-/opt/trustagent}

# the env directory is not configurable; it is defined as TRUSTAGENT_HOME/env.d and the
# administrator may use a symlink if necessary to place it anywhere else
export TRUSTAGENT_ENV=$TRUSTAGENT_HOME/env.d

trustagent_load_env() {
  local env_files="$@"
  local env_file_exports
  for env_file in $env_files; do
    if [ -n "$env_file" ] && [ -f "$env_file" ]; then
      . $env_file
      env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
      if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
    fi
  done
}

load_trustagent_prov_env() {
  local env_file="$@"
  if [ -z $env_file ]; then
    echo "No environment file provided"
    return
  fi

  # load installer environment file, if present
  if [ -r $env_file ]; then
    echo "Loading environment variables from $env_file"
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    #echo $env_file_exports
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  else
    echo "Trust Agent does not have permission to read environment file"
  fi
}

if [ -a "$2" ]; then
  load_trustagent_prov_env $2 2>&1 >/dev/null
fi

register_tpm_password() {
  prompt_with_default REGISTER_TPM_PASSWORD       "Register TPM password with service to support asset tag automation? [y/n]" ${REGISTER_TPM_PASSWORD:-no}
  if [[ "$REGISTER_TPM_PASSWORD" == "y" || "$REGISTER_TPM_PASSWORD" == "Y" || "$REGISTER_TPM_PASSWORD" == "yes" ]]; then
    trustagent_setup register-tpm-password
fi


}

# load environment variables; these override any existing environment variables.
# the idea is that if someone wants to override these, they must have write
# access to the environment files that we load here. 
if [ -d $TRUSTAGENT_ENV ]; then
  trustagent_load_env $(ls -1 $TRUSTAGENT_ENV/*)
fi

###################################################################################################

# default directory layout follows the 'home' style
TRUSTAGENT_CONFIGURATION=${TRUSTAGENT_CONFIGURATION:-${TRUSTAGENT_CONF:-$TRUSTAGENT_HOME/configuration}}
TRUSTAGENT_JAVA=${TRUSTAGENT_JAVA:-$TRUSTAGENT_HOME/java}
TRUSTAGENT_BIN=${TRUSTAGENT_BIN:-$TRUSTAGENT_HOME/bin}
TRUSTAGENT_ENV=${TRUSTAGENT_ENV:-$TRUSTAGENT_HOME/env.d}
TRUSTAGENT_VAR=${TRUSTAGENT_VAR:-$TRUSTAGENT_HOME/var}
TRUSTAGENT_LOGS=${TRUSTAGENT_LOGS:-$TRUSTAGENT_HOME/logs}
TRUSTAGENT_TMP=${TRUSTAGENT_TMP:-$TRUSTAGENT_HOME/var/tmp}
TRUSTAGENT_TMPCLN_INT=${TRUSTAGENT_TMPCLN_INT:-* 0 * * *}
TRUSTAGENT_TMPCLN_AGE=${TRUSTAGENT_TMPCLN_AGE:-7}
###################################################################################################

# load linux utility
if [ -f "$TRUSTAGENT_HOME/share/scripts/functions.sh" ]; then
  . $TRUSTAGENT_HOME/share/scripts/functions.sh
fi

# stored master password
if [ -z "$TRUSTAGENT_PASSWORD" ] && [ -f $TRUSTAGENT_CONFIGURATION/.trustagent_password ]; then
  export TRUSTAGENT_PASSWORD=$(cat $TRUSTAGENT_CONFIGURATION/.trustagent_password)
fi

###################################################################################################

# all other variables with defaults
TRUSTAGENT_PID_FILE=$TRUSTAGENT_HOME/trustagent.pid
TRUSTAGENT_HTTP_LOG_FILE=$TRUSTAGENT_LOGS/http.log
TRUSTAGENT_LOG4J_LOG_FILE=$TRUSTAGENT_LOGS/trustagent-log4j.log
TRUSTAGENT_AUTHORIZE_TASKS="download-mtwilson-privacy-ca-certificate request-endorsement-certificate request-aik-certificate $TRUSTAGENT_VM_ATTESTATION_SETUP_TASKS"
TRUSTAGENT_REGISTRATION_TASKS="attestation-registration"
TRUSTAGENT_CREATE_FLAVOR_TASK="create-host-unique-flavor"
TRUSTAGENT_GET_MANIFEST_TASK="get-configured-manifest"
TRUSTAGENT_TPM_TASKS="create-tpm-owner-secret create-tpm-srk-secret create-aik-secret take-ownership"
TRUSTAGENT_START_TASKS="secure-store create-tpm-owner-secret take-ownership"
TRUSTAGENT_VM_ATTESTATION_SETUP_TASKS="create-binding-key certify-binding-key create-signing-key certify-signing-key"
TRUSTAGENT_SETUP_TASKS="update-extensions-cache-file secure-store jetty-tls-keystore $TRUSTAGENT_TPM_TASKS"
#TRUSTAGENT_SETUP_TASKS="update-extensions-cache-file jetty-tls-keystore"
# not including configure-from-environment because we are running it always before the user-chosen tasks
# not including register-tpm-password because we are prompting for it in the setup.sh
JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.8}
JAVA_OPTS="-Dlogback.configurationFile=$TRUSTAGENT_CONFIGURATION/logback.xml -Dlog4j.configuration=file:$TRUSTAGENT_CONFIGURATION/log4j.properties -Dfs.name=trustagent -Djdk.tls.ephemeralDHKeySize=2048 -Djava.io.tmpdir=$TRUSTAGENT_TMP"
JAVA_OPTS="${JAVA_OPTS} -Djava.net.preferIPv4Stack=true"
if [[ -n ${DEBUG} ]]; then
    JAVA_OPTS="${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n"
fi

###################################################################################################

# ensure that our commands can be found
export PATH=$TRUSTAGENT_BIN/bin:$PATH

# ensure that trousers (/usr/sbin) and tpm tools (/usr/local/sbin) are found
export PATH=$PATH:/usr/sbin:/usr/local/sbin

# java command
if [ -z "$JAVA_CMD" ]; then
  if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD=$JAVA_HOME/bin/java
  else
    JAVA_CMD=`which java`
  fi
fi

if [ -z "$JAVA_CMD" ]; then
  echo_failure "Cannot find java binary from values in $TRUSTAGENT_ENV"
  exit -1
fi

###################################################################################################

# generated variables
JARS=$(ls -1 $TRUSTAGENT_JAVA/*.jar)
CLASSPATH=$(echo $JARS | tr ' ' ':')

# the classpath is long and if we use the java -cp option we will not be
# able to see the full command line in ps because the output is normally
# truncated at 4096 characters. so we export the classpath to the environment
export CLASSPATH

###################################################################################################

# writes system information into files non-root trustagent can read
# (root access required to run)
trustagent_update_system_info() {
  if [ "$(whoami)" == "root" ]; then
    # user is root, so run the commands and cache the output
    mkdir -p $TRUSTAGENT_VAR/system-info

    $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.trustagent.ws.v2.PlatformInfoCollector > $TRUSTAGENT_VAR/system-info/platform-info
    chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $TRUSTAGENT_VAR
  else
    echo_failure "Must run 'tagent update-system-info' as root"
  fi
}

if ([ "$1" == "start" ] || [ "$1" == "restart" ]) && [ $(whoami) == "root" ]; then
   trustagent_update_system_info $*
fi

###################################################################################################

# if non-root execution is specified, and we are currently root, start over; the TRUSTAGENT_SUDO variable limits this to one attempt
# we make an exception for the following commands:
# - 'uninstall' may require root access to delete users and certain directories
# - 'update-system-info' requires root access to use dmidecode and virsh commands
# - 'restart' requires root access as it calls trustagent_update_system_info to update system information
if [ -n "$TRUSTAGENT_USERNAME" ] && [ "$TRUSTAGENT_USERNAME" != "root" ] && [ $(whoami) == "root" ] && [ -z "$TRUSTAGENT_SUDO" ] && [ "$1" != "uninstall" ] && [ "$1" != "update-system-info" ] && [ "$1" != "restart" ] && [[ "$1" != "replace-"* ]]; then

  #Start TPM resource manager
  service tcsd2 stop >/dev/null 2>&1
  service tpm2-abrmd start >/dev/null 2>&1

  export TRUSTAGENT_SUDO=true
  sudo -u $TRUSTAGENT_USERNAME -H -E $TRUSTAGENT_BIN/tagent $*
  exit $?
fi

###################################################################################################

# run a trustagent command
trustagent_run() {
  local args="$*"
  $JAVA_CMD $JAVA_OPTS com.intel.dcsg.cpg.console.Main $args
  return $?
}

# arguments are optional, if provided they are the names of the tasks to run, in order
trustagent_setup() {
  export HARDWARE_UUID=$(trustagent_system_info "hardware-uuid")
  local tasklist="$*"

  #DEFAULT_TRUSTAGENT_TLS_CERT_IP=`hostaddress_list_csv`
  #echo $DEFAULT_TRUSTAGENT_TLS_CERT_IP
  #if [ -n "$TRUSTAGENT_TLS_CERT_IP" ]; then
  #  export TRUSTAGENT_TLS_CERT_IP=$DEFAULT_TRUSTAGENT_TLS_CERT_IP
  #fi
  #echo $TRUSTAGENT_TLS_CERT_IP

  if [ -z "$tasklist" ]; then
    tasklist=$TRUSTAGENT_SETUP_TASKS
  elif [ "$tasklist" == "--force" ]; then
    tasklist="$TRUSTAGENT_SETUP_TASKS --force"
  fi
  "$JAVA_CMD" $JAVA_OPTS com.intel.mtwilson.launcher.console.Main setup configure-from-environment $tasklist | grep -v "TPM ERROR:"

  return $?
}

# Check and update the TPM version in the trustagent configuration
trustagent_check_tpm_version() {
   detect_tpm_version
   echo -n "$TPM_VERSION" > $TRUSTAGENT_CONFIGURATION/tpm-version
}

# Creates a password if not created earlier
trustagent_gen_master_password() {
  if [ ! -f $TRUSTAGENT_CONFIGURATION/.trustagent_password ]; then
     touch $TRUSTAGENT_CONFIGURATION/.trustagent_password
     chown $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $TRUSTAGENT_CONFIGURATION/.trustagent_password

     # Generate a password for config encryption
     "$JAVA_CMD" $JAVA_OPTS com.intel.mtwilson.launcher.console.Main generate-password > $TRUSTAGENT_CONFIGURATION/.trustagent_password
   fi
}

# Encrypts the configuration file with the generated password
trustagent_encrypt_config() {
   # Encrypt the config with the password
   export TRUSTAGENT_PASSWORD=$(cat $TRUSTAGENT_CONFIGURATION/.trustagent_password)
   "$JAVA_CMD" $JAVA_OPTS com.intel.mtwilson.launcher.console.Main import-config --in=${TRUSTAGENT_CONFIGURATION}/trustagent.properties --out=${TRUSTAGENT_CONFIGURATION}/trustagent.properties
}

trustagent_authorize() {
  export HARDWARE_UUID=$(trustagent_system_info "hardware-uuid")
  local authorize_vars="MTWILSON_API_URL"
  local default_value
  for v in $authorize_vars
  do
    default_value=$(eval "echo \$$v")
    prompt_with_default $v "Required: $v" $default_value
  done

  export_vars $authorize_vars

  trustagent_setup --force $TRUSTAGENT_AUTHORIZE_TASKS
  return $?
}

trustagent_start() {

    # check if we're already running - don't start a second instance
    if trustagent_is_running; then
        echo "Trust Agent is running"
        return 0
    fi

    if [[ ${DOCKER} != "true" ]]; then
        # regenerate Measurement log when trustagent is started, but only if not in Docker, since the Docker entrypoint handles it
        rm -rf $TRUSTAGENT_HOME/var/measureLog.xml
        $TRUSTAGENT_HOME/bin/module_analysis.sh 2>/dev/null
    fi

    # the subshell allows the java process to have a reasonable current working
    # directory without affecting the user's working directory. 
    # the last background process pid $! must be stored from the subshell.
    (
      cd /opt/trustagent
      "$JAVA_CMD" $JAVA_OPTS com.intel.mtwilson.launcher.console.Main start-http-server >>$TRUSTAGENT_HTTP_LOG_FILE 2>&1 &
      echo $! > $TRUSTAGENT_PID_FILE
    )
    touch $TRUSTAGENT_LOG4J_LOG_FILE
    if trustagent_is_running; then
      echo_success "Started trust agent"
    else
      echo_failure "Failed to start trust agent"
    fi
}

# returns 0 if trust agent is running, 1 if not running
# side effects: sets TRUSTAGENT_PID if trust agent is running, or to empty otherwise
trustagent_is_running() {
  TRUSTAGENT_PID=
  if [ -f $TRUSTAGENT_PID_FILE ]; then
    TRUSTAGENT_PID=$(cat $TRUSTAGENT_PID_FILE)
    local is_running=`ps -eo pid | grep "^\s*${TRUSTAGENT_PID}$"`
    if [ -z "$is_running" ]; then
      # stale PID file
      TRUSTAGENT_PID=
    fi
  fi
  if [ -z "$TRUSTAGENT_PID" ]; then
    # check the process list just in case the pid file is stale
    TRUSTAGENT_PID=$(ps ww | grep -v grep | grep java | grep "com.intel.mtwilson.launcher.console.Main start-http-server" | awk '{ print $1 }')
  fi
  if [ -z "$TRUSTAGENT_PID" ]; then
    # trust agent is not running
    return 1
  fi
  # trust agent is running and TRUSTAGENT_PID is set
  return 0
}


trustagent_stop() {
  if trustagent_is_running; then
    kill -9 $TRUSTAGENT_PID
    if [ $? ]; then
      echo "Stopped trust agent"
      rm -f $TRUSTAGENT_PID_FILE
    else
      echo "Failed to stop trust agent"
    fi
  fi
}

workload_agent_uninstall() {
  # if an existing tagent is already running, stop it while we install
  existing_wlagent=`which wlagent 2>/dev/null`
  if [ -f "$existing_wlagent" ]; then
    $existing_wlagent uninstall --purge
  fi
}

vrtm_uninstall() {
  VRTM_UNINSTALL_SCRIPT="/opt/vrtm/bin/vrtm-uninstall.sh"
  if [ -f "$VRTM_UNINSTALL_SCRIPT" ]; then
    "$VRTM_UNINSTALL_SCRIPT"
  fi
}

tbootxm_uninstall() {
  TBOOTXM_UNINSTALL_SCRIPT="/opt/tbootxm/bin/tboot-xm-uninstall.sh"
  if [ -f "$TBOOTXM_UNINSTALL_SCRIPT" ]; then
    "$TBOOTXM_UNINSTALL_SCRIPT"
  fi
}

policyagent_uninstall() {
  POLICYAGENT_UNINSTALL_SCRIPT="/opt/policyagent/bin/policyagent.py"
  if [ -f "$POLICYAGENT_UNINSTALL_SCRIPT" ]; then
    "$POLICYAGENT_UNINSTALL_SCRIPT" uninstall
  fi
}

openstack_extensions_uninstall() {
  OPENSTACK_EXTENSIONS_UNINSTALL_SCRIPT="/opt/openstack-ext/bin/mtwilson-openstack-node-uninstall.sh"
  if [ -f "$OPENSTACK_EXTENSIONS_UNINSTALL_SCRIPT" ]; then
    "$OPENSTACK_EXTENSIONS_UNINSTALL_SCRIPT"
  fi
}

docker_proxy_uninstall() {
  DOCKER_PROXY_UNINSTALL_SCRIPT="/opt/docker-proxy/bin/docker-proxy.sh"
  if [ -f "$DOCKER_PROXY_UNINSTALL_SCRIPT" ]; then
    "$DOCKER_PROXY_UNINSTALL_SCRIPT" uninstall --purge
  fi
}

# backs up the configuration directory and removes all trustagent files,
# except for configuration files which are saved and restored
trustagent_uninstall() {
	rm -f /usr/local/bin/tagent
    if [ -n "$TRUSTAGENT_HOME" ] && [ -d "$TRUSTAGENT_HOME" ]; then
      rm -rf $TRUSTAGENT_HOME
    fi
    remove_startup_script tagent
    rm -rf /opt/tbootxm
	rm -rf /var/log/trustagent/measurement.*
    configure_cron remove "$TRUSTAGENT_TMPCLN_INT" "find "$TRUSTAGENT_TMP" -mtime +"$TRUSTAGENT_TMPCLN_AGE" -exec /bin/rm -- '{}' \;"
}



print_help() {
    echo "Usage: $0 start|stop|restart|java-detect|fingerprint|status|uninstall|zeroize|version|create-host|create-host-unique-flavor"
    echo "Usage: $0 provision-attestation (only for non-container deployment)"
    echo "Usage: $0 setup [--force|--noexec] [task1 task2 ...]"
    echo "Usage: $0 export-config [outfile|--in=infile|--out=outfile|--stdout] [--env-password=PASSWORD_VAR]"
    echo "Usage: $0 config [key] [--delete|newValue]"
    echo "Usage: $0 get-configured-manifest [environment-file]"
    echo "Available setup tasks:"
    echo $TRUSTAGENT_SETUP_TASKS | tr ' ' '\n'
    echo register-tpm-password
}


# uses only information cached by trustagent_update_system_info 
# (root access not required)
trustagent_system_info() {
    case "$*" in
      "hardware-uuid")
          cat $TRUSTAGENT_VAR/system-info/platform-info | python -c "import sys, json; print json.load(sys.stdin)[\"hardware-uuid\"]"
          ;;
      "platform-info")
          cat $TRUSTAGENT_VAR/system-info/platform-info
          ;;
      *)
          echo_failure "tagent system-info command not supported: $*"
          ;;
    esac
    return $?
}

tagent_tls_fingerprint() {
  local sha384=`read_property_from_file tls.cert.sha384 "${TRUSTAGENT_CONFIGURATION}/https.properties"`
  echo "SHA384: $sha384"
}

###################################################################################################

# here we look for specific commands first that we will handle in the
# script, and anything else we send to the java application

case "$1" in
  help)
    print_help
    ;;
  start)

    # run setup before starting trust agent to allow taking ownership again if
    # the tpm has been cleared, or re-initializing the keystore if the server
    # ssl cert has changed and the user has updated the fingerprint in
    # the trustagent.properties file
    trustagent_setup $TRUSTAGENT_START_TASKS
    trustagent_start
    ;;
  stop)
    trustagent_stop
    ;;
  restart)
    $TRUSTAGENT_BIN/tagent stop
    $TRUSTAGENT_BIN/tagent start
    ;;
  java-detect)
    java_detect $2
    java_env_report
    ;;
  fingerprint)
    echo "TLS Certificate Fingerprint:"
    tagent_tls_fingerprint
    ;;
  authorize)
    if trustagent_authorize; then
      trustagent_stop
      trustagent_start
    fi
    ;;
  status)
    if trustagent_is_running; then
      echo "Trust agent is running"
      exit 0
    else
      echo "Trust agent is not running"
      exit 1
    fi
    ;;
  update-system-info)
    shift;
    trustagent_update_system_info $*
    ;;
  system-info)
    shift;
    trustagent_system_info $*
    ;;
  setup)

    shift
    if [ -z "$*" ]; then
       trustagent_check_tpm_version
       trustagent_gen_master_password
       trustagent_encrypt_config
    fi

    trustagent_setup $*


    ;;
  zeroize)
    echo "Shredding Trust Agent configuration"
    cd /tmp && find "$TRUSTAGENT_CONFIGURATION/" -type f -exec shred -uzn 3 {} \;
    ;;
  localhost-integration)
    #shiro_localhost_integration "/opt/trustagent/configuration/shiro.ini"
    #/opt/trustagent/bin/tagent.sh restart
    if [ -f "/opt/trustagent/configuration/shiro-localhost.ini" ]; then
      mv /opt/trustagent/configuration/shiro.ini /opt/trustagent/configuration/shiro.ini.bkup 2>/dev/null
      mv /opt/trustagent/configuration/shiro-localhost.ini /opt/trustagent/configuration/shiro.ini 2>/dev/null
      /opt/trustagent/bin/tagent.sh restart
    fi
    ;;
  provision-attestation)
    #if [ -z "$2" ]
    #then
    #  echo "Attestation provisioning needs attestation server details"
    #  exit
    #fi

    trustagent_start
    trustagent_setup
    
    if trustagent_authorize; then
      trustagent_stop
      trustagent_start
    fi
    
    register_tpm_password
    rm -f $TRUSTAGENT_ENV/trustagent-setup

    registration_vars="CURRENT_IP"
    for variables in $registration_vars
    do
        if [ ! -v $variables ]
        then
            echo "Parameters needed to register host with attestation server are not provided. So skipping the registration step."
            exit 0;
        fi
    done

    ;;
  create-host)
   
    trustagent_setup --force $TRUSTAGENT_REGISTRATION_TASKS
    
    ;;
  create-host-unique-flavor)
   
    trustagent_setup --force $TRUSTAGENT_CREATE_FLAVOR_TASK
    ;;
  get-configured-manifest)

    trustagent_setup --force $TRUSTAGENT_GET_MANIFEST_TASK
    ;;
  uninstall)
    workload_agent_uninstall
    trustagent_stop
	policyagent_uninstall
    vrtm_uninstall
	tbootxm_uninstall    
    openstack_extensions_uninstall
	docker_proxy_uninstall
    trustagent_uninstall
    userdel tagent > /dev/null 2>&1
    ;;
  *)
    if [ -z "$*" ]; then
      print_help
    else
      #echo "args: $*"
      "$JAVA_CMD" $JAVA_OPTS com.intel.mtwilson.launcher.console.Main $*
    fi
    ;;
esac


exit $?
