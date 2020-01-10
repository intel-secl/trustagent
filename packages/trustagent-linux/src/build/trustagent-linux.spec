# RPM SPEC for ISecL Trust Agent
%global ta_home       %{_datadir}/%{name}
%global ta_conf       %{_sysconfdir}/%{name}
%global ta_env_d      %{ta_conf}/env.d
%global ta_profile_d  %{ta_conf}/profile.d
%global ta_var        %{ta_home}/var
%global ta_tmp        %{ta_var}/tmp
%global ta_java       %{_javadir}/%{name}
%global ta_log        %{_localstatedir}/log/%{name}
%global ta_tbootxm    /opt/tbootxm
%global ta_username   tagent

%define debug_package %{nil}

#Trust Agent Log Rotation Defaults
%define ta_log_rotation_period  monthly
%define ta_log_compress         compress
%define ta_log_delaycompress    delaycompress
%define ta_log_copytruncate     copytruncate
%define ta_log_size             1G
%define ta_log_old              12

#Trust Agent RPM Metadata
Name:      trustagent
Version:   4.5
Release:   1%{?dist}
Summary:   RPM Package for ISecL Trust Agent Component
Group:     Applications/System			
License:   BSD-3-Clause
URL:       https://github.com/intel-secl/trustagent.git
Source0:   trustagent-rpm.tar.gz
Source1:   common-libs-ta-rpm.tar.gz
Source2:   functions
Source3:   version
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
BuildArch: x86_64
ExclusiveArch:  x86_64

#Dependencies to Build Trust Agent RPM from Sources 
%if 0%{?centos}
BuildRequires: rh-maven35
BuildRequires: epel-release
%endif

%if 0%{?rhel}
BuildRequires: maven
BuildRequires: epel-release
%endif

%if 0%{?fedora}
BuildRequires: maven
%endif

BuildRequires:     wget
BuildRequires:     git
BuildRequires:     zip
BuildRequires:     unzip
BuildRequires:     ant
BuildRequires:     gcc
BuildRequires:     patch
BuildRequires:     gcc-c++
BuildRequires:     trousers-devel
BuildRequires:     openssl-devel
#BuildRequires:     epel-release
BuildRequires:     makeself
BuildRequires:     rpm-build
BuildRequires:     deltarpm

#Trust Agent Runtime Dependencies
Requires(pre):    shadow-utils
Requires(postun): shadow-utils
Requires(post):   chkconfig
Requires(preun):  chkconfig
Requires(preun):  initscripts
Requires(postun): initscripts
Requires:         redhat-lsb
Requires:         redhat-lsb-core
Requires:         net-tools
Requires:         grub2-efi-x64-modules
Requires:         compat-openssl10
Requires:         dmidecode
#TODO: Cross-validate EPEL Dependencies & Fedora Guidelines on Adding Repos from RPM
#Requires:        epel-release-latest-7

#TODO: Remove Packaged h2x2bin
#Requires:        hex2bin
Requires:         openssl
Requires:         openssl-devel
Requires:         java-1.8.0-openjdk-devel

#Dependencies Specific to Applicaiton Agent
Requires:        zip
Requires:        unzip
Requires:        dos2unix
Requires:        perl
Requires:        vim-common
Requires:        cpuid
Requires:        msr-tools

# Java Component Dependencies not Packaged with Trust Agent
Requires:       apache-commons-beanutils = 1.9.3
Requires:       apache-commons-codec = 1.11
Requires:       apache-commons-collections = 3.2.2
Requires:       apache-commons-compress
Requires:       apache-commons-configuration = 1.10
Requires:       apache-commons-digester = 2.1
Requires:       apache-commons-exec = 1.3
Requires:       apache-commons-io
Requires:       apache-commons-lang = 2.6
Requires:       apache-commons-lang3 = 3.7
Requires:       apache-commons-logging
#Requires:       apache-commons-math3
Requires:       apache-commons-pool = 1.6
Requires:       apache-commons-validator
Requires:       google-gson = 2.8.2
Requires:       guava
Requires:       glassfish-hk2-api
Requires:       glassfish-hk2-locator
Requires:       glassfish-hk2-utils
#Requires:       apache-commons-httpclient 
#Requires:       apache-commons-httpcore
Requires:       jackson-annotations
Requires:       jackson-core
Requires:       jackson-databind
Requires:       jackson-dataformat-xml
Requires:       jackson-dataformat-yaml
#Requires:       jackson-jaxrs-base
Requires:       jackson-jaxrs-json-provider
Requires:       jackson-jaxrs-xml-provider
Requires:       jackson-module-jaxb-annotations
Requires:       javassist
#Requires:       glassfish-jaxb-core
#Requires:       glassfish-jaxb 
#Requires:       jdbi
#Requires:       jersey
#Requires:       jetty-http
#Requires:       jetty-io
#Requires:       jetty-security
#Requires:       jetty-server
#Requires:       jetty-servlet
#Requires:       jetty-util
#Requires:       jetty-webapp
#Requires:       jetty-xml
Requires:       jna
Requires:       joda-time = 2.9.9
#Requires:       jsr305
#Requires:       logback 
#Requires:       mvn(javax.mail:mailapi)
Requires:       mimepull = 1.9.6
#Requires:       objenesis
#Requires:       osgi-resource-locator
#Requires:       glassfish-servlet-api
Requires:       slf4j = 1.7.25
Requires:       snakeyaml
#Requires:       stringtemplate
Requires:       stax2-api
#Requires:       validation-api
#Requires:       woodstox-core = 5.0.3
#Requires:       xml-commons-apis
#Requires:       xmlpull-api
Requires:       xpp3-minimal
Requires:       xstream

