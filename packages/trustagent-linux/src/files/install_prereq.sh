#!/bin/bash

# Preconditions:
# * http_proxy and https_proxy are already set, if required
# * date and time are synchronized with remote server, if using remote attestation service
# * the mtwilson linux util functions already sourced
#   (for add_package_repository, echo_success, echo_failure)
# * TPM_VERSION is set, for example 1.2 or else it will be auto-detected

# Postconditions:
# * All messages logged to stdout/stderr; caller redirect to logfile as needed

# NOTE:  \cp escapes alias, needed because some systems alias cp to always prompt before override

# Outline:
# 1. Install redhat-lsb-core and other redhat-specific packages
# 2. Install unzip vim-common packages
# 3. Install java

if [[ ${container} == "docker" ]]; then
    DOCKER=true
else
    DOCKER=false
fi

# source functions file
if [ -f functions ]; then . functions; fi

TRUSTAGENT_HOME=${TRUSTAGENT_HOME:-/opt/trustagent}
LOGFILE=${TRUSTAGENT_INSTALL_LOG_FILE:-$TRUSTAGENT_HOME/logs/install.log}
mkdir -p $(dirname $LOGFILE)

if [ -z "$TPM_VERSION" ]; then
  detect_tpm_version
fi

################################################################################

# 1. Install redhat-lsb-core and other redhat-specific packages
install_redhat_packages() {
  if [ "$IS_RPM" != "true" ]; then
    # Add epel-release-latest-7.noarch repository;
    add_package_repository https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
    TRUSTAGENT_REDHAT_YUM_PACKAGES="redhat-lsb net-tools redhat-lsb-core"
    install_packages "redhat" "TRUSTAGENT_REDHAT"
  fi
}

if yum_detect; then
  if [[ "$SKIP_INSTALL_REDHAT_PACKAGES" != "y" && "$SKIP_INSTALL_REDHAT_PACKAGES" != "Y" && "$SKIP_INSTALL_REDHAT_PACKAGES" != "yes" ]]; then
    install_redhat_packages
  fi
fi

install_openssl() {
if [ "$IS_RPM" != "true" ]; then
  TRUSTAGENT_OPENSSL_YUM_PACKAGES="openssl openssl-devel"
fi
  TRUSTAGENT_OPENSSL_APT_PACKAGES="openssl libssl-dev"
  TRUSTAGENT_OPENSSL_YAST_PACKAGES="openssl libopenssl-devel"
  TRUSTAGENT_OPENSSL_ZYPPER_PACKAGES="openssl libopenssl-devel libopenssl1_0_0 openssl-certs"
  install_packages "openssl" "TRUSTAGENT_OPENSSL" > /dev/null 2>&1
}

# 2. Install unzip vim-common packages
# make sure unzip is installed
#java_required_version=1.8
#Adding redhat-lsb for bug 5289
#Adding net-tools for bug 5285
#adding openssl-devel for bug 5284
TRUSTAGENT_YUM_PACKAGES="unzip vim-common cpuid msr-tools"
TRUSTAGENT_APT_PACKAGES="unzip dpkg-dev vim-common cpuid msr-tools"
TRUSTAGENT_YAST_PACKAGES="unzip vim-common"
TRUSTAGENT_ZYPPER_PACKAGES="unzip vim-common"

##### install prereqs can only be done as root
if [ "$(whoami)" == "root" ]; then
  install_packages "Installer requirements" "TRUSTAGENT"
  if [ $? -ne 0 ]; then echo_failure "Failed to install prerequisites through package installer"; exit 1; fi
else
  echo_warning "Required packages:"
  auto_install_preview "TrustAgent requirements" "TRUSTAGENT"
fi

###### Check if sUEFI enabled #######
if is_suefi_enabled; then
  export SUEFI_ENABLED="true"
  echo_warning "As sUEFI feature is enabled, skipping tboot installation"
  export SKIP_INSTALL_TBOOT="y"
fi

install_tboot() {
  ./tboot-linux-*.bin
}

if [[ "$SKIP_INSTALL_TBOOT" != "y" && "$SKIP_INSTALL_TBOOT" != "Y" && "$SKIP_INSTALL_TBOOT" != "yes" ]]; then
  install_tboot
  result=$?
fi
install_openssl


# 3. Install java
# Trust Agent requires java 1.8 or later
echo "Installing Java..."
if [ "$IS_RPM" != "true" ]; then
  java_install_openjdk
fi


if [ -n $result ]; then exit $result; fi
