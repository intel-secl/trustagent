#!/bin/bash

# Outline:
# 1. Install tboot
# 2. Add grub menu item for tboot and select as default
# 3. Ask for reboot (only if we are not already in trusted boot)

TRUSTAGENT_HOME=${TRUSTAGENT_HOME:-/opt/trustagent}
if [ -f functions ]; then . functions; else echo "Missing file: functions"; exit 1; fi

install_tboot() {
  if yum_detect; then
    TBOOT_RPM=`ls -1 tboot-*.rpm | head -n 1`
    echo "Installing ${TBOOT_RPM}..."
    if [ -n "$TBOOT_RPM" ]; then
      yum -y install ${TBOOT_RPM} >>/tmp/mtwilson-package-install.log 2>&1
    fi
  elif aptget_detect; then
    TBOOT_DEB=`ls -1 tboot-*.deb | head -n 1`
    echo "Installing ${TBOOT_DEB}..."
    if [ -n "$TBOOT_DEB" ]; then
      dpkg -i ${TBOOT_DEB}
      apt-get -y install -f >>/tmp/mtwilson-package-install.log 2>&1
    fi
  fi
  if [ $? -eq 0 ]; then return 255; fi
}

is_uefi_boot() {
  if [ -d /sys/firmware/efi ]; then
    return 0
  else
    return 1
  fi
}

define_grub_file() {
  if is_uefi_boot; then
    if [ -f "/boot/efi/EFI/redhat/grub.cfg" ]; then
      DEFAULT_GRUB_FILE="/boot/efi/EFI/redhat/grub.cfg"
    else
      DEFAULT_GRUB_FILE="/boot/efi/EFI/ubuntu/grub.cfg"
    fi
  else
    if [ -f "/boot/grub2/grub.cfg" ]; then
      DEFAULT_GRUB_FILE="/boot/grub2/grub.cfg"
    else
      DEFAULT_GRUB_FILE="/boot/grub/grub.cfg"
    fi
  fi
  GRUB_FILE=${GRUB_FILE:-$DEFAULT_GRUB_FILE}
}

update_tboot_grub_configuration_script() {
  local tbootGrubConfigScript="/etc/grub.d/05_linux_tboot"
  if [ -f "${tbootGrubConfigScript}" ]; then
    grubHasAssetTag=$(grep 'measure_nv=true' ${tbootGrubConfigScript})
    if [ -z "${grubHasAssetTag}" ]; then
      sed -i '/export TEXTDOMAIN=grub/i GRUB_CMDLINE_TBOOT="${GRUB_CMDLINE_TBOOT} measure_nv=true"' ${tbootGrubConfigScript}
    fi
    if [ "$TPM_VERSION" == "2.0" ]; then
      local grubHasSha256Bank=$(grep 'extpol=embedded' ${tbootGrubConfigScript})
      if [ -z "${grubHasSha256Bank}" ]; then
        sed -i 's|GRUB_CMDLINE_TBOOT="${GRUB_CMDLINE_TBOOT} measure_nv=true"|GRUB_CMDLINE_TBOOT="${GRUB_CMDLINE_TBOOT} measure_nv=true extpol=embedded"|g' ${tbootGrubConfigScript}
      fi
    fi
  fi
}

configure_grub() {
  define_grub_file
  
  # /etc/default/grub appears in both ubuntu and redhat
  if [ -f /etc/default/grub ]; then
    update_property_in_file GRUB_DEFAULT /etc/default/grub 0
  else
    echo "Cannot update grub default boot selection in /etc/default/grub"
  fi

  if [ -f /etc/grub.d/20_linux_tboot ]; then
    mv /etc/grub.d/20_linux_tboot /etc/grub.d/05_linux_tboot
  elif [ -f /etc/grub.d/05_linux_tboot ]; then
    echo "Already moved tboot menuentry to first position in /etc/grub.d"
  else
    echo "Cannot find tboot menuentry in /etc/grub.d"
  fi
  update_tboot_grub_configuration_script
  
  # copy grub2-efi-modules into the modules directory
  if [ -d /boot/efi/EFI/redhat ]; then
    mkdir -p /boot/efi/EFI/redhat/x86_64-efi
  fi
  if [ -f /usr/lib/grub/x86_64-efi/relocator.mod ] && [ -d /boot/efi/EFI/redhat/x86_64-efi ]; then
    \cp /usr/lib/grub/x86_64-efi/relocator.mod /boot/efi/EFI/redhat/x86_64-efi/
  fi
  if [ -f /usr/lib/grub/x86_64-efi/multiboot2.mod ] && [ -d /boot/efi/EFI/redhat/x86_64-efi ]; then
    \cp /usr/lib/grub/x86_64-efi/multiboot2.mod /boot/efi/EFI/redhat/x86_64-efi/
  fi
  
  if is_command_available grub2-mkconfig; then
    grub2-mkconfig -o $GRUB_FILE
  else
    update-grub
  fi
}