#Trust Agent Provides
Provides:       %{name} = %{version}-%{release}
Provides:       mtwilson-configuration = %{version}
Provides:       mtwilson-core-setup = %{version}
Provides:       mtwilson-extensions-cache = %{version}
Provides:       mtwilson-http-security = %{version}
Provides:       mtwilson-http-servlets = %{version}
Provides:       mtwilson-launcher = %{version}
Provides:       mtwilson-launcher-api = %{version}
Provides:       mtwilson-localization = %{version}
Provides:       mtwilson-password-vault = %{version}
Provides:       mtwilson-privacyca-client-jaxrs2 = %{version}
Provides:       mtwilson-privacyca-model = %{version}
Provides:       mtwilson-privacyca-tpm-endorsement-client-jaxrs2 = %{version}
Provides:       mtwilson-privacyca-tpm-endorsement-model = %{version}
Provides:       mtwilson-repository-api = %{version}
Provides:       mtwilson-rpc-model = %{version}
Provides:       mtwilson-setup-ext = %{version}
Provides:       mtwilson-shiro-file = %{version}
Provides:       mtwilson-shiro-util = %{version}
Provides:       mtwilson-trustagent-attestation-client-jaxrs2 = %{version}
Provides:       mtwilson-trustagent-attestation-model = %{version}
Provides:       mtwilson-trustagent-configuration = %{version}
Provides:       mtwilson-trustagent-console = %{version}
Provides:       mtwilson-trustagent-model = %{version}
Provides:       mtwilson-trustagent-privacyca-niarl = %{version}
Provides:       mtwilson-trustagent-setup = %{version}
Provides:       mtwilson-trustagent-tpm-tools = %{version}
Provides:       mtwilson-trustagent-version = %{version}
Provides:       mtwilson-trustagent-vmquote-xml = %{version}
Provides:       mtwilson-trustagent-ws-v2 = %{version}
Provides:       mtwilson-util = %{version}
Provides:       mtwilson-util-authz-token = %{version}
Provides:       mtwilson-util-classpath = %{version}
Provides:       mtwilson-util-codec = %{version}
Provides:       mtwilson-util-collection = %{version}
Provides:       mtwilson-util-configuration = %{version}
Provides:       mtwilson-util-console = %{version}
Provides:       mtwilson-util-crypto = %{version}
Provides:       mtwilson-util-crypto-jca = %{version}
Provides:       mtwilson-util-crypto-key = %{version}
Provides:       mtwilson-util-crypto-password = %{version}
Provides:       mtwilson-util-exec = %{version}
Provides:       mtwilson-util-extensions = %{version}
Provides:       mtwilson-util-http = %{version}
Provides:       mtwilson-util-i18n = %{version}
Provides:       mtwilson-util-io = %{version}
Provides:       mtwilson-util-jackson = %{version}
Provides:       mtwilson-util-jaxrs2 = %{version}
Provides:       mtwilson-util-jaxrs2-client = %{version}
Provides:       mtwilson-util-jaxrs2-server = %{version}
Provides:       mtwilson-util-jersey2 = %{version}
Provides:       mtwilson-util-jetty9 = %{version}
Provides:       mtwilson-util-locale = %{version}
Provides:       mtwilson-util-net = %{version}
Provides:       mtwilson-util-patch = %{version}
Provides:       mtwilson-util-pem = %{version}
Provides:       mtwilson-util-performance = %{version}
Provides:       mtwilson-util-pipe = %{version}
Provides:       mtwilson-util-rfc822 = %{version}
Provides:       mtwilson-util-shiro = %{version}
Provides:       mtwilson-util-text-transform = %{version}
Provides:       mtwilson-util-tls-policy = %{version}
Provides:       mtwilson-util-tpm20 = %{version}
Provides:       mtwilson-util-tree = %{version}
Provides:       mtwilson-util-validation = %{version}
Provides:       mtwilson-util-xml = %{version}
Provides:       mtwilson-version = %{version}
Provides:       mtwilson-version-ws-v2 = %{version}
Provides:       mtwilson-webservice-util = %{version}

Provides:       lib-common = %{version}
Provides:       lib-flavor = %{version}
Provides:       lib-platform-info = %{version}
Provides:       lib-tpm-provider = %{version}

%description
Trust Agent is a ISecL component that resides on physical servers and enables both remote attestation and the extended chain of trust capabilities. It maintains ownership of the server's Trusted Platform Module, allowing secure attestation quotes to be sent to the Verification Service.

%define __jar_repack %{nil}

%prep
%setup -q -n trustagent
%setup -q -T -D -b 1 -n .

