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
# 2. Install unzip authbind vim-common packages
# 3. Install trousers and trousers-devel packages (current is trousers-0.3.13-1.el7.x86_64)
# 4. Install the patched tpm-tools
# 5. Install java
# 6. Install monit

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
    # Add epel-release-latest-7.noarch repository; required for monit
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

# 2. Install unzip authbind vim-common packages
# make sure unzip is installed
#java_required_version=1.8
#Adding redhat-lsb for bug 5289
#Adding net-tools for bug 5285
#adding openssl-devel for bug 5284
TRUSTAGENT_YUM_PACKAGES="unzip vim-common cpuid msr-tools mokutil"
TRUSTAGENT_APT_PACKAGES="unzip dpkg-dev authbind vim-common cpuid msr-tools mokutil"
TRUSTAGENT_YAST_PACKAGES="unzip authbind vim-common"
TRUSTAGENT_ZYPPER_PACKAGES="unzip authbind vim-common"

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

# 3. Install trousers and trousers-devel packages (current is trousers-0.3.13-1.el7.x86_64)
# tpm 1.2
install_trousers() {
if [ "$IS_RPM" != "true" ]; then
  TRUSTAGENT_TROUSERS_YUM_PACKAGES="trousers trousers-devel"
fi
  TRUSTAGENT_TROUSERS_APT_PACKAGES="trousers trousers-dbg libtspi-dev libtspi1"
  TRUSTAGENT_TROUSERS_YAST_PACKAGES="trousers trousers-devel"
  TRUSTAGENT_TROUSERS_ZYPPER_PACKAGES="trousers trousers-devel"
  install_packages "trousers" "TRUSTAGENT_TROUSERS" > /dev/null 2>&1
}

# 4. Install the patched tpm-tools
# tpm 1.2
install_tpm_tools() {
if [ "$IS_RPM" != "true" ]; then
  TRUSTAGENT_TPMTOOLS_YUM_PACKAGES="tpm-tools"
fi
  TRUSTAGENT_TPMTOOLS_APT_PACKAGES="tpm-tools"
  TRUSTAGENT_TPMTOOLS_YAST_PACKAGES="tpm-tools"
  TRUSTAGENT_TPMTOOLS_ZYPPER_PACKAGES="tpm-tools"
  install_packages "tpm-tools" "TRUSTAGENT_TPMTOOLS" > /dev/null 2>&1
}
# tpm 2
install_tpm2_tools() {
if [ "$IS_RPM" != "true" ]; then
  TRUSTAGENT_TPMTOOLS_YUM_PACKAGES="tpm2-tools-3.0.*"
fi
  TRUSTAGENT_TPMTOOLS_APT_PACKAGES="tpm2-tools"
  TRUSTAGENT_TPMTOOLS_YAST_PACKAGES="tpm2-tools"
  TRUSTAGENT_TPMTOOLS_ZYPPER_PACKAGES="tpm2-tools"
  install_packages "tpm2-tools" "TRUSTAGENT_TPMTOOLS" > /dev/null 2>&1
  if [ $? -ne 0 ]; then echo_failure "Failed to install tpm2-tools version 3.0.* through package installer"; exit 1; fi
  if yum_detect; then
    # TODO: This can be removed once we upgrade tpm2-tools (https://github.com/tpm2-software/tpm2-abrmd/issues/408)
    udevadm control --reload-rules && sudo udevadm trigger
  fi
}
# tpm 1.2
install_patched_tpm_tools() {
  local PATCHED_TPMTOOLS_BIN=`ls -1 patched-*.bin | head -n 1`
  if [ -n "$PATCHED_TPMTOOLS_BIN" ]; then
    chmod +x $PATCHED_TPMTOOLS_BIN
    ./$PATCHED_TPMTOOLS_BIN
  fi
}

install_tboot() {
  ./tboot-linux-*.bin
}

if [[ "$SKIP_INSTALL_TBOOT" != "y" && "$SKIP_INSTALL_TBOOT" != "Y" && "$SKIP_INSTALL_TBOOT" != "yes" ]]; then
  install_tboot
  result=$?
fi
install_openssl

# tpm 2.0
install_tss2_tpmtools2() {
  #install tpm2-tss, tpm2-tools for tpm2
  # (do not install trousers and its dev packages for tpm 2.0)
#  ./mtwilson-trustagent-tpm2-packages-*.bin
  yum -y install tpm2-tools-3.0.*
  if [ $? -ne 0 ]; then echo_failure "Failed to install tpm2-tools version 3.0.* through package installer"; exit 1; fi
}

clean_existing_tpm2tools() {
 if [[ -e "/usr/local/sbin/tpm2_takeownership" ]]; then
   rm -rf /usr/local/sbin/tpm2_*
 fi
}

