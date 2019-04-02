#!/bin/bash

# Preconditions:
# * http_proxy and https_proxy are already set, if required
# * date and time are synchronized with remote server, if using remote attestation service
# * the mtwilson linux util functions already sourced
#   (for add_package_repository, echo_success, echo_failure)
# * TRUSTAGENT_HOME is set, for example /opt/trustagent
# * TRUSTAGENT_INSTALL_LOG_FILE is set, for example /opt/trustagent/logs/install.log
# * TPM_VERSION is set, for example 1.2 or else it will be auto-detected

# Postconditions:
# * All messages logged to stdout/stderr; caller redirect to logfile as needed

# NOTE:  \cp escapes alias, needed because some systems alias cp to always prompt before override

# Outline:
# 1. Start tcsd (it already has an init script for next boot, but we need it now)

# source functions file
if [ -f functions ]; then . functions; fi

TRUSTAGENT_HOME=${TRUSTAGENT_HOME:-/opt/trustagent}
LOGFILE=${TRUSTAGENT_INSTALL_LOG_FILE:-$TRUSTAGENT_HOME/logs/install.log}
mkdir -p $(dirname $LOGFILE)

if [ -z "$TPM_VERSION" ]; then
  detect_tpm_version
fi

# 3. Start tcsd (it already has an init script for next boot, but we need it now)

# tpm 1.2
is_tcsd_running() {
  local tcsd_pid=$(ps aux | grep tcsd | grep -v grep)
  if [ -n "$tcsd_pid" ]; then
    return 0
  else
    return 1
  fi
}

# tpm 1.2
start_tcsd() {
  local tcsd_cmd=$(which tcsd 2>/dev/null)
  if [ -n "$tcsd_cmd" ]; then
    echo "starting tcsd"
    tcsd
  fi
}

# tpm 2.0
is_tcsd2_running() {
  systemctl status tcsd2 >/dev/null 2>&1
}

start_tcsd2() {
  systemctl start tcsd2 >/dev/null 2>&1
}

if [ "$TPM_VERSION" == "1.2" ]; then
  if is_tcsd_running; then
    echo "tcsd already running"
  else
    start_tcsd
  fi
elif [ "$TPM_VERSION" == "2.0" ]; then
  if is_tcsd2_running; then
    echo "tcsd2 already running"
  else
    start_tcsd2
  fi
elif [ -z "$TPM_VERSION" ]; then
  echo "Cannot detect TPM version"
else
  echo "Unrecognized TPM version: $TPM_VERSION"
fi