%build
#TODO: Build from Trust Agent Sources
declare -a TA_REPOSITORY_ORDER
TA_REPOSITORY_ORDER=(
external-artifacts
contrib
common-java
lib-workload-measurement
lib-common
lib-privacyca
lib-tpm-provider
lib-platform-info
lib-flavor
tboot-xm
privacyca
trustagent
)


ant_build_repos() {
  local start_repo=${2}
  local required_to_build=false
  cat /dev/null > ant.log
  echo "Running ant build on repositories (log file: ant.log)..."
  for repo in ${!TA_REPOSITORY_ORDER[@]}; do
    local repo_name=${TA_REPOSITORY_ORDER[${repo}]}
    if [ -n "${start_repo}" ] && ! $required_to_build && [ "${repo_name}" != "${start_repo}" ]; then
      echo "Skipping ant build in repository [${repo_name}]..."
      continue
    else
      required_to_build=true
    fi
    echo "Running ant build for repository [${repo_name}]..."
    (
    cd "${repo_name}"
    ant >> ../ant.log 2>&1
    )
    local return_code=$?
    if [ ${return_code} -ne 0 ]; then
      echo "ERROR: Issue while running build on repository [${repo_name}]"
      return ${return_code}
    fi
  done
}

ant_build_repos
if [ $? -ne 0 ]; then exit 10; fi

cp -r %{_topdir}/BUILD/trustagent/packages/trustagent-linux/target/trustagent-4.5-SNAPSHOT-rpm.tar.gz %{_topdir}/BUILD/.

rm -rf ${TA_REPOSITORY_ORDER[@]}

tar -xf trustagent-4.5-SNAPSHOT-rpm.tar.gz


%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/%{_sbindir}
mkdir -p %{buildroot}/%{ta_conf}
mkdir -p %{buildroot}/%{ta_env_d}
mkdir -p %{buildroot}/%{ta_profile_d}
mkdir -p %{buildroot}/%{ta_home}
mkdir -p %{buildroot}/%{ta_home}/cert
mkdir -p %{buildroot}/%{ta_home}/backup
mkdir -p %{buildroot}/%{ta_home}/scripts
mkdir -p %{buildroot}/%{ta_home}/hypertext
mkdir -p %{buildroot}/%{ta_home}/var
mkdir -p %{buildroot}/%{ta_var}/tmp
mkdir -p %{buildroot}/%{ta_var}/ramfs
mkdir -p %{buildroot}/%{_localstatedir}/log/%{name}
mkdir -p %{buildroot}/%{_javadir}/%{name}
mkdir -p %{buildroot}/%{_sysconfdir}/logrotate.d
mkdir -p %{buildroot}/%{ta_home}/share