is_tpm_driver_loaded() {
  define_grub_file
  
  if [ ! -e /dev/tpm0 ]; then
    local is_tpm_tis_force=$(grep '^GRUB_CMDLINE_LINUX' /etc/default/grub | grep 'tpm_tis.force=1')
    local is_tpm_tis_force_any=$(grep '^GRUB_CMDLINE_LINUX' /etc/default/grub | grep 'tpm_tis.force')
    if [ -n "$is_tpm_tis_force" ]; then
      echo "TPM driver not loaded, tpm_tis.force=1 already in /etc/default/grub"
    elif [ -n "$is_tpm_tis_force_any" ]; then
      echo "TPM driver not loaded, tpm_tis.force present but disabled in /etc/default/grub"
    else
      #echo "TPM driver not loaded, adding tpm_tis.force=1 to /etc/default/grub"
      sed -i -e '/^GRUB_CMDLINE_LINUX/ s/"$/ tpm_tis.force=1"/' /etc/default/grub
      is_tpm_tis_force=$(grep '^GRUB_CMDLINE_LINUX' /etc/default/grub | grep 'tpm_tis.force=1')
      if [ -n "$is_tpm_tis_force" ]; then
        echo "TPM driver not loaded, added tpm_tis.force=1 to /etc/default/grub"
        grub2-mkconfig -o $GRUB_FILE
      else
        echo "TPM driver not loaded, failed to add tpm_tis.force=1 to /etc/default/grub"
      fi
    fi
    return 1
  fi
  return 0
}

is_txtstat_installed() {
  is_command_available txt-stat
}

is_measured_launch() {
  local mle=$(txt-stat | grep 'TXT measured launch: TRUE')
  if [ -n "$mle" ]; then
    return 0
  else
    return 1
  fi
}

is_reboot_required() {
  local should_reboot=no

  if is_txtstat_installed; then
    if ! is_measured_launch; then
      echo "Not in measured launch environment, reboot required"
      should_reboot=yes
    else
      echo "Already in measured launch environment"
    fi
  fi

  if [ "$should_reboot" == "yes" ]; then
    return 0
  else
    return 1
  fi
}

install_tboot_prereqs() {
  if yum_detect; then
    TBOOT_PREREQ_YUM_PACKAGES="grub2-efi-modules" #redhat-lsb libvirt net-tools redhat-lsb-core
    install_packages "tboot-prereq" "TBOOT_PREREQ"
    tpErrorCode=$?
    if [ $tpErrorCode -ne 0 ]; then
      echo_failure "grub2-efi-modules installation failed"
      exit $tpErrorCode
    fi
  fi
}

# install tboot pre-requisites: grub2-efi-modules
install_tboot_prereqs

# install tboot
install_tboot
tbootReboot=$?

# only configure grub if tboot was installed
if [[ $tbootReboot -eq 255 ]]; then
  configure_grub
fi

# run other checks to see if reboot is required
is_reboot_required
rebootRequired=$?

if ! is_tpm_driver_loaded; then
  echo "TPM driver is not loaded, reboot required"
  exit 254
else
  echo "TPM driver is already loaded"
fi

if [[ $tbootReboot -eq 255 ]] || [[ $rebootRequired -eq 0 ]]; then
    echo
    echo "A reboot is required."
    echo
    exit 255
fi
