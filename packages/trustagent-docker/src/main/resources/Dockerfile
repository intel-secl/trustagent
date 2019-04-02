#####
#Copyright © 2018 Intel Corporation
#SPDX-License-Identifier: BSD-3-Clause
#####

FROM registry.access.redhat.com/rhel7-init:7.5
ENV container docker

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
ENV JAVA_CMD $JAVA_HOME/bin/java
ENV JAVA_REQUIRED_VERSION 1.8.0_151

ENV TRUSTAGENT_USERNAME root
ENV SKIP_INSTALL_REDHAT_PACKAGES yes

# Copy yum repositories (If there is a non-redhat repo)
COPY yum.repos.d/ /etc/yum.repos.d/

RUN yum -y install which sudo redhat-lsb-core openssl unzip vim-common

# Copy the scripts
COPY trustagent-linux-*.bin /root/trustagent-linux.bin

RUN chmod +x /root/trustagent-linux.bin

# Copy the installer and extract just the static components 
WORKDIR /root/

RUN mkdir /root/prereqs

RUN export SKIP_INSTALL_TBOOT=yes && export TRUSTAGENT_SETUP_PREREQS=no && export TRUSTAGENT_NOSETUP=yes && ./trustagent-linux.bin --keep --target /root/tagent_installer &> /root/install.log
RUN cp /root/tagent_installer/setup_prereqs.sh /root/tagent_installer/install_prereq.sh /root/tagent_installer/functions /root/prereqs

# Remove system-info from /opt/trustagent/var. Need to recreate this for every instance
RUN rm -rf /opt/trustagent/var/system-info/
RUN rm -rf /opt/trustagent/configuration/.trustagent_password

# Remove the TrustAgent installer bin
RUN rm /root/trustagent-linux.bin

COPY start_isecl-agent /root/
RUN chmod +x /root/start_isecl-agent

RUN tagent stop
RUN rm -rf /root/tagent_installer

ENTRYPOINT [ "/root/start_isecl-agent" ]