#Trust Agent Essential Files
%define app_target trustagent-%{version}-SNAPSHOT
unzip -o trustagent-%{version}-SNAPSHOT/trustagent-%{version}-SNAPSHOT.zip
cp bin/*                              %{buildroot}/%{_sbindir} 
cp configuration/*                    %{buildroot}/%{ta_conf}
cp env.d/*                            %{buildroot}/%{ta_env_d}
cp feature.xml                        %{buildroot}/%{ta_conf}
cp -r hypertext/*                     %{buildroot}/%{ta_home}/hypertext
cp java/*                             %{buildroot}/%{_javadir}/%{name}
cp %{app_target}/functions            %{buildroot}/%{ta_home}/scripts/functions.sh
cp %{app_target}/version              %{buildroot}/%{ta_home}/scripts/version.sh
cp %{app_target}/setup_prereqs.sh     %{buildroot}/%{ta_home}/scripts/setup_prereqs.sh
cp %{app_target}/version              %{buildroot}/%{ta_conf}/trustagent-version
cp %{app_target}/openssl.sh           %{buildroot}/%{ta_home}/scripts/openssl.sh
cp %{app_target}/java.security        %{buildroot}/%{ta_home}/backup
cp %{app_target}/application-agent*.bin %{buildroot}/%{ta_var}/tmp
cp %{app_target}/manifest_tpm12.xml   %{buildroot}/%{ta_conf}
cp %{app_target}/manifest_tpm20.xml   %{buildroot}/%{ta_conf}
cp %{app_target}/manifest_wlagent.xml %{buildroot}/%{ta_conf}
cp -r share/*           %{buildroot}/%{ta_home}/share
touch %{buildroot}/%{ta_log}/trustagent.log

# Check for Trust Agent Version
if [ -f %SOURCE3 ]; then . %SOURCE3; else echo_warning "Missing file: version"; fi

%pre
%include %SOURCE2

# Load Trust Agent ENV if Already Present
if [ -d %{ta_env_d} ]; then
  TRUSTAGENT_ENV_FILES=$(ls -1 %{ta_env_d}/*)
  for env_file in $TRUSTAGENT_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# Check for Trust Agent Environment Configuration
if [ -f ~/trustagent.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/trustagent.env"
  . ~/trustagent.env
  env_file_exports=$(cat ~/trustagent.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
  exit 1
fi

# Detect TPM Version
detect_tpm_version

if [ $TPM_VERSION == "1.2" ]; then
  echo
  echo "Trust Agent: TPM1.2 is Not Supported in this release. TrustAgent could not be installed."
  echo
  exit 1
fi

# Validate Secure Boot and Tboot Installation
if is_suefi_enabled; then
  export SUEFI_ENABLED="true"
  echo_warning "As SUEFI feature is enabled, skipping tboot installation"
  export SKIP_INSTALL_TBOOT="y"
else
  if rpm -q tboot 2>&1 > /dev/null;then
   echo "Tboot package is installed, continuing installation"
  else
   echo "Tboot package (tboot-dist-1.9.7-x86_64) is required for Non-SUEFI setup. Halting installation."
   exit 1
  fi
fi

if [[ "$SKIP_INSTALL_TBOOT" != "y" && "$SKIP_INSTALL_TBOOT" != "Y" && "$SKIP_INSTALL_TBOOT" != "yes" ]]; then
is_uefi_boot() {
  if [ -d /sys/firmware/efi ]; then
    return 0
  else
    return 1
  fi
}

# Identify Grub Configuration for Supported OS Flavors
define_grub_file() {
  flavor=$(getFlavour)
  if is_uefi_boot; then
    case $flavor in
    "ubuntu" )
        DEFAULT_GRUB_FILE="boot/efi/EFI/ubuntu/grub.cfg"
        dir_name="ubuntu" ;;
    "rhel" )
        DEFAULT_GRUB_FILE="/boot/efi/EFI/redhat/grub.cfg"
        dir_name="redhat" ;;
    "fedora" )
        DEFAULT_GRUB_FILE="/boot/efi/EFI/fedora/grub.cfg"
        dir_name="fedora" ;;
    "centos" )
        DEFAULT_GRUB_FILE="/boot/efi/EFI/CENTOS/grub.cfg"
        dir_name="CENTOS" ;;
    esac
  else
    if [ -f "/boot/grub2/grub.cfg" ]; then
      DEFAULT_GRUB_FILE="/boot/grub2/grub.cfg"
    else
      DEFAULT_GRUB_FILE="/boot/grub/grub.cfg"
    fi
  fi
  GRUB_FILE=${GRUB_FILE:-$DEFAULT_GRUB_FILE}
}

#Update Grub for Tboot
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
  if [ -d /boot/efi/EFI/${dir_name} ]; then
    mkdir -p /boot/efi/EFI/${dir_name}/x86_64-efi
  fi
  if [ -f /usr/lib/grub/x86_64-efi/relocator.mod ] && [ -d /boot/efi/EFI/${dir_name}/x86_64-efi ]; then
    \cp /usr/lib/grub/x86_64-efi/relocator.mod /boot/efi/EFI/${dir_name}/x86_64-efi/
  fi
  if [ -f /usr/lib/grub/x86_64-efi/multiboot2.mod ] && [ -d /boot/efi/EFI/${dir_name}/x86_64-efi ]; then
    \cp /usr/lib/grub/x86_64-efi/multiboot2.mod /boot/efi/EFI/${dir_name}/x86_64-efi/
  fi

  if is_command_available grub2-mkconfig; then
    grub2-mkconfig -o $GRUB_FILE
  else
    update-grub
  fi
}

configure_grub

#Validate if Host is in Measured Launch Environment
is_measured_launch() {
  local mle=$(txt-stat | grep 'TXT measured launch: TRUE')
  if [ -n "$mle" ]; then
    return 0
  else
    return 1
  fi
}

is_txtstat_installed() {
  is_command_available txt-stat
}

is_reboot_required() {
  local should_reboot=no

  if is_txtstat_installed; then
    if ! is_measured_launch; then
      echo "Not in measured launch environment"
      should_reboot=yes
    else
      echo "Already in measured launch environment"
    fi
  fi

  if [ "$should_reboot" == "yes" ]; then
    echo "A reboot is required"
  fi
}

# Check if a Reboot is Required
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

is_reboot_required
rebootRequired=$?

if ! is_tpm_driver_loaded; then
  echo "TPM driver is not loaded, reboot required"
  exit 254
else
  echo "TPM driver is already loaded"
fi

fi
 
# Stop Trust Agent if Running
existing_tagent=`which tagent 2>/dev/null`
if [ -f "$existing_tagent" ]; then
  $existing_tagent stop
fi

# Create Trust Agent User
if ! getent passwd %{ta_username} >/dev/null; then \
    useradd -r -s /sbin/nologin \
   -c "Mt Wilson Trust Agent" %{ta_username}
    usermod --lock %{ta_username}
fi

%post
. %{ta_home}/scripts/functions.sh >/dev/null

#Setup Environment for Trust Agent
if [ -d %{ta_env_d} ]; then
  TRUSTAGENT_ENV_FILES=$(ls -1 %{ta_env_d}/*)
  for env_file in $TRUSTAGENT_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# Check for TA Environment Configuration
  . ~/trustagent.env
  env_file_exports=$(cat ~/trustagent.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi

if [ "${TRUSTAGENT_SETUP_PREREQS:-yes}" == "yes" ]; then
  # set TRUSTAGENT_REBOOT=no (in trustagent.env) if you want to ensure it doesn't reboot
  # set TRUSTAGENT_SETUP_PREREQS=no (in trustagent.env) if you want to skip this step
  chmod +x %{ta_home}/scripts/setup_prereqs.sh
 . %{ta_home}/scripts/setup_prereqs.sh
  spResult=$?
fi

# Store TA Directory Layout in ENV File
echo "# $(date)" > %{ta_env_d}/trustagent-layout
echo "TRUSTAGENT_HOME=%{ta_home}" >> %{ta_env_d}/trustagent-layout
echo "TRUSTAGENT_CONFIGURATION=%{ta_conf}" >> %{ta_env_d}/trustagent-layout
echo "TRUSTAGENT_JAVA=%{ta_java}" >> %{ta_env_d}/trustagent-layout
echo "TRUSTAGENT_BIN=%{_sbindir}" >> %{ta_env_d}/trustagent-layout
echo "TRUSTAGENT_LOGS=%{ta_log}" >> %{ta_env_d}/trustagent-layout
echo "TRUSTAGENT_TMP=%{ta_tmp}" >> %{ta_env_d}/trustagent-layout

# Export Trust Agent Directory Layout
export TRUSTAGENT_HOME=%{ta_home}
export TRUSTAGENT_CONFIGURATION=%{ta_conf}
export TRUSTAGENT_REPOSITORY=%{ta_var}/repository
export TRUSTAGENT_TMP=%{ta_tmp}
export TRUSTAGENT_LOGS=%{ta_log}
export TRUSTAGENT_VAR=%{ta_var}
export TRUSTAGENT_BIN=%{_sbindir}
export TRUSTAGENT_JAVA=%{ta_java}
export TRUSTAGENT_BACKUP=%{ta_home}/backup
export INSTALL_LOG_FILE=%{ta_log}/install.log
export TRUSTAGENT_USERNAME=%{ta_username}
export AUTOMATIC_REGISTRATION=${AUTOMATIC_REGISTRATION:-y}
export PROVISION_ATTESTATION=${PROVISION_ATTESTATION:-y}
export AUTOMATIC_PULL_MANIFEST=${AUTOMATIC_PULL_MANIFEST:-y}
export TRUSTAGENT_ADMIN_USERNAME=${TRUSTAGENT_ADMIN_USERNAME:-tagent-admin}
export REGISTER_TPM_PASSWORD=${REGISTER_TPM_PASSWORD:-y}
export TRUSTAGENT_LOGIN_REGISTER=${TRUSTAGENT_LOGIN_REGISTER:-true}
export LOG_ROTATION_PERIOD=${LOG_ROTATION_PERIOD:-%{ta_log_rotation_period}}
export LOG_COMPRESS=${LOG_COMPRESS:-%{ta_log_compress}}
export LOG_DELAYCOMPRESS=${LOG_DELAYCOMPRESS:-%{ta_log_delaycompress}}
export LOG_COPYTRUNCATE=${LOG_COPYTRUNCATE:-%{ta_log_copytruncate}}
export LOG_SIZE=${LOG_SIZE:-%{ta_log_size}}
export LOG_OLD=${LOG_OLD:-%{ta_log_old}}

# Set Location of PID File needed for trust Agent Service
TRUSTAGENT_PID_FILE=$TRUSTAGENT_HOME/trustagent.pid

#Setup Cron Job Parameters for Trust Agent Log Rotation
TRUSTAGENT_TMPCLN_INT=${TRUSTAGENT_TMPCLN_INT:-* 0 * * *}
TRUSTAGENT_TMPCLN_AGE=${TRUSTAGENT_TMPCLN_AGE:-7}

# Check if it is Docker Environment
if [[ ${container} == "docker" ]]; then
    DOCKER=true
else
    DOCKER=false
fi

# Store Trust Agent username in ENV File
echo "# $(date)" > %{ta_env_d}/trustagent-username
echo "TRUSTAGENT_USERNAME=%{ta_username}" >> %{ta_env_d}/trustagent-username

# Store Trust Agent Log Level in ENV File
if [ -n "$TRUSTAGENT_LOG_LEVEL" ]; then
  echo "# $(date)" > %{ta_env_d}/trustagent-logging
  echo "TRUSTAGENT_LOG_LEVEL=$TRUSTAGENT_LOG_LEVEL" >> %{ta_env_d}/trustagent-logging
fi

# Store Exported ENV Variable for User Switch, to be Deleted Later
echo "# $(date)" > %{ta_env_d}/trustagent-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "export $env_file_var_name='$env_file_var_value'" >> %{ta_env_d}/trustagent-setup
done

# Import Previous Configuraiton if Present
TRUSTAGENT_V_1_2_HOME=/opt/intel/cloudsecurity/trustagent
TRUSTAGENT_V_1_2_CONFIGURATION=/etc/intel/cloudsecurity
package_config_filename=${TRUSTAGENT_V_1_2_CONFIGURATION}/trustagent.properties
ASSET_TAG_SETUP="y"

# Store TPM Version for Non-Docker Setup
if [ -z "$TPM_VERSION" ]; then
  detect_tpm_version
fi

if [ "$DOCKER" != "true" ]; then
    echo -n "$TPM_VERSION" > %{ta_conf}/tpm-version
fi

# Copy VISH URL to ENV File
if [ -n "$LIBVIRT_DEFAULT_URI" ]; then
  echo "LIBVIRT_DEFAULT_URI=$LIBVIRT_DEFAULT_URI" > %{ta_env_d}/virsh
elif [ -n "$VIRSH_DEFAULT_CONNECT_URI" ]; then
  echo "VIRSH_DEFAULT_CONNECT_URI=$VIRSH_DEFAULT_CONNECT_URI" > %{ta_env_d}/virsh
fi

# Configure Logback for TA
if [ -f "%{ta_conf}/logback.xml" ]; then
  sed -e "s|<file>.*/trustagent.log</file>|<file>%{_localstatedir}/log/%{name}/trustagent.log</file>|" %{ta_conf}/logback.xml > %{ta_conf}/logback.xml.edited
  if [ $? -eq 0 ]; then
    mv %{ta_conf}/logback.xml.edited %{_sysconfdir}/%{name}/logback.xml
  fi
