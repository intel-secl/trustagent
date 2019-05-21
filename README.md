# Intel<sup>®</sup> Security Libraries for Data Center  - Trust Agent
#### The `Trust Agent` resides on physical servers and enables both remote attestation and the extended chain of trust capabilities. The `Trust Agent` maintains ownership of the server's Trusted Platform Module, allowing secure attestation quotes to be sent to the Verification Service. 

## Key features
- Provides host specific information
- Provides secure attestation quotes
- RESTful APIs for easy and versatile access to above features

## System Requirements
- RHEL 7.5/7.6
- Epel 7 Repo
- Proxy settings if applicable

## Software requirements
- git
- maven (v3.3.1)
- ant (v1.9.10 or more)
- Visual Studio 2017 Professional Edition

# Step By Step Build Instructions
## Install required shell commands
Please make sure that you have the right `http proxy` settings if you are behind a proxy
```shell
export HTTP_PROXY=http://<proxy>:<port>
export HTTPS_PROXY=https://<proxy>:<port>
```
### Install tools from `yum`
```shell
$ sudo yum install -y wget git zip unzip ant gcc patch gcc-c++ trousers-devel openssl-devel makeself
```

## Direct dependencies
Following repositories needs to be build before building this repository,

| Name                       | Repo URL                                                 |
| -------------------------- | -------------------------------------------------------- |
| external-artifacts         | https://github.com/intel-secl/external-artifacts         |
| contrib                    | https://github.com/intel-secl/contrib                    |
| tpm-tools-windows          | https://github.com/intel-secl/tpm-tools-windows          |
| common-java                | https://github.com/intel-secl/common-java                |
| lib-common                 | https://github.com/intel-secl/lib-common                 |
| lib-privacyca              | https://github.com/intel-secl/lib-privacyca              |
| lib-tpm-provider           | https://github.com/intel-secl/lib-tpm-provider           |
| lib-platform-info          | https://github.com/intel-secl/lib-platform-info          |
| privacyca                  | https://github.com/intel-secl/privacyca                  |

## Build Trust Agent

- Git clone the `Trust Agent`
- Run scripts to build the `Trust Agent`

```shell
$ git clone https://github.com/intel-secl/trustagent.git
$ cd trustagent
$ ant
```

# Links
 - Use [Automated Build Steps](https://01.org/intel-secl/documentation/build-installation-scripts) to build all repositories in one go, this will also provide provision to install prerequisites and would handle order and version of dependent repositories.

***Note:** Automated script would install a specific version of the build tools, which might be different than the one you are currently using*
 - [Instructions to build hardened NSIS Installer on Windows](NSISInstaller.md)
 - [Product Documentation](https://01.org/intel-secl/documentation/intel%C2%AE-secl-dc-product-guide)