if [ ${DOCKER} != "true" ]; then
  if [ "$TPM_VERSION" == "1.2" ]; then
    install_trousers
    install_tpm_tools
    #install_patched_tpm_tools
  elif [ "$TPM_VERSION" == "2.0" ]; then
    clean_existing_tpm2tools
    install_tpm2_tools
    service tcsd2 stop >/dev/null 2>&1
    service tpm2-abrmd start >/dev/null 2>&1
  elif [ -z "$TPM_VERSION" ]; then
    echo "Cannot detect TPM version"
  else
    echo "Unrecognized TPM version: $TPM_VERSION"
  fi
else
  # for a Docker image, install tools for both
  install_trousers
  install_tpm_tools
  install_tss2_tpmtools2
  if [[ "$SKIP_INSTALL_TBOOT" != "y" && "$SKIP_INSTALL_TBOOT" != "Y" && "$SKIP_INSTALL_TBOOT" != "yes" ]]; then
    install_tboot
  fi
fi

# 5. Install java
# Trust Agent requires java 1.8 or later
echo "Installing Java..."
if [ "$IS_RPM" != "true" ]; then
  java_install_openjdk
fi

# 6. Install monit
monit_required_version=5.5

# detect the packages we have to install
MONIT_PACKAGE=`ls -1 monit-*.tar.gz 2>/dev/null | tail -n 1`

# SCRIPT EXECUTION
monit_clear() {
  #MONIT_HOME=""
  monit=""
}

monit_detect() {
  local monitrc=`ls -1 /etc/monitrc 2>/dev/null | tail -n 1`
  monit=`which monit 2>/dev/null`
}

monit_install() {
if [ "$IS_RPM" != "true" ]; then
  MONIT_YUM_PACKAGES="monit"
fi
  MONIT_APT_PACKAGES="monit"
  MONIT_YAST_PACKAGES=""
  MONIT_ZYPPER_PACKAGES="monit"
  install_packages "Monit" "MONIT"
  if [ $? -ne 0 ]; then echo_failure "Failed to install monit through package installer"; return 1; fi
  monit_clear; monit_detect;
    if [[ -z "$monit" ]]; then
      echo_failure "Unable to auto-install Monit"
      echo "  Monit download URL:"
      echo "  http://www.mmonit.com"
    else
      echo_success "Monit installed in $monit"
    fi
}

monit_src_install() {
  local MONIT_PACKAGE="${1:-monit-5.5-linux-src.tar.gz}"
  DEVELOPER_YUM_PACKAGES="make gcc" #openssl libssl-dev
  DEVELOPER_APT_PACKAGES="dpkg-dev make gcc" #openssl libssl-dev
  install_packages "Developer tools" "DEVELOPER"
  if [ $? -ne 0 ]; then echo_failure "Failed to install developer tools through package installer"; return 1; fi
  monit_clear; monit_detect;
  if [[ -z "$monit" ]]; then
    if [[ -z "$MONIT_PACKAGE" || ! -f "$MONIT_PACKAGE" ]]; then
      echo_failure "Missing Monit installer: $MONIT_PACKAGE"
      return 1
    fi
    local monitfile=$MONIT_PACKAGE
    echo "Installing $monitfile"
    is_targz=`echo $monitfile | grep ".tar.gz$"`
    is_tgz=`echo $monitfile | grep ".tgz$"`
    if [[ -n "$is_targz" || -n "$is_tgz" ]]; then
      gunzip -c $monitfile | tar xf -
    fi
    local monit_unpacked=`ls -1d monit-* 2>/dev/null`
    local monit_srcdir
    for f in $monit_unpacked
    do
      if [ -d "$f" ]; then
        monit_srcdir="$f"
      fi
    done
    if [[ -n "$monit_srcdir" && -d "$monit_srcdir" ]]; then
      echo "Compiling monit..."
      cd $monit_srcdir
      ./configure --without-pam --without-ssl 2>&1 >/dev/null
      make 2>&1 >/dev/null
      make install  2>&1 >/dev/null
    fi
    monit_clear; monit_detect
    if [[ -z "$monit" ]]; then
      echo_failure "Unable to auto-install Monit"
      echo "  Monit download URL:"
      echo "  http://www.mmonit.com"
    else
      echo_success "Monit installed in $monit"
    fi
  else
    echo "Monit is already installed"
  fi
}

if [ "$(whoami)" == "root" ] && [ ${DOCKER} != "true" ]; then
  monit_install $MONIT_PACKAGE
else
  echo_warning "Skipping monit installation"
fi

if [ -n $result ]; then exit $result; fi