else
  echo_warning "Logback configuration not found: %{ta_conf}/logback.xml"
fi

# Configure Logrotate for TA
if [ ! -a %{_sysconfdir}/logrotate.d/trustagent ]; then
 echo "%{buildroot}/%{_localstatedir}/log/%{name}.log {
        missingok
        notifempty
        rotate %{ta_log_old}
        maxsize %{ta_log_size}
        nodateext
        %{ta_log_rotation_period}
        %{ta_log_compress}
        %{ta_log_delaycompress}
        %{ta_log_copytruncate}
}" > %{_sysconfdir}/logrotate.d/trustagent
fi

# Fix Existing AIK Cert
aikdir=%{ta_conf}/cert
if [ -f %{ta_home}/cert/aikcert.cer ]; then
  # trust agent aikcert.cer is in broken PEM format... it needs newlines every 76 characters to be correct
  cat %{ta_home}/cert/aikcert.cer | sed 's/.\{76\}/&\n/g' > %{ta_home}/cert/aikcert.pem
  rm %{ta_home}/cert/aikcert.cer
  if [ -f %{_sysconfdir}/%{name}/trustagent.properties ]; then
     # update aikcert.filename=aikcert.cer to aikcert.filename=aikcert.pem
     update_property_in_file aikcert.filename %{_sysconfdir}/%{name}/trustagent.properties aikcert.pem
  fi
fi

# Set Permissions for TA
for directory in $TRUSTAGENT_HOME $TRUSTAGENT_CONFIGURATION $TRUSTAGENT_ENV $TRUSTAGENT_REPOSITORY $TRUSTAGENT_VAR $TRUSTAGENT_LOGS $TRUSTAGENT_TMP; do
  echo "chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $directory" >>$INSTALL_LOG_FILE
  chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $directory 2>>$INSTALL_LOG_FILE
done

ln -s %{_sbindir}/tagent.sh %{_sbindir}/tagent

#Install Application Agent
if [[ "$(whoami)" == "root" && ${DOCKER} == "false" ]]; then
  if [ "$TBOOTXM_INSTALL" != "N" ] && [ "$TBOOTXM_INSTALL" != "No" ] && [ "$TBOOTXM_INSTALL" != "n" ] && [ "$TBOOTXM_INSTALL" != "no" ]; then
    echo "Installing application agent..."
    TBOOTXM_PACKAGE=`ls -1 %{ta_var}/tmp/application-agent*.bin 2>/dev/null | tail -n 1`
    if [ -z "$TBOOTXM_PACKAGE" ]; then
      echo_failure "Failed to find application agent installer package"
      exit -1
    fi
    ./$TBOOTXM_PACKAGE
    if [ $? -ne 0 ]; then echo_failure "Failed to install application agent"; exit -1; fi
    #Added execute permission for measure binary
    chmod o+x /opt/tbootxm
    chmod o+x /opt/tbootxm/bin/
    chmod o+x /opt/tbootxm/lib/
    chmod o+x /opt/tbootxm/bin/measure
    chmod o+x /opt/tbootxm/lib/libwml.so
  fi

  #Copy default and workload software manifest
  if ! stat $TRUSTAGENT_VAR/manifest_* 1> /dev/null 2>&1; then
    UUID=$(uuidgen)
    if [ "$TPM_VERSION" == "1.2" ]; then
      cp %{ta_conf}/manifest_tpm12.xml $TRUSTAGENT_VAR/manifest_"$UUID".xml
    else
      cp %{ta_conf}/manifest_tpm20.xml $TRUSTAGENT_VAR/manifest_"$UUID".xml
    fi
    sed -i "s/Uuid=\"\"/Uuid=\"${UUID}\"/g" $TRUSTAGENT_VAR/manifest_"$UUID".xml
    UUID=$(uuidgen)
    cp %{ta_conf}/manifest_wlagent.xml $TRUSTAGENT_VAR/manifest_"$UUID".xml
    sed -i "s/Uuid=\"\"/Uuid=\"${UUID}\"/g" $TRUSTAGENT_VAR/manifest_"$UUID".xml
  fi
fi

# Migrate Old Data to New Locations (v1 - v3)  (should be rewritten in java)
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

#hex2bin=`which hex2bin 2>/dev/null`
#if [ -z "$hex2bin" ]; then
#  echo_failure "cannot find command: hex2bin"
#  exit 1
#fi

# Redefine Configuration Locations
package_config_filename=$TRUSTAGENT_CONFIGURATION/trustagent.properties

# Update TPM Devices Permissions
if [[ "$(whoami)" == "root" && $TPM_VERSION == "2.0" ]]; then
  # tpm devices can only be accessed by the trustagent user and group
  echo "KERNEL==\"tpmrm[0-9]*|tpm[0-9]*\", MODE=\"0660\", OWNER=\"$TRUSTAGENT_USERNAME\", GROUP=\"$TRUSTAGENT_USERNAME\"" > /lib/udev/rules.d/tpm-udev.rules
  /sbin/udevadm control --reload-rules
  /sbin/udevadm trigger --type=devices --action=change
else
  echo_warning "Skipping update tpm devices permissions"
fi

# Update System Information
if [[ "$(whoami)" == "root" && ${DOCKER} != "true" ]]; then
  echo "Updating system information"
  tagent update-system-info 2>/dev/null
else
  echo_warning "Skipping updating system information"
fi

# Setup Trust Agent Access for Logs
chown -R $TRUSTAGENT_USERNAME:$TRUSTAGENT_USERNAME $TRUSTAGENT_LOGS/

# Update the Extensions Cache File
tagent setup update-extensions-cache-file --force 2>/dev/null

# Create Trust Agent Version File
package_version_filename=%{ta_env_d}/trustagent-version
datestr=`date +%Y-%m-%d.%H%M`
touch $package_version_filename
chmod 600 $package_version_filename
chown %{ta_username}:%{ta_username} $package_version_filename
echo "# Installed Trust Agent on ${datestr}" > $package_version_filename
echo "TRUSTAGENT_VERSION=${VERSION}" >> $package_version_filename
echo "TRUSTAGENT_RELEASE=\"${BUILD}\"" >> $package_version_filename

# Register Trust Agent Service Startup
if [[ "$(whoami)" == "root" && ${DOCKER} == "false" ]]; then
  echo "Registering tagent in start up"
  register_startup_script %{_sbindir}/tagent tagent %{ta_home}/trustagent.pid 21 >>%{_localstatedir}/log/%{name}/install.log 2>&1
else
  echo_warning "Skipping startup script registration"
fi

# Configure TMP Clenup Cron for Trust Agent
configure_cron add "$TRUSTAGENT_TMPCLN_INT" "find "$TRUSTAGENT_TMP" -mtime +"$TRUSTAGENT_TMPCLN_AGE" -exec /bin/rm -- '{}' \;"

# Add sudoers for TA to execute txt-stat
if is_suefi_enabled; then
  export SUEFI_ENABLED="true"
  echo_warning "As sUEFI feature is enabled, skipping tboot installation"
  export SKIP_INSTALL_TBOOT="y"
fi

if [[ "$(whoami)" == "root" && ${DOCKER} != "true" && "$SKIP_INSTALL_TBOOT" != "y" && "$SKIP_INSTALL_TBOOT" != "Y" && "$SKIP_INSTALL_TBOOT" != "yes" ]]; then
  txtStat=$(which txt-stat 2>/dev/null)
  if [ -z "$txtStat" ]; then
    echo_failure "cannot find command: txt-stat (from tboot)"
    exit 1
  else
    echo -e "Cmnd_Alias PACKAGE_MANAGER = ${txtStat}\nDefaults:%{ta_username} "'!'"requiretty\n%{ta_username} ALL=(root) NOPASSWD: PACKAGE_MANAGER" > "/etc/sudoers.d/%{ta_username}"
    chmod 440 "/etc/sudoers.d/%{ta_username}"
  fi
fi

# Fix libcrypto for RHEL
fix_libcrypto() {
  #yum_detect; yast_detect; zypper_detect; rpm_detect; aptget_detect; dpkg_detect;
  local has_libcrypto=`find / -name libcrypto.so.1.0.0 2>/dev/null | head -1`
  local libdir=`dirname $has_libcrypto 2>/dev/null`
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

# Fix Existing AIK Cert
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
fix_existing_aikcert

# Check Java Installation and Update ENV File
JAVA_CMD=$(type -p java | xargs readlink -f)
JAVA_HOME=$(dirname $JAVA_CMD | xargs dirname | xargs dirname)
JAVA_REQUIRED_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')

echo "# $(date)" > %{ta_env_d}/trustagent-java
echo "export JAVA_HOME=$JAVA_HOME" >> %{ta_env_d}/trustagent-java
echo "export JAVA_CMD=$JAVA_CMD" >> %{ta_env_d}/trustagent-java
echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> %{ta_env_d}/trustagent-java

# Configure Java Security
if [ -f "${JAVA_HOME}/jre/lib/security/java.security" ]; then
  echo "Replacing java.security file, existing file will be backed up"
  backup_file "${JAVA_HOME}/jre/lib/security/java.security"
  cp %{ta_home}/backup/java.security "${JAVA_HOME}/jre/lib/security/java.security"
fi

#Update Trust Agent Command Path
hash -r tagent >/dev/null 2>&1

# Setup Trust Agent
tagent setup || true

# Start Trust Agent
tagent start

# Execute Trust Agent Attestation Provision if Enabled
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
    tagent provision-attestation || true
    if [ $? -ne 0 ]; then
        exit 1
    fi
fi

# TODO: when sequence of events are optimized
tagent restart

# Register Trust Agent Host with HVS
if [[( "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ) && ( "$AUTOMATIC_REGISTRATION" == "y" || "$AUTOMATIC_REGISTRATION" == "Y" || "$AUTOMATIC_REGISTRATION" == "yes" )]]; then
    tagent create-host || true
fi

if [[( "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ) && ( "$AUTOMATIC_PULL_MANIFEST" == "y" || "$AUTOMATIC_PULL_MANIFEST" == "Y" || "$AUTOMATIC_PULL_MANIFEST" == "yes" )]]; then
   if [ -z $FLAVOR_LABELS ] && [ -z $FLAVOR_UUIDS ]; then
    echo "Automatic Pull Manifest is enabled, but neither flavor UUID nor Flavor Label is set. Skipping manifest pull."
   else
    tagent get-configured-manifest || true
   fi
fi

if [[( "$PROVISION_ATTESTATION" == "y" || "$PROVISION_ATTESTATION" == "Y" || "$PROVISION_ATTESTATION" == "yes" ) && ( "$AUTOMATIC_FLAVOR_CREATION" == "y" || "$AUTOMATIC_FLAVOR_CREATION" == "Y" || "$AUTOMATIC_FLAVOR_CREATION" == "yes" )]]; then
    if [[ $ipResult -ne 255 ]]; then
        tagent create-host-unique-flavor || true
    else
        echo "Host not in measured launch environment. So skipping the automatic flavor creation step."
    fi
fi

%clean
rm -rf %{buildroot}

%files
%defattr(0755,%{ta_username},%{ta_username},0700)

%{ta_conf}
%attr(0755, %{ta_username},%{ta_username}) %{ta_conf}

%attr(0755,%{ta_username},%{ta_username}) %{_sbindir}/tpm-find-device.sh
%attr(0755,%{ta_username},%{ta_username}) %{_sbindir}/tpm-check-type.sh
%attr(0755,%{ta_username},%{ta_username}) %{_sbindir}/tagent.sh
%attr(0755,%{ta_username},%{ta_username}) %{_sbindir}/module_analysis.sh
%attr(0755,%{ta_username},%{ta_username}) %{_sbindir}/module_analysis_da_tcg.sh
%attr(0755,%{ta_username},%{ta_username}) %{_sbindir}/module_analysis_da.sh

%dir %{ta_home}
%attr(0755, %{ta_username},%{ta_username}) %{ta_home}

%{ta_java}
%attr(0755, %{ta_username},%{ta_username}) %{ta_java}
%attr(0644, %{ta_username},%{ta_username}) %{ta_java}/*

%{ta_home}/cert
%{ta_home}/backup
%{ta_home}/var
%{ta_home}/share
%{ta_var}/tmp
%{ta_var}/ramfs

%{ta_home}/hypertext
%attr(0700, %{ta_username},%{ta_username}) %{ta_home}/hypertext

%{ta_home}/scripts
%attr(0755, %{ta_username},%{ta_username}) %{ta_home}/scripts
%attr(0755, %{ta_username},%{ta_username}) %{ta_home}/scripts/setup_prereqs.sh
%attr(0700,%{ta_username},%{ta_username}) %{ta_home}/scripts/functions.sh
%attr(0700,%{ta_username},%{ta_username}) %{ta_home}/scripts/version.sh
%attr(0700,%{ta_username},%{ta_username}) %{ta_home}/scripts/openssl.sh

%attr(0600,%{ta_username},%{ta_username}) %{ta_log}/trustagent.log

%preun
#if [ $1 -eq 0 ] ; then
    # For systemd
    ## systemctl stop %{name}
    ## systemctl disable %{name}
#fi
#. %{ta_home}/scripts/functions.sh >/dev/null
#remove_startup_script tagent
%systemd_preun tagent.service
tagent uninstall-rpm
/opt/tbootxm/bin/tboot-xm-uninstall.sh

%postun
#if [ "$1" -ge "1" ] ; then
    # For systemd
    ## systemctl restart %{name}
#fi
# Cleanup Trust Agent Installation
if [ -d %{ta_conf} ]; then
   rm -rf %{ta_conf}
fi

if [ -d %{ta_home} ]; then
   rm -rf %{ta_home}
fi

if [ -d %{ta_java} ]; then
   rm -rf %{ta_java}
fi

if [ -d %{ta_log} ]; then
   rm -rf %{ta_log}
fi

if [ -d /opt/tbootxm ]; then
   rm -rf /opt/tbootxm
fi

if [ -L %{_sbindir}/tagent ]; then
   rm -rf %{_sbindir}/tagent
fi

#Remove Trust Agent Sudoers Entry
if [ -f %{_sysconfdir}/sudoers.d/%{ta_username} ]; then
   rm %{_sysconfdir}/sudoers.d/%{ta_username}
fi

if [ -L %{_sbindir}/tagent ]; then
   rm -rf %{_sbindir}/tagent
fi

#Remove Trust Agent 
if [ "$1" == 0 ]; then
   /usr/sbin/userdel --force %{ta_username} 2> /dev/null; true
fi

%changelog
* Thu Sep 12 2019 Uday K <uday.tejx.kommuri@intel.com>
- First release of Trust Agent RPM.
